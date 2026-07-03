import request from '@/utils/request'

// 查询用户安全属性列表
export function listUserSecurityAttr(query) {
  return request({
    url: '/system/userSecurityAttr/list',
    method: 'get',
    params: query
  })
}

// 查询用户安全属性详细
export function getUserSecurityAttr(id) {
  return request({
    url: '/system/userSecurityAttr/' + id,
    method: 'get'
  })
}

// 新增用户安全属性
export function addUserSecurityAttr(data) {
  return request({
    url: '/system/userSecurityAttr',
    method: 'post',
    data: data
  })
}

// 修改用户安全属性
export function updateUserSecurityAttr(data) {
  return request({
    url: '/system/userSecurityAttr',
    method: 'put',
    data: data
  })
}

// 删除用户安全属性
export function delUserSecurityAttr(id) {
  return request({
    url: '/system/userSecurityAttr/' + id,
    method: 'delete'
  })
}

// 导出用户安全属性
export function exportUserSecurityAttr(query) {
  return request({
    url: '/system/userSecurityAttr/export',
    method: 'get',
    params: query
  })
}
