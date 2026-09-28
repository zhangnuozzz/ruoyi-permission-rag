import request from '@/utils/request'

// RAG 安全检索测试
export function ragSearch(data) {
  return request({
    url: '/rag/search',
    method: 'post',
    data: data,
    returnErrorData: true
  })
}

// 查询当前登录用户的知识库权限摘要
export function getRagUserContext() {
  return request({
    url: '/rag/context',
    method: 'get'
  })
}
