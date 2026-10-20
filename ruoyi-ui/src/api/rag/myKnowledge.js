import request from '@/utils/request'
import { FILE_UPLOAD_TIMEOUT } from '@/utils/fileTransfer'

export function listMyKnowledge(params) {
  return request({ url: '/rag/file/mine/list', method: 'get', params })
}

export function listMyUploadGroups() {
  return request({ url: '/rag/file/mine/groups', method: 'get' })
}

export function uploadMyKnowledge(data) {
  return request({
    url: '/rag/file/mine/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: FILE_UPLOAD_TIMEOUT
  })
}

export function changeMyKnowledgeStatus(fileId, enabled) {
  return request({
    url: '/rag/file/mine/status',
    method: 'post',
    params: { fileId, enabled }
  })
}
