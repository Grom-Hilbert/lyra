<template>
  <div class="relative w-full max-w-md">
    <!-- 搜索输入框 -->
    <div class="relative">
      <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        ref="searchInput"
        v-model="searchQuery"
        type="text"
        placeholder="搜索文件和文件夹..."
        class="pl-9 pr-10"
        @input="handleInput"
        @keydown="handleKeydown"
        @focus="showDropdown = true"
        @blur="handleBlur"
      />
      <Button
        v-if="searchQuery"
        variant="ghost"
        size="sm"
        class="absolute right-1 top-1/2 h-6 w-6 -translate-y-1/2 p-0"
        @click="clearSearch"
      >
        <X class="h-3 w-3" />
      </Button>
    </div>

    <!-- 下拉菜单 -->
    <div
      v-if="showDropdown && (suggestions.length > 0 || recentSearches.length > 0)"
      class="absolute top-full z-50 mt-1 w-full rounded-md border bg-popover p-1 text-popover-foreground shadow-md"
    >
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="mb-2">
        <div class="px-2 py-1 text-xs font-medium text-muted-foreground">
          搜索建议
        </div>
        <div
          v-for="(suggestion, index) in suggestions"
          :key="`suggestion-${index}`"
          class="flex cursor-pointer items-center rounded-sm px-2 py-1.5 text-sm hover:bg-accent hover:text-accent-foreground"
          :class="{ 'bg-accent text-accent-foreground': selectedIndex === index }"
          @click="selectSuggestion(suggestion.text)"
        >
          <Search class="mr-2 h-3 w-3" />
          <span class="flex-1">{{ suggestion.text }}</span>
          <Badge v-if="suggestion.count > 0" variant="secondary" class="ml-2 text-xs">
            {{ suggestion.count }}
          </Badge>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="recentSearches.length > 0">
        <div class="flex items-center justify-between px-2 py-1">
          <span class="text-xs font-medium text-muted-foreground">最近搜索</span>
          <Button
            variant="ghost"
            size="sm"
            class="h-auto p-0 text-xs text-muted-foreground hover:text-foreground"
            @click="clearHistory"
          >
            清空
          </Button>
        </div>
        <div
          v-for="(item, index) in recentSearches"
          :key="item.id"
          class="flex cursor-pointer items-center rounded-sm px-2 py-1.5 text-sm hover:bg-accent hover:text-accent-foreground"
          :class="{ 'bg-accent text-accent-foreground': selectedIndex === suggestions.length + index }"
          @click="selectHistory(item.keyword)"
        >
          <History class="mr-2 h-3 w-3" />
          <span class="flex-1">{{ item.keyword }}</span>
          <span class="text-xs text-muted-foreground">
            {{ item.resultCount }} 个结果
          </span>
          <Button
            variant="ghost"
            size="sm"
            class="ml-1 h-auto p-0 opacity-0 group-hover:opacity-100"
            @click.stop="removeHistory(item.id)"
          >
            <X class="h-3 w-3" />
          </Button>
        </div>
      </div>

      <!-- 无结果提示 -->
      <div
        v-if="suggestions.length === 0 && recentSearches.length === 0 && searchQuery"
        class="px-2 py-3 text-center text-sm text-muted-foreground"
      >
        暂无搜索建议
      </div>
    </div>

    <!-- 加载状态 -->
    <div
      v-if="loadingSuggestions"
      class="absolute right-3 top-1/2 -translate-y-1/2"
    >
      <div class="h-4 w-4 animate-spin rounded-full border-2 border-primary border-t-transparent"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, X, History } from 'lucide-vue-next'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useSearchStore } from '@/stores/search'
import { debounce } from 'lodash-es'

interface Props {
  placeholder?: string
  spaceId?: number
  autoFocus?: boolean
  showHistory?: boolean
  showSuggestions?: boolean
}

interface Emits {
  (e: 'search', query: string): void
  (e: 'clear'): void
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '搜索文件和文件夹...',
  showHistory: true,
  showSuggestions: true
})

const emit = defineEmits<Emits>()

const router = useRouter()
const searchStore = useSearchStore()

// 响应式数据
const searchInput = ref<HTMLInputElement>()
const searchQuery = ref('')
const showDropdown = ref(false)
const selectedIndex = ref(-1)

// 计算属性
const suggestions = computed(() => 
  props.showSuggestions ? searchStore.suggestions : []
)

const recentSearches = computed(() => 
  props.showHistory ? searchStore.recentSearches : []
)

const loadingSuggestions = computed(() => searchStore.loadingSuggestions)

const totalItems = computed(() => suggestions.value.length + recentSearches.value.length)

// 防抖的搜索建议获取
const debouncedGetSuggestions = debounce(async (query: string) => {
  if (query.trim() && props.showSuggestions) {
    await searchStore.getSuggestions(query, props.spaceId)
  }
}, 300)

// 方法
const handleInput = () => {
  selectedIndex.value = -1
  if (searchQuery.value.trim()) {
    debouncedGetSuggestions(searchQuery.value)
  } else {
    searchStore.suggestions = []
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!showDropdown.value) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, totalItems.value - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, -1)
      break
    case 'Enter':
      event.preventDefault()
      if (selectedIndex.value >= 0) {
        if (selectedIndex.value < suggestions.value.length) {
          selectSuggestion(suggestions.value[selectedIndex.value].text)
        } else {
          const historyIndex = selectedIndex.value - suggestions.value.length
          selectHistory(recentSearches.value[historyIndex].keyword)
        }
      } else if (searchQuery.value.trim()) {
        performSearch()
      }
      break
    case 'Escape':
      showDropdown.value = false
      selectedIndex.value = -1
      searchInput.value?.blur()
      break
  }
}

const handleBlur = () => {
  // 延迟隐藏下拉菜单，以便点击事件能够触发
  setTimeout(() => {
    showDropdown.value = false
    selectedIndex.value = -1
  }, 200)
}

const selectSuggestion = (text: string) => {
  searchQuery.value = text
  showDropdown.value = false
  selectedIndex.value = -1
  performSearch()
}

const selectHistory = (keyword: string) => {
  searchQuery.value = keyword
  showDropdown.value = false
  selectedIndex.value = -1
  performSearch()
}

const performSearch = () => {
  if (!searchQuery.value.trim()) return
  
  emit('search', searchQuery.value)
  
  // 导航到搜索结果页面
  router.push({
    name: 'Search',
    query: {
      q: searchQuery.value,
      spaceId: props.spaceId?.toString()
    }
  })
}

const clearSearch = () => {
  searchQuery.value = ''
  searchStore.clearResults()
  emit('clear')
  searchInput.value?.focus()
}

const clearHistory = () => {
  searchStore.clearHistory()
}

const removeHistory = (id: string) => {
  searchStore.removeFromHistory(id)
}

// 监听外部点击
const handleClickOutside = (event: Event) => {
  const target = event.target as Element
  if (!target.closest('.relative')) {
    showDropdown.value = false
    selectedIndex.value = -1
  }
}

// 生命周期
onMounted(() => {
  if (props.autoFocus) {
    nextTick(() => {
      searchInput.value?.focus()
    })
  }
  
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 监听搜索查询变化
watch(searchQuery, (newQuery) => {
  if (!newQuery.trim()) {
    searchStore.suggestions = []
  }
})

// 暴露方法给父组件
defineExpose({
  focus: () => searchInput.value?.focus(),
  blur: () => searchInput.value?.blur(),
  clear: clearSearch
})
</script>

<style scoped>
.group:hover .group-hover\:opacity-100 {
  opacity: 1;
}
</style>
