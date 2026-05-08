<template>
  <div class="product-management-container">

    <!-- 操作栏区域：包含发布按钮、批量删除按钮、搜索框和筛选下拉框 -->
    <div class="action-bar">
      <!-- 左侧操作按钮组 -->
      <div class="action-left">
        <!-- 发布商品按钮：跳转到商品发布页面 -->
        <RouterLink to="/seller/products/create" class="btn btn-primary">
          <i class="fas fa-plus-circle"></i> 发布商品
        </RouterLink>
        <!-- 批量删除按钮：删除选中的商品，未选中时禁用 -->
        <button class="btn btn-outline" @click="handleBatchDelete" :disabled="selectedIds.length === 0">
          <i class="fas fa-trash-alt"></i> 批量删除
          <!-- 显示已选中的商品数量 -->
          <span v-if="selectedIds.length > 0" class="badge">{{ selectedIds.length }}</span>
        </button>
      </div>
      
      <!-- 右侧操作区域：搜索框和筛选下拉框 -->
      <div class="action-right">
        <!-- 搜索框组件 -->
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="搜索商品名称、品牌..."
            @keyup.enter="handleSearch"
          />
          <!-- 清空搜索按钮：只在有搜索关键词时显示 -->
          <button v-if="searchKeyword" class="clear-search" @click="clearSearch">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <!-- 状态筛选下拉框 -->
        <select v-model="filterStatus" class="filter-select" @change="loadProducts">
          <option value="">全部商品</option>
          <option value="1">上架中</option>
          <option value="0">已下架</option>
        </select>
      </div>
    </div>

    <!-- 商品列表卡片区域 -->
    <div class="card">
      <div class="card-body">
        <!-- 加载状态：数据加载时显示 -->
        <div v-if="isLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
        
        <!-- 空状态：没有商品时显示 -->
        <div v-else-if="products.length === 0" class="empty-state">
          <i class="fas fa-box-open"></i>
          <p>暂无商品</p>
          <RouterLink to="/seller/products/create" class="btn btn-primary btn-sm">
            立即发布商品
          </RouterLink>
        </div>
        
        <!-- 商品表格：有商品时显示 -->
        <div v-else class="product-table-wrapper">
          <table class="product-table">
            <!-- 表格头部 -->
            <thead>
              <tr>
                <!-- 复选框列：用于批量选择商品 -->
                <th class="checkbox-col">
                  <input type="checkbox" @change="toggleSelectAll" v-model="selectAll" />
                </th>
                <!-- 商品图片列 -->
                <th class="image-col">商品图片</th>
                <!-- 商品信息列：包含名称和品牌 -->
                <th class="name-col">商品信息</th>
                <!-- 商品价格列：显示现价和原价 -->
                <th class="price-col">价格</th>
                <!-- 库存列 -->
                <th class="stock-col">库存</th>
                <!-- 状态列：上架/下架状态 -->
                <th class="status-col">状态</th>
                <!-- 操作列：编辑、上架/下架、删除按钮 -->
                <th class="action-col">操作</th>
              </tr>
            </thead>
            <!-- 表格主体：遍历商品列表 -->
            <tbody>
              <tr v-for="product in products" :key="product.id">
                <!-- 复选框：绑定当前商品的选中状态 -->
                <td class="checkbox-col">
                  <input type="checkbox" v-model="selectedIds" :value="product.id" />
                </td>
                <!-- 商品图片：显示主图，没有则显示默认图片 -->
                <td class="image-col">
                  <img 
                    :src="product.images || '/images/default-product.jpg'" 
                    :alt="product.name"
                    class="product-image"
                  />
                </td>
                <!-- 商品信息：名称、品牌、分类 -->
                <td class="name-col">
                  <div class="product-info">
                    <div class="product-name">{{ product.name }}</div>
                    <div class="product-brand">{{ product.brand }}</div>
                  </div>
                </td>
                <!-- 价格信息：现价 -->
                <td class="price-col">
                  <div class="product-price">¥{{ formatPrice(product.price) }}</div>
                </td>
                <!-- 库存信息：显示库存数量 -->
                <td class="stock-col">
                  <span :class="['stock-badge', getStockClass(product.stock)]">
                    {{ product.stock }}件
                  </span>
                </td>
                <!-- 状态列：使用优化的状态开关组件 -->
                <td class="status-col">
                  <!-- 状态开关组件：点击切换上架/下架状态 -->
                  <div class="status-switch" @click="toggleStatus(product, product.status === 1 ? 0 : 1)">
                    <!-- 开关滑块 -->
                    <div class="switch-slider" :class="{ active: product.status === 1 }">
                      <!-- 上架状态图标 -->
                      <i v-if="product.status === 1" class="fas fa-eye"></i>
                      <!-- 下架状态图标 -->
                      <i v-else class="fas fa-eye-slash"></i>
                    </div>
                    <!-- 状态文字 -->
                    <span class="status-text" :class="{ active: product.status === 1 }">
                      {{ product.status === 1 ? '上架' : '下架' }}
                    </span>
                  </div>
                </td>
                <!-- 操作按钮组 -->
                <td class="action-col">
                  <div class="action-buttons">
                    <!-- 编辑按钮：跳转到编辑页 -->
                    <button class="action-btn edit-btn" @click="goToEdit(product.id)" title="编辑">
                      <i class="fas fa-edit"></i>
                    </button>
                    <!-- 删除按钮 -->
                    <button class="action-btn delete-btn" @click="deleteProduct(product)" title="删除">
                      <i class="fas fa-trash-alt"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- 分页组件：总数据大于0时显示 -->
        <div class="pagination" v-if="total > 0">
          <!-- 上一页按钮 -->
          <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <i class="fas fa-chevron-left"></i>
          </button>
          <!-- 页码信息 -->
          <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
          <!-- 下一页按钮 -->
          <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            <i class="fas fa-chevron-right"></i>
          </button>
          <!-- 总数据量信息 -->
          <span class="total-info">共 {{ total }} 条商品</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import 'vant/es/dialog/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  // ==================== 类型定义 ====================

  interface Product {
    id: number
    name: string
    brand: string
    description?: string
    price: number
    stock: number
    status: number
    images?: string
    categoryId: number
  }

  // ==================== 响应式数据 ====================

  const isLoading = ref(false)
  const products = ref<Product[]>([])
  const selectedIds = ref<number[]>([])
  const selectAll = ref(false)
  const searchKeyword = ref('')
  const filterStatus = ref('')
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)

  // ==================== 计算属性 ====================

  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

  // ==================== 数据加载 ====================

  /**
   * 加载商品列表
   * @returns {Promise<void>}
   */
  const loadProducts = async () => {
    isLoading.value = true
    
    try {
      const params = {
        page: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
        status: filterStatus.value
      }
      
      const response = await authAPI.getSellerProducts(params)
      
      if (response.success) {
        products.value = response.data || []
        total.value = response.total || 0
      } else {
        throw new Error(response.message || '加载商品失败')
      }
    } catch (error: any) {
      console.error('加载商品失败:', error)
      showToast({
        message: error.message || '加载商品失败，请重试',
        type: 'fail',
        duration: 2000
      })
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 搜索商品
   */
  const handleSearch = () => {
    currentPage.value = 1
    loadProducts()
  }

  const clearSearch = () => {
    searchKeyword.value = ''
    handleSearch()
  }

  /**
   * 切换页码
   * @param {number} page - 目标页码
   */
  const changePage = (page: number) => {
    currentPage.value = page
    loadProducts()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // ==================== 批量操作 ====================

  /**
   * 全选/取消全选
   */
  const toggleSelectAll = () => {
    if (selectAll.value) {
      selectedIds.value = products.value.map(p => p.id)
    } else {
      selectedIds.value = []
    }
  }

  /**
   * 批量删除商品
   * @returns {Promise<void>}
   */
  const handleBatchDelete = async () => {
    if (selectedIds.value.length === 0) return
    
    try {
      await showConfirmDialog({
        title: '批量删除确认',
        message: `确定要删除选中的 ${selectedIds.value.length} 个商品吗？删除后不可恢复。`,
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      })
      
      const response = await authAPI.batchDeleteProducts(selectedIds.value)
      
      if (response.success) {
        showToast({
          message: `成功删除 ${selectedIds.value.length} 个商品`,
          type: 'success',
          duration: 2000
        })
        selectedIds.value = []
        selectAll.value = false
        loadProducts()
      } else {
        throw new Error(response.message || '批量删除失败')
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({
          message: error.message || '批量删除失败',
          type: 'fail',
          duration: 2000
        })
      }
    }
  }

  // ==================== 单个操作 ====================

  /**
   * 跳转到编辑页
   * @param {number} productId - 商品ID
   */
  const goToEdit = (productId: number) => {
    router.push({
      name: 'SellerProductEdit',
      params: { productId }
    })
  }

  /**
   * 切换商品状态
   * @param {Product} product - 商品对象
   * @param {number} status - 目标状态（0-下架，1-上架）
   * @returns {Promise<void>}
   */
  const toggleStatus = async (product: Product, status: number) => {
    const action = status === 1 ? '上架' : '下架'
    
    try {
      await showConfirmDialog({
        title: `${action}确认`,
        message: `确定要${action}「${product.name}」吗？`,
        confirmButtonText: `确定${action}`,
        cancelButtonText: '取消',
        confirmButtonColor: status === 1 ? '#10b981' : '#f59e0b'
      })
      
      const response = await authAPI.updateProductStatus(product.id, status)
      
      if (response.success) {
        showToast({
          message: `${action}成功`,
          type: 'success',
          duration: 1500
        })
        loadProducts()
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({
          message: error.message || `${action}失败`,
          type: 'fail',
          duration: 2000
        })
      }
    }
  }

  /**
   * 删除单个商品
   * @param {Product} product - 商品对象
   * @returns {Promise<void>}
   */
  const deleteProduct = async (product: Product) => {
    try {
      await showConfirmDialog({
        title: '删除确认',
        message: `确定要删除商品「${product.name}」吗？删除后不可恢复。`,
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonColor: '#ef4444'
      })
      
      const response = await authAPI.deleteProduct(product.id)
      
      if (response.success) {
        showToast({
          message: '删除成功',
          type: 'success',
          duration: 1500
        })
        loadProducts()
      } else {
        throw new Error(response.message || '删除失败')
      }
    } catch (error: any) {
      if (error !== 'cancel') {
        showToast({
          message: error.message || '删除失败',
          type: 'fail',
          duration: 2000
        })
      }
    }
  }

  // ==================== 工具函数 ====================

  /**
   * 格式化价格
   * @param {number} price - 原始价格
   * @returns {string} 保留两位小数的价格字符串
   */
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  /**
   * 获取库存样式类
   * @param {number} stock - 库存数量
   * @returns {string} CSS类名
   */
  const getStockClass = (stock: number): string => {
    if (stock <= 0) return 'stock-out'
    if (stock < 10) return 'stock-low'
    return 'stock-normal'
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateSellerPermission()) return
    
    loadProducts()
  })
</script>

<style scoped>
  @import url('@/static/css/seller/商品管理页.css');
</style>