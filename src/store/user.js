import { defineStore } from 'pinia'
import { login } from '../api/auth'
import router from '../router'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: localStorage.getItem('token') || '',
        user: JSON.parse(localStorage.getItem('user') || '{}')
    }),
    actions: {
        async loginAction(loginForm) {
            // 调用后端接口
            const res = await login(loginForm)
            // 存状态
            this.token = res.token
            this.user = res.user
            // 存本地缓存 (防止刷新丢失)
            localStorage.setItem('token', res.token)
            localStorage.setItem('user', JSON.stringify(res.user))

            return res
        },
        logout() {
            this.token = ''
            this.user = {}
            localStorage.clear()
            router.push('/login')
        }
    }
})