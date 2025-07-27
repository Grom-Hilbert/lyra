<script setup lang="ts">
import { onMounted } from 'vue'
import Layout from '@/components/Layout.vue'

onMounted(() => {
  // 初始化主题
  document.documentElement.classList.add('dark')
})
</script>

<template>
  <div id="app">
    <Layout>
      <router-view />
    </Layout>
  </div>
</template>


