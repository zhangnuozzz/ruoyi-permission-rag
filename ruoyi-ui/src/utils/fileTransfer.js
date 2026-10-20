export const FILE_UPLOAD_TIMEOUT = 120000
export const FILE_LIST_TIMEOUT = 20000

export function fileUploadFailureMessage(error) {
  const status = error && error.response && error.response.status
  if (status === 413) return '单个文件不能超过 10 MB，请重新选择文件。'
  const message = error && typeof error.message === 'string' ? error.message : ''
  if ((error && ['ECONNABORTED', 'ETIMEDOUT', 'ERR_NETWORK'].includes(error.code)) ||
      /timeout|Network Error|Request failed with status code/i.test(message)) {
    return '连接中断或等待超时，文件处理结果尚未确认；请先核对文件列表或联系管理员，避免重复上传。'
  }
  return message || '文件处理结果尚未确认，请先核对文件列表或联系管理员，避免重复上传。'
}
