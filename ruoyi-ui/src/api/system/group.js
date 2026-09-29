import request from '@/utils/request'

// 查询用户组列表
export function listGroup(query) {
  return request({
    url: '/system/group/list',
    method: 'get',
    params: query
  })
}

// 查询用户组详细
export function getGroup(id) {
  return request({
    url: '/system/group/' + id,
    method: 'get'
  })
}

// 新增用户组
export function addGroup(data) {
  return request({
    url: '/system/group',
    method: 'post',
    data: data
  })
}

// 修改用户组
export function updateGroup(data) {
  return request({
    url: '/system/group',
    method: 'put',
    data: data
  })
}

// 删除用户组
export function delGroup(id) {
  return request({
    url: '/system/group/' + id,
    method: 'delete'
  })
}

// 查询各用户组成员数量
export function getGroupMemberCounts() {
  return request({
    url: '/system/group/member/counts',
    method: 'get'
  })
}

// 查询用户组成员
export function listGroupMembers(groupId) {
  return request({
    url: '/system/group/member/list/' + groupId,
    method: 'get'
  })
}

// 查询可加入用户
export function listGroupCandidates(groupId, keyword) {
  return request({
    url: '/system/group/member/candidates/' + groupId,
    method: 'get',
    params: {
      keyword: keyword
    }
  })
}

// 添加用户组成员
export function addGroupMember(data) {
  return request({
    url: '/system/group/member',
    method: 'post',
    data: data
  })
}

// 移除用户组成员
export function removeGroupMember(groupId, userId) {
  return request({
    url: '/system/group/member/' + groupId + '/' + userId,
    method: 'delete'
  })
}
