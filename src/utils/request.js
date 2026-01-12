import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const service = axios.create({
    baseURL: '/api',
    timeout: 30000 // 导出文件可能慢，延长超时时间
})

service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => Promise.reject(error)
)

service.interceptors.response.use(
    response => {
        const res = response.data

        // ★★★ 核心修复：如果是二进制文件(Blob)，直接放行，不进行 code 判断 ★★★
        if (response.config.responseType === 'blob') {
            return res
        }

        // 普通 JSON 请求的处理
        if (res.code !== 200) {
            // ★★★ 核心修改：如果配置了 hideError: true，就不弹窗 ★★★
            if (!response.config.hideError) {
                ElMessage.error(res.msg || '系统错误')
            }

            if (res.code === 401) {
                localStorage.clear()
                router.push('/login')
            }
            return Promise.reject(new Error(res.msg))
        }
        return res.data
    },
    error => {
        // ★★★ 核心修改：网络错误也支持静默，兼容 error.config 不存在的情况 ★★★
        if (!error.config || !error.config.hideError) {
            ElMessage.error(error.message || '网络请求失败')
        }
        return Promise.reject(error)
    }
)

export default service