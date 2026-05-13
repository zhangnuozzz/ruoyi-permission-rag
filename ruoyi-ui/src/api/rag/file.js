import request from '@/utils/request'

// RAG独立HTTP服务代理API
// author: fufu
// date: 2026-05-13 12:37:47 CST

export function uploadRagFile(data) {
  return request({
    url: '/rag/proxy/file/upload',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data',
      repeatSubmit: false
    },
    timeout: 120000,
    data: data
  })
}

export function listMilvusChunks(query) {
  return request({
    url: '/rag/proxy/file/milvus/list',
    method: 'get',
    params: query
  })
}

export function listMariadbFiles(query) {
  return request({
    url: '/rag/proxy/file/mariadb/list',
    method: 'get',
    params: query
  })
}

export function listMinioObjects(query) {
  return request({
    url: '/rag/proxy/file/minio/list',
    method: 'get',
    params: query
  })
}
