import request from '@/utils/request'

// 上传文件到 RAG Server
export function uploadRagFile(data) {
  return request({
    url: '/rag/file/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 查询平台侧增强版 MariaDB 文件元数据
export function listRagFileMariadb() {
  return request({
    url: '/rag/file/platform/mariadb/list',
    method: 'get'
  })
}

// 查询可绑定用户组
export function listRagUploadGroups() {
  return request({
    url: '/rag/file/groups',
    method: 'get'
  })
}

// 查询 Milvus 切块内容
export function listRagFileMilvus(limit = 100) {
  return request({
    url: '/rag/file/milvus/list',
    method: 'get',
    params: { limit }
  })
}

// 查询 MinIO 原始文件对象
export function listRagFileMinio(limit = 100) {
  return request({
    url: '/rag/file/minio/list',
    method: 'get',
    params: { limit }
  })
}
