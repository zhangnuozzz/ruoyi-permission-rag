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

// 新增人员资料及访问配置，服务端在同一事务内保存
export function createPersonnel(data) {
  return request({
    url: '/system/personnelAccess',
    method: 'post',
    data: data,
    returnErrorData: true
  })
}

// 更新人员资料及访问配置，服务端在同一事务内保存
export function updatePersonnel(userId, data) {
  return request({
    url: '/system/personnelAccess/save/' + userId,
    method: 'put',
    data: data,
    returnErrorData: true
  })
}
