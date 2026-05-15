<template>
  <div class="add-product">
    <h2 class="page-title">发布新商品</h2>
    
    <div class="form-container">
      <div class="form-group">
        <label class="form-label">商品名称 <span class="required">*</span></label>
        <input 
          type="text" 
          class="form-input" 
          v-model="form.name"
          placeholder="请输入商品名称"
          maxlength="100"
        />
        <div class="input-tip">{{ form.name.length }}/100</div>
      </div>

      <div class="form-group">
        <label class="form-label">商品分类 <span class="required">*</span></label>
        <select class="form-select" v-model="form.category">
          <option value="">请选择分类</option>
          <option value="meat">烤肉类</option>
          <option value="seafood">烤海鲜</option>
          <option value="vegetable">烤蔬菜</option>
          <option value="staple">主食类</option>
          <option value="drink">酒水饮料</option>
          <option value="snack">特色小吃</option>
        </select>
      </div>

      <div class="form-row">
        <div class="form-group half">
          <label class="form-label">价格 <span class="required">*</span></label>
          <div class="input-with-unit">
            <input 
              type="number" 
              class="form-input" 
              v-model="form.price"
              placeholder="0.00"
              min="0"
              step="0.01"
            />
            <span class="unit">元</span>
          </div>
        </div>

        <div class="form-group half">
          <label class="form-label">原价 <span class="required">*</span></label>
          <div class="input-with-unit">
            <input 
              type="number" 
              class="form-input" 
              v-model="form.originalPrice"
              placeholder="0.00"
              min="0"
              step="0.01"
            />
            <span class="unit">元</span>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">商品图片 <span class="required">*</span></label>
        <div class="upload-area">
          <div class="image-preview" v-if="form.image">
            <img :src="form.image" alt="商品图片" />
            <button class="image-remove" @click="removeImage">×</button>
          </div>
          <div v-else class="upload-box" @click="triggerUpload">
            <span class="upload-icon">+</span>
            <span class="upload-text">点击上传</span>
          </div>
          <input 
            type="file" 
            ref="fileInput" 
            accept="image/*" 
            @change="handleImageUpload" 
            style="display: none"
          />
          <div class="upload-tip">
            <p>支持jpg/png格式，大小不超过5MB</p>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">商品描述</label>
        <textarea 
          class="form-textarea" 
          v-model="form.description"
          placeholder="请描述商品的特色、口味、配料等"
          rows="4"
          maxlength="500"
        ></textarea>
        <div class="input-tip">{{ form.description.length }}/500</div>
      </div>

      <div class="form-actions">
        <button class="btn-submit" @click="submitForm" :disabled="submitting">
          {{ submitting ? '发布中...' : '发布商品' }}
        </button>
        <button class="btn-reset" @click="resetForm">重置</button>
      </div>
    </div>

    <!-- 预览数据 -->
    <div class="preview" v-if="previewData">
      <h3>提交数据预览</h3>
      <pre>{{ JSON.stringify(previewData, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { authAPI } from '@/api/auth';
  import { ref, reactive } from 'vue';
  import { showToast, showSuccessToast, showFailToast } from 'vant';
  import 'vant/es/toast/style';

  interface ProductForm {
    name: string;
    description: string;
    price: string;
    originalPrice: string;
    image: string;
    category: string;
  }

  const fileInput = ref<HTMLInputElement | null>(null); // 文件元素
  const submitting = ref(false);
  const previewData = ref<ProductForm | null>(null);
  const selectedFile = ref<File | null>(null); // 存储实际的文件对象

  const form = reactive<ProductForm>({
    name: '',
    description: '',
    price: '',
    originalPrice: '',
    image: '',
    category: ''
  });

  // 触发文件上传
  const triggerUpload = () => {
    fileInput.value?.click();
  };

  // 处理图片上传
  const handleImageUpload = (e: Event) => {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    
    if (!file) return;
    
    // 验证文件大小
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过5MB');
      input.value = '';
      return;
    }
    
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      alert('请上传图片文件');
      input.value = '';
      return;
    }

    // ✅ 保存实际的文件对象
    selectedFile.value = file;
    
    // 设置预览图片的代码
    const reader = new FileReader();
    reader.onload = (e) => {
      form.image = e.target?.result as string; // 设置预览
    };
    reader.readAsDataURL(file);
  };

  // 移除图片
  const removeImage = () => {
    form.image = '';
    if (fileInput.value) {
      fileInput.value.value = '';
    }
  };

  // 验证表单
  const validateForm = (): boolean => {
    if (!form.name.trim()) {
      alert('请输入商品名称');
      return false;
    }
    if (!form.category) {
      alert('请选择商品分类');
      return false;
    }
    if (!form.price || Number(form.price) <= 0) {
      alert('请输入有效的价格');
      return false;
    }
    if (!form.originalPrice || Number(form.originalPrice) <= 0) {
      alert('请输入有效的原价');
      return false;
    }
    // 检查 selectedFile.value（实际要上传的文件）
    if (!selectedFile.value) {
        alert('请上传商品图片');
        return false;
    }
    if (!form.image) {
      alert('图片预览生成失败，请重新上传');
      return false;
    }
    return true;
  };

  // 提交表单
  const submitForm = async () => {
    // 验证表单
    if (!validateForm()) return;
    
    submitting.value = true;
    
    // 显示加载提示
    const toast = showToast({
      type: 'loading',
      message: '发布中...',
      forbidClick: true,
      duration: 0,
    });
    
    try {
      // 创建表单数据
      const data = new FormData();

      // 发送基本数据
      const submitData = {
        name: form.name,
        description: form.description,
        price: Number(form.price),
        originalPrice: Number(form.originalPrice),
        category: form.category
      };

      // 添加数据
      const productBlob = new Blob([JSON.stringify(submitData)], { 
        type: 'application/json' 
      });
      data.append("product", productBlob);
      data.append("image", selectedFile.value!)

      // 调用API
      await authAPI.addProduct(data)
      
      toast.close();
      showSuccessToast('商品发布成功');
      resetForm(); // 发布成功后重置表单
      
    } catch (error) {
      toast.close();
      showFailToast('商品发布失败，请稍后重试');
    } finally {
      submitting.value = false;
    }
  };

  // 重置表单
  const resetForm = () => {
    form.name = '';
    form.description = '';
    form.price = '';
    form.originalPrice = '';
    form.image = '';
    form.category = '';
    if (fileInput.value) {
      fileInput.value.value = '';
    }
    selectedFile.value = null; // 清空选中的文件
    previewData.value = null;
  };

</script>

<style scoped>
  @import url("@/static/css/商家发布页.css")
</style>