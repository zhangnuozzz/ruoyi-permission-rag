import request from '@/utils/request'

// 查询策略绑定管理列表
export function listPolicyBind(query) {
  return request({
    url: '/system/policyBind/list',
    method: 'get',
    params: query
  })
}

// 查询策略绑定管理详细
export function getPolicyBind(id) {
  return request({
    url: '/system/policyBind/' + id,
    method: 'get'
  })
}

// 新增策略绑定管理
export function addPolicyBind(data) {
  return request({
    url: '/system/policyBind',
    method: 'post',
    data: data
  })
}

// 修改策略绑定管理
export function updatePolicyBind(data) {
  return request({
    url: '/system/policyBind',
    method: 'put',
    data: data
  })
}

// 删除策略绑定管理
export function delPolicyBind(id) {
  return request({
    url: '/system/policyBind/' + id,
    method: 'delete'
  })
}
