import request from '@/utils/request'

// 查询RAG检索审计日志列表
export function listLog(query) {
  return request({
    url: '/system/log/list',
    method: 'get',
    params: query
  })
}

// 查询RAG检索审计日志详细
export function getLog(id) {
  return request({
    url: '/system/log/' + id,
    method: 'get'
  })
}

// 新增RAG检索审计日志
export function addLog(data) {
  return request({
    url: '/system/log',
    method: 'post',
    data: data
  })
}

// 修改RAG检索审计日志
export function updateLog(data) {
  return request({
    url: '/system/log',
    method: 'put',
    data: data
  })
}

// 删除RAG检索审计日志
export function delLog(id) {
  return request({
    url: '/system/log/' + id,
    method: 'delete'
  })
}
