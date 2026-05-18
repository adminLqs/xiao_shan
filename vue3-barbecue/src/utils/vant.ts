// @/utils/vant.ts
import {
  showToast,
  closeToast,
  showLoadingToast,
  showDialog,
  showConfirmDialog,
  showImagePreview,
  showNotify,
  closeNotify,
  closeDialog,
} from 'vant'

// 样式按需导入
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
import 'vant/es/image-preview/style'
import 'vant/es/notify/style'
import 'vant/es/action-sheet/style'

import type {
  ToastOptions,
  DialogOptions,
  NotifyOptions,
  ActionSheetAction,
} from 'vant'

// ==================== Toast 轻提示 ====================
class Toast {
  static success(message: string, options?: Partial<ToastOptions>) {
    return showToast({ type: 'success', message, duration: 2000, ...options })
  }

  static fail(message: string, options?: Partial<ToastOptions>) {
    return showToast({ type: 'fail', message, duration: 2000, ...options })
  }

  static loading(message = '加载中...', forbidClick = true) {
    return showToast({ type: 'loading', message, duration: 0, forbidClick })
  }

  static info(message: string, options?: Partial<ToastOptions>) {
    return showToast({ type: 'text', message, duration: 2000, ...options })
  }

  static close() {
    closeToast()
  }
}

// ==================== Dialog 对话框 ====================
class Dialog {
  static alert(message: string, options?: Partial<DialogOptions>) {
    return showDialog({
      message,
      confirmButtonText: '确定',
      showCancelButton: false,
      ...options,
    })
  }

  static confirm(message: string, options?: Partial<DialogOptions>) {
    return showConfirmDialog({
      message,
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      ...options,
    })
  }

  static deleteConfirm(message = '确定要删除吗？此操作不可恢复！') {
    return showConfirmDialog({
      message,
      confirmButtonText: '确定删除',
      confirmButtonColor: '#ee0a24',
    })
  }

  static success(message: string) {
    return showDialog({
      message,
      confirmButtonText: '知道了',
      showCancelButton: false,
    })
  }

  static fail(message: string) {
    return showDialog({
      message,
      confirmButtonText: '知道了',
      confirmButtonColor: '#ee0a24',
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
    closeDialog()
  }
}

// ==================== Notify 通知栏 ====================
class Notify {
  static success(message: string, options?: Partial<NotifyOptions>) {
    return showNotify({ type: 'success', message, duration: 2000, ...options })
  }

  static warning(message: string, options?: Partial<NotifyOptions>) {
    return showNotify({ type: 'warning', message, duration: 2000, ...options })
  }

  static danger(message: string, options?: Partial<NotifyOptions>) {
    return showNotify({ type: 'danger', message, duration: 2000, ...options })
  }

  static info(message: string, options?: Partial<NotifyOptions>) {
    return showNotify({ type: 'primary', message, duration: 2000, ...options })
  }

  static close() {
    closeNotify()
  }
}

// ==================== Loading 加载 ====================
class Loading {
  static show(message = '加载中...') {
    return showLoadingToast({
      message,
      forbidClick: true,
      duration: 0,
    })
  }

  static close() {
    closeToast()
  }
}

export { Toast, Dialog, Notify, Loading }