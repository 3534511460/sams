import request from '../utils/request'

// 获取所有部门
export function getDepartments() {
  return request.get('/department/list')
}

// 根据ID获取部门
export function getDepartmentById(id) {
  return request.get(`/department/${id}`)
}