<template>
  <div class="add-product-container">
    <div class="page-header">
      <i class="fas fa-arrow-left" @click="$router.back()"></i>
      <span>发布商品</span>
      <span class="placeholder"></span>
    </div>

    <div class="steps-bar">
      <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
        <span class="step-num">1</span>
        <span class="step-text">基本信息</span>
      </div>
      <div class="step-line" :class="{ active: currentStep > 1 }"></div>
      <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
        <span class="step-num">2</span>
        <span class="step-text">规格设置</span>
      </div>
      <div class="step-line" :class="{ active: currentStep > 2 }"></div>
      <div class="step" :class="{ active: currentStep >= 3 }">
        <span class="step-num">3</span>
        <span class="step-text">图片上传</span>
      </div>
    </div>

    <div class="form-content">
      <div v-if="currentStep === 1" class="step-panel">
        <div class="form-group">
          <label class="form-label required">商品名称</label>
          <input type="text" class="form-control" v-model="productForm.name" maxlength="100" placeholder="请输入商品名称" />
        </div>
        <div class="form-group">
          <label class="form-label">商品品牌</label>
          <input type="text" class="form-control" v-model="productForm.brand" maxlength="50" placeholder="请输入商品品牌" />
        </div>
        <div class="form-group">
          <label class="form-label">商品描述</label>
          <textarea class="form-control" v-model="productForm.description" rows="4" maxlength="500" placeholder="请输入商品详细描述"></textarea>
          <small class="form-hint">{{ productForm.description.length }}/500</small>
        </div>
        <div class="form-group">
          <label class="form-label required">商品分类</label>
          <div class="category-selector">
            <select class="form-control" v-model="selectedLevel1" @change="onLevel1Change">
              <option value="">请选择一级分类</option>
              <option v-for="cat in level1Categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
            <select class="form-control" v-model="selectedLevel2" @change="onLevel2Change" :disabled="!selectedLevel1">
              <option value="">请选择二级分类</option>
              <option v-for="cat in level2Categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
        </div>
        <button class="btn-next" @click="nextStep" :disabled="!canNextStep1">下一步</button>
      </div>

      <div v-if="currentStep === 2" class="step-panel">
        <div class="spec-section">
          <div class="spec-header">
            <span>规格设置</span>
            <button class="btn-add-spec" @click="addSpecItem">
              <i class="fas fa-plus"></i> 添加规格项
            </button>
          </div>
          <div v-for="(spec, specIndex) in specItems" :key="specIndex" class="spec-item">
            <div class="spec-item-header">
              <input type="text" class="spec-name-input" v-model="spec.name" placeholder="规格名（如颜色）" />
              <button class="btn-remove-spec" @click="removeSpecItem(specIndex)" :disabled="specItems.length <= 1">
                <i class="fas fa-trash"></i>
              </button>
            </div>
            <div class="spec-values">
              <span v-for="(value, valueIndex) in spec.values" :key="valueIndex" class="spec-value-tag">
                {{ value }}
                <i class="fas fa-times" @click="removeSpecValue(specIndex, valueIndex)"></i>
              </span>
              <input
                type="text"
                class="spec-value-input"
                v-model="spec.newValue"
                @keyup.enter="addSpecValue(specIndex)"
                placeholder="输入后回车添加"
              />
            </div>
          </div>
        </div>

        <div v-if="generatedSkus.length > 0" class="sku-list-section">
          <div class="sku-list-header">
            <span>SKU列表（{{ generatedSkus.length }}个）</span>
            <div class="batch-fill">
              <input type="number" v-model="batchPrice" placeholder="批量价格" />
              <input type="number" v-model="batchStock" placeholder="批量库存" />
              <button @click="batchFillSkus">应用</button>
            </div>
          </div>
          <div class="sku-table">
            <div class="sku-table-header">
              <span class="sku-col-image">图片</span>
              <span class="sku-col-name">规格组合</span>
              <span class="sku-col-price">价格</span>
              <span class="sku-col-original">原价</span>
              <span class="sku-col-stock">库存</span>
            </div>
            <div v-for="(sku, index) in generatedSkus" :key="index" class="sku-table-row">
              <div class="sku-col-image">
                <div class="sku-image-upload" @click="triggerSkuImageUpload(index)">
                  <img v-if="sku.skuImage" :src="sku.skuImage" />
                  <i v-else class="fas fa-image"></i>
                </div>
                <input
                  type="file"
                  :ref="el => skuImageInputs[index] = el as HTMLInputElement"
                  accept="image/*"
                  style="display:none"
                  @change="(e) => handleSkuImageSelect(e, index)"
                />
              </div>
              <span class="sku-col-name">{{ sku.skuName }}</span>
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
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-prev" @click="prevStep">上一步</button>
          <button class="btn-next" @click="nextStep">下一步</button>
        </div>
      </div>

      <div v-if="currentStep === 3" class="step-panel">
        <div class="form-group">
          <label class="form-label required">商品主图</label>
          <div class="upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="onFileDrop">
            <i class="fas fa-cloud-upload-alt"></i>
            <p>点击或拖拽上传</p>
            <small>支持 JPG、PNG，最多5张</small>
          </div>
          <input type="file" ref="fileInput" multiple accept="image/jpeg,image/png" style="display:none" @change="handleFileSelect" />
          <div class="image-preview-list" v-if="productImages.length > 0">
            <div v-for="(image, index) in productImages" :key="image.id" class="image-preview-item">
              <img :src="image.url" />
              <button class="remove-btn" @click="removeImage(index)"><i class="fas fa-times"></i></button>
            </div>
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-prev" @click="prevStep">上一步</button>
          <button class="btn-submit" @click="submitProduct" :disabled="isSubmitting">
            <i v-if="isSubmitting" class="fas fa-spinner fa-spin"></i>
            {{ isSubmitting ? '发布中...' : '发布商品' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'

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
  file: File
}

interface SpecItem {
  name: string
  values: string[]
  newValue: string
}

interface SkuItem {
  skuName: string
  specInfo: Record<string, string>
  price: number | null
  originalPrice: number | null
  stock: number | null
  skuImage: string
  skuImageFile: File | null
}

const currentStep = ref(1)

const productForm = reactive({
  name: '',
  brand: '',
  description: '',
  categoryId: ''
})

const selectedLevel1 = ref('')
const selectedLevel2 = ref('')
const allCategories = ref<Category[]>([])
const level1Categories = ref<Category[]>([])
const level2Categories = ref<Category[]>([])

// 默认初始化一个规格项
const specItems = ref<SpecItem[]>([{ name: '默认', values: ['单品'], newValue: '' }])
const generatedSkus = ref<SkuItem[]>([])
const batchPrice = ref<number | null>(null)
const batchStock = ref<number | null>(null)
const skuImageInputs = ref<(HTMLInputElement | null)[]>([])

const productImages = ref<ProductImage[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const isSubmitting = ref(false)

const canNextStep1 = computed(() => {
  return productForm.name.trim() && selectedLevel2.value
})

const loadCategories = async () => {
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      allCategories.value = response.data.categories
      level1Categories.value = allCategories.value.filter(cat => cat.parentId === null && cat.isActive)
    }
  } catch {
    Message.error('加载分类失败')
  }
}

const onLevel1Change = () => {
  selectedLevel2.value = ''
  level2Categories.value = []
  productForm.categoryId = ''
  if (selectedLevel1.value) {
    level2Categories.value = allCategories.value.filter(cat => cat.parentId === Number(selectedLevel1.value) && cat.isActive)
  }
}

const onLevel2Change = () => {
  productForm.categoryId = selectedLevel2.value
}

const addSpecItem = () => {
  specItems.value.push({ name: '', values: [], newValue: '' })
}

const removeSpecItem = (index: number) => {
  if (specItems.value.length > 1) {
    specItems.value.splice(index, 1)
  }
}

const addSpecValue = (specIndex: number) => {
  const spec = specItems.value[specIndex]
  if (spec.newValue.trim()) {
    spec.values.push(spec.newValue.trim())
    spec.newValue = ''
  }
}

const removeSpecValue = (specIndex: number, valueIndex: number) => {
  specItems.value[specIndex].values.splice(valueIndex, 1)
}

const generateSkus = () => {
  const validSpecs = specItems.value.filter(s => s.name.trim() && s.values.length > 0)
  if (validSpecs.length === 0) {
    generatedSkus.value = []
    return
  }

  const cartesian = (arrays: string[][]): string[][] => {
    return arrays.reduce((acc, arr) => acc.flatMap(x => arr.map(y => [...x, y])), [[]] as string[][])
  }

  const specNames = validSpecs.map(s => s.name)
  const specValues = validSpecs.map(s => s.values)
  const combinations = cartesian(specValues)

  generatedSkus.value = combinations.map(combo => {
    const specInfo: Record<string, string> = {}
    specNames.forEach((name, i) => {
      specInfo[name] = combo[i]
    })
    return {
      skuName: combo.join('-'),
      specInfo,
      price: null,
      originalPrice: null,
      stock: null,
      skuImage: '',
      skuImageFile: null as File | null
    }
  })
}

const batchFillSkus = () => {
  if (batchPrice.value !== null) {
    generatedSkus.value.forEach(sku => sku.price = batchPrice.value)
  }
  if (batchStock.value !== null) {
    generatedSkus.value.forEach(sku => sku.stock = batchStock.value)
  }
}

const triggerSkuImageUpload = (index: number) => {
  skuImageInputs.value[index]?.click()
}

const handleSkuImageSelect = (e: Event, index: number) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (event) => {
      generatedSkus.value[index].skuImage = event.target?.result as string
    }
    reader.readAsDataURL(file)
    generatedSkus.value[index].skuImageFile = file
  } else {
    generatedSkus.value[index].skuImageFile = null
  }
  input.value = ''
}

watch(specItems, generateSkus, { deep: true })

const nextStep = () => {
  if (currentStep.value === 2) {
    if (generatedSkus.value.length === 0) {
      Message.error('请完善规格信息')
      return
    }
    const hasInvalidSku = generatedSkus.value.some(sku => !sku.price || sku.price <= 0 || sku.stock === null || sku.stock < 0)
    if (hasInvalidSku) {
      Message.error('请完善所有SKU的价格和库存')
      return
    }
  }
  if (currentStep.value < 3) currentStep.value++
}

const prevStep = () => {
  if (currentStep.value > 1) currentStep.value--
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files) handleFiles(Array.from(input.files))
  input.value = ''
}

const onFileDrop = (e: DragEvent) => {
  const files = e.dataTransfer?.files
  if (files) handleFiles(Array.from(files))
}

const handleFiles = (files: File[]) => {
  if (productImages.value.length + files.length > 5) {
    Message.error('最多上传5张图片')
    return
  }
  for (const file of files) {
    if (!file.type.match('image/jpeg') && !file.type.match('image/png')) {
      Message.error(`${file.name} 格式不支持`)
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      Message.error(`${file.name} 超过5MB`)
      continue
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      productImages.value.push({ id: Date.now() + Math.random(), url: e.target?.result as string, file })
    }
    reader.readAsDataURL(file)
  }
}

const removeImage = (index: number) => {
  productImages.value.splice(index, 1)
}

const submitProduct = async () => {
  if (productImages.value.length === 0) {
    Message.error('请至少上传一张商品图片')
    return
  }

  isSubmitting.value = true
  try {
    const formData = new FormData()
    const productData: any = {
      name: productForm.name.trim(),
      brand: productForm.brand.trim(),
      description: productForm.description.trim(),
      categoryId: parseInt(productForm.categoryId)
    }

    // 构建SKU数据
    productData.skus = generatedSkus.value.map((sku, index) => ({
      skuName: sku.skuName,
      specInfo: sku.specInfo,
      price: sku.price,
      originalPrice: sku.originalPrice,
      stock: sku.stock,
      skuImage: '',
      skuImageIndex: sku.skuImageFile ? index : -1
    }))

    formData.append('products', new Blob([JSON.stringify(productData)], { type: 'application/json' }))
    productImages.value.forEach(img => formData.append('images', img.file))
    
    // 上传SKU图片
    generatedSkus.value.forEach((sku, index) => {
      if (sku.skuImageFile) {
        formData.append('skuImages', sku.skuImageFile, `sku_${index}_${sku.skuImageFile.name}`)
      }
    })

    const response = await authAPI.addProduct(formData)
    if (response.success) {
      Message.success('商品发布成功')
      router.push({ name: 'SellerProducts' })
    } else {
      throw new Error(response.message)
    }
  } catch (error: any) {
    Message.error(error.message || '发布失败')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  generateSkus() // 生成初始SKU
})
</script>

<style scoped>
@import url('@/static/css/seller/发布商品.css');
</style>
