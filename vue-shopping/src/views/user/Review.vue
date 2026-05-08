<template>
  <!-- 评价页面容器 -->
  <div class="review-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-edit"></i>
        发表评价
      </h1>
      <div class="breadcrumb">
        <RouterLink to="/">首页</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <RouterLink :to="{ name: 'UserOrders' }">我的订单</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="current">发表评价</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 评价表单 -->
    <div v-else class="review-form-card">
      <!-- 商品信息 -->
      <div class="product-info-section">
        <img :src="productInfo.image" class="product-image">
        <div class="product-detail">
          <div class="product-name">{{ productInfo.name }}</div>
          <div class="product-spec">数量：{{ productInfo.quantity }}</div>
          <div class="product-price">¥{{ productInfo.price }}</div>
        </div>
      </div>

      <!-- 评分区域 -->
      <div class="rating-section">
        <div class="rating-label">商品评分</div>
        <div class="rating-stars">
          <i 
            v-for="star in 5" 
            :key="star"
            class="star-icon"
            :class="{ 'fas fa-star active': star <= form.rating, 'far fa-star': star > form.rating }"
            @click="form.rating = star"
          ></i>
        </div>
        <div class="rating-tip">{{ getRatingText(form.rating) }}</div>
      </div>

      <!-- 评价内容 -->
      <div class="comment-section">
        <div class="comment-label">评价内容</div>
        <textarea 
          v-model="form.comment"
          class="comment-input"
          placeholder="分享使用感受，帮助更多买家选择（最多500字）"
          maxlength="500"
          rows="5"
        ></textarea>
        <div class="comment-count">{{ form.comment.length }}/500</div>
      </div>

      <!-- 图片上传 -->
      <div class="image-section">
        <div class="image-label">上传图片（最多9张）</div>
        <div class="image-list">
          <div 
            v-for="(img, index) in form.images" 
            :key="index"
            class="image-item"
          >
            <img :src="img.url" class="uploaded-image">
            <i class="fas fa-times-circle remove-icon" @click="removeImage(index)"></i>
          </div>
          <div 
            v-if="form.images.length < 9"
            class="upload-btn"
            @click="triggerFileInput"
          >
            <i class="fas fa-plus"></i>
            <span>添加图片</span>
          </div>
        </div>
        <input 
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          multiple
          class="hidden-input"
          @change="handleImageUpload"
        >
        <div class="image-tip">支持jpg、png格式，单张不超过5MB</div>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-section">
        <button class="btn-submit" :disabled="submitting" @click="submitReview">
          {{ submitting ? '提交中...' : '提交评价' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import { showToast, showConfirmDialog } from 'vant'
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const fileInputRef = ref(null)

const productInfo = ref({
  id: 0,
  name: '',
  image: '',
  price: '',
  quantity: 0
})

const form = ref({
  orderItemId: 0,
  rating: 5,
  comment: '',
  images: []
})

/**
 * 获取评分对应的文字描述
 * @param {number} rating - 评分值（1-5）
 * @returns {string} 评分文字描述
 */
const getRatingText = (rating) => {
  const texts = {
    1: '很差',
    2: '较差',
    3: '一般',
    4: '满意',
    5: '非常满意'
  }
  return texts[rating] || ''
}

/**
 * 触发文件选择器
 */
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

/**
 * 处理图片上传
 * @description 校验图片数量、大小、类型，生成预览URL
 * @param {Event} event - 文件上传事件
 */
const handleImageUpload = (event) => {
  const input = event.target
  const files = input.files
  
  if (!files) return
  
  // 最多9张图片限制
  if (form.value.images.length + files.length > 9) {
    showToast({ message: '最多只能上传9张图片', type: 'fail' })
    return
  }
  
  Array.from(files).forEach(file => {
    // 单张图片不超过5MB
    if (file.size > 5 * 1024 * 1024) {
      showToast({ message: `${file.name} 超过5MB限制`, type: 'fail' })
      return
    }
    
    if (!file.type.startsWith('image/')) {
      showToast({ message: `${file.name} 不是图片文件`, type: 'fail' })
      return
    }
    
    // 创建本地预览URL，避免上传前无法预览
    const url = URL.createObjectURL(file)
    form.value.images.push({ file, url })
  })

  // 清空input，允许重复上传同一文件
  input.value = ''
}

/**
 * 删除已选图片
 * @description 释放Blob URL避免内存泄漏
 * @param {number} index - 图片索引
 */
const removeImage = (index) => {
  URL.revokeObjectURL(form.value.images[index].url)
  form.value.images.splice(index, 1)
}

/**
 * 加载待评价订单项信息
 * @description 根据路由参数获取商品信息，用于评价页展示
 * @returns {Promise<void>}
 */
const loadOrderItemInfo = async () => {
  loading.value = true
  
  try {
    const orderItemId = Number(route.params.orderItemId)
    
    if (!orderItemId) {
      showToast({ message: '参数错误', type: 'fail' })
      router.back()
      return
    }
    
    form.value.orderItemId = orderItemId
    
    const response = await authAPI.getOrderItemDetail(orderItemId)
    
    if (response.success && response.data) {
      // 检查订单项是否已被评价
      if (response.data.isReviewed === true) {
        showToast({ 
          message: '该商品已完成评价，无法重复评价', 
          type: 'fail',
          duration: 2000
        })
        // 延迟回退，确保Toast可见
        setTimeout(() => {
          router.back()
        }, 2000)
        return
      }
      
      productInfo.value = {
        id: response.data.productId,
        name: response.data.productName,
        image: response.data.productImage,
        price: response.data.price,
        quantity: response.data.quantity
      }
    }
  } catch (error) {
    showToast({ message: error.message || '加载失败', type: 'fail' })
    // 加载失败也回退
    setTimeout(() => {
      router.back()
    }, 1500)
  } finally {
    loading.value = false
  }
}

/**
 * 提交评价
 * @description 校验评分和内容，使用FormData同时提交文本和图片
 * @returns {Promise<void>}
 */
const submitReview = async () => {
  if (!form.value.rating) {
    showToast({ message: '请选择评分', type: 'fail' })
    return
  }
  
  if (!form.value.comment.trim()) {
    showToast({ message: '请填写评价内容', type: 'fail' })
    return
  }
  
  // 使用 try-catch 捕获用户取消操作
  try {
    await showConfirmDialog({
      title: '提交评价',
      message: '确认提交评价吗？提交后无法修改',
      confirmButtonText: '确认提交',
      cancelButtonText: '再想想'
    })
  } catch (error) {
    return
  }
  
  submitting.value = true
  
  try {
    const formData = new FormData()
    
    const reviewData = {
      orderItemId: form.value.orderItemId,
      rating: form.value.rating,
      comment: form.value.comment
    }
    
    // 将JSON数据以Blob形式添加到FormData，避免嵌套对象丢失
    formData.append('reviewData', new Blob([JSON.stringify(reviewData)], {
      type: 'application/json'
    }))
    
    form.value.images.forEach(item => {
      formData.append('images', item.file)
    })
    
    const response = await authAPI.submitReview(formData)
    
    if (response.success) {
      showToast({ message: '评价成功', type: 'success' })
      
      // 延迟跳转，确保Toast可见
      setTimeout(() => {
        router.push({ name: 'UserOrders' })
      }, 1500)
    }
  } catch (error) {
    showToast({ message: error.message || '提交失败', type: 'fail' })
  } finally {
    submitting.value = false
  }
}

/**
 * 页面初始化
 * @description 校验登录权限后加载待评价订单信息
 */
onMounted(() => {
  if (!authStore.validateUserPermission()) return

  loadOrderItemInfo()
})
</script>

<style scoped>
 @import url('@/static/css/user/评论页.css');
</style>