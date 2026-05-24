<template>
  <div class="edit-product-container">
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-edit"></i>
        编辑商品
      </h1>
    </div>

    <div v-if="isLoading" class="skeleton-form">
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
            <div class="skeleton-form-group">
              <div class="skeleton skeleton-label"></div>
              <div class="skeleton skeleton-textarea"></div>
            </div>
            <div class="skeleton-form-group">
              <div class="skeleton skeleton-label"></div>
              <div class="skeleton skeleton-input"></div>
            </div>
          </div>
          <div class="skeleton skeleton-image" style="margin-top: 20px; height: 200px;"></div>
        </div>
      </div>
      <div class="section-card">
        <div class="section-body">
          <div class="skeleton skeleton-table">
            <div class="skeleton-table-header">
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
            </div>
            <div v-for="i in 3" :key="i" class="skeleton-table-row">
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
              <div class="skeleton" style="flex: 1;"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="content-wrapper">
      <!-- 基本信息区块 -->
      <div class="section-card" :class="{ collapsed: sectionCollapsed.basic }">
        <div class="section-header" @click="toggleSection('basic')">
          <h3 class="section-title">
            <i class="fas fa-info-circle"></i>
            基本信息
          </h3>
          <div class="section-toggle">
            <i class="fas fa-chevron-down"></i>
          </div>
        </div>
        <div class="section-body">
          <div class="form-grid">
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

            <div class="form-group form-group-full">
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
              <small class="form-hint"> 💡 商品详情不填也没关系，系统会自动用描述代替展示哦~</small>
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
              <label class="form-label">发货城市</label>
              <input type="text" class="form-control" v-model="formData.deliveryCity"
                placeholder="如：广东广州" maxlength="50" />
            </div>

            <div class="form-group">
              <label class="form-label">商品重量（kg）</label>
              <input type="number" class="form-control" v-model="formData.weight"
                min="0" step="0.001" placeholder="如：0.5" />
            </div>

            <div class="form-group">
              <label class="form-label">服务保障</label>
              <div class="service-capsule-grid">
                <label
                  v-for="item in serviceOptions"
                  :key="item.value"
                  class="service-capsule"
                  :class="{ active: formData.serviceGuarantee.includes(item.value) }"
                >
                  <input type="checkbox" :value="item.value" v-model="formData.serviceGuarantee" />
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
              <div class="selected-capsules" v-if="formData.serviceGuarantee.length > 0">
                <span v-for="(item, idx) in formData.serviceGuarantee" :key="idx" class="selected-capsule">
                  {{ item }}
                  <i class="fas fa-times" @click="removeService(idx)"></i>
                </span>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">商品参数</label>
              <div class="param-list">
                <div v-for="(param, index) in formData.params" :key="index" class="param-row">
                  <input type="text" v-model="param.name" placeholder="参数名" maxlength="20" />
                  <input type="text" v-model="param.value" placeholder="参数值" maxlength="100" />
                  <button type="button" class="btn-remove" @click="removeParam(index)">
                    <i class="fas fa-times"></i>
                  </button>
                </div>
              </div>
              <button type="button" class="btn-add-param" @click="addParam">
                <i class="fas fa-plus"></i> 添加参数
              </button>
            </div>

            <div class="form-group form-group-full">
              <label class="form-label required">商品图片</label>
              <div
                class="file-upload-area"
                @click="triggerFileInput"
                @dragover.prevent="isDragOver = true"
                @dragleave.prevent="isDragOver = false"
                @drop.prevent="onFileDrop"
                :class="{ 'drag-over': isDragOver }"
              >
                <i class="fas fa-cloud-upload-alt upload-icon"></i>
                <p class="upload-text">点击或拖拽图片到此处上传</p>
                <p class="upload-hint">支持 JPG、PNG 格式，单张不超过 5MB，最多15张</p>
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
                  :class="{ 'drag-active': dragIndex === index }"
                  draggable="true"
                  @dragstart="onDragStart(index)"
                  @dragover.prevent="onDragOver(index)"
                  @dragleave="onDragLeave"
                  @drop="onDrop(index)"
                >
                  <img :src="image.url" :alt="`商品图片${index + 1}`" />
                  <button type="button" class="remove-image-btn" @click="removeImage(index)">
                    <i class="fas fa-times"></i>
                  </button>
                  <div class="image-index">{{ index + 1 }}</div>
                  <span class="drag-handle">⠿</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- SKU规格区块 -->
      <div class="section-card" :class="{ collapsed: sectionCollapsed.sku }">
        <div class="section-header" @click="toggleSection('sku')">
          <h3 class="section-title">
            <i class="fas fa-list"></i>
            SKU规格
          </h3>
          <div class="section-toggle">
            <i class="fas fa-chevron-down"></i>
          </div>
        </div>
        <div class="section-body">
          <div class="sku-edit-section">
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
                      <span class="spec-value-left">
                        <img v-if="getSpecImage(spec.name, value)" :src="getSpecImage(spec.name, value)" class="spec-value-img" />
                        <i v-else class="fas fa-image spec-value-placeholder"></i>
                      </span>
                      <span class="spec-value-text">{{ value }}</span>
                      <i class="fas fa-camera" @click.stop="uploadSpecImage(spec.name, value, specIndex)" title="设置/更换规格图片"></i>
                      <i class="fas fa-chevron-up" v-if="spec.values.length > 1 && valueIndex > 0" @click.stop="moveSpecValueUp(specIndex, valueIndex)" title="上移"></i>
                      <i class="fas fa-chevron-down" v-if="spec.values.length > 1 && valueIndex < spec.values.length - 1" @click.stop="moveSpecValueDown(specIndex, valueIndex)" title="下移"></i>
                      <i class="fas fa-pen" @click.stop="startEditSpecValue(specIndex, valueIndex, value)" title="编辑"></i>
                      <i class="fas fa-times" @click.stop="removeSpecValue(specIndex, valueIndex)"></i>
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
                <input type="file"
                  :ref="el => { if (el) specImageInputRefs[specIndex] = el as any }"
                  :data-spec-index="specIndex"
                  accept="image/*"
                  style="display:none"
                  @change="(e) => handleSpecImageUpload(e, specIndex)" />
              </div>
            </div>

            <div class="batch-fill-bar">
              <span class="batch-fill-title">批量填充</span>
              <div class="batch-fill-inputs">
                <input type="number" v-model="batchPrice" placeholder="批量价格" />
                <input type="number" v-model="batchStock" placeholder="批量库存" />
              </div>
              <button class="batch-fill-btn" @click="batchFillSkus">应用</button>
            </div>

            <div class="sku-table">
              <div class="sku-table-header">
                <span class="sku-row-num">#</span>
                <span class="sku-col-image">图片</span>
                <span class="sku-col-name">规格组合</span>
                <span class="sku-col-price">价格</span>
                <span class="sku-col-original">原价</span>
                <span class="sku-col-stock">库存</span>
              </div>
              <div v-for="(sku, index) in skuList" :key="sku.id || index" class="sku-table-row">
                <div class="sku-row-num">{{ index + 1 }}</div>
                <div class="sku-col-image">
                  <div class="sku-image-upload" @click="triggerSkuImage(index)">
                    <img v-if="sku.skuImagePreview" :src="sku.skuImagePreview" />
                    <img v-else-if="sku.skuImage" :src="sku.skuImage" />
                    <i v-else class="fas fa-image"></i>
                  </div>
                  <input type="file" :ref="(el) => skuImageInputs[index] = el as HTMLInputElement | null"
                    accept="image/*" style="display:none"
                    @change="(e) => handleSkuImageChange(e, index)" />
                </div>
                <div class="sku-col-name">
                  <input type="text" v-model="sku.skuName" class="sku-name-input" placeholder="规格名称" />
                </div>
                <div class="sku-col-price">
                  <input type="number" v-model="sku.price" min="0" step="0.01" placeholder="价格" />
                </div>
                <div class="sku-col-original">
                  <input type="number" v-model="sku.originalPrice" min="0" step="0.01" placeholder="原价" />
                </div>
                <div class="sku-col-stock">
                  <input type="number" v-model="sku.stock" min="0" />
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

    <!-- 悬浮操作栏 -->
    <div class="floating-actions">
      <button class="btn btn-outline" @click="goBack">
        <i class="fas fa-arrow-left"></i>
        <span>返回</span>
      </button>
      <button class="btn btn-primary" @click="submitForm" :disabled="isSubmitting">
        <i v-if="isSubmitting" class="fas fa-spinner fa-spin"></i>
        <i v-else class="fas fa-save"></i>
        {{ isSubmitting ? '保存中...' : '保存修改' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const sortSkusBySpec = (skus: any[]): any[] => {
  return [...skus].sort((a, b) => {
    const aSpec = typeof a.specInfo === 'string' ? JSON.parse(a.specInfo) : (a.specInfo || {})
    const bSpec = typeof b.specInfo === 'string' ? JSON.parse(b.specInfo) : (b.specInfo || {})

    const aValues = Object.values(aSpec)
    const bValues = Object.values(bSpec)

    const aFirstValue = aValues[0] || ''
    const bFirstValue = bValues[0] || ''

    if (aFirstValue !== bFirstValue) {
      return (aFirstValue as string).localeCompare(bFirstValue as string)
    }

    const aSecondValue = aValues[1] || ''
    const bSecondValue = bValues[1] || ''
    return (aSecondValue as string).localeCompare(bSecondValue as string)
  })
}

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
  skuImage?: string       // 已有图片 URL（后端返回的路径）
  skuImageFile?: File | null  // 新图片文件
  skuImageDeleted?: boolean   // 是否删除了图片
  sortOrder?: number
  skuImagePreview?: string    // 临时预览用的图片（URL.createObjectURL，只用于前端显示）
}

interface SpecItem {
  name: string
  values: string[]
  newValue: string
  batchInput: string
}

const productId = ref<number>(0)

const formData = reactive({
  name: '',
  brand: '',
  description: '',
  categoryId: '',
  status: 1,
  deliveryCity: '',
  weight: null as number | null,
  isFreeShipping: false,
  serviceGuarantee: [] as string[],
  params: [] as { name: string; value: string }[]
})

const customService = ref('')
const richEditor = ref<HTMLDivElement | null>(null)
const detailImageInput = ref<HTMLInputElement | null>(null)
const detailHtmlContent = ref('')

const sectionCollapsed = reactive({
  basic: false,
  sku: false
})

const toggleSection = (section: 'basic' | 'sku') => {
  sectionCollapsed[section] = !sectionCollapsed[section]
}

const addCustomService = () => {
  const value = customService.value.trim()
  if (value && !formData.serviceGuarantee.includes(value)) {
    formData.serviceGuarantee.push(value)
    customService.value = ''
  }
}

const removeService = (index: number) => {
  formData.serviceGuarantee.splice(index, 1)
}

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

const addParam = () => {
  formData.params.push({ name: '', value: '' })
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

const removeParam = (index: number) => {
  formData.params.splice(index, 1)
}

const allCategories = ref<Category[]>([])
const level1Categories = ref<Category[]>([])
const level2Categories = ref<Category[]>([])
const selectedLevel1 = ref('')
const selectedLevel2 = ref('')

// 服务保障联动包邮设置
watch(() => formData.serviceGuarantee, (val) => {
  formData.isFreeShipping = val.includes('全国包邮')
}, { deep: true })

const imageList = ref<ProductImage[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)

// 拖拽排序相关
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

  const list = [...imageList.value]
  const [movedItem] = list.splice(dragIndex.value, 1)
  if (movedItem) {
    list.splice(index, 0, movedItem)
  }
  imageList.value = list

  dragIndex.value = null
  dragOverIndex.value = null
}

const skuList = ref<SkuItem[]>([])
const skuImageInputs = ref<(HTMLInputElement | null)[]>([])
const specItems = ref<SpecItem[]>([{ name: '默认', values: ['单品'], newValue: '', batchInput: '' }])

// 规格值编辑状态
const editingSpecValue = ref<{ specIndex: number; valueIndex: number; value: string } | null>(null)

const batchPrice = ref<number | null>(null)
const batchStock = ref<number | null>(null)

const batchFillSkus = () => {
  if (batchPrice.value !== null) {
    skuList.value.forEach(sku => sku.price = batchPrice.value)
  }
  if (batchStock.value !== null) {
    skuList.value.forEach(sku => sku.stock = batchStock.value)
  }
}

const addSpecItem = () => {
  specItems.value.push({ name: '', values: [], newValue: '', batchInput: '' })
}

const removeSpecItem = (index: number) => {
  if (specItems.value.length > 1) {
    const spec = specItems.value[index]
    if (spec && spec.name) {
      const specName = spec.name
      for (const key of Object.keys(specImageMap.value)) {
        if (key.startsWith(specName + ':')) {
          delete specImageMap.value[key]
        }
      }
      for (const key of Object.keys(specImageFileMap.value)) {
        if (key.startsWith(specName + ':')) {
          delete specImageFileMap.value[key]
        }
      }
    }
    specItems.value.splice(index, 1)
    generateSkus()
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
    const removedValue = spec.values[valueIndex]
    const key = `${spec.name}:${removedValue}`
    delete specImageMap.value[key]
    delete specImageFileMap.value[key]
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
    const spec = specItems.value[specIndex]
    if (spec && spec.values) {
      const oldValue = spec.values[valueIndex]
      const newValue = value.trim()
      if (oldValue !== newValue) {
        const oldKey = `${spec.name}:${oldValue}`
        const newKey = `${spec.name}:${newValue}`
        if (specImageMap.value[oldKey]) {
          specImageMap.value[newKey] = specImageMap.value[oldKey]
          delete specImageMap.value[oldKey]
        }
        if (specImageFileMap.value[oldKey]) {
          specImageFileMap.value[newKey] = specImageFileMap.value[oldKey]
          delete specImageFileMap.value[oldKey]
        }
      }
      spec.values[valueIndex] = newValue
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

  const currentValue = spec.values[valueIndex]!
  const prevValue = spec.values[valueIndex - 1]!

  // 交换值
  ;[spec.values[valueIndex], spec.values[valueIndex - 1]] = [prevValue, currentValue]

  // 同步更新图片映射
  const specName = spec.name
  const currentKey = `${specName}:${currentValue}`
  const prevKey = `${specName}:${prevValue}`

  // 交换图片
  const tempImage = specImageMap.value[currentKey]
  const tempFile = specImageFileMap.value[currentKey]
  if (specImageMap.value[prevKey] !== undefined) {
    specImageMap.value[currentKey] = specImageMap.value[prevKey]!
  }
  if (specImageFileMap.value[prevKey] !== undefined) {
    specImageFileMap.value[currentKey] = specImageFileMap.value[prevKey]!
  }
  if (tempImage !== undefined) {
    specImageMap.value[prevKey] = tempImage
  }
  if (tempFile !== undefined) {
    specImageFileMap.value[prevKey] = tempFile
  }

  generateSkus()
}

// 下移规格值
const moveSpecValueDown = (specIndex: number, valueIndex: number) => {
  const spec = specItems.value[specIndex]
  if (!spec) return
  if (valueIndex >= spec.values.length - 1) return

  const currentValue = spec.values[valueIndex]!
  const nextValue = spec.values[valueIndex + 1]!

  // 交换值
  ;[spec.values[valueIndex], spec.values[valueIndex + 1]] = [nextValue, currentValue]

  // 同步更新图片映射
  const specName = spec.name
  const currentKey = `${specName}:${currentValue}`
  const nextKey = `${specName}:${nextValue}`

  // 交换图片
  const tempImage = specImageMap.value[currentKey]
  const tempFile = specImageFileMap.value[currentKey]
  if (specImageMap.value[nextKey] !== undefined) {
    specImageMap.value[currentKey] = specImageMap.value[nextKey]!
  }
  if (specImageFileMap.value[nextKey] !== undefined) {
    specImageFileMap.value[currentKey] = specImageFileMap.value[nextKey]!
  }
  if (tempImage !== undefined) {
    specImageMap.value[nextKey] = tempImage
  }
  if (tempFile !== undefined) {
    specImageFileMap.value[nextKey] = tempFile
  }

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
  // 使用当前 specItems 中的规格名，确保 key 与 specItems 保持一致
  const spec = specItems.value[specIndex]
  if (spec && spec.name && currentSpecValue) {
    const key = `${spec.name}:${currentSpecValue}`
    specImageMap.value[key] = previewUrl
    specImageFileMap.value[key] = file
  }

  syncSpecImagesForIndex(specIndex)
  input.value = ''
}

// 同步指定规格的图片到所有相关 SKU
const syncSpecImagesForIndex = (specIndex: number) => {
  const spec = specItems.value[specIndex]
  if (!spec) return

  skuList.value.forEach(sku => {
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
  skuList.value.forEach(sku => {
    const specInfo = sku.specInfo || {}
    specItems.value.forEach(spec => {
      if (!spec.name) return
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
  })
}

// 获取规格图片
const getSpecImage = (specName: string, specValue: string) => {
  const key = `${specName}:${specValue}`
  return specImageMap.value[key] || ''
}

// 迁移规格名变更时的图片缓存
const migrateSpecImageCache = (oldSpecNames: string[], newSpecNames: string[]) => {
  for (let i = 0; i < oldSpecNames.length && i < newSpecNames.length; i++) {
    const oldName = oldSpecNames[i]
    const newName = newSpecNames[i]
    if (oldName && newName && oldName !== newName) {
      for (const key of Object.keys(specImageMap.value)) {
        if (key.startsWith(oldName + ':')) {
          const value = key.substring(oldName.length + 1)
          const newKey = `${newName}:${value}`
          specImageMap.value[newKey] = specImageMap.value[key]!
          delete specImageMap.value[key]
        }
      }
      for (const key of Object.keys(specImageFileMap.value)) {
        if (key.startsWith(oldName + ':')) {
          const value = key.substring(oldName.length + 1)
          const newKey = `${newName}:${value}`
          specImageFileMap.value[newKey] = specImageFileMap.value[key]!
          delete specImageFileMap.value[key]
        }
      }
    }
  }
}

// 存储上次生成SKU时的规格名，用于检测规格名变更
let lastSpecNames: string[] = []

const generateSkus = () => {
  const validSpecs = specItems.value.filter(s => s.name.trim() && s.values.length > 0)
  if (validSpecs.length === 0) return

  // 检测规格名变更并迁移图片缓存
  const currentSpecNames = validSpecs.map(s => s.name)
  migrateSpecImageCache(lastSpecNames, currentSpecNames)
  lastSpecNames = [...currentSpecNames]

  const cartesian = (arrays: string[][]): string[][] => {
    return arrays.reduce((acc, arr) => acc.flatMap(x => arr.map(y => [...x, y])), [[]] as string[][])
  }

  const specNames = validSpecs.map(s => s.name)
  const specValues = validSpecs.map(s => s.values)
  const combinations = cartesian(specValues)

  // 计算当前最大 sortOrder
  const maxSort = skuList.value.reduce((max, s) => Math.max(max, s.sortOrder || 0), 0)

  // 只比较 specItems 中的有效规格名，保留旧 SKU 的所有信息（包括图片）
  const oldSkuMap = new Map()
  skuList.value.forEach(sku => {
    const specInfo = sku.specInfo || {}
    const validSpecNames = specItems.value.filter(s => s.name.trim()).map(s => s.name)
    const validKey = validSpecNames.map(name => `${name}:${specInfo[name] || ''}`).join('|')
    oldSkuMap.set(validKey, sku)
  })

  skuList.value = combinations.map((combo, index) => {
    const skuName = combo.join('-')
    const specInfo: Record<string, string> = {}
    specNames.forEach((name, i) => { specInfo[name] = combo[i] || '' })

    const validSpecNames = specItems.value.filter(s => s.name.trim()).map(s => s.name)
    const matchKey = validSpecNames.map(name => `${name}:${specInfo[name] || ''}`).join('|')
    const oldSku = oldSkuMap.get(matchKey)

    // 从规格图片缓存中获取当前规格组合的图片
    let skuImagePreview: string | undefined
    let skuImageFile: File | undefined
    for (const name of specNames) {
      const value = specInfo[name]
      if (value) {
        const cacheKey = `${name}:${value}`
        if (specImageMap.value[cacheKey]) {
          skuImagePreview = specImageMap.value[cacheKey]
          skuImageFile = specImageFileMap.value[cacheKey]
          break
        }
      }
    }

    return {
      id: oldSku?.id,
      skuName,
      specInfo,
      price: oldSku?.price ?? null,
      originalPrice: oldSku?.originalPrice ?? null,
      stock: oldSku?.stock ?? null,
      skuImage: oldSku?.skuImage,
      skuImageFile: skuImageFile || oldSku?.skuImageFile,
      skuImagePreview: skuImagePreview || oldSku?.skuImagePreview,
      skuImageDeleted: oldSku?.skuImageDeleted,
      sortOrder: oldSku ? oldSku.sortOrder : (maxSort + index + 1)
    }
  })
}

const triggerSkuImage = (index: number) => {
  skuImageInputs.value[index]?.click()
}

const handleSkuImageChange = (e: Event, index: number) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  const sku = skuList.value[index]
  if (!sku) return

  if (file) {
    // 使用 URL.createObjectURL 生成临时预览 URL，不转 Base64
    sku.skuImagePreview = URL.createObjectURL(file)
    sku.skuImageFile = file
    sku.skuImageDeleted = false
  }
  input.value = ''
}

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
      router.push({ name: 'SellerProducts' })
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

      formData.deliveryCity = product.deliveryCity || ''
      formData.weight = product.weight || null
      formData.isFreeShipping = product.isFreeShipping ?? false
      formData.serviceGuarantee = product.serviceGuarantee
        ? product.serviceGuarantee.split(',').filter(Boolean)
        : []
      formData.params = product.params ? product.params.map((p: any) => ({
        name: p.paramName || '',
        value: p.paramValue || ''
      })) : []

      const detailHtml = product.detailHtml || ''
      detailHtmlContent.value = detailHtml
      if (richEditor.value && detailHtml) {
        richEditor.value.innerHTML = detailHtml
      }

      if (product.categoryId) {
        setCategoryByLevel2Id(Number(product.categoryId))
      }

      if (product.productImages && product.productImages.length > 0) {
        imageList.value = product.productImages.map((img: any, index: number) => ({
          id: img.id || Date.now() + index,
          url: img.image,
          isNew: false
        }))
      } else if (product.images && product.images !== 'null') {
        // 兼容旧数据格式
        const imageUrls = product.images.split(',')
        imageList.value = imageUrls.map((url: string, index: number) => ({
          id: Date.now() + index,
          url: url,
          isNew: false
        }))
      }
    } else {
      throw new Error(productResponse.message || '加载商品失败')
    }

    const skuResponse = await authAPI.getSellerProductSkus(productId.value)
    if (skuResponse.success && skuResponse.data?.skus) {
      skuList.value = sortSkusBySpec(skuResponse.data.skus).map((sku: any) => ({
        id: sku.id,
        skuName: sku.skuName,
        specInfo: sku.specInfo ? JSON.parse(sku.specInfo) : {},
        price: sku.price,
        originalPrice: sku.originalPrice,
        stock: sku.stock,
        skuImage: sku.skuImage,
        sortOrder: sku.sortOrder
      }))

      // 从已有 SKU 反推规格项
      if (skuList.value.length > 0 && skuList.value[0]?.specInfo) {
        const firstSpec = skuList.value[0]?.specInfo
        const names = Object.keys(firstSpec)
        if (names.length > 0) {
          const allValues = names.map(name => {
            const values = [...new Set(skuList.value.map(s => s.specInfo?.[name]).filter(Boolean))]
            return { name, values: values as string[] }
          })
          specItems.value = allValues.map(s => ({ name: s.name, values: s.values, newValue: '', batchInput: '' }))
        }
      }
    }

  } catch (error: any) {
    Message.error(error.message || '加载商品失败')
    router.push({ name: 'SellerProducts' })
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

  const validValues = serviceOptions.value.map(s => s.value)
  formData.serviceGuarantee = formData.serviceGuarantee.filter(v => validValues.includes(v))
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
  if (imageList.value.length + files.length > 15) {
    Message.error('最多只能上传15张图片！')
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
  if (isSubmitting.value) return
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    const formDataObj = new FormData()

    const productData: any = {
      name: formData.name.trim(),
      brand: formData.brand.trim(),
      description: formData.description.trim(),
      detailHtml: detailHtmlContent.value,
      categoryId: parseInt(formData.categoryId, 10),
      status: formData.status,
      deliveryCity: formData.deliveryCity.trim(),
      weight: formData.weight,
      isFreeShipping: formData.isFreeShipping,
      serviceGuarantee: formData.serviceGuarantee.join(','),
      params: formData.params.filter((p: { name: string; value: string }) => p.name.trim() && p.value.trim()).map((p, index) => ({
        name: p.name.trim(),
        value: p.value.trim(),
        sortOrder: index
      })),
      skus: skuList.value.map((sku, index) => ({
        id: sku.id || undefined,
        skuName: sku.skuName || '',
        specInfo: sku.specInfo || {},
        price: sku.price,
        originalPrice: sku.originalPrice,
        stock: sku.stock,
        skuImage: sku.skuImageFile ? undefined : (sku.skuImage || undefined),
        skuImageDeleted: sku.skuImageDeleted || false,
        sortOrder: index
      }))
    }

    formDataObj.append('products', new Blob([JSON.stringify(productData)], { type: 'application/json' }))

    const imageSortOrder = imageList.value
      .filter(img => !img.isNew && img.id)
      .map((img, index) => ({ id: img.id, sortOrder: index }))
    formDataObj.append('imageSortOrder', JSON.stringify(imageSortOrder))

    const existingImageIds = imageList.value
      .filter(img => !img.isNew && img.id)
      .map(img => img.id)
    formDataObj.append('existingImageIds', JSON.stringify(existingImageIds))

    imageList.value.forEach((img) => {
      if (img.isNew && img.file) {
        formDataObj.append('images', img.file)
      }
    })

    skuList.value.forEach((sku, index) => {
      if (sku.skuImageFile) {
        formDataObj.append('skuImages', sku.skuImageFile, `sku_${index}_${sku.skuImageFile.name}`)
      }
    })

    const response = await authAPI.updateProduct(productId.value, formDataObj)

    if (response.success) {
      Message.success('保存成功')
      // 不重置 isSubmitting，防止重复点击，直到跳转
      setTimeout(() => {
        router.push({ name: 'SellerProducts' })
      }, 1500)
    } else {
      isSubmitting.value = false // 失败才允许重试
      throw new Error(response.message || '保存失败')
    }
  } catch (error: any) {
    Message.error(error.message || '保存失败')
    isSubmitting.value = false
  }
}

const goBack = () => {
  router.push({ name: 'SellerProducts' })
}

onMounted(() => {
  if (!authStore.validateSellerPermission()) return

  // 检查套餐
  authAPI.checkActivePackage().then(res => {
    if (!res.data?.active) {
      Message.warning('套餐已过期，请续费')
      router.push({ name: 'SellerPackage' })
      return
    }
    loadProductDetail()
  }).catch(() => {
    Message.error('检查套餐状态失败')
    router.push({ name: 'SellerPackage' })
  })
})
</script>

<style scoped>
@import url('@/static/css/seller/商品编辑页.css');
</style>
