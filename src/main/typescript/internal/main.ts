import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from '@/router/index.ts'
import { useUserStore } from '@/stores/user.ts'
import App from './App.vue'
import './style.css'

// 创建Vue应用实例
const app = createApp(App)

// 创建Pinia实例
const pinia = createPinia()

// 注册插件
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 初始化认证状态
const userStore = useUserStore()

// 在应用挂载前初始化用户认证状态
router.isReady().then(async () => {
  // 尝试从本地存储恢复用户登录状态
  await userStore.initAuth()
  
  // 挂载应用
  app.mount('#app')
})
