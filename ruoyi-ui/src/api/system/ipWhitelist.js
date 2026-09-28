import request from '@/utils/request'

// 查询IP白名单列表
export function listIpWhitelist(query) {
  return request({
    url: '/system/ipWhitelist/list',
    method: 'get',
    params: query
  })
}

// 查询IP白名单详细
export function getIpWhitelist(whitelistId) {
  return request({
    url: '/system/ipWhitelist/' + whitelistId,
    method: 'get'
  })
}

// 新增IP白名单
export function addIpWhitelist(data) {
  return request({
    url: '/system/ipWhitelist',
    method: 'post',
    data: data
  })
}

// 修改IP白名单
export function updateIpWhitelist(data) {
  return request({
    url: '/system/ipWhitelist',
    method: 'put',
    data: data
  })
}

// 删除IP白名单
export function delIpWhitelist(whitelistId) {
  return request({
    url: '/system/ipWhitelist/' + whitelistId,
    method: 'delete'
  })
}
