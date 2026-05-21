import Message from '@/utils/message'
import { ElMessageBox } from 'element-plus'
import { showImagePreview } from 'vant'
import 'vant/es/image-preview/style'

export type { ActionSheetAction } from 'vant'

class Toast {
  static success(message: string, options?: any) {
    return Message.success(message, options)
  }

  static fail(message: string, options?: any) {
    return Message.error(message, options)
  }

  static loading(message = '加载中...', forbidClick = true) {
    return Message.loading({ text: message, lock: forbidClick })
  }

  static info(message: string, options?: any) {
    return Message.info(message, options)
  }

  static close() {
    Message.closeAll()
  }
}

class Dialog {
  static alert(message: string, options?: any) {
    return ElMessageBox.alert(message, options?.title || '提示', {
      confirmButtonText: '确定',
      showCancelButton: false,
      ...options,
    })
  }

  static confirm(message: string, options?: any) {
    return Message.confirm(message, options?.title || '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      ...options,
    })
  }

  static deleteConfirm(message = '确定要删除吗？此操作不可恢复！') {
    return Message.deleteConfirm(message, '删除确认')
  }

  static success(message: string) {
    return ElMessageBox.alert(message, '提示', {
      confirmButtonText: '知道了',
      showCancelButton: false,
    })
  }

  static fail(message: string) {
    return ElMessageBox.alert(message, '提示', {
      confirmButtonText: '知道了',
      confirmButtonClass: 'el-button--danger',
      showCancelButton: false,
    })
  }

  static previewImage(imageUrl: string) {
    return showImagePreview({ images: [imageUrl], closeable: true })
  }

  static previewImages(imageUrls: string[], startPosition = 0) {
    return showImagePreview({ images: imageUrls, startPosition, closeable: true })
  }

  static close() {
    ElMessageBox.close()
  }
}

class Notify {
  static success(message: string, options?: any) {
    return Message.notifySuccess(message, options?.title || '成功')
  }

  static warning(message: string, options?: any) {
    return Message.notifyWarning(message, options?.title || '警告')
  }

  static danger(message: string, options?: any) {
    return Message.notifyError(message, options?.title || '错误')
  }

  static info(message: string, options?: any) {
    return Message.notifyInfo(message, options?.title || '提示')
  }

  static close() {
    Message.closeAll()
  }
}

class Loading {
  static show(message = '加载中...') {
    return Message.loading({ text: message })
  }

  static close() {
    Message.closeAll()
  }
}

export { Toast, Dialog, Notify, Loading }