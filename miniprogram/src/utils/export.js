// 导出 Excel — GET /approval/leaves/export?status= 返回 xlsx 文件流（需 Authorization）
// H5：fetch blob -> <a download>；小程序：uni.downloadFile -> uni.openDocument
import { BASE_URL } from './request'
import { getToken } from './auth'

export function exportLeavesFile(status = '') {
  const token = getToken()
  const qs = status ? `?status=${encodeURIComponent(status)}` : ''
  const url = BASE_URL + '/approval/leaves/export' + qs
  const header = token ? { Authorization: `Bearer ${token}` } : {}

  // #ifdef H5
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '导出中…' })
    fetch(url, { headers: header })
      .then(res => {
        if (!res.ok) throw new Error('导出失败 (' + res.status + ')')
        return res.blob()
      })
      .then(blob => {
        const a = document.createElement('a')
        const href = URL.createObjectURL(blob)
        a.href = href
        a.download = `请假记录_${Date.now()}.xlsx`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(href)
        uni.hideLoading()
        uni.showToast({ title: '已导出', icon: 'none' })
        resolve()
      })
      .catch(e => {
        uni.hideLoading()
        uni.showToast({ title: e.message || '导出失败', icon: 'none' })
        reject(e)
      })
  })
  // #endif

  // #ifndef H5
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '导出中…' })
    uni.downloadFile({
      url,
      header,
      success: res => {
        uni.hideLoading()
        if (res.statusCode !== 200) {
          uni.showToast({ title: '导出失败 (' + res.statusCode + ')', icon: 'none' })
          reject(new Error('export failed'))
          return
        }
        uni.openDocument({
          filePath: res.tempFilePath,
          fileType: 'xlsx',
          showMenu: true,
          success: () => resolve(),
          fail: () => {
            uni.showToast({ title: '已下载，无法直接预览', icon: 'none' })
            resolve()
          }
        })
      },
      fail: () => {
        uni.hideLoading()
        uni.showToast({ title: '导出失败，请检查网络', icon: 'none' })
        reject(new Error('download failed'))
      }
    })
  })
  // #endif
}
