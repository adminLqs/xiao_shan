<template>
  <div class="add-product-container">

    <!-- 商品发布表单 -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">
          <i class="fas fa-info-circle"></i>
          商品信息
        </h3>
      </div>
      <div class="card-body">
        <form @submit.prevent="submitProductForm">
          <!-- 商品名称和品牌 -->
          <div class="form-row">
            <div class="form-col">
              <div class="form-group">
                <label class="form-label required">商品名称</label>
                <input
                  type="text"
                  class="form-control"
                  placeholder="请输入商品名称"
                  v-model="productForm.name"
                  maxlength="100"
                  required
                />
                <small class="form-hint">最多100个字符</small>
              </div>
            </div>
            <div class="form-col">
              <div class="form-group">
                <label class="form-label required">商品品牌</label>
                <input
                  type="text"
                  class="form-control"
                  placeholder="请输入商品品牌"
                  v-model="productForm.brand"
                  maxlength="50"
                />
              </div>
            </div>
          </div>

          <!-- 商品描述 -->
          <div class="form-group">
            <label class="form-label">商品描述</label>
            <textarea
              class="form-control"
              placeholder="请输入商品详细描述，包括功能、规格、材质等信息"
              v-model="productForm.description"
              rows="4"
              maxlength="500"
            ></textarea>
            <small class="form-hint">{{ productForm.description.length }}/500</small>
          </div>

          <!-- 价格信息：原价和现价 -->
          <div class="form-row">
            <div class="form-col">
              <div class="form-group">
                <label class="form-label">商品原价</label>
                <div class="price-input">
                  <span class="price-symbol">¥</span>
                  <input
                    type="number"
                    class="form-control"
                    placeholder="0.00"
                    min="0"
                    step="0.01"
                    v-model="productForm.originalPrice"
                  />
                </div>
                <small class="form-hint">划线价，用于显示折扣（选填）</small>
              </div>
            </div>
            <div class="form-col">
              <div class="form-group">
                <label class="form-label required">商品价格</label>
                <div class="price-input">
                  <span class="price-symbol">¥</span>
                  <input
                    type="number"
                    class="form-control"
                    placeholder="0.00"
                    min="0"
                    step="0.01"
                    v-model="productForm.price"
                    required
                  />
                </div>
                <small class="form-hint">实际售价，必须填写</small>
              </div>
            </div>
          </div>

          <!-- 库存数量 -->
          <div class="form-row">
            <div class="form-col">
              <div class="form-group">
                <label class="form-label required">库存数量</label>
                <input
                  type="number"
                  class="form-control"
                  placeholder="0"
                  min="0"
                  v-model="productForm.stock"
                  required
                />
              </div>
            </div>
          </div>

          <!-- 商品分类选择（二级分类） -->
          <div class="form-group">
            <label class="form-label required">商品分类</label>
            <div class="category-selector">
              <!-- 一级分类 -->
              <div class="category-level">
                <select
                  class="form-control"
                  v-model="selectedLevel1"
                  @change="onLevel1Change"
                  required
                >
                  <option value="">请选择一级分类</option>
                  <option
                    v-for="cat in level1Categories"
                    :key="cat.id"
                    :value="cat.id"
                  >
                    {{ cat.name }}
                  </option>
                </select>
              </div>
              <!-- 二级分类 -->
              <div class="category-level">
                <select
                  class="form-control"
                  v-model="selectedLevel2"
                  @change="onLevel2Change"
                  :disabled="!selectedLevel1"
                  required
                >
                  <option value="">请选择二级分类</option>
                  <option
                    v-for="cat in level2Categories"
                    :key="cat.id"
                    :value="cat.id"
                  >
                    {{ cat.name }}
                  </option>
                </select>
              </div>
            </div>
          </div>

          <!-- 商品图片上传 -->
          <div class="form-group">
            <label class="form-label required">商品图片</label>
            <div
              class="file-upload-area"
              @click="triggerFileInput"
              @dragover.prevent="onDragOver"
              @dragleave.prevent="onDragLeave"
              @drop.prevent="onFileDrop"
              :class="{ 'drag-over': isDragOver }"
            >
              <i class="fas fa-cloud-upload-alt upload-icon"></i>
              <p class="upload-text">点击或拖拽图片到此处上传</p>
              <p class="upload-hint">支持 JPG、PNG 格式，单张图片不超过 5MB，最多上传5张</p>
              <button type="button" class="btn btn-outline btn-sm">选择图片</button>
            </div>
            <input
              type="file"
              ref="fileInput"
              multiple
              accept="image/jpeg,image/png,image/jpg"
              style="display: none"
              @change="handleFileSelect"
            />
            
            <!-- 图片预览 -->
            <div class="image-preview-list" v-if="productImages.length > 0">
              <div
                v-for="(image, index) in productImages"
                :key="image.id"
                class="image-preview-item"
              >
                <img :src="image.url" :alt="`商品图片${index + 1}`" />
                <button
                  type="button"
                  class="remove-image-btn"
                  @click="removeImage(index)"
                >
                  <i class="fas fa-times"></i>
                </button>
                <div class="image-index">{{ index + 1 }}</div>
              </div>
            </div>
          </div>

          <!-- 表单操作按钮 -->
          <div class="form-actions">
            <button type="button" class="btn btn-outline" @click="cancelForm">
              <i class="fas fa-times"></i> 取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
              <i v-if="isSubmitting" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-save"></i>
              {{ isSubmitting ? '发布中...' : '发布商品' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast, showConfirmDialog } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'

  const authStore = useAuthStore()
  const router = useRouter()

  // ==================== 类型定义 ====================

  interface Category {
    id: number
    name: string
    parentId: number | null
    sortOrder: number
    isActive: boolean
  }

  interface ProductImage {
    id: number
    url: string
    file: File
  }

  interface ProductForm {
    name: string
    brand: string
    description: string
    originalPrice: string
    price: string
    stock: string
    categoryId: string
  }

  // ==================== 表单数据 ====================

  const productForm = reactive<ProductForm>({
    name: '',
    brand: '',
    description: '',
    originalPrice: '',
    price: '',
    stock: '',
    categoryId: ''
  })

  const selectedLevel1 = ref('')
  const selectedLevel2 = ref('')

  const allCategories = ref<Category[]>([])
  const level1Categories = ref<Category[]>([])
  const level2Categories = ref<Category[]>([])

  const productImages = ref<ProductImage[]>([])
  const fileInput = ref<HTMLInputElement | null>(null)
  const isDragOver = ref(false)
  const isSubmitting = ref(false)

  // ==================== 分类数据加载 ====================

  /**
   * 加载商品分类
   * @returns {Promise<void>}
   */
  const loadCategories = async () => {
    try {
      const response = await authAPI.getAllCategories()
      
      if (response.success && response.data) {
        allCategories.value = response.data
        level1Categories.value = allCategories.value.filter(
          cat => cat.parentId === null && cat.isActive
        )
      } else {
        throw new Error(response.message || '加载分类失败')
      }
    } catch (error) {
      console.error('加载分类失败:', error)
      showToast({
        message: '加载分类失败，请刷新重试',
        type: 'fail',
        duration: 2000
      })
    }
  }

  // ==================== 分类联动 ====================

  const onLevel1Change = () => {
    selectedLevel2.value = ''
    level2Categories.value = []
    productForm.categoryId = ''
    
    if (!selectedLevel1.value) return
    
    level2Categories.value = allCategories.value.filter(
      cat => cat.parentId === Number(selectedLevel1.value) && cat.isActive
    )
  }

  const onLevel2Change = () => {
    productForm.categoryId = selectedLevel2.value || ''
  }

  // ==================== 图片上传 ====================

  const triggerFileInput = () => {
    fileInput.value?.click()
  }

  const onDragOver = () => {
    isDragOver.value = true
  }

  const onDragLeave = () => {
    isDragOver.value = false
  }

  const onFileDrop = (e: DragEvent) => {
    isDragOver.value = false
    const files = e.dataTransfer?.files
    if (files && files.length > 0) {
      handleFiles(Array.from(files))
    }
  }

  const handleFileSelect = (e: Event) => {
    const input = e.target as HTMLInputElement
    const files = input.files
    if (files && files.length > 0) {
      handleFiles(Array.from(files))
    }
    input.value = ''
  }

  /**
   * 处理上传的文件
   * @param {File[]} files - 文件数组
   */
  const handleFiles = (files: File[]) => {
    if (productImages.value.length + files.length > 5) {
      showToast({ message: '最多只能上传5张图片！', type: 'fail', duration: 2000 })
      return
    }
    
    for (const file of files) {
      if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
        showToast({ message: `文件 ${file.name} 格式不支持，请上传JPG或PNG格式`, type: 'fail', duration: 2000 })
        continue
      }
      
      if (file.size > 5 * 1024 * 1024) {
        showToast({ message: `文件 ${file.name} 超过5MB限制！`, type: 'fail', duration: 2000 })
        continue
      }
      
      const reader = new FileReader()
      reader.onload = (e) => {
        productImages.value.push({
          id: Date.now() + Math.random(),
          url: e.target?.result as string,
          file: file
        })
      }
      reader.readAsDataURL(file)
    }
  }

  const removeImage = (index: number) => {
    productImages.value.splice(index, 1)
  }

  // ==================== 表单验证 ====================

  /**
   * 验证表单数据
   * @returns {boolean} 验证是否通过
   */
  const validateForm = (): boolean => {
    if (!productForm.name.trim()) {
      showToast({ message: '请输入商品名称', type: 'fail', duration: 1500 })
      return false
    }
    
    if (!productForm.price || parseFloat(productForm.price) <= 0) {
      showToast({ message: '请输入有效的商品价格', type: 'fail', duration: 1500 })
      return false
    }
    
    // 原价必须大于现价，用于显示折扣
    if (productForm.originalPrice && parseFloat(productForm.originalPrice) > 0) {
      const originalPrice = parseFloat(productForm.originalPrice)
      const currentPrice = parseFloat(productForm.price)
      if (originalPrice <= currentPrice) {
        showToast({ message: '原价必须大于现价，以便显示折扣', type: 'fail', duration: 2000 })
        return false
      }
    }
    
    if (!productForm.stock || parseInt(productForm.stock) < 0) {
      showToast({ message: '请输入有效的库存数量', type: 'fail', duration: 1500 })
      return false
    }
    
    if (!productForm.categoryId) {
      showToast({ message: '请选择商品分类', type: 'fail', duration: 1500 })
      return false
    }
    
    if (productImages.value.length === 0) {
      showToast({ message: '请至少上传一张商品图片', type: 'fail', duration: 1500 })
      return false
    }
    
    return true
  }

  // ==================== 提交表单 ====================

  /**
   * 提交商品表单
   * @returns {Promise<void>}
   */
  const submitProductForm = async () => {
    if (!validateForm()) return
    
    isSubmitting.value = true
    
    try {
      const formData = new FormData()
      
      const productData = {
        name: productForm.name.trim(),
        brand: productForm.brand.trim(),
        description: productForm.description.trim(),
        originalPrice: productForm.originalPrice ? parseFloat(productForm.originalPrice) : null,
        price: parseFloat(productForm.price),
        stock: parseInt(productForm.stock, 10),
        categoryId: parseInt(productForm.categoryId, 10)
      }
      
      formData.append('products', new Blob([JSON.stringify(productData)], { type: 'application/json' }))
      
      productImages.value.forEach((img) => {
        formData.append('images', img.file)
      })
      
      const response = await authAPI.addProduct(formData)
      
      if (response.success) {
        showToast({ message: response.message || '商品发布成功！', type: 'success', duration: 2000 })
        resetForm()
      } else {
        throw new Error(response.message || '发布失败')
      }
    } catch (error: any) {
      console.error('发布商品失败:', error)
      showToast({ message: error.message || '商品发布失败，请重试', type: 'fail', duration: 2000 })
    } finally {
      isSubmitting.value = false
    }
  }

  // ==================== 重置与取消 ====================

  const resetForm = () => {
    productForm.name = ''
    productForm.brand = ''
    productForm.description = ''
    productForm.originalPrice = ''
    productForm.price = ''
    productForm.stock = ''
    productForm.categoryId = ''
    
    selectedLevel1.value = ''
    selectedLevel2.value = ''
    
    level2Categories.value = []
    productImages.value = []
  }

  /**
   * 取消操作
   */
  const cancelForm = () => {
    const hasUnsavedData = productForm.name || productForm.brand || productImages.value.length > 0
    
    if (hasUnsavedData) {
      showConfirmDialog({
        title: '确认取消',
        message: '确定要取消吗？所有未保存的更改将会丢失。',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(() => {
        resetForm()
        router.push('/seller/products')
      }).catch(() => {
        // 用户取消，静默处理
      })
    } else {
      router.push('/seller/products')
    }
  }

  // ==================== 生命周期 ====================

  onMounted(() => {
    if (!authStore.validateSellerPermission()) return

    loadCategories()
  })
</script>

<style scoped>
@import url('@/static/css/seller/商家商品发布页.css');
</style>