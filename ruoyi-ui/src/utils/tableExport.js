import request from '@/utils/request'
import { downloadExcel } from '@/utils/excelDownload'

/** 生成导出文件后，沿用平台按文件名下载的接口。 */
export async function exportTable(url, query) {
  const response = await request({
    url,
    method: 'post',
    params: { ...query },
    timeout: 60000
  })
  if (!response || response.code !== 200) {
    throw new Error((response && response.msg) || '导出结果未确认，请稍后重试。')
  }
  const fileName = response.msg
  if (typeof fileName !== 'string' || !/^[^/\\\x00-\x1f?:#%&=]+\.xlsx$/i.test(fileName)) {
    throw new Error('导出接口未返回有效文件名，请联系管理员。')
  }
  return downloadExcel(fileName)
}
