import request from '@/utils/request'

// 查询 RAG 检索审计日志列表
export function listLog(query) {
  return request({
    url: '/system/log/list',
    method: 'get',
    params: query
  })
}

// 查询 RAG 检索审计日志详细
export function getLog(id) {
  return request({
    url: '/system/log/' + id,
    method: 'get'
  })
}

// 删除 RAG 检索审计日志
export function delLog(id) {
  return request({
    url: '/system/log/' + id,
    method: 'delete'
  })
}

// 导出 RAG 检索审计日志
export function exportLog(query) {
  return request({
    url: '/system/log/export',
    method: 'post',
    params: query
  })
}
