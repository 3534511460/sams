/**
 * 格式化日期时间字符串
 * @param {String} timeStr 后端返回的时间字符串
 * @param {String} pattern 格式 (暂未实现复杂格式，仅做基础处理)
 * @returns {String} 2025-12-30 12:00:00
 */
export function formatDate(timeStr) {
    if (!timeStr) return '-'
    // 简单粗暴处理：把 T 换成 空格，并截取前19位 (去掉毫秒)
    return timeStr.replace('T', ' ').substring(0, 19)
}

/**
 * 获取当前时间
 */
export function getNow() {
    const now = new Date()
    return now.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}