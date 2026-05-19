import request from '@/utils/request'

// 查询访问监控日志列表
export function listAccessLog(query) {
  return request({
    url: '/system/accessLog/list',
    method: 'get',
    params: query
  })
}

// 查询访问监控日志详细
export function getAccessLog(accessId) {
  return request({
    url: '/system/accessLog/' + accessId,
    method: 'get'
  })
}

// 删除访问监控日志
export function delAccessLog(accessId) {
  return request({
    url: '/system/accessLog/' + accessId,
    method: 'delete'
  })
}

// 清空访问监控日志
export function cleanAccessLog() {
  return request({
    url: '/system/accessLog/clean',
    method: 'delete'
  })
}
