<template>
  <div class="edit-product-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-edit"></i>
        编辑商品
      </h1>
      <div class="action-buttons">
        <button class="btn btn-outline" @click="goBack">
          <i class="fas fa-arrow-left"></i> 返回
        </button>
        <button class="btn btn-primary" @click="submitForm" :disabled="isSubmitting">
          <i v-if="isSubmitting" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-save"></i>
          {{ isSubmitting ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 商品编辑表单 -->
    <div v-else class="card">
      <div class="card-header">
        <h3 class="card-title">
          <i class="fas fa-info-circle"></i>
          基本信息
        </h3>
      </div>
      <div class="card-body">
        <form @submit.prevent="submitForm">
          <!-- 商品名称 -->
          <div class="form-group">
            <label class="form-label required">商品名称</label>
            <input
              type="text"
              class="form-control"
              placeholder="请输入商品名称"
              v-model="formData.name"
              maxlength="100"
              required
            />
            <small class="form-hint">{{ formData.name.length }}/100</small>
          </div>

          <!-- 商品品牌 -->
          <div class="form-group">
            <label class="form-label required">商品品牌</label>
            <input
              type="text"
              class="form-control"
              placeholder="请输入商品品牌"
              v-model="formData.brand"
              maxlength="50"
              required
            />
          </div>

          <!-- 商品描述 -->
          <div class="form-group">
            <label class="form-label">商品描述</label>
            <textarea
              class="form-control"
              placeholder="请输入商品详细描述"
              v-model="formData.description"
              rows="4"
              maxlength="500"
            ></textarea>
            <small class="form-hint">{{ formData.description.length }}/500</small>
          </div>

          <!-- 价格信息 -->
          <div class="form-row">
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
                    v-model="formData.price"
                    required
                  />
                </div>
              </div>
            </div>
            <div class="form-col">
              <div class="form-group">
                <label class="form-label">原价（选填）</label>
                <div class="price-input">
                  <span class="price-symbol">¥</span>
                  <input
                    type="number"
                    class="form-control"
                    placeholder="0.00"
                    min="0"
                    step="0.01"
                    v-model="formData.originalPrice"
                  />
                </div>
                <small class="form-hint">原价必须大于现价，用于显示折扣</small>
              </div>
            </div>
          </div>

          <!-- 库存 -->
          <div class="form-group">
            <label class="form-label required">库存数量</label>
            <input
              type="number"
              class="form-control"
              placeholder="0"
              min="0"
              v-model="formData.stock"
              required
            />
          </div>

          <!-- 商品分类 -->
          <div class="form-group">
            <label class="form-label required">商品分类</label>
            <div class="category-selector">
              <select
                class="form-control"
                v-model="selectedLevel1"
                @change="onLevel1Change"
                required
              >
                <option value="">请选择一级分类</option>
                <option v-for="cat in level1Categories" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
              <select
                class="form-control"
                v-model="selectedLevel2"
                @change="onLevel2Change"
                :disabled="!selectedLevel1"
                required
              >
                <option value="">请选择二级分类</option>
                <option v-for="cat in level2Categories" :key="cat.id" :value="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </div>
          </div>

          <!-- 商品状态 -->
          <div class="form-group">
            <label class="form-label">商品状态</label>
            <div class="status-selector">
              <label class="radio-label">
                <input type="radio" v-model="formData.status" :value="1" />
                <span>上架</span>
              </label>
              <label class="radio-label">
                <input type="radio" v-model="formData.status" :value="0" />
                <span>下架</span>
              </label>
            </div>
          </div>

          <!-- 商品图片 -->
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
              <p class="upload-hint">支持 JPG、PNG 格式，单张不超过 5MB，最多5张</p>
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
            
            <!-- 图片预览列表 -->
            <div class="image-preview-list" v-if="imageList.length > 0">
              <div
                v-for="(image, index) in imageList"
                :key="image.id"
                class="image-preview-item"
              >
                <img :src="image.url" :alt="`商品图片${index + 1}`" />
                <button type="button" class="remove-image-btn" @click="removeImage(index)">
                  <i class="fas fa-times"></i>
                </button>
                <div class="image-index">{{ index + 1 }}</div>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { authAPI } from '@/api/authAPI'
  import { showToast } from 'vant'
  import 'vant/es/toast/style'
  import { useAuthStore } from '@/stores/auth'             
  import { storeToRefs } from 'pinia'                        

  // ============ Pinia 权限状态 ============
  const authStore = useAuthStore()
  const { isLoggedIn, role, status } = storeToRefs(authStore)

  // ============ 类型定义 ============
  interface Category {
    id: number
    name: string
    parentId: number | null
    isActive: boolean
  }

  interface ProductImage {
    id: number
    url: string
    file?: File
    isNew?: boolean
  }

  // ============ 路由 ============
  const route = useRoute()
  const router = useRouter()

  // ============ 商品ID ============
  const productId = ref<number>(0)

  // ============ 表单数据 ============
  const formData = reactive({
    name: '',
    brand: '',
    description: '',
    price: '',
    originalPrice: '',
    stock: '',
    categoryId: '',
    status: 1
  })

  // ============ 分类数据 ============
  const allCategories = ref<Category[]>([])
  const level1Categories = ref<Category[]>([])
  const level2Categories = ref<Category[]>([])
  const selectedLevel1 = ref('')
  const selectedLevel2 = ref('')

  // ============ 图片相关 ============
  const imageList = ref<ProductImage[]>([])
  const fileInput = ref<HTMLInputElement | null>(null)
  const isDragOver = ref(false)

  // ============ 状态 ============
  const isSubmitting = ref(false)
  const isLoading = ref(true)


  // ============ 加载所有分类 ============
  const loadAllCategories = async () => {
    try {
      const response = await authAPI.getAllCategories()
      if (response.success && response.data) {
        allCategories.value = response.data
        
        // 提取一级分类（parentId为null）
        level1Categories.value = allCategories.value.filter(
          cat => cat.parentId === null && cat.isActive
        )
      }
    } catch (error) {
      console.error('加载分类失败:', error)
      showToast({ message: '加载分类失败', type: 'fail' })
    }
  }

  // ============ 根据二级分类ID设置一级分类 ============
  const setCategoryByLevel2Id = (categoryId: number) => {
    // 查找二级分类
    const level2Cat = allCategories.value.find(cat => cat.id === categoryId)
    if (level2Cat && level2Cat.parentId) {
      // 设置一级分类
      selectedLevel1.value = String(level2Cat.parentId)
      // 加载二级分类列表
      loadLevel2Categories()
      // 设置二级分类
      selectedLevel2.value = String(categoryId)
      // 更新表单分类ID
      formData.categoryId = String(categoryId)
    }
  }

  // ============ 加载二级分类 ============
  const loadLevel2Categories = () => {
    if (!selectedLevel1.value) {
      level2Categories.value = []
      return
    }
    
    level2Categories.value = allCategories.value.filter(
      cat => cat.parentId === Number(selectedLevel1.value) && cat.isActive
    )
  }

  // ============ 加载商品详情 ============
  const loadProductDetail = async () => {
    isLoading.value = true
    
    try {
      // 从路由参数获取商品ID
      productId.value = Number(route.params.productId)
      
      if (!productId.value) {
        showToast({ message: '商品ID不存在', type: 'fail' })
        router.push('/seller/products')
        return
      }
      
      // 1. 先加载所有分类
      await loadAllCategories()
      
      // 2. 调用API获取商品详情
      const response = await authAPI.getProduct(productId.value)
      
      if (response.success && response.data) {
        const product = response.data
        
        // 填充表单数据
        formData.name = product.name || ''
        formData.brand = product.brand || ''
        formData.description = product.description || ''
        formData.price = product.price || ''
        formData.originalPrice = product.originalPrice || ''
        formData.stock = product.stock || ''
        formData.status = product.status ?? 1
        formData.categoryId = product.categoryId || ''
        
        // 设置分类默认值
        if (product.categoryId) {
          setCategoryByLevel2Id(Number(product.categoryId))
        }
        
        // 处理图片（后端返回的是逗号分隔的字符串）
        if (product.images && product.images !== 'null') {
          const imageUrls = product.images.split(',')
          imageList.value = imageUrls.map((url, index) => ({
            id: Date.now() + index,
            url: url,
            isNew: false
          }))
        }
        
      } else {
        throw new Error(response.message || '加载商品失败')
      }
    } catch (error: any) {
      console.error('加载商品失败:', error)
      showToast({
        message: error.message || '加载商品失败',
        type: 'fail',
        duration: 2000
      })
      router.push('/seller/products')
    } finally {
      isLoading.value = false
    }
  }

  // ============ 分类联动 ============
  const onLevel1Change = () => {
    // 清空二级分类选择
    selectedLevel2.value = ''
    formData.categoryId = ''
    
    if (!selectedLevel1.value) {
      level2Categories.value = []
      return
    }
    
    // 加载对应的二级分类
    level2Categories.value = allCategories.value.filter(
      cat => cat.parentId === Number(selectedLevel1.value) && cat.isActive
    )
  }

  const onLevel2Change = () => {
    if (selectedLevel2.value) {
      formData.categoryId = selectedLevel2.value
    } else {
      formData.categoryId = ''
    }
  }

  // ============ 图片上传 ============
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

  const handleFiles = (files: File[]) => {
    if (imageList.value.length + files.length > 5) {
      showToast({ message: '最多只能上传5张图片！', type: 'fail' })
      return
    }
    
    for (const file of files) {
      if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
        showToast({ message: `文件 ${file.name} 格式不支持`, type: 'fail' })
        continue
      }
      
      if (file.size > 5 * 1024 * 1024) {
        showToast({ message: `文件 ${file.name} 超过5MB`, type: 'fail' })
        continue
      }
      
      const reader = new FileReader()
      reader.onload = (e) => {
        imageList.value.push({
          id: Date.now() + Math.random(),
          url: e.target?.result as string,
          file: file,
          isNew: true
        })
      }
      reader.readAsDataURL(file)
    }
  }

  const removeImage = (index: number) => {
    imageList.value.splice(index, 1)
  }

  // ============ 表单验证 ============
  const validateForm = (): boolean => {
    if (!formData.name.trim()) {
      showToast({ message: '请输入商品名称', type: 'fail' })
      return false
    }
    if (!formData.brand.trim()) {
      showToast({ message: '请输入商品品牌', type: 'fail' })
      return false
    }
    if (!formData.price || parseFloat(formData.price) <= 0) {
      showToast({ message: '请输入有效的商品价格', type: 'fail' })
      return false
    }
    if (!formData.stock || parseInt(formData.stock) < 0) {
      showToast({ message: '请输入有效的库存数量', type: 'fail' })
      return false
    }
    if (!formData.categoryId) {
      showToast({ message: '请选择商品分类', type: 'fail' })
      return false
    }
    if (imageList.value.length === 0) {
      showToast({ message: '请至少上传一张商品图片', type: 'fail' })
      return false
    }
    return true
  }

  // ============ 提交表单 ============
  const submitForm = async () => {
    if (!validateForm()) return
    
    isSubmitting.value = true
    
    try {
      const formDataObj = new FormData()
      
      // 商品数据
      const productData = {
        name: formData.name.trim(),
        brand: formData.brand.trim(),
        description: formData.description.trim(),
        price: parseFloat(formData.price),
        originalPrice: formData.originalPrice ? parseFloat(formData.originalPrice) : null,
        stock: parseInt(formData.stock, 10),
        categoryId: parseInt(formData.categoryId, 10),
        status: formData.status
      }
      
      formDataObj.append('products', new Blob([JSON.stringify(productData)], { type: 'application/json' }))
      
      // 只上传新增的图片
      imageList.value.forEach((img) => {
        if (img.isNew && img.file) {
          formDataObj.append('images', img.file)
        }
      })
      
      const response = await authAPI.updateProduct(productId.value, formDataObj)
      
      if (response.success) {
        showToast({ message: '保存成功', type: 'success', duration: 1500 })
        setTimeout(() => {
          router.push('/seller/products')
        }, 1500)
      } else {
        throw new Error(response.message || '保存失败')
      }
    } catch (error: any) {
      showToast({ message: error.message || '保存失败', type: 'fail' })
    } finally {
      isSubmitting.value = false
    }
  }

  // ============ 返回 ============
  const goBack = () => {
    router.push('/seller/products')
  }

  // ============ 生命周期 ============
  onMounted(() => {
    // 校验权限
    if (!authStore.validateSellerPermission()) return;

    loadProductDetail()
  })
</script>

<style scoped>
  @import url('@/static/css/seller/商品编辑页.css');
</style>