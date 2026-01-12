import request from '../utils/request'

export function applyLeave(data) {
    return request({
        url: '/leave/apply',
        method: 'post',
        data
    })
}

export function getLeaveList(params) {
    return request({
        url: '/leave/list',
        method: 'get',
        params // { userId, role }
    })
}

export function auditLeave(data) {
    return request({
        url: '/leave/audit',
        method: 'post',
        data // { id, status }
    })
}
export function cancelLeaveApply(data) {
    return request({
        url: '/leave/cancelApply',
        method: 'post',
        data // { id }
    })
}
export function getTodayLeaves() {
    return request({
        url: '/leave/today',
        method: 'get'
    })
}