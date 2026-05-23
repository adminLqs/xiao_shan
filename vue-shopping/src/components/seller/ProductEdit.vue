<template>
  <div class="edit-product-container">
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

    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="card">
      <div class="card-header">
        <h3 class="card-title">
          <i class="fas fa-info-circle"></i>
          基本信息
        </h3>
      </div>
      <div class="card-body">
        <form @submit.prevent="submitForm">
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

          <div class="form-group">
            <label class="form-label">商品品牌</label>
            <input
              type="text"
              class="form-control"
              placeholder="请输入商品品牌"
              v-model="formData.brand"
              maxlength="50"
            />
          </div>

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

    <div v-if="!isLoading" class="card">
      <div class="card-header">
        <h3 class="card-title">
          <i class="fas fa-list"></i>
          SKU规格
        </h3>
      </div>
      <div class="card-body">
        <div class="sku-edit-section">
          <div class="sku-table">
            <div class="sku-table-header">
              <span class="sku-col-name">规格组合</span>
              <span class="sku-col-price">价格</span>
              <span class="sku-col-original">原价</span>
              <span class="sku-col-stock">库存</span>
            </div>
            <div v-for="(sku, index) in skuList" :key="sku.id || index" class="sku-table-row">
              <span class="sku-name">{{ sku.skuName }}</span>
              <div class="sku-col-price">
                <input type="number" v-model="sku.price" min="0" step="0.01" placeholder="¥" />
              </div>
              <div class="sku-col-original">
                <input type="number" v-model="sku.originalPrice" min="0" step="0.01" placeholder="¥" />
              </div>
              <div class="sku-col-stock">
                <input type="number" v-model="sku.stock" min="0" placeholder="库存" />
              </div>
            </div>
            <div v-if="skuList.length === 0" class="sku-empty">
              暂无规格信息
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

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

interface SkuItem {
  id?: number
  skuName: string
  specInfo?: Record<string, string>
  price: number | null
  originalPrice: number | null
  stock: number | null
  skuImage?: string
  sortOrder?: number
}

const productId = ref<number>(0)

const formData = reactive({
  name: '',
  brand: '',
  description: '',
  categoryId: '',
  status: 1
})

const allCategories = ref<Category[]>([])
const level1Categories = ref<Category[]>([])
const level2Categories = ref<Category[]>([])
const selectedLevel1 = ref('')
const selectedLevel2 = ref('')

const imageList = ref<ProductImage[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)

const skuList = ref<SkuItem[]>([])

const isSubmitting = ref(false)
const isLoading = ref(true)

const loadAllCategories = async () => {
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      allCategories.value = response.data.categories
      level1Categories.value = allCategories.value.filter(
        cat => cat.parentId === null && cat.isActive
      )
    }
  } catch (error) {
    Message.error('加载分类失败')
  }
}

const setCategoryByLevel2Id = (categoryId: number) => {
  const level2Cat = allCategories.value.find(cat => cat.id === categoryId)
  if (level2Cat && level2Cat.parentId) {
    selectedLevel1.value = String(level2Cat.parentId)
    loadLevel2Categories()
    selectedLevel2.value = String(categoryId)
    formData.categoryId = String(categoryId)
  }
}

const loadLevel2Categories = () => {
  if (!selectedLevel1.value) {
    level2Categories.value = []
    return
  }

  level2Categories.value = allCategories.value.filter(
    cat => cat.parentId === Number(selectedLevel1.value) && cat.isActive
  )
}

const loadProductDetail = async () => {
  isLoading.value = true

  try {
    productId.value = Number(route.params.productId)

    if (!productId.value) {
      Message.error('商品ID不存在')
      router.push('/seller/products')
      return
    }

    await loadAllCategories()

    const productResponse = await authAPI.getProduct(productId.value)
    if (productResponse.success && productResponse.data?.product) {
      const product = productResponse.data.product

      formData.name = product.name || ''
      formData.brand = product.brand || ''
      formData.description = product.description || ''
      formData.status = product.status ?? 1
      formData.categoryId = product.categoryId || ''

      if (product.categoryId) {
        setCategoryByLevel2Id(Number(product.categoryId))
      }

      if (product.images && product.images !== 'null') {
        const imageUrls = product.images.split(',')
        imageList.value = imageUrls.map((url, index) => ({
          id: Date.now() + index,
          url: url,
          isNew: false
        }))
      }
    } else {
      throw new Error(productResponse.message || '加载商品失败')
    }

    const skuResponse = await authAPI.getProductSkus(productId.value)
    if (skuResponse.success && skuResponse.data?.skus) {
      skuList.value = skuResponse.data.skus.map((sku: any) => ({
        id: sku.id,
        skuName: sku.skuName,
        specInfo: sku.specInfo ? JSON.parse(sku.specInfo) : {},
        price: sku.price,
        originalPrice: sku.originalPrice,
        stock: sku.stock,
        skuImage: sku.skuImage,
        sortOrder: sku.sortOrder
      }))
    }

  } catch (error: any) {
    Message.error(error.message || '加载商品失败')
    router.push('/seller/products')
  } finally {
    isLoading.value = false
  }
}

const onLevel1Change = () => {
  selectedLevel2.value = ''
  formData.categoryId = ''

  if (!selectedLevel1.value) {
    level2Categories.value = []
    return
  }

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
    Message.error('最多只能上传5张图片！')
    return
  }

  for (const file of files) {
    if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
      Message.error(`文件 ${file.name} 格式不支持`)
      continue
    }

    if (file.size > 5 * 1024 * 1024) {
      Message.error(`文件 ${file.name} 超过5MB`)
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

const validateForm = (): boolean => {
  if (!formData.name.trim()) {
    Message.error('请输入商品名称')
    return false
  }
  if (!formData.categoryId) {
    Message.error('请选择商品分类')
    return false
  }
  if (imageList.value.length === 0) {
    Message.error('请至少上传一张商品图片')
    return false
  }
  for (const sku of skuList.value) {
    if (!sku.price || sku.price < 0.01) {
      Message.error('SKU价格不能低于0.01元')
      return false
    }
    if (sku.stock == null || sku.stock < 0) {
      Message.error('SKU库存不能为负数')
      return false
    }
  }
  return true
}

const submitForm = async () => {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    const formDataObj = new FormData()

    const productData: any = {
      name: formData.name.trim(),
      brand: formData.brand.trim(),
      description: formData.description.trim(),
      categoryId: parseInt(formData.categoryId, 10),
      status: formData.status,
      skus: skuList.value.map(sku => ({
        id: sku.id,
        skuName: sku.skuName,
        specInfo: sku.specInfo,
        price: sku.price,
        originalPrice: sku.originalPrice,
        stock: sku.stock
      }))
    }

    formDataObj.append('products', new Blob([JSON.stringify(productData)], { type: 'application/json' }))

    imageList.value.forEach((img) => {
      if (img.isNew && img.file) {
        formDataObj.append('images', img.file)
      }
    })

    const response = await authAPI.updateProduct(productId.value, formDataObj)

    if (response.success) {
      Message.success('保存成功')
      setTimeout(() => {
        router.push('/seller/products')
      }, 1500)
    } else {
      throw new Error(response.message || '保存失败')
    }
  } catch (error: any) {
    Message.error(error.message || '保存失败')
  } finally {
    isSubmitting.value = false
  }
}

const goBack = () => {
  router.push('/seller/products')
}

onMounted(() => {
  if (!authStore.validateSellerPermission()) return

  loadProductDetail()
})
</script>

<style scoped>
@import url('@/static/css/seller/商品编辑页.css');
</style>
