import request from '@/utils/request'

export function passwordStatus() {
  return request({ url: '/account/password/status', method: 'get' })
}

export function changePassword(data) {
  return request({ url: '/account/password/change', method: 'post', data })
}
