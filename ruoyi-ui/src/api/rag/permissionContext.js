import request from '@/utils/request'

// 查询当前登录用户 RAG 权限上下文
export function getCurrentPermissionContext() {
  return request({
    url: '/rag/permission/context/current',
    method: 'get'
  })
}
