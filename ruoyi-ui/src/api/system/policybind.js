import request from '@/utils/request'

// 查询策略绑定管理列表
export function listPolicybind(query) {
  return request({
    url: '/system/policybind/list',
    method: 'get',
    params: query
  })
}

// 查询策略绑定管理详细
export function getPolicybind(id) {
  return request({
    url: '/system/policybind/' + id,
    method: 'get'
  })
}

// 新增策略绑定管理
export function addPolicybind(data) {
  return request({
    url: '/system/policybind',
    method: 'post',
    data: data
  })
}

// 修改策略绑定管理
export function updatePolicybind(data) {
  return request({
    url: '/system/policybind',
    method: 'put',
    data: data
  })
}

// 删除策略绑定管理
export function delPolicybind(id) {
  return request({
    url: '/system/policybind/' + id,
    method: 'delete'
  })
}
