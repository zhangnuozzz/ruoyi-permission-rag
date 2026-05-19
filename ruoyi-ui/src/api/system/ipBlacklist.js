import request from '@/utils/request'

// 查询IP黑名单列表
export function listIpBlacklist(query) {
  return request({
    url: '/system/ipBlacklist/list',
    method: 'get',
    params: query
  })
}

// 查询IP黑名单详细
export function getIpBlacklist(blacklistId) {
  return request({
    url: '/system/ipBlacklist/' + blacklistId,
    method: 'get'
  })
}

// 新增IP黑名单
export function addIpBlacklist(data) {
  return request({
    url: '/system/ipBlacklist',
    method: 'post',
    data: data
  })
}

// 修改IP黑名单
export function updateIpBlacklist(data) {
  return request({
    url: '/system/ipBlacklist',
    method: 'put',
    data: data
  })
}

// 删除IP黑名单
export function delIpBlacklist(blacklistId) {
  return request({
    url: '/system/ipBlacklist/' + blacklistId,
    method: 'delete'
  })
}
