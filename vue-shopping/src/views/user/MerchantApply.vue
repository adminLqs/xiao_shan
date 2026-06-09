<!-- views/user/MerchantApply.vue -->
<template>
  <div class="merchant-apply-container">
    <!-- 页面头部 -->
    <div class="apply-header">
      <div class="header-content">
        <div class="header-icon">
          <i class="fas fa-store"></i>
        </div>
        <h1 class="header-title">商家入驻申请</h1>
        <p class="header-desc">填写以下信息，开启您的电商之旅</p>
      </div>
    </div>

    <!-- 入驻流程步骤 -->
    <div class="apply-steps">
      <div class="step-item" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
        <div class="step-number">
          <i v-if="currentStep > 1" class="fas fa-check"></i>
          <span v-else>1</span>
        </div>
        <span class="step-label">基本信息</span>
      </div>
      <div class="step-line" :class="{ active: currentStep > 1 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
        <div class="step-number">
          <i v-if="currentStep > 2" class="fas fa-check"></i>
          <span v-else>2</span>
        </div>
        <span class="step-label">店铺信息</span>
      </div>
      <div class="step-line" :class="{ active: currentStep > 2 }"></div>
      <div class="step-item" :class="{ active: currentStep >= 3 }">
        <div class="step-number">3</div>
        <span class="step-label">资质上传</span>
      </div>
    </div>

    <!-- 表单内容 -->
    <div class="apply-form">
      <form @submit.prevent="handleSubmit">
        <!-- 第一步：基本信息 -->
        <div v-show="currentStep === 1" class="form-step">
          <div class="step-title">
            <i class="fas fa-user"></i>
            <span>基本信息</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 联系人姓名
            </label>
            <input
              v-model="formData.contactName"
              type="text"
              class="form-input"
              placeholder="请输入联系人姓名"
              maxlength="50"
              @input="validateField('contactName')"
            />
            <span v-if="errors.contactName" class="error-text">{{ errors.contactName }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 联系电话
            </label>
            <input
              v-model="formData.contactPhone"
              type="tel"
              class="form-input"
              placeholder="请输入联系电话"
              maxlength="11"
              @input="validateField('contactPhone')"
            />
            <span v-if="errors.contactPhone" class="error-text">{{ errors.contactPhone }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 联系邮箱
            </label>
            <input
              v-model="formData.contactEmail"
              type="email"
              class="form-input"
              placeholder="请输入联系邮箱"
              @input="validateField('contactEmail')"
            />
            <span v-if="errors.contactEmail" class="error-text">{{ errors.contactEmail }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 经营类型
            </label>
            <div class="radio-group">
              <label
                v-for="item in businessTypes"
                :key="item.value"
                class="radio-item"
                :class="{ active: formData.businessType === item.value }"
              >
                <input
                  type="radio"
                  v-model="formData.businessType"
                  :value="item.value"
                  class="radio-input"
                  @change="validateField('businessType')"
                />
                <span class="radio-label">{{ item.label }}</span>
              </label>
            </div>
            <span v-if="errors.businessType" class="error-text">{{ errors.businessType }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 主营类目
            </label>
            <select v-model="formData.mainCategory" class="form-select" @change="validateField('mainCategory')">
              <option value="">请选择主营类目</option>
              <option v-for="item in mainCategories" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
            <span v-if="errors.mainCategory" class="error-text">{{ errors.mainCategory }}</span>
          </div>
        </div>

        <!-- 第二步：店铺信息 -->
        <div v-show="currentStep === 2" class="form-step">
          <div class="step-title">
            <i class="fas fa-store-alt"></i>
            <span>店铺信息</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 店铺名称
            </label>
            <input
              v-model="formData.storeName"
              type="text"
              class="form-input"
              placeholder="请输入店铺名称（2-20个字符）"
              maxlength="100"
              @input="validateField('storeName')"
            />
            <span class="input-hint">{{ formData.storeName.length }}/100</span>
            <span v-if="errors.storeName" class="error-text">{{ errors.storeName }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">店铺详细描述</label>
            <textarea
              v-model="formData.storeDetail"
              class="form-textarea"
              placeholder="请输入店铺描述（选填，最多500字）"
              rows="5"
              maxlength="500"
            ></textarea>
            <span class="input-hint">{{ formData.storeDetail.length }}/500</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 店铺地址
            </label>
            <AddressSelector v-model="formData.addressRegion" @change="validateField('address')" />
          </div>
          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 详细地址
            </label>
            <input
              v-model="formData.addressDetail"
              type="text"
              class="form-input"
              placeholder="请输入街道、门牌号等详细信息（不要包含省市区名称）"
              maxlength="200"
              @input="validateField('address')"
            />
            <span v-if="errors.address" class="error-text">{{ errors.address }}</span>
          </div>
        </div>

        <!-- 第三步：资质上传 -->
        <div v-show="currentStep === 3" class="form-step">
          <div class="step-title">
            <i class="fas fa-file-alt"></i>
            <span>资质上传</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 营业执照
            </label>
            <div class="upload-area" @click="triggerUpload('businessLicense')">
              <input
                ref="businessLicenseInput"
                type="file"
                accept="image/*"
                class="upload-input"
                @change="handleFileChange($event, 'businessLicense')"
              />
              <div v-if="!formData.businessLicense" class="upload-placeholder">
                <i class="fas fa-cloud-upload-alt"></i>
                <p>点击上传营业执照</p>
                <span>支持 jpg、png 格式，大小不超过 5MB</span>
              </div>
              <div v-else class="upload-preview">
                <img :src="previewUrls.businessLicense" alt="营业执照预览" />
                <button type="button" class="preview-remove" @click.stop="removeFile('businessLicense')">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>
            <span v-if="errors.businessLicense" class="error-text">{{ errors.businessLicense }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 身份证正面
            </label>
            <div class="upload-area" @click="triggerUpload('idCardFront')">
              <input
                ref="idCardFrontInput"
                type="file"
                accept="image/*"
                class="upload-input"
                @change="handleFileChange($event, 'idCardFront')"
              />
              <div v-if="!formData.idCardFront" class="upload-placeholder">
                <i class="fas fa-id-card"></i>
                <p>点击上传身份证正面</p>
              </div>
              <div v-else class="upload-preview">
                <img :src="previewUrls.idCardFront" alt="身份证正面预览" />
                <button type="button" class="preview-remove" @click.stop="removeFile('idCardFront')">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>
            <span v-if="errors.idCardFront" class="error-text">{{ errors.idCardFront }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">
              <span class="required">*</span> 身份证反面
            </label>
            <div class="upload-area" @click="triggerUpload('idCardBack')">
              <input
                ref="idCardBackInput"
                type="file"
                accept="image/*"
                class="upload-input"
                @change="handleFileChange($event, 'idCardBack')"
              />
              <div v-if="!formData.idCardBack" class="upload-placeholder">
                <i class="fas fa-id-card"></i>
                <p>点击上传身份证反面</p>
              </div>
              <div v-else class="upload-preview">
                <img :src="previewUrls.idCardBack" alt="身份证反面预览" />
                <button type="button" class="preview-remove" @click.stop="removeFile('idCardBack')">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>
            <span v-if="errors.idCardBack" class="error-text">{{ errors.idCardBack }}</span>
          </div>
        </div>
      </form>
    </div>

    <!-- 底部操作按钮 -->
    <div class="form-actions">
      <button v-if="currentStep > 1" type="button" class="btn btn-prev" @click="prevStep">
        <i class="fas fa-arrow-left"></i> 上一步
      </button>
      <button v-if="currentStep < 3" type="button" class="btn btn-next"
        :class="{ 'btn-disabled': currentStep === 1 ? !canNextStep1 : !canNextStep2 }"
        :disabled="currentStep === 1 ? !canNextStep1 : !canNextStep2"
        @click="nextStep">
        下一步 <i class="fas fa-arrow-right"></i>
      </button>
      <button v-if="currentStep === 3" type="button" class="btn btn-submit"
        :class="{ 'btn-disabled': !canNextStep3 }"
        :disabled="!canNextStep3 || submitting"
        @click="handleSubmit">
        <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
        <span v-else>提交申请</span>
      </button>
    </div>

    <!-- 申请须知 -->
    <div class="apply-notice">
      <h3><i class="fas fa-info-circle"></i> 入驻须知</h3>
      <ul>
        <li>请确保填写的所有信息真实有效，虚假信息将导致申请被驳回</li>
        <li>上传的证件照片需清晰可见，支持 jpg、png 格式</li>
        <li>审核周期一般为 1-3 个工作日，请耐心等待</li>
        <li>如有疑问，请联系客服：400-888-6666</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import AddressSelector from '@/components/user/AddressSelector.vue'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn } = storeToRefs(authStore)

// ==================== 步骤控制 ====================
const currentStep = ref(1)
const submitting = ref(false)

// ==================== 表单数据 ====================
const formData = reactive({
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  storeName: '',
  storeDetail: '',
  addressRegion: undefined as { province: string; city: string; district: string } | undefined,
  addressDetail: '',
  businessType: '',
  mainCategory: '',
  businessLicense: null as File | null,
  idCardFront: null as File | null,
  idCardBack: null as File | null
})

// ==================== 预览URL ====================
const previewUrls = reactive({
  businessLicense: '',
  idCardFront: '',
  idCardBack: ''
})

// ==================== 验证错误 ====================
const errors = reactive({
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  storeName: '',
  address: '',
  businessType: '',
  mainCategory: '',
  businessLicense: '',
  idCardFront: '',
  idCardBack: ''
})

// ==================== 选项数据 ====================
const businessTypes = [
  { label: '个人', value: 'INDIVIDUAL' },
  { label: '个体工商户', value: 'SOLE_PROPRIETOR' },
  { label: '企业', value: 'COMPANY' }
]

const mainCategories = [
  { label: '手机数码', value: 'DIGITAL' },
  { label: '电脑办公', value: 'COMPUTER' },
  { label: '家用电器', value: 'APPLIANCE' },
  { label: '服饰鞋包', value: 'CLOTHING' },
  { label: '美妆护肤', value: 'BEAUTY' },
  { label: '食品生鲜', value: 'FOOD' },
  { label: '母婴玩具', value: 'BABY' },
  { label: '家居家装', value: 'HOME' },
  { label: '运动户外', value: 'SPORTS' },
  { label: '图书文娱', value: 'BOOKS' },
  { label: '蛋糕烘焙', value: 'BAKERY' },
  { label: '宠物生活', value: 'PET' },
  { label: '医药健康', value: 'HEALTH' },
  { label: '汽车用品', value: 'AUTO' },
  { label: '花卉绿植', value: 'PLANTS' },
  { label: '礼品鲜花', value: 'GIFT' },
  { label: '酒水冲调', value: 'DRINKS' },
  { label: '农资园艺', value: 'FARMING' },
  { label: '二手闲置', value: 'SECONDHAND' },
  { label: '钟表珠宝', value: 'JEWELRY' },
  { label: '其他', value: 'OTHER' }
]

// ==================== 步骤可用状态 ====================
const canNextStep1 = computed(() => {
  return formData.contactName.trim().length >= 2
    && /^1[3-9]\d{9}$/.test(formData.contactPhone)
    && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.contactEmail)
    && formData.businessType !== ''
    && formData.mainCategory !== ''
})

const canNextStep2 = computed(() => {
  return formData.storeName.trim().length >= 2
    && formData.addressRegion && formData.addressRegion.province
    && formData.addressDetail.trim().length > 0
})

const canNextStep3 = computed(() => {
  return formData.businessLicense !== null
    && formData.idCardFront !== null
    && formData.idCardBack !== null
})

// ==================== 文件上传引用 ====================
const businessLicenseInput = ref<HTMLInputElement>()
const idCardFrontInput = ref<HTMLInputElement>()
const idCardBackInput = ref<HTMLInputElement>()

// ==================== 权限检查 ====================
onMounted(() => {
  if (!isLoggedIn.value) {
    Message.error('请先登录')
    setTimeout(() => router.replace({ name: 'Login' }), 1500)
    return
  }

  if (authStore.hasRole('ROLE_SELLER') || authStore.hasRole('ROLE_ADMIN')) {
    Message.confirm('您已经是商家账号，无需重复申请', '提示').then(() => {
      router.replace({ name: 'SellerDashboard' })
    }).catch(() => {
      router.replace({ name: 'UserDashboard' })
    })
    return
  }

  loadApplicationStatus()
})

// ==================== 加载申请状态 ====================
const loadApplicationStatus = async (): Promise<void> => {
  try {
    const response = await authAPI.getMerchantApplicationStatus()
    if (response.success && response.data?.status) {
      const status = response.data.status
      if (status === 'PENDING') {
        Message.confirm('您已提交过入驻申请，正在审核中，请耐心等待', '提示').then(() => {
          router.replace({ name: 'UserDashboard' })
        }).catch(() => {
          router.replace({ name: 'UserDashboard' })
        })
      } else if (status === 'APPROVED') {
        Message.confirm('您的入驻申请已通过审核，请重新登录以激活商家权限', '提示').then(() => {
          authStore.clear()
          router.replace({ name: 'Login' })
        }).catch(() => {
          router.replace({ name: 'UserDashboard' })
        })
      }
    }
  } catch {
    // 没有申请记录，可以继续申请
  }
}

// ==================== 步骤导航 ====================
const nextStep = () => {
  if (currentStep.value === 1) {
    if (!validateStep1()) return
  } else if (currentStep.value === 2) {
    if (!validateStep2()) return
  }
  currentStep.value++
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const prevStep = () => {
  currentStep.value--
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// ==================== 表单验证 ====================
const validateField = (field: string) => {
  switch (field) {
    case 'contactName':
      if (!formData.contactName.trim()) {
        errors.contactName = '请输入联系人姓名'
      } else if (formData.contactName.trim().length < 2) {
        errors.contactName = '姓名至少2个字符'
      } else {
        errors.contactName = ''
      }
      break

    case 'contactPhone':
      const phoneReg = /^1[3-9]\d{9}$/
      if (!formData.contactPhone) {
        errors.contactPhone = '请输入联系电话'
      } else if (!phoneReg.test(formData.contactPhone)) {
        errors.contactPhone = '请输入正确的手机号码'
      } else {
        errors.contactPhone = ''
      }
      break

    case 'contactEmail':
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!formData.contactEmail) {
        errors.contactEmail = '请输入联系邮箱'
      } else if (!emailReg.test(formData.contactEmail)) {
        errors.contactEmail = '请输入正确的邮箱地址'
      } else {
        errors.contactEmail = ''
      }
      break

    case 'storeName':
      if (!formData.storeName.trim()) {
        errors.storeName = '请输入店铺名称'
      } else if (formData.storeName.trim().length < 2 || formData.storeName.trim().length > 20) {
        errors.storeName = '店铺名称长度应在2-20个字符之间'
      } else {
        errors.storeName = ''
      }
      break

    case 'businessType':
      if (!formData.businessType) {
        errors.businessType = '请选择经营类型'
      } else {
        errors.businessType = ''
      }
      break

    case 'mainCategory':
      if (!formData.mainCategory) {
        errors.mainCategory = '请选择主营类目'
      } else {
        errors.mainCategory = ''
      }
      break

    case 'address':
      if (!formData.addressRegion || !formData.addressRegion.province) {
        errors.address = '请选择店铺地址'
      } else if (!formData.addressDetail.trim()) {
        errors.address = '请输入详细地址'
      } else {
        const isValid = validateAddressDetail(formData.addressDetail, formData.addressRegion.province, formData.addressRegion.city, formData.addressRegion.district)
        if (!isValid) {
          // 错误信息由 validateAddressDetail 内部通过 Message 显示
        } else {
          errors.address = ''
        }
      }
      break
  }
}

// 验证详细地址是否包含省市区名称
const validateAddressDetail = (detail: string, province?: string, city?: string, district?: string): boolean => {
  if (!detail) return true
  const forbiddenWords: string[] = []
  if (province) forbiddenWords.push(province)
  if (city) forbiddenWords.push(city)
  if (district) forbiddenWords.push(district)
  
  for (const word of forbiddenWords) {
    if (word && detail.includes(word)) {
      Message.error(`详细地址不能包含"${word}"，请去掉省市区信息`)
      return false
    }
  }
  return true
}

const validateStep1 = (): boolean => {
  validateField('contactName')
  validateField('contactPhone')
  validateField('contactEmail')
  validateField('businessType')
  validateField('mainCategory')

  return !errors.contactName && !errors.contactPhone && !errors.contactEmail &&
         !errors.businessType && !errors.mainCategory
}

const validateStep2 = (): boolean => {
  validateField('storeName')
  validateField('address')
  return !errors.storeName && !errors.address
}

const validateStep3 = (): boolean => {
  let valid = true

  if (!formData.businessLicense) {
    errors.businessLicense = '请上传营业执照'
    valid = false
  } else {
    errors.businessLicense = ''
  }

  if (!formData.idCardFront) {
    errors.idCardFront = '请上传身份证正面'
    valid = false
  } else {
    errors.idCardFront = ''
  }

  if (!formData.idCardBack) {
    errors.idCardBack = '请上传身份证反面'
    valid = false
  } else {
    errors.idCardBack = ''
  }

  return valid
}

// ==================== 文件上传处理 ====================
const triggerUpload = (type: string) => {
  switch (type) {
    case 'businessLicense':
      businessLicenseInput.value?.click()
      break
    case 'idCardFront':
      idCardFrontInput.value?.click()
      break
    case 'idCardBack':
      idCardBackInput.value?.click()
      break
  }
}

const handleFileChange = (event: Event, type: string) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) return

  // 验证文件类型
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    Message.error('只支持 jpg、png 格式的图片')
    return
  }

  // 验证文件大小
  if (file.size > 5 * 1024 * 1024) {
    Message.error('图片大小不能超过 5MB')
    return
  }

  // 保存文件
  switch (type) {
    case 'businessLicense':
      formData.businessLicense = file
      break
    case 'idCardFront':
      formData.idCardFront = file
      break
    case 'idCardBack':
      formData.idCardBack = file
      break
  }

  // 生成预览
  const reader = new FileReader()
  reader.onload = (e) => {
    if (e.target?.result) {
      previewUrls[type as keyof typeof previewUrls] = e.target.result as string
    }
  }
  reader.readAsDataURL(file)

  // 清除错误提示
  if (errors[type as keyof typeof errors]) {
    errors[type as keyof typeof errors] = ''
  }
}

const removeFile = (type: string) => {
  switch (type) {
    case 'businessLicense':
      formData.businessLicense = null
      previewUrls.businessLicense = ''
      break
    case 'idCardFront':
      formData.idCardFront = null
      previewUrls.idCardFront = ''
      break
    case 'idCardBack':
      formData.idCardBack = null
      previewUrls.idCardBack = ''
      break
  }
}

// ==================== 提交申请 ====================
const handleSubmit = async () => {
  // 验证所有步骤
  if (!validateStep1()) {
    currentStep.value = 1
    window.scrollTo({ top: 0 })
    return
  }
  if (!validateStep2()) {
    currentStep.value = 2
    window.scrollTo({ top: 0 })
    return
  }
  if (!validateStep3()) return

  submitting.value = true

  try {
    // 构建 FormData
    const formDataToSend = new FormData()
    formDataToSend.append('contactName', formData.contactName.trim())
    formDataToSend.append('contactPhone', formData.contactPhone)
    formDataToSend.append('contactEmail', formData.contactEmail.trim())
    formDataToSend.append('storeName', formData.storeName.trim())
    formDataToSend.append('storeDetail', formData.storeDetail.trim())
    // 拼接完整地址：省市区 + 详细地址
    const addressStr = formData.addressRegion
      ? `${formData.addressRegion.province}${formData.addressRegion.city}${formData.addressRegion.district}${formData.addressDetail.trim()}`
      : ''
    formDataToSend.append('address', addressStr)
    formDataToSend.append('businessType', formData.businessType)
    formDataToSend.append('mainCategory', formData.mainCategory)

    if (formData.businessLicense) {
      formDataToSend.append('businessLicense', formData.businessLicense)
    }
    if (formData.idCardFront) {
      formDataToSend.append('idCardFront', formData.idCardFront)
    }
    if (formData.idCardBack) {
      formDataToSend.append('idCardBack', formData.idCardBack)
    }

    const response = await authAPI.submitMerchantApplication(formDataToSend)

    if (response.success) {
      Message.success('申请提交成功，请等待审核')
      setTimeout(() => {
        router.replace({ name: 'UserDashboard' })
      }, 1500)
    } else {
      throw new Error(response.message || '提交失败')
    }
  } catch (error: any) {
    Message.error(error.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import url('@/static/css/user/商家入驻.css');
</style>
