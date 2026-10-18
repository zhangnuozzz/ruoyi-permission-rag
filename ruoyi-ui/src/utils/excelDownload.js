import axios from 'axios'

const activeDownloads = new Map()
const workbookType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

function validateFileName(fileName) {
  if (typeof fileName !== 'string' || !/^[^/\\\x00-\x1f?:#%&=]+\.xlsx$/i.test(fileName)) {
    throw new Error('导出接口未返回有效的 Excel 文件名，请重试。')
  }
}

function readBlob(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(new Error('无法读取导出文件，请重试。'))
    reader.onabort = () => reject(new Error('导出文件读取已中断，请重试。'))
    reader.readAsArrayBuffer(blob)
  })
}

/** 核对工作簿的 ZIP 目录，避免把错误响应或其他压缩包保存为 Excel。 */
function isWorkbook(buffer) {
  const view = new DataView(buffer)
  if (view.byteLength < 22 || view.getUint32(0, true) !== 0x04034b50) return false
  const lowerBound = Math.max(0, view.byteLength - 65557)
  let end = view.byteLength - 22
  while (end >= lowerBound) {
    if (view.getUint32(end, true) === 0x06054b50 && end + 22 + view.getUint16(end + 20, true) === view.byteLength) break
    end--
  }
  if (end < lowerBound || view.getUint16(end + 4, true) !== 0 || view.getUint16(end + 6, true) !== 0) return false
  const count = view.getUint16(end + 10, true)
  const size = view.getUint32(end + 12, true)
  let offset = view.getUint32(end + 16, true)
  if (!count || count === 0xffff || view.getUint16(end + 8, true) !== count || offset + size !== end) return false
  const names = new Set()
  for (let index = 0; index < count; index++) {
    if (offset + 46 > end || view.getUint32(offset, true) !== 0x02014b50) return false
    const nameLength = view.getUint16(offset + 28, true)
    const next = offset + 46 + nameLength + view.getUint16(offset + 30, true) + view.getUint16(offset + 32, true)
    if (next > end || (view.getUint16(offset + 8, true) & 1) !== 0) return false
    let name = ''
    for (let cursor = offset + 46; cursor < offset + 46 + nameLength; cursor++) name += String.fromCharCode(view.getUint8(cursor))
    names.add(name)
    offset = next
  }
  return offset === end && names.has('[Content_Types].xml') && names.has('xl/workbook.xml') &&
    Array.from(names).some(name => /^xl\/worksheets\/[^/]+\.xml$/.test(name))
}

function responseFileName(headers, fallback) {
  const disposition = (headers && headers['content-disposition']) || ''
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  const simple = /filename="([^"]+)"|filename=([^;]+)/i.exec(disposition)
  const value = encoded ? encoded[1] : simple ? (simple[1] || simple[2]).trim() : fallback
  try {
    const decoded = decodeURIComponent(value)
    validateFileName(decoded)
    return decoded
  } catch (error) {
    return fallback
  }
}

async function receiveWorkbook(fileName) {
  let response
  try {
    // 保持原有文件下载接口的认证方式；导出生成仍由各业务接口校验权限。
    response = await axios.get(process.env.VUE_APP_BASE_API + '/common/download', {
      params: { fileName, delete: true }, responseType: 'blob', timeout: 60000
    })
  } catch (error) {
    if (error && error.code === 'ECONNABORTED') throw new Error('Excel 下载超时，请稍后重试。')
    throw new Error('Excel 下载失败，请检查登录状态或网络后重试。')
  }
  const blob = response && response.data
  if (!(blob instanceof Blob) || !blob.size || !isWorkbook(await readBlob(blob))) {
    throw new Error('未收到有效的 Excel 工作簿，请重新导出；错误响应未保存为文件。')
  }
  const name = responseFileName(response.headers, fileName)
  const url = URL.createObjectURL(new Blob([blob], { type: workbookType }))
  const link = document.createElement('a')
  try {
    link.href = url
    link.download = name
    document.body.appendChild(link)
    link.click()
  } finally {
    if (link.parentNode) link.parentNode.removeChild(link)
    // 下载触发后延迟释放，避免浏览器尚未读取对象 URL。
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
  return { fileName: name, size: blob.size }
}

export function downloadExcel(fileName) {
  try {
    validateFileName(fileName)
  } catch (error) {
    return Promise.reject(error)
  }
  if (activeDownloads.has(fileName)) return activeDownloads.get(fileName)
  const pending = receiveWorkbook(fileName).finally(() => activeDownloads.delete(fileName))
  activeDownloads.set(fileName, pending)
  return pending
}
