const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8080, // 前端端口
    proxy: {
      '/api': {
        target: 'http://localhost:9096', // 后端地址
        changeOrigin: true,
        pathRewrite: {
          '^/api': '' // 去掉 /api 前缀
        }
      }
    }
  }
})