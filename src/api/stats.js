import request from '../utils/request'

export function getStatsCounts() {
    return request({
        url: '/stats/counts',
        method: 'get'
    })
}

export function getCourseRank() {
    return request({
        url: '/stats/course-rank',
        method: 'get'
    })
}