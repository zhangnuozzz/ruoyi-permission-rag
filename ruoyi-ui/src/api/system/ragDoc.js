import request from '@/utils/request'

// 查询文档权限标签列表
export function listRagDoc(query) {
  return request({
    url: '/system/ragDoc/list',
    method: 'get',
    params: query
  })
}

// 查询文档权限标签详细
export function getRagDoc(id) {
  return request({
    url: '/system/ragDoc/' + id,
    method: 'get'
  })
}

// 新增文档权限标签
export function addRagDoc(data) {
  return request({
    url: '/system/ragDoc',
    method: 'post',
    data: data
  })
}

// 修改文档权限标签
export function updateRagDoc(data) {
  return request({
    url: '/system/ragDoc',
    method: 'put',
    data: data
  })
}

// 删除文档权限标签
export function delRagDoc(id) {
  return request({
    url: '/system/ragDoc/' + id,
    method: 'delete'
  })
}
