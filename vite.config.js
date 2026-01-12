import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    server: {
        port: 8080, // 设置前端启动端口为 8080
        open: true, // 启动后自动打开浏览器
        proxy: {
            // 配置跨域代理，解决前端(8080)访问后端(9096)的跨域问题
            '/api': {
                target: 'http://localhost:9096', // 后端接口地址
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/api/, '') // 去掉路径中的 /api
            }
        }
    }
})