import request from '@/utils/request'

// 查询人员安全属性和知悉用户组
export function getPersonnelAccess(userId) {
  return request({
    url: '/system/personnelAccess/' + userId,
    method: 'get'
  })
}

// 保存人员安全属性和知悉用户组
export function savePersonnelAccess(userId, data) {
  return request({
    url: '/system/personnelAccess/' + userId,
    method: 'put',
    data: data
  })
}
