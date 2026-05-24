<template>
  <div class="add-product-container">

    <div v-if="isLoadingCategories" class="skeleton-form">
      <div class="skeleton" style="height: 60px; margin-bottom: 24px; border-radius: 8px;"></div>
      <div class="section-card">
        <div class="section-body">
          <div class="skeleton-form-grid">
            <div class="skeleton-form-group">
              <div class="skeleton skeleton-label"></div>
              <div class="skeleton skeleton-input"></div>
            </div>
            <div class="skeleton-form-group">
              <div class="skeleton skeleton-label"></div>
              <div class="skeleton skeleton-input"></div>
            </div>
            <div class="skeleton-form-group" style="grid-column: span 2;">
              <div class="skeleton skeleton-label"></div>
              <div class="skeleton skeleton-textarea"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template v-else>
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
      <div class="step" :class="{ active: currentStep >= 3, completed: currentStep > 3 }">
        <span class="step-num">3</span>
        <span class="step-text">图片详情</span>
      </div>
      <div class="step-line" :class="{ active: currentStep > 3 }"></div>
      <div class="step" :class="{ active: currentStep >= 4 }">
        <span class="step-num">4</span>
        <span class="step-text">其他信息</span>
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
                  <span v-for="(value, valueIndex) in spec.values" :key="valueIndex" class="spec-value-tag" @dblclick="startEditSpecValue(specIndex, valueIndex, value)">
                    <span v-if="editingSpecValue?.specIndex === specIndex && editingSpecValue?.valueIndex === valueIndex">
                      <input
                        v-model="editingSpecValue.value"
                        @blur="saveSpecValueEdit"
                        @keyup.enter="saveSpecValueEdit"
                        class="spec-value-edit-input"
                        autofocus
                      />
                    </span>
                    <template v-else>
                      <img v-if="getSpecImage(spec.name, value)" :src="getSpecImage(spec.name, value)" class="spec-value-img" />
                      <span>{{ value }}</span>
                      <i class="fas fa-chevron-up" v-if="spec.values.length > 1 && valueIndex > 0" @click.stop="moveSpecValueUp(specIndex, valueIndex)" title="上移"></i>
                      <i class="fas fa-chevron-down" v-if="spec.values.length > 1 && valueIndex < spec.values.length - 1" @click.stop="moveSpecValueDown(specIndex, valueIndex)" title="下移"></i>
                      <i class="fas fa-pen" @click.stop="startEditSpecValue(specIndex, valueIndex, value)" title="编辑"></i>
                      <i class="fas fa-times" @click.stop="removeSpecValue(specIndex, valueIndex)"></i>
                      <i class="fas fa-camera" @click.stop="uploadSpecImage(spec.name, value, specIndex)" title="设置规格图片"></i>
                    </template>
                  </span>
                  <input
                    type="text"
                    class="spec-value-input"
                    v-model="spec.newValue"
                    @keyup.enter="addSpecValue(specIndex)"
                    placeholder="输入后回车添加"
                  />
                </div>
                <!-- 批量添加规格值 -->
                <div class="batch-add-spec-values">
                  <input
                    type="text"
                    class="form-control"
                    v-model="spec.batchInput"
                    @keyup.enter="batchAddSpecValues(specIndex)"
                    placeholder="批量添加：用逗号或空格分隔，如 红色,蓝色,黑色"
                  />
                  <button class="btn-batch-add" @click="batchAddSpecValues(specIndex)">批量添加</button>
                  <small class="batch-add-hint">支持逗号、空格或中文逗号分隔</small>
                </div>
            <!-- 规格图片上传 -->
            <input type="file"
              :ref="el => { if (el) specImageInputRefs[specIndex] = el as any }"
              :data-spec-index="specIndex"
              accept="image/*"
              style="display:none"
              @change="(e) => handleSpecImageUpload(e, specIndex)" />
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
                  <img v-if="sku.skuImagePreview" :src="sku.skuImagePreview" />
                  <img v-else-if="sku.skuImage" :src="sku.skuImage" />
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
                <input type="number" v-model="sku.price" min="0" step="0.01" placeholder="价格" />
              </div>
              <div class="sku-col-original">
                <input type="number" v-model="sku.originalPrice" min="0" step="0.01" placeholder="原价" />
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
        <!-- 商品主图上传 -->
        <div class="form-group">
          <label class="form-label required">商品主图</label>
          <div class="upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="onFileDrop">
            <i class="fas fa-cloud-upload-alt"></i>
            <p>点击或拖拽上传</p>
            <small>支持 JPG、PNG，最多15张，每张不超过5MB</small>
          </div>
          <input type="file" ref="fileInput" multiple accept="image/jpeg,image/png" style="display:none" @change="handleFileSelect" />
          <div class="image-preview-list" v-if="productImages.length > 0">
            <div
              v-for="(image, index) in productImages"
              :key="image.id"
              class="image-preview-item"
              :class="{ 'drag-active': dragIndex === index }"
              draggable="true"
              @dragstart="onDragStart(index)"
              @dragover.prevent="onDragOver(index)"
              @dragleave="onDragLeave"
              @drop="onDrop(index)"
            >
              <img :src="image.url" />
              <button class="remove-btn" @click="removeImage(index)"><i class="fas fa-times"></i></button>
              <span v-if="index === 0" class="cover-badge">封面</span>
              <span class="drag-handle">⠿</span>
            </div>
          </div>
        </div>

        <!-- 商品详情富文本 -->
        <div class="form-group">
          <label class="form-label">商品详情</label>
          <div class="editor-toolbar">
            <button @click="execCommand('bold')" title="加粗"><i class="fas fa-bold"></i></button>
            <button @click="execCommand('italic')" title="斜体"><i class="fas fa-italic"></i></button>
            <button @click="execCommand('underline')" title="下划线"><i class="fas fa-underline"></i></button>
            <button @click="execCommand('insertUnorderedList')" title="无序列表"><i class="fas fa-list-ul"></i></button>
            <button @click="execCommand('insertOrderedList')" title="有序列表"><i class="fas fa-list-ol"></i></button>
            <button @click="execCommand('formatBlock', '<h3>')" title="标题"><i class="fas fa-heading"></i></button>
            <button @click="triggerDetailImage" title="插入图片"><i class="fas fa-image"></i></button>
            <input type="file" ref="detailImageInput" accept="image/*" style="display:none" @change="insertDetailImage" />

          </div>
          <div
            class="rich-editor"
            contenteditable="true"
            ref="richEditor"
            @input="onEditorInput"
            placeholder="请输入商品详情（支持富文本编辑）"
          ></div>
          <small class="form-hint">支持文字排版和图片插入，用于展示商品细节、使用说明等</small>
        </div>

        <div class="step-actions">
          <button class="btn-prev" @click="prevStep">上一步</button>
          <button class="btn-next" @click="nextStep">下一步</button>
        </div>
      </div>

      <!-- 第4步-其他信息 -->
      <div v-if="currentStep === 4" class="step-panel">
        <!-- 发货信息 -->
        <div class="form-section">
          <h3 class="section-title"><i class="fas fa-truck"></i> 发货信息</h3>

          <div class="form-group">
            <label class="form-label">发货城市</label>
            <input type="text" class="form-control" v-model="productForm.deliveryCity" placeholder="如：广东广州" maxlength="50" />
          </div>

          <div class="form-group">
            <label class="form-label">商品重量（kg）</label>
            <input type="number" class="form-control" v-model="productForm.weight" placeholder="如：0.5" min="0" step="0.001" />
          </div>


        </div>

        <!-- 服务保障 -->
        <div class="form-section">
          <h3 class="section-title"><i class="fas fa-shield-alt"></i> 服务保障</h3>

          <div class="service-capsule-grid">
            <label
              v-for="item in serviceOptions"
              :key="item.value"
              class="service-capsule"
              :class="{ active: productForm.serviceGuarantee.includes(item.value) }"
            >
              <input
                type="checkbox"
                :value="item.value"
                v-model="productForm.serviceGuarantee"
              />
              <span>{{ item.label }}</span>
              <i class="fas fa-check capsule-icon"></i>
            </label>
          </div>
          <div class="custom-service-input">
            <input
              type="text"
              class="form-control"
              v-model="customService"
              @keyup.enter="addCustomService"
              placeholder="自定义服务保障，回车添加"
              maxlength="20"
            />
            <button type="button" class="btn btn-outline btn-sm" @click="addCustomService">添加</button>
          </div>
          <!-- 已选服务保障标签 -->
          <div class="selected-capsules" v-if="productForm.serviceGuarantee.length > 0">
            <span v-for="(item, idx) in productForm.serviceGuarantee" :key="idx" class="service-tag">
              {{ item }}
              <i class="fas fa-times" @click="removeService(idx)"></i>
            </span>
          </div>
        </div>

        <!-- 商品参数 -->
        <div class="form-section">
          <h3 class="section-title"><i class="fas fa-list-ul"></i> 商品参数</h3>
          <div class="param-list">
            <div v-for="(param, index) in productParams" :key="index" class="param-row">
              <input type="text" class="param-name-input" v-model="param.name" placeholder="参数名（如屏幕尺寸）" maxlength="20" />
              <span class="param-separator">：</span>
              <input type="text" class="param-value-input" v-model="param.value" placeholder="参数值（如6.7英寸）" maxlength="100" />
              <button class="btn-remove-param" @click="removeParam(index)" :disabled="productParams.length <= 1">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>
          <button class="btn-add-param" @click="addParam">
            <i class="fas fa-plus"></i> 添加参数
          </button>
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
    </template>
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
  batchInput: string
}

interface SkuItem {
  skuName: string
  specInfo: Record<string, string>
  price: number | null
  originalPrice: number | null
  stock: number | null
  skuImage?: string
  skuImageFile?: File | null
  skuImagePreview?: string    // 临时预览用的图片（URL.createObjectURL，只用于前端显示）
}

interface ParamItem {
  name: string
  value: string
}

const currentStep = ref(1)
const isLoadingCategories = ref(true)

const productForm = reactive({
  name: '',
  brand: '',
  description: '',
  categoryId: '',
  deliveryCity: '',
  weight: null as number | null,
  isFreeShipping: false,
  serviceGuarantee: [] as string[]
})

const selectedLevel1 = ref('')
const selectedLevel2 = ref('')
const allCategories = ref<Category[]>([])
const level1Categories = ref<Category[]>([])
const level2Categories = ref<Category[]>([])

// 服务保障联动包邮设置
watch(() => productForm.serviceGuarantee, (val) => {
  productForm.isFreeShipping = val.includes('全国包邮')
}, { deep: true })

// 默认初始化一个规格项
const specItems = ref<SpecItem[]>([{ name: '默认', values: ['单品'], newValue: '', batchInput: '' }])

// 规格值编辑状态
const editingSpecValue = ref<{ specIndex: number; valueIndex: number; value: string } | null>(null)
const generatedSkus = ref<SkuItem[]>([])
const batchPrice = ref<number | null>(null)
const batchStock = ref<number | null>(null)
const skuImageInputs = ref<(HTMLInputElement | null)[]>([])

const productImages = ref<ProductImage[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const isSubmitting = ref(false)

const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

const onDragStart = (index: number) => {
  dragIndex.value = index
}

const onDragOver = (index: number) => {
  dragOverIndex.value = index
}

const onDragLeave = () => {
  dragOverIndex.value = null
}

const onDrop = (index: number) => {
  if (dragIndex.value === null || dragIndex.value === index) return

  const list = [...productImages.value]
  const [movedItem] = list.splice(dragIndex.value, 1)
  if (movedItem) {
    list.splice(index, 0, movedItem)
  }
  productImages.value = list

  dragIndex.value = null
  dragOverIndex.value = null
}

// 富文本编辑器
const richEditor = ref<HTMLDivElement | null>(null)
const detailHtmlContent = ref('')
const detailImageInput = ref<HTMLInputElement | null>(null)
const customService = ref('')
const productParams = ref<{ name: string; value: string }[]>([{ name: '', value: '' }])

const allServiceOptions = [
  { label: '7天无理由退货', value: '7天无理由退货', categoryIds: [] },
  { label: '假一赔十', value: '假一赔十', categoryIds: [] },
  { label: '运费险', value: '运费险', categoryIds: [] },
  { label: '极速退款', value: '极速退款', categoryIds: [] },
  { label: '全国包邮', value: '全国包邮', categoryIds: [] },
  { label: '坏单包赔', value: '坏单包赔', categoryIds: [6, 11, 15, 16] },
  { label: '顺丰冷链', value: '顺丰冷链', categoryIds: [6, 11, 15, 16, 17] },
  { label: '化冻包赔', value: '化冻包赔', categoryIds: [6, 11] },
  { label: '48小时发货', value: '48小时发货', categoryIds: [6, 11, 15, 16] },
  { label: '官方验机', value: '官方验机', categoryIds: [1, 2] },
  { label: '价保30天', value: '价保30天', categoryIds: [1, 2, 3, 20] },
  { label: '全国联保', value: '全国联保', categoryIds: [1, 2, 3] },
  { label: '上门安装', value: '上门安装', categoryIds: [3, 8, 14] },
  { label: '专业鉴定', value: '专业鉴定', categoryIds: [20] },
  { label: '过敏包退', value: '过敏包退', categoryIds: [5] },
]

const serviceOptions = computed(() => {
  if (!selectedLevel1.value) {
    return allServiceOptions.filter(s => s.categoryIds.length === 0)
  }
  const level1Id = Number(selectedLevel1.value)
  return allServiceOptions.filter(s => {
    if (s.categoryIds.length === 0) return true
    return s.categoryIds.includes(level1Id)
  })
})

const canNextStep1 = computed(() => {
  return productForm.name.trim() && selectedLevel2.value
})

const loadCategories = async () => {
  isLoadingCategories.value = true
  try {
    const response = await authAPI.getAllCategories()
    if (response.success && response.data?.categories) {
      allCategories.value = response.data.categories
      level1Categories.value = allCategories.value.filter(cat => cat.parentId === null && cat.isActive)
    }
  } catch {
    Message.error('加载分类失败')
  } finally {
    isLoadingCategories.value = false
  }
}

const onLevel1Change = () => {
  selectedLevel2.value = ''
  level2Categories.value = []
  productForm.categoryId = ''
  
  if (selectedLevel1.value) {
    level2Categories.value = allCategories.value.filter(
      cat => cat.parentId === Number(selectedLevel1.value) && cat.isActive
    )
  }
  
  const validValues = serviceOptions.value.map(s => s.value)
  productForm.serviceGuarantee = productForm.serviceGuarantee.filter(v => validValues.includes(v))
}

const onLevel2Change = () => {
  productForm.categoryId = selectedLevel2.value
}

const addSpecItem = () => {
  specItems.value.push({ name: '', values: [], newValue: '', batchInput: '' })
}

const removeSpecItem = (index: number) => {
  if (specItems.value.length > 1) {
    specItems.value.splice(index, 1)
  }
}

const addSpecValue = (specIndex: number) => {
  const spec = specItems.value[specIndex]
  if (spec && spec.newValue.trim()) {
    spec.values.push(spec.newValue.trim())
    spec.newValue = ''
    generateSkus()
  }
}

const removeSpecValue = (specIndex: number, valueIndex: number) => {
  const spec = specItems.value[specIndex]
  if (spec && spec.values) {
    spec.values.splice(valueIndex, 1)
    generateSkus()
  }
}

// 开始编辑规格值
const startEditSpecValue = (specIndex: number, valueIndex: number, value: string) => {
  editingSpecValue.value = { specIndex, valueIndex, value }
}

// 保存规格值编辑
const saveSpecValueEdit = () => {
  if (!editingSpecValue.value) return
  const { specIndex, valueIndex, value } = editingSpecValue.value
  if (value.trim()) {
    if (specItems.value[specIndex]?.values) {
      specItems.value[specIndex].values[valueIndex] = value.trim()
      generateSkus()
    }
  }
  editingSpecValue.value = null
}

// 批量添加规格值
const batchAddSpecValues = (specIndex: number) => {
  const spec = specItems.value[specIndex]
  if (!spec || !spec.batchInput.trim()) return

  const values = spec.batchInput.split(/[ ,，]+/).filter(v => v.trim())
  const existingValues = new Set(spec.values)

  values.forEach(v => {
    const trimmedValue = v.trim()
    if (trimmedValue && !existingValues.has(trimmedValue)) {
      spec.values.push(trimmedValue)
    }
  })

  spec.batchInput = ''
  generateSkus()
}

// 上移规格值
const moveSpecValueUp = (specIndex: number, valueIndex: number) => {
  if (valueIndex <= 0) return
  const spec = specItems.value[specIndex]
  if (!spec) return
  const temp = spec.values[valueIndex]
  spec.values[valueIndex] = spec.values[valueIndex - 1] || ''
  spec.values[valueIndex - 1] = temp || ''
  generateSkus()
}

// 下移规格值
const moveSpecValueDown = (specIndex: number, valueIndex: number) => {
  const spec = specItems.value[specIndex]
  if (!spec) return
  if (valueIndex >= spec.values.length - 1) return
  const temp = spec.values[valueIndex]
  spec.values[valueIndex] = spec.values[valueIndex + 1] || ''
  spec.values[valueIndex + 1] = temp || ''
  generateSkus()
}

// 规格图片预览 URL 映射：{ "颜色:红色": "blobUrl" }
const specImageMap = ref<Record<string, string>>({})
// 规格图片文件映射：{ "颜色:红色": File }
const specImageFileMap = ref<Record<string, File>>({})
const specImageInputRefs = ref<Record<number, HTMLInputElement>>({})
let currentSpecName = ''
let currentSpecValue = ''

// 上传规格图片
const uploadSpecImage = (specName: string, specValue: string, specIndex: number) => {
  currentSpecName = specName
  currentSpecValue = specValue
  specImageInputRefs.value[specIndex]?.click()
}

const handleSpecImageUpload = (e: Event, specIndex: number) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 使用 URL.createObjectURL 生成预览 URL，不转 Base64
  const previewUrl = URL.createObjectURL(file)
  const key = `${currentSpecName}:${currentSpecValue}`
  specImageMap.value[key] = previewUrl
  specImageFileMap.value[key] = file

  syncSpecImagesForIndex(specIndex)
  input.value = ''
}

// 同步指定规格的图片到所有相关 SKU
const syncSpecImagesForIndex = (specIndex: number) => {
  const spec = specItems.value[specIndex]
  if (!spec) return

  generatedSkus.value.forEach(sku => {
    const specInfo = sku.specInfo || {}
    const specValue = specInfo[spec.name]
    if (specValue) {
      const key = `${spec.name}:${specValue}`
      if (specImageMap.value[key]) {
        // 赋值给预览字段，不污染 skuImage（数据库字段）
        sku.skuImagePreview = specImageMap.value[key]
        // 保存文件对象，用于提交时上传
        sku.skuImageFile = specImageFileMap.value[key]
      }
    }
  })
}

// 同步规格图片到 SKU
const syncSkuImages = () => {
  generatedSkus.value.forEach(sku => {
    const specInfo = sku.specInfo || {}
    for (const [specName, specValue] of Object.entries(specInfo)) {
      const key = `${specName}:${specValue}`
      if (specImageMap.value[key]) {
        // 赋值给预览字段，不污染 skuImage（数据库字段）
        sku.skuImagePreview = specImageMap.value[key]
        // 保存文件对象，用于提交时上传
        sku.skuImageFile = specImageFileMap.value[key]
      }
    }
  })
}

// 获取规格图片
const getSpecImage = (specName: string, specValue: string) => {
  const key = `${specName}:${specValue}`
  return specImageMap.value[key] || ''
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
      specInfo[name] = combo[i] || ''
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
  const sku = generatedSkus.value[index]
  if (!sku) return

  if (file) {
    // 使用 URL.createObjectURL 生成临时预览 URL，不转 Base64
    sku.skuImagePreview = URL.createObjectURL(file)
    sku.skuImageFile = file
  } else {
    sku.skuImageFile = null
  }
  input.value = ''
}

watch(specItems, generateSkus, { deep: true })

// 富文本内容保存
const savedEditorContent = ref('')

// 监听步骤切换，保存和恢复富文本内容
watch(currentStep, (newStep, oldStep) => {
  // 离开第3步时保存
  if (oldStep === 3 && richEditor.value) {
    savedEditorContent.value = richEditor.value.innerHTML || ''
    detailHtmlContent.value = savedEditorContent.value
  }
  // 回到第3步时恢复
  if (newStep === 3 && richEditor.value) {
    if (savedEditorContent.value) {
      richEditor.value.innerHTML = savedEditorContent.value
    } else if (detailHtmlContent.value) {
      richEditor.value.innerHTML = detailHtmlContent.value
    }
  }
})

const nextStep = () => {
  if (currentStep.value === 1) {
    if (!productForm.name.trim() || !selectedLevel2.value) {
      Message.error('请完善基本信息')
      return
    }
  }
  if (currentStep.value === 2) {
    if (generatedSkus.value.length === 0) {
      Message.error('请完善规格信息')
      return
    }
    const hasInvalidSku = generatedSkus.value.some(
      sku => !sku.price || sku.price <= 0 || sku.stock === null || sku.stock < 0
    )
    if (hasInvalidSku) {
      Message.error('请完善所有SKU的价格和库存（价格必须大于0，库存不能为负）')
      return
    }
  }
  if (currentStep.value === 3) {
    if (productImages.value.length === 0) {
      Message.error('请至少上传一张商品主图')
      return
    }
  }
  if (currentStep.value < 4) currentStep.value++
}

const prevStep = () => {
  if (currentStep.value > 1) currentStep.value--
}

const execCommand = (command: string, value?: string) => {
  document.execCommand(command, false, value)
  richEditor.value?.focus()
}

const onEditorInput = () => {
  if (richEditor.value) {
    detailHtmlContent.value = richEditor.value.innerHTML || ''
  }
}

const triggerDetailImage = () => {
  detailImageInput.value?.click()
}

const insertDetailImage = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    const img = document.createElement('img')
    img.src = event.target?.result as string
    img.style.maxWidth = '100%'
    richEditor.value?.focus()
    document.execCommand('insertImage', false, event.target?.result as string)
  }
  reader.readAsDataURL(file)
  input.value = ''
}

const addCustomService = () => {
  const value = customService.value.trim()
  if (value && !productForm.serviceGuarantee.includes(value)) {
    productForm.serviceGuarantee.push(value)
    customService.value = ''
  }
}

const removeService = (index: number) => {
  productForm.serviceGuarantee.splice(index, 1)
}

const addParam = () => {
  productParams.value.push({ name: '', value: '' })
}

const removeParam = (index: number) => {
  if (productParams.value.length > 1) {
    productParams.value.splice(index, 1)
  }
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
  if (productImages.value.length + files.length > 15) {
    Message.error('最多上传15张图片')
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
  isSubmitting.value = true
  try {
    const formData = new FormData()

    const productData: any = {
      name: productForm.name.trim(),
      brand: productForm.brand.trim(),
      description: productForm.description.trim(),
      categoryId: parseInt(productForm.categoryId),
      detailHtml: detailHtmlContent.value,
      deliveryCity: productForm.deliveryCity.trim(),
      weight: productForm.weight,
      isFreeShipping: productForm.isFreeShipping,
      serviceGuarantee: productForm.serviceGuarantee.join(','),
      skus: generatedSkus.value.map((sku, index) => ({
        skuName: sku.skuName,
        specInfo: sku.specInfo,
        price: sku.price,
        originalPrice: sku.originalPrice,
        stock: sku.stock,
        skuImage: '',
        skuImageIndex: sku.skuImageFile ? index : -1
      })),
      params: productParams.value.filter(p => p.name.trim() && p.value.trim()).map((p, index) => ({
        name: p.name.trim(),
        value: p.value.trim(),
        sortOrder: index
      }))
    }

    formData.append('products', new Blob([JSON.stringify(productData)], { type: 'application/json' }))
    productImages.value.forEach(img => formData.append('images', img.file))

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

onMounted(async () => {
  // 先检查套餐权限
  try {
    const packageStatus = await authAPI.checkActivePackage()

    if (!packageStatus.data?.active) {
      Message.warning('套餐已过期，请续费后发布商品')
      router.push({ name: 'SellerPackage' })
      return
    }
  } catch (error) {
    Message.error('检查套餐状态失败')
    router.push({ name: 'SellerPackage' })
    return
  }

  loadCategories()
  generateSkus() // 生成初始SKU

  // 初始化富文本内容
  if (richEditor.value && detailHtmlContent.value) {
    richEditor.value.innerHTML = detailHtmlContent.value
  }
})
</script>

<style scoped>
@import url('@/static/css/common/骨架屏.css');
@import url('@/static/css/seller/商品发布页.css');
</style>
