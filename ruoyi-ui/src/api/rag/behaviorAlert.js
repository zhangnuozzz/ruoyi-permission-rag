import request from '@/utils/request'

// 查询RAG行为分析告警列表
export function listBehaviorAlert(query) {
  return request({
    url: '/system/behaviorAlert/list',
    method: 'get',
    params: query
  })
}

// 触发行为分析
export function analyzeBehaviorAlert() {
  return request({
    url: '/system/behaviorAlert/analyze',
    method: 'post'
  })
}

// 删除RAG行为分析告警
export function delBehaviorAlert(id) {
  return request({
    url: '/system/behaviorAlert/' + id,
    method: 'delete'
  })
}

// 导出RAG行为分析告警
export function exportBehaviorAlert(query) {
  return request({
    url: '/system/behaviorAlert/export',
    method: 'get',
    params: query
  })
}
