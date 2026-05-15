<template>
  <div class="seller-products">
    <div class="page-header">
      <h2>商品管理</h2>
      <button class="add-btn" @click="openAddModal">
        <span class="icon">➕</span>
        添加商品
      </button>
    </div>

    <div class="products-container">
      <div v-if="loading" class="loading-state">
        <span>加载中...</span>
      </div>
      
      <div v-else-if="products.length === 0" class="empty-state">
        <span>📦 暂无商品</span>
        <button class="empty-add-btn" @click="openAddModal">添加第一个商品</button>
      </div>
      
      <div v-else class="product-grid">
        <div 
          v-for="product in products" 
          :key="product.id"
          class="product-card"
        >
          <div class="product-image">
            <img 
              v-if="product.image" 
              :src="product.image" 
              :alt="product.name"
            />
            <span v-else class="image-placeholder">🥩</span>
          </div>
          
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc" v-if="product.description">{{ product.description }}</p>
            
            <div class="product-footer">
              <div class="price-info">
                <span class="current-price">¥{{ formatPrice(product.price) }}</span>
                <span v-if="product.originalPrice && product.originalPrice > product.price" class="original-price">
                  ¥{{ formatPrice(product.originalPrice) }}
                </span>
              </div>
              
              <div class="action-buttons">
                <button class="action-btn edit" @click="openEditModal(product)">
                  <span>✏️</span>
                </button>
                <button class="action-btn delete" @click="handleDelete(product)">
                  <span>🗑️</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑商品模态框 -->
    <div v-if="showModal" class="modal-mask" @click="closeModal">
      <div class="modal-container" @click.stop>
        <div class="modal-header">
          <h3>{{ isEdit ? '编辑商品' : '添加商品' }}</h3>
          <button class="close-modal" @click="closeModal">✕</button>
        </div>
        
        <div class="modal-body">
          <!-- 商品图片 -->
          <div class="form-item">
            <label class="form-label">商品图片</label>
            <div class="image-upload">
              <div class="image-preview" v-if="formData.imagePreview || formData.image">
                <img :src="formData.imagePreview || formData.image" alt="商品图片" />
                <button class="remove-image" @click="removeImage">✕</button>
              </div>
              <div v-else class="upload-placeholder" @click="triggerFileInput">
                <span class="upload-icon">📷</span>
                <span class="upload-text">点击上传图片</span>
              </div>
              <input 
                type="file" 
                ref="fileInput" 
                style="display: none" 
                accept="image/*" 
                @change="handleImageChange" 
              />
            </div>
          </div>

          <!-- 商品名称 -->
          <div class="form-item">
            <label class="form-label required">商品名称</label>
            <input 
              type="text" 
              v-model="formData.name" 
              class="form-input" 
              placeholder="请输入商品名称"
              maxlength="50"
            />
          </div>

          <!-- 商品分类 -->
          <div class="form-item">
            <label class="form-label required">商品分类</label>
            <select v-model="formData.category" class="form-select">
              <option value="">请选择分类</option>
              <option value="meat">🔥 招牌烤肉</option>
              <option value="seafood">🦐 海鲜烧烤</option>
              <option value="vegetable">🥬 烤蔬菜</option>
              <option value="staple">🍚 主食类</option>
              <option value="drink">🍺 酒水饮料</option>
              <option value="snack">🍡 特色小吃</option>
              <option value="skewer">🍢 经典串烧</option>
              <option value="cold">🥗 爽口凉菜</option>
            </select>
          </div>

          <!-- 价格 -->
          <div class="form-row">
            <div class="form-item half">
              <label class="form-label required">售价 (¥)</label>
              <input 
                type="number" 
                v-model="formData.price" 
                class="form-input" 
                placeholder="0.00"
                step="0.01"
                min="0"
              />
            </div>
            <div class="form-item half">
              <label class="form-label">原价 (¥)</label>
              <input 
                type="number" 
                v-model="formData.originalPrice" 
                class="form-input" 
                placeholder="0.00"
                step="0.01"
                min="0"
              />
            </div>
          </div>

          <!-- 商品描述 -->
          <div class="form-item">
            <label class="form-label">商品描述</label>
            <textarea 
              v-model="formData.description" 
              class="form-textarea" 
              rows="3" 
              placeholder="请输入商品描述"
              maxlength="200"
            ></textarea>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="closeModal">取消</button>
          <button class="submit-btn" @click="submitForm" :disabled="submitting">
            {{ submitting ? '提交中...' : (isEdit ? '保存修改' : '添加商品') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="modal-mask" @click="closeDeleteConfirm">
      <div class="confirm-dialog" @click.stop>
        <div class="confirm-header">
          <span class="warning-icon">⚠️</span>
          <h3>确认删除</h3>
        </div>
        <div class="confirm-body">
          <p>确定要删除商品 <strong>{{ deleteTarget?.name }}</strong> 吗？</p>
          <p class="warning-text">删除后无法恢复，请谨慎操作！</p>
        </div>
        <div class="confirm-footer">
          <button class="cancel-btn" @click="closeDeleteConfirm">取消</button>
          <button class="delete-btn" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, reactive, onMounted } from 'vue'
  import { showSuccessToast, showFailToast } from 'vant'
  import { authAPI } from '@/api/auth'

  // 商品接口
  interface Product {
    id: number
    name: string
    description: string
    price: number
    originalPrice: number
    image: string
    category: string
    createdAt?: string
  }

  // 表单数据接口
  interface ProductForm {
    id?: number
    name: string
    description: string
    price: number
    originalPrice: number
    image: string
    imagePreview: string
    imageFile: File | null
    category: string
  }

  // 响应式数据
  const products = ref<Product[]>([])
  const loading = ref(false)
  const showModal = ref(false)
  const showDeleteConfirm = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const deleteTarget = ref<Product | null>(null)
  const fileInput = ref<HTMLInputElement | null>(null)

  // 表单数据
  const formData = reactive<ProductForm>({
    name: '',
    description: '',
    price: 0,
    originalPrice: 0,
    image: '',
    imagePreview: '',
    imageFile: null,
    category: ''
  })

  // 格式化价格
  const formatPrice = (price: number): string => {
    return price.toFixed(2)
  }

  // 获取所有商品
  const loadProducts = async () => {
    loading.value = true
    try {
      const response = await authAPI.getAllProducts()
      const data = response.data || response
      
      if (Array.isArray(data)) {
        products.value = data.map((item: any) => ({
          id: item.id,
          name: item.name,
          description: item.description || '',
          price: Number(item.price),
          originalPrice: Number(item.originalPrice) || Number(item.price),
          image: item.image || '',
          category: item.category || 'skewer',
          createdAt: item.createdAt
        }))
      }
    } catch (error) {
      console.error('获取商品列表失败:', error)
      showFailToast('获取商品列表失败')
    } finally {
      loading.value = false
    }
  }

  // 打开添加模态框
  const openAddModal = () => {
    isEdit.value = false
    resetForm()
    showModal.value = true
  }

  // 打开编辑模态框
  const openEditModal = (product: Product) => {
    isEdit.value = true
    formData.id = product.id
    formData.name = product.name
    formData.description = product.description
    formData.price = product.price
    formData.originalPrice = product.originalPrice
    formData.image = product.image
    formData.category = product.category
    // 编辑时不设置预览图，保持原有图片
    showModal.value = true
  }

  // 重置表单
  const resetForm = () => {
    formData.id = undefined
    formData.name = ''
    formData.description = ''
    formData.price = 0
    formData.originalPrice = 0
    formData.image = ''
    formData.imagePreview = ''
    formData.imageFile = null
    formData.category = ''
  }

  // 关闭模态框
  const closeModal = () => {
    showModal.value = false
    resetForm()
  }

  // 触发文件选择
  const triggerFileInput = () => {
    fileInput.value?.click()
  }

  // 处理图片选择
  const handleImageChange = (e: Event) => {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]
    
    if (!file) return
    
    // 验证文件
    if (file.size > 5 * 1024 * 1024) {
      showFailToast('图片大小不能超过5MB')
      return
    }
    
    if (!file.type.startsWith('image/')) {
      showFailToast('请选择图片文件')
      return
    }
    
    // 创建预览
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.imagePreview = e.target?.result as string
    }
    reader.readAsDataURL(file)
    
    formData.imageFile = file
  }

  // 移除图片
  const removeImage = () => {
    formData.imagePreview = ''
    formData.imageFile = null
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }

  // 提交表单
  const submitForm = async () => {
    // 验证表单
    if (!formData.name.trim()) {
      showFailToast('请输入商品名称')
      return
    }
    
    if (!formData.category) {
      showFailToast('请选择商品分类')
      return
    }
    
    if (formData.price <= 0) {
      showFailToast('请输入有效的价格')
      return
    }
    
    submitting.value = true
    
    try {
      // 构建 FormData
      const form = new FormData()
      
      // 商品数据（JSON字符串）
      const productData = {
        name: formData.name,
        description: formData.description,
        price: formData.price,
        originalPrice: formData.originalPrice,
        category: formData.category
      }
      
      // 使用 Blob 封装 JSON
      const productBlob = new Blob([JSON.stringify(productData)], {
        type: 'application/json'
      })
      form.append('product', productBlob)
      
      // ✅ 只有上传新图片时才添加
      if (formData.imageFile) {
        form.append('image', formData.imageFile)
      }
      
      let response
      if (isEdit.value && formData.id) {
        // 更新商品
        response = await authAPI.updateProduct(formData.id, form)
      } else {
        // 添加商品
        response = await authAPI.addProduct(form)
      }
      
      const data = response.data || response
      
      if (data.success) {
        showSuccessToast(isEdit.value ? '商品更新成功' : '商品添加成功')
        closeModal()
        await loadProducts()
      } else {
        showFailToast(data.message || '操作失败')
      }
    } catch (error) {
      console.error('提交失败:', error)
      showFailToast('操作失败，请重试')
    } finally {
      submitting.value = false
    }
  }

  // 处理删除
  const handleDelete = (product: Product) => {
    deleteTarget.value = product
    showDeleteConfirm.value = true
  }

  // 确认删除
  const confirmDelete = async () => {
    if (!deleteTarget.value) return
    
    try {
      const response = await authAPI.deleteProduct(deleteTarget.value.id)
      const data = response.data || response
      
      if (data.success) {
        showSuccessToast('商品删除成功')
        closeDeleteConfirm()
        await loadProducts()
      } else {
        showFailToast(data.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      showFailToast('删除失败，请重试')
    }
  }

  // 关闭删除确认框
  const closeDeleteConfirm = () => {
    showDeleteConfirm.value = false
    deleteTarget.value = null
  }

  // 初始化
  onMounted(() => {
    loadProducts()
  })
</script>

<style scoped>
 @import url('@/static/css/商家商品管理页.css')
</style>