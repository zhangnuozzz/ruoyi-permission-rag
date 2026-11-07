import request from '@/utils/request'

export function getSituationOverview() {
  return request({
    url: '/rag/dashboard/overview',
    method: 'get',
    headers: { 'X-Session-Touch': 'false' }
  })
}
