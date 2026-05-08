<template>
  <div class="product-detail-container">
    <!-- 加载状态：数据加载中显示 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 商品详情内容：商品数据存在时显示 -->
    <div v-else-if="product" class="product-detail-content">
      <!-- ========== 面包屑导航 ========== -->
      <div class="breadcrumb">
        <RouterLink to="/">首页</RouterLink>
        <i class="fas fa-chevron-right"></i>
        <span class="active">{{ product.categoryName || '商品详情' }}</span>
      </div>

      <!-- ========== 商品主体区域 ========== -->
      <div class="product-main">
        <!-- ========== 左侧图片区域 ========== -->
        <div class="product-gallery">
          <div class="main-image" @mousemove="handleMouseMove" @mouseenter="showZoom = true" @mouseleave="showZoom = false">
            <img :src="currentImage || (product.images && product.images[0]) || '/images/default-product.jpg'" :alt="product.name">
            <div class="image-zoom" v-if="showZoom" :style="{ backgroundImage: `url(${currentImage})`, backgroundPosition: zoomPosition }"></div>
          </div>
          
          <div class="thumbnail-list" v-if="product.images && product.images.length > 1">
            <div 
              v-for="(image, index) in product.images" 
              :key="index"
              class="thumbnail-item"
              :class="{ active: currentImage === image }"
              @click="currentImage = image"
            >
              <img :src="image" :alt="`商品图${index + 1}`">
            </div>
          </div>
        </div>

        <!-- ========== 右侧信息区域 ========== -->
        <div class="product-info">
          <h1 class="product-title">{{ product.name }}</h1>
          
          <div class="product-brand" v-if="product.brand">
            <span class="label">品牌：</span>
            <span>{{ product.brand }}</span>
          </div>

          <div class="product-price-section">
            <div class="price-wrapper">
              <span class="label">价格：</span>
              <span class="current-price">¥{{ formatPrice(product.price) }}</span>
              <span v-if="product.originalPrice" class="original-price">
                原价 ¥{{ formatPrice(product.originalPrice) }}
              </span>
              <span v-if="product.originalPrice" class="discount">
                {{ Math.round((1 - product.price / product.originalPrice) * 100) }}% off
              </span>
            </div>
          </div>

          <div class="product-stats">
            <div class="stat-item">
              <span class="label">销量：</span>
              <span class="value">{{ product.salesCount || 0 }}件</span>
            </div>
            <div class="stat-item">
              <span class="label">库存：</span>
              <span class="value" :class="{ 'stock-warning': product.stock < 10 }">
                {{ product.stock || 0 }}件
              </span>
            </div>
            <div class="stat-item" v-if="product.categoryName">
              <span class="label">分类：</span>
              <span class="value">{{ product.categoryName }}</span>
            </div>
          </div>

          <div class="product-description" v-if="product.description">
            <div class="label">商品描述：</div>
            <p>{{ product.description }}</p>
          </div>

          <div v-if="product.status === 0" class="offline-banner">
            <i class="fas fa-eye-slash"></i>
            <span>该商品已下架，无法购买</span>
          </div>

          <div class="product-actions">
            <div class="quantity-selector">
              <span class="label">数量：</span>
              <div class="quantity-control">
                <button class="quantity-btn" @click="decreaseQuantity" :disabled="quantity <= 1 || product.status === 0 || product.stock <= 0">
                  <i class="fas fa-minus"></i>
                </button>
                <input 
                  type="number" 
                  v-model.number="quantity" 
                  class="quantity-input"
                  min="1"
                  :max="product.stock"
                  :disabled="product.status === 0 || product.stock <= 0"
                  @change="validateQuantity"
                >
                <button class="quantity-btn" @click="increaseQuantity" :disabled="quantity >= product.stock || product.status === 0 || product.stock <= 0">
                  <i class="fas fa-plus"></i>
                </button>
              </div>
              <span class="unit">件</span>
            </div>

            <div v-if="product.stock <= 0 && product.status === 1" class="out-of-stock-tip">
              <i class="fas fa-exclamation-circle"></i>
              <span>商品缺货，暂时无法购买</span>
            </div>

            <div class="action-buttons">
              <button class="action-btn cart-btn" @click="addToCart" :disabled="product.status === 0 || product.stock <= 0">
                <i class="fas fa-shopping-cart"></i>
                <span>{{ product.stock <= 0 ? '缺货' : '加入购物车' }}</span>
              </button>
              
              <button class="action-btn buy-btn" @click="buyNow" :disabled="product.status === 0 || product.stock <= 0">
                <i class="fas fa-bolt"></i>
                <span>{{ product.stock <= 0 ? '缺货' : '立即购买' }}</span>
              </button>
            </div>

            <div class="action-links">
              <button class="link-btn" @click="toggleFavorite" :class="{ active: isFavorited }">
                <i :class="isFavorited ? 'fas fa-heart' : 'far fa-heart'"></i>
                <span>{{ isFavorited ? '已收藏' : '收藏商品' }}</span>
              </button>
              <button class="link-btn" @click="shareProduct">
                <i class="fas fa-share-alt"></i>
                <span>分享</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 商品详情选项卡 ========== -->
      <div class="product-tabs">
        <div class="tab-header">
          <button class="tab-btn" :class="{ active: activeTab === 'detail' }" @click="switchTab('detail')">
            商品详情
          </button>
          <button class="tab-btn" :class="{ active: activeTab === 'spec' }" @click="switchTab('spec')" v-if="product.specifications">
            规格参数
          </button>
          <button class="tab-btn" :class="{ active: activeTab === 'comment' }" @click="switchTab('comment')">
            用户评价
          </button>
        </div>
        
        <div class="tab-content">
          <div v-show="activeTab === 'detail'" class="tab-pane">
            <div class="detail-content" v-html="product.detailHtml || product.description"></div>
          </div>
          
          <div v-show="activeTab === 'spec'" class="tab-pane" v-if="product.specifications">
            <table class="spec-table">
              <tr v-for="(spec, key) in product.specifications" :key="key">
                <td class="spec-key">{{ key }}</td>
                <td class="spec-value">{{ spec }}</td>
              </tr>
            </table>
          </div>
          
          <!-- 用户评价面板（滚动加载） -->
          <div v-show="activeTab === 'comment'" class="tab-pane">
            <div class="comment-section">
              <!-- 评价统计区域 -->
              <div class="comment-stats">
                <div class="rating-score">
                  <span class="score">{{ product.avgRating || 5.0 }}</span>
                  <div class="stars">
                    <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= (product.avgRating || 5) }"></i>
                  </div>
                  <span class="count">共 {{ commentTotal }} 条评价</span>
                </div>
                
                <!-- 评分筛选按钮 -->
                <div class="rating-filter">
                  <span 
                    v-for="star in [5,4,3,2,1]" 
                    :key="star"
                    class="filter-btn"
                    :class="{ active: currentRating === star }"
                    @click="filterByRating(star)"
                  >
                    {{ star }}星
                  </span>
                  <span 
                    class="filter-btn"
                    :class="{ active: currentRating === null }"
                    @click="filterByRating(null)"
                  >
                    全部
                  </span>
                </div>
              </div>
              
              <!-- 评价列表（滚动容器） -->
              <div 
                class="comment-list" 
                ref="commentScrollContainer"
                @scroll="handleCommentScroll"
                v-if="comments.length > 0"
              >
                <div v-for="comment in comments" :key="comment.review?.id || comment.id" class="comment-item">
                  <img 
                    :src="comment.userProfile?.avatar || comment.avatar || '/images/default-avatar.jpg'" 
                    class="comment-avatar"
                  >
                  <div class="comment-content">
                    <div class="comment-user">{{ comment.userProfile?.nickname || comment.userName || '匿名用户' }}</div>
                    <div class="comment-rating">
                      <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= (comment.review?.rating || comment.rating) }"></i>
                    </div>
                    <div class="comment-text">{{ comment.review?.comment || comment.content }}</div>
                    
                    <!-- 评论图片区域 -->
                    <div v-if="comment.reviewImages && comment.reviewImages.length > 0" class="comment-images">
                      <img 
                        v-for="(img, imgIndex) in comment.reviewImages" 
                        :key="imgIndex"
                        :src="img.image" 
                        class="comment-image"
                        alt="评论图片"
                      >
                    </div>
                    
                    <div class="comment-time">{{ formatDate(comment.review?.createdAt || comment.createTime) }}</div>
                  </div>
                </div>
                
                <!-- 加载更多状态 -->
                <div v-if="commentLoading" class="loading-more">
                  <div class="loading-spinner-small"></div>
                  <span>加载中...</span>
                </div>
                
                <!-- 没有更多数据提示 -->
                <div v-if="!commentHasMore && comments.length > 0" class="no-more">
                  没有更多评价了
                </div>
              </div>
              
              <div v-else-if="!commentLoading" class="no-comment">
                暂无评价，快来成为第一个评价者吧！
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>商品不存在或已下架</p>
      <RouterLink to="/" class="back-home">返回首页</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  const props = defineProps<{ productId: string }>()
  const productId = computed(() => Number(props.productId))

  // ==================== 商品相关 ====================

  const loading = ref(true)
  const product = ref<any>(null)
  const currentImage = ref('')
  const quantity = ref(1)
  const isFavorited = ref(false)
  const activeTab = ref<'detail' | 'spec' | 'comment'>('detail')
  const showZoom = ref(false)
  const zoomPosition = ref('0% 0%')

  // ==================== 评价相关（滚动加载） ====================

  const comments = ref<any[]>([])
  const commentLoading = ref(false)
  const commentHasMore = ref(true)
  const commentPage = ref(1)
  const commentSize = ref(10)
  const commentTotal = ref(0)
  const currentRating = ref<number | null>(null)
  const commentScrollContainer = ref<HTMLElement | null>(null)

  // ==================== 商品详情加载 ====================

  /**
   * 加载商品详情
   * @returns {Promise<void>}
   */
  const loadProductDetail = async () => {
    loading.value = true
    try {
      const response = await authAPI.getProduct(productId.value)
      if (response.success && response.data) {
        product.value = response.data
        if (product.value.images) {
          if (typeof product.value.images === 'string') {
            product.value.images = product.value.images.split(',')
          }
          currentImage.value = product.value.images[0] || ''
        }
        // 初始化评价总数
        commentTotal.value = product.value.commentCount || 0
      }
    } catch (error: any) {
      showToast({ message: error.message || '加载商品失败', type: 'fail' })
    } finally {
      loading.value = false
    }
  }

  // ==================== 评价加载（滚动加载） ====================

  /**
   * 加载商品评价列表
   * @returns {Promise<void>}
   */
  const loadComments = async () => {
    if (commentLoading.value || !commentHasMore.value) return
    
    commentLoading.value = true
    
    try {
      const response = await authAPI.getProductReviews({
        productId: productId.value,
        page: commentPage.value,
        size: commentSize.value,
        rating: currentRating.value || undefined
      })
      
      if (response.success) {
        const newComments = response.data || []
        comments.value.push(...newComments)
        commentHasMore.value = response.hasMore || false
        commentTotal.value = response.total || commentTotal.value
        commentPage.value++
      }
    } catch (error: any) {
      console.error('加载评价失败:', error)
      showToast({ message: error.message || '加载评价失败', type: 'fail' })
    } finally {
      commentLoading.value = false
    }
  }

  /**
   * 重置评价列表
   */
  const resetComments = () => {
    comments.value = []
    commentPage.value = 1
    commentHasMore.value = true
    commentLoading.value = false
  }

  /**
   * 按评分筛选评价
   * @param {number|null} rating - 评分（1-5）或 null 表示全部
   */
  const filterByRating = (rating: number | null) => {
    currentRating.value = rating
    resetComments()
    loadComments()
  }

  /**
   * 处理滚动加载事件
   */
  const handleCommentScroll = () => {
    if (!commentScrollContainer.value) return
    
    const container = commentScrollContainer.value
    const isBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 100
    
    if (isBottom && commentHasMore.value && !commentLoading.value) {
      loadComments()
    }
  }

  /**
   * 切换选项卡
   * @param {string} tab - 选项卡名称
   */
  const switchTab = (tab: 'detail' | 'spec' | 'comment') => {
    activeTab.value = tab
    
    // 切换到评价选项卡时，如果评价列表为空，则加载
    if (tab === 'comment' && comments.value.length === 0 && commentHasMore.value) {
      loadComments()
    }
  }

  // ==================== 收藏相关 ====================

  const checkFavoriteStatus = async () => {
    try {
      const response = await authAPI.checkFavorite(productId.value)
      if (response.success) {
        isFavorited.value = response.data.isFavorited
      }
    } catch (error) {
      console.error('检查收藏状态失败:', error)
    }
  }

  const toggleFavorite = async () => {
    try {
      if (isFavorited.value) {
        const response = await authAPI.removeFavorite(productId.value)
        if (response.success) {
          isFavorited.value = false
          showToast({ message: '已取消收藏', type: 'success' })
        }
      } else {
        const response = await authAPI.addFavorite(productId.value)
        if (response.success) {
          isFavorited.value = true
          showToast({ message: '收藏成功', type: 'success' })
        }
      }
    } catch (error: any) {
      showToast({ message: error.message || '操作失败', type: 'fail' })
    }
  }

  // ==================== 数量操作 ====================

  const decreaseQuantity = () => {
    if (quantity.value > 1) quantity.value--
  }

  const increaseQuantity = () => {
    if (product.value && quantity.value < product.value.stock) quantity.value++
  }

  const validateQuantity = () => {
    if (quantity.value < 1) {
      quantity.value = 1
    } else if (product.value && quantity.value > product.value.stock) {
      quantity.value = product.value.stock
      showToast({ message: '库存不足', type: 'fail' })
    }
  }

  // ==================== 购物车和购买 ====================

  const addToCart = async () => {
    try {
      const response = await authAPI.addToCart({
        productId: product.value.id,
        quantity: quantity.value
      })
      if (response.success) {
        showToast({ message: '已添加到购物车', type: 'success' })
      }
    } catch (error: any) {
      showToast({ message: error.message || '添加失败', type: 'fail' })
    }
  }

  const buyNow = () => {
    router.push({
      name: 'Checkout',
      query: {
        source: 'product',
        productId: product.value.id,
        quantity: quantity.value
      }
    })
  }

  const shareProduct = () => {
    const url = `${window.location.origin}/products/${productId.value}`
    navigator.clipboard.writeText(url)
    showToast({ message: '链接已复制，快去分享吧', type: 'success' })
  }

  // ==================== 图片放大镜 ====================

  const handleMouseMove = (e: MouseEvent) => {
    const img = e.currentTarget as HTMLElement
    const rect = img.getBoundingClientRect()
    const x = ((e.clientX - rect.left) / rect.width) * 100
    const y = ((e.clientY - rect.top) / rect.height) * 100
    zoomPosition.value = `${x}% ${y}%`
  }

  // ==================== 工具函数 ====================

  const formatPrice = (price: number) => price.toFixed(2)

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }

  // ==================== 生命周期 ====================

  onMounted(async () => {
    if (!authStore.validateUserPermission()) return

    await loadProductDetail()
    await checkFavoriteStatus()
  })

  onUnmounted(() => {
    // 清理滚动容器引用
    commentScrollContainer.value = null
  })
</script>

<style scoped>
  @import url("@/static/css/user/商品详情.css");
</style>