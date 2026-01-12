import request from '../utils/request'

// 获取系统全局统计信息（仅管理员）
export const getSystemStats = () => {
    return request({
        url: '/admin/system-stats',
        method: 'get'
    })
}