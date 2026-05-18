import request from '@/utils/request'

// 查询权限策略定义列表
export function listPolicy(query) {
  return request({
    url: '/system/policy/list',
    method: 'get',
    params: query
  })
}

// 查询权限策略定义详细
export function getPolicy(id) {
  return request({
    url: '/system/policy/' + id,
    method: 'get'
  })
}

// 新增权限策略定义
export function addPolicy(data) {
  return request({
    url: '/system/policy',
    method: 'post',
    data: data
  })
}

// 修改权限策略定义
export function updatePolicy(data) {
  return request({
    url: '/system/policy',
    method: 'put',
    data: data
  })
}

// 删除权限策略定义
export function delPolicy(id) {
  return request({
    url: '/system/policy/' + id,
    method: 'delete'
  })
}
