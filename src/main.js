import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './styles/main.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误处理：捕获并忽略特定的getComputedStyle和ResizeObserver错误
app.config.errorHandler = (err, instance, info) => {
  if (err.message && err.message.includes('getComputedStyle') && err.message.includes('parameter 1 is not of type \'Element\'')) {
    console.warn('Ignored Element Plus Select getComputedStyle error:', err)
    return // 忽略这个特定错误
  }
  
  if (err.message && err.message.includes('ResizeObserver loop completed with undelivered notifications')) {
    console.warn('Ignored ResizeObserver error:', err)
    return // 忽略这个特定错误
  }
  
  // 对于其他错误，正常处理
  console.error('Global error:', err)
  console.error('Vue component stack:', info)
}

app.mount('#app')