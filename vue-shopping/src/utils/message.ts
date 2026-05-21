// @/utils/message.ts
import { ElMessage, ElMessageBox, ElNotification, ElLoading } from 'element-plus'
import type { MessageOptions, ElMessageBoxOptions, NotificationOptions, LoadingInstance } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import 'element-plus/es/components/loading/style/css'

/**
 * Element Plus 消息提示统一管理工具
 */
class Message {

  // ==================== 基础消息 ====================
  /**
   * 成功消息
   * @param message 消息内容
   * @param options 其他配置
   */
  static success(message: string, options?: Partial<MessageOptions>) {
    return ElMessage({
      type: 'success',
      message,
      duration: 2000,
      showClose: true,
      ...options,
    })
  }

  /**
   * 警告消息
   * @param message 消息内容
   * @param options 其他配置
   */
  static warning(message: string, options?: Partial<MessageOptions>) {
    return ElMessage({
      type: 'warning',
      message,
      duration: 3000,
      showClose: true,
      ...options,
    })
  }

  /**
   * 错误消息
   * @param message 消息内容
   * @param options 其他配置
   */
  static error(message: string, options?: Partial<MessageOptions>) {
    return ElMessage({
      type: 'error',
      message,
      duration: 3000,
      showClose: true,
      ...options,
    })
  }

  /**
   * 普通信息消息
   * @param message 消息内容
   * @param options 其他配置
   */
  static info(message: string, options?: Partial<MessageOptions>) {
    return ElMessage({
      type: 'info',
      message,
      duration: 2000,
      showClose: true,
      ...options,
    })
  }

  // ==================== 对话框 ====================

  /**
   * 确认对话框
   * @param message 消息内容
   * @param title 标题
   * @param options 其他配置
   */
  static confirm(
    message: string,
    title = '提示',
    options?: Partial<ElMessageBoxOptions>
  ) {
    return ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      ...options,
    })
  }

  /**
   * 删除确认对话框（红色警告）
   * @param message 消息内容
   * @param title 标题
   * @param options 其他配置
   */
  static deleteConfirm(
    message = '确定要删除吗？此操作不可恢复！',
    title = '删除确认',
    options?: Partial<ElMessageBoxOptions>
  ) {
    return ElMessageBox.confirm(message, title, {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger',
      ...options,
    })
  }

  /**
   * 消息提示框（非模态）
   * @param message 消息内容
   * @param title 标题
   * @param options 其他配置
   */
  static alert(
    message: string,
    title = '提示',
    options?: Partial<ElMessageBoxOptions>
  ) {
    return ElMessageBox.alert(message, title, {
      confirmButtonText: '确定',
      type: 'info',
      ...options,
    })
  }

  /**
   * 输入对话框
   * @param message 消息内容
   * @param title 标题
   * @param options 其他配置
   */
  static prompt(
    message: string,
    title = '请输入',
    options?: Partial<ElMessageBoxOptions>
  ) {
    return ElMessageBox.prompt(message, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      ...options,
    })
  }

  // ==================== 通知 ====================

  /**
   * 通知（右上角弹出）
   * @param title 标题
   * @param message 消息内容
   * @param type 通知类型
   * @param options 其他配置
   */
  static notify(
    title: string,
    message: string,
    type: 'success' | 'warning' | 'info' | 'error' = 'info',
    options?: Partial<NotificationOptions>
  ) {
    return ElNotification({
      title,
      message,
      type,
      duration: 4500,
      ...options,
    })
  }

  /**
   * 成功通知
   * @param message 消息内容
   * @param title 标题
   */
  static notifySuccess(message: string, title = '成功') {
    return this.notify(title, message, 'success')
  }

  /**
   * 错误通知
   * @param message 消息内容
   * @param title 标题
   */
  static notifyError(message: string, title = '错误') {
    return this.notify(title, message, 'error', { duration: 0 })
  }

  /**
   * 警告通知
   * @param message 消息内容
   * @param title 标题
   */
  static notifyWarning(message: string, title = '警告') {
    return this.notify(title, message, 'warning')
  }

  /**
   * 信息通知
   * @param message 消息内容
   * @param title 标题
   */
  static notifyInfo(message: string, title = '提示') {
    return this.notify(title, message, 'info')
  }

  /**
   * Loading 加载
   * @param options 配置项
   */
  static loading(options?: {
    target?: string | HTMLElement
    text?: string
    background?: string
  }): LoadingInstance {
    return ElLoading.service({
      lock: true,
      text: options?.text || '加载中...',
      background: options?.background || 'rgba(0, 0, 0, 0.7)',
      target: options?.target || document.body,
    })
  }

  // ==================== 工具方法 ====================

  /**
   * 关闭所有消息
   */
  static closeAll() {
    ElMessage.closeAll()
    ElNotification.closeAll()
  }
}

export default Message