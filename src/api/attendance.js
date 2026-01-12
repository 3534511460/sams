import request from '../utils/request'

// 1. 老师：发布签到 (新增 targetClass)
export function publishCheckIn(data) {
    return request({
        url: '/attendance/publish',
        method: 'post',
        data
    })
}

// 2. 老师：结束签到 (新增)
export function stopCheckIn(data) {
    return request({
        url: '/attendance/stop',
        method: 'post',
        data // { sessionId }
    })
}

// 3. 学生：签到
export function studentCheckIn(data) {
    return request({
        url: '/attendance/checkIn',
        method: 'post',
        data
    })
}

// 4. 获取记录 (带上用户身份)
export function getRecordList(params) {
    return request({
        url: '/attendance/list',
        method: 'get',
        params
    })
}

// 5. 学生：获取当前正在进行的签到 (通知)
export function getActiveSessions(params) {
    return request({
        url: '/attendance/active',
        method: 'get',
        params // { className }
    })
}
export function updateRecord(data) {
    return request({
        url: '/attendance/update',
        method: 'post',
        data // { id, status, operatorId, operatorRole }
    })
}
// 7. 学生申诉
export function appealRecord(data) {
    return request({
        url: '/attendance/appeal',
        method: 'post',
        data // { recordId, reason }
    })
}

// 8. 审核申诉
export function auditAppeal(data) {
    return request({
        url: '/attendance/auditAppeal',
        method: 'post',
        data // { recordId, pass: 2或3 }
    })
}
// 9. 批量修改
export function batchUpdateRecord(data) {
    return request({
        url: '/attendance/batchUpdate',
        method: 'post',
        data // { ids: [], status: 1 }
    })
}