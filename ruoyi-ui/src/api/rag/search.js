import request from '@/utils/request'

// RAG 安全检索测试
export function ragSearch(data) {
  return request({
    url: '/rag/search',
    method: 'post',
    data: data
  })
}
