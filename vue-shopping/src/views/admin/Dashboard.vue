<template>
  <div class="admin-dashboard">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'sidebar-collapsed': isSidebarCollapsed }" id="sidebar">
      <div class="sidebar-header">
        <h2><i class="fas fa-crown"></i> <span v-show="!isSidebarCollapsed">企业管理系统</span></h2>
      </div>
      <div class="sidebar-menu">
        <div class="menu-section">
          <div class="menu-title" v-show="!isSidebarCollapsed">主导航</div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'dashboard' }"
            @click="navigateTo('/store/admin/dashboard')"
            title="控制台"
          >
            <i class="fas fa-tachometer-alt"></i>
            <span v-show="!isSidebarCollapsed">控制台</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'analytics' }"
            @click="navigateTo('/store/admin/analytics')"
            title="数据分析"
          >
            <i class="fas fa-chart-line"></i>
            <span v-show="!isSidebarCollapsed">数据分析</span>
          </div>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-show="!isSidebarCollapsed">用户管理</div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'users' }"
            @click="navigateTo('/store/admin/users')"
            title="用户列表"
          >
            <i class="fas fa-users"></i>
            <span v-show="!isSidebarCollapsed">用户列表</span>
            <span class="menu-badge" v-show="!isSidebarCollapsed">12</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'addUser' }"
            @click="navigateTo('/store/admin/users/add')"
            title="添加用户"
          >
            <i class="fas fa-user-plus"></i>
            <span v-show="!isSidebarCollapsed">添加用户</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'permissions' }"
            @click="navigateTo('/store/admin/permissions')"
            title="权限管理"
          >
            <i class="fas fa-user-shield"></i>
            <span v-show="!isSidebarCollapsed">权限管理</span>
          </div>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-show="!isSidebarCollapsed">商品管理</div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'products' }"
            @click="navigateTo('/store/admin/products')"
            title="商品列表"
          >
            <i class="fas fa-box"></i>
            <span v-show="!isSidebarCollapsed">商品列表</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'addProduct' }"
            @click="navigateTo('/store/admin/products/add')"
            title="发布商品"
          >
            <i class="fas fa-plus-circle"></i>
            <span v-show="!isSidebarCollapsed">发布商品</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'categories' }"
            @click="navigateTo('/store/admin/categories')"
            title="分类管理"
          >
            <i class="fas fa-tags"></i>
            <span v-show="!isSidebarCollapsed">分类管理</span>
          </div>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-show="!isSidebarCollapsed">订单管理</div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'orders' }"
            @click="navigateTo('/store/admin/orders')"
            title="所有订单"
          >
            <i class="fas fa-shopping-cart"></i>
            <span v-show="!isSidebarCollapsed">所有订单</span>
            <span class="menu-badge" v-show="!isSidebarCollapsed">5</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'pendingOrders' }"
            @click="navigateTo('/store/admin/orders/pending')"
            title="待处理订单"
          >
            <i class="fas fa-clock"></i>
            <span v-show="!isSidebarCollapsed">待处理订单</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'salesStats' }"
            @click="navigateTo('/store/admin/sales')"
            title="销售统计"
          >
            <i class="fas fa-chart-bar"></i>
            <span v-show="!isSidebarCollapsed">销售统计</span>
          </div>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-show="!isSidebarCollapsed">商家管理</div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'merchants' }"
            @click="navigateTo('/store/admin/merchants')"
            title="商家列表"
          >
            <i class="fas fa-store"></i>
            <span v-show="!isSidebarCollapsed">商家列表</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'merchantReview' }"
            @click="navigateTo('/store/merchant/review')"
            title="商家审核"
          >
            <i class="fas fa-user-check"></i>
            <span v-show="!isSidebarCollapsed">商家审核</span>
            <span class="menu-badge" v-show="!isSidebarCollapsed">{{ pendingMerchantCount }}</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'settlement' }"
            @click="navigateTo('/store/admin/settlement')"
            title="结算管理"
          >
            <i class="fas fa-file-invoice-dollar"></i>
            <span v-show="!isSidebarCollapsed">结算管理</span>
          </div>
        </div>

        <div class="menu-section">
          <div class="menu-title" v-show="!isSidebarCollapsed">系统设置</div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'systemConfig' }"
            @click="navigateTo('/store/admin/system')"
            title="系统配置"
          >
            <i class="fas fa-cog"></i>
            <span v-show="!isSidebarCollapsed">系统配置</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'notifications' }"
            @click="navigateTo('/store/admin/notifications')"
            title="通知管理"
          >
            <i class="fas fa-bell"></i>
            <span v-show="!isSidebarCollapsed">通知管理</span>
          </div>
          <div
            class="menu-item"
            :class="{ active: activeMenu === 'security' }"
            @click="navigateTo('/store/admin/security')"
            title="安全设置"
          >
            <i class="fas fa-shield-alt"></i>
            <span v-show="!isSidebarCollapsed">安全设置</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content" :class="{ expanded: isSidebarCollapsed }">
      <!-- 顶部导航栏 -->
      <div class="top-header">
        <div class="header-left">
          <button class="toggle-sidebar" @click="toggleSidebar">
            <i class="fas fa-bars"></i>
          </button>
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input
              type="text"
              class="search-input"
              placeholder="搜索..."
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
        <div class="header-right">
          <div class="header-icon" @click="showNotifications">
            <i class="fas fa-bell"></i>
            <span class="icon-badge">{{ unreadNotifications }}</span>
          </div>
          <div class="header-icon" @click="showMessages">
            <i class="fas fa-envelope"></i>
            <span class="icon-badge">{{ unreadMessages }}</span>
          </div>
          <div class="user-info" @click="toggleUserMenu">
            <img :src="userAvatar" alt="管理员头像" class="user-avatar" />
            <div class="user-details" v-show="!isSidebarCollapsed">
              <div class="user-name">{{ userName }}</div>
              <div class="user-role">管理员</div>
            </div>
            <i class="fas fa-chevron-down"></i>
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="content">
        <!-- 页面标题 -->
        <div class="page-header">
          <h1 class="page-title">管理员控制台</h1>
          <div class="breadcrumb">
            <a href="#" @click.prevent="navigateTo('/store/admin/dashboard')">首页</a> /
            <span>控制台</span>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-container">
          <div class="stat-card" v-for="stat in stats" :key="stat.id">
            <div class="stat-icon" :style="{ background: stat.iconBg }">
              <i :class="stat.icon"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-change" :class="stat.changeType">
                <i :class="stat.changeIcon"></i> {{ stat.changeText }}
              </div>
            </div>
          </div>
        </div>

        <!-- 图表区域 -->
        <div class="charts-container">
          <div class="chart-card">
            <div class="card-header">
              <h3 class="card-title">销售趋势</h3>
              <div class="card-actions">
                <button class="card-action" @click="refreshSalesChart">
                  <i class="fas fa-sync-alt"></i>
                </button>
                <button class="card-action" @click="showChartOptions('sales')">
                  <i class="fas fa-ellipsis-v"></i>
                </button>
              </div>
            </div>
            <div class="card-body">
              <div class="chart-container">
                <canvas id="salesChart"></canvas>
              </div>
            </div>
          </div>
          <div class="chart-card">
            <div class="card-header">
              <h3 class="card-title">用户分布</h3>
              <div class="card-actions">
                <button class="card-action" @click="refreshUserChart">
                  <i class="fas fa-sync-alt"></i>
                </button>
                <button class="card-action" @click="showChartOptions('users')">
                  <i class="fas fa-ellipsis-v"></i>
                </button>
              </div>
            </div>
            <div class="card-body">
              <div class="chart-container">
                <canvas id="userChart"></canvas>
              </div>
            </div>
          </div>
        </div>

        <!-- 表格区域 -->
        <div class="table-card">
          <div class="card-header">
            <h3 class="card-title">最近订单</h3>
            <button class="btn btn-primary" @click="exportData">
              <i class="fas fa-download"></i> 导出数据
            </button>
          </div>
          <div class="card-body">
            <div class="table-responsive">
              <table class="table">
                <thead>
                  <tr>
                    <th>订单ID</th>
                    <th>用户</th>
                    <th>商家</th>
                    <th>金额</th>
                    <th>状态</th>
                    <th>日期</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="order in recentOrders" :key="order.id">
                    <td>{{ order.id }}</td>
                    <td>{{ order.user }}</td>
                    <td>{{ order.merchant }}</td>
                    <td>{{ order.amount }}</td>
                    <td>
                      <span class="status-badge" :class="`status-${order.status}`">
                        {{ getStatusText(order.status) }}
                      </span>
                    </td>
                    <td>{{ order.date }}</td>
                    <td class="action-buttons">
                      <button class="action-btn edit" @click="editOrder(order)">
                        <i class="fas fa-edit"></i>
                      </button>
                      <button class="action-btn delete" @click="deleteOrder(order)">
                        <i class="fas fa-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 底部区域 -->
        <div class="footer">
          <p>© 2023 企业管理系统 - 版权所有 | 设计开发: 技术研发部</p>
        </div>
      </div>
    </div>

    <!-- 用户菜单下拉框 -->
    <div class="user-dropdown" v-if="showUserDropdown" ref="userDropdown">
      <div class="dropdown-item" @click="navigateTo('/store/admin/profile')">
        <i class="fas fa-user"></i> 个人资料
      </div>
      <div class="dropdown-item" @click="navigateTo('/store/admin/settings')">
        <i class="fas fa-cog"></i> 设置
      </div>
      <div class="dropdown-divider"></div>
      <div class="dropdown-item logout" @click="logout">
        <i class="fas fa-sign-out-alt"></i> 退出登录
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Chart, registerables } from 'chart.js'

// 注册Chart.js组件
Chart.register(...registerables)

// Vue Router
const route = useRoute()
const router = useRouter()

// 响应式数据
const isSidebarCollapsed = ref(false)
const activeMenu = ref('dashboard')
const showUserDropdown = ref(false)

// 用户信息
const userName = ref('管理员')
const userAvatar = ref('https://via.placeholder.com/150')
const unreadNotifications = ref(5)
const unreadMessages = ref(3)
const pendingMerchantCount = ref(3)

// 搜索关键词
const searchKeyword = ref('')

// 统计数据
const stats = ref([
  {
    id: 1,
    value: '12,584',
    label: '总用户数',
    changeType: 'positive',
    changeIcon: 'fas fa-arrow-up',
    changeText: '8.2% 较上月',
    icon: 'fas fa-users',
    iconBg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  },
  {
    id: 2,
    value: '856',
    label: '入驻商家',
    changeType: 'positive',
    changeIcon: 'fas fa-arrow-up',
    changeText: '3.5% 较上月',
    icon: 'fas fa-store',
    iconBg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  },
  {
    id: 3,
    value: '5,247',
    label: '今日订单',
    changeType: 'positive',
    changeIcon: 'fas fa-arrow-up',
    changeText: '12.4% 较昨日',
    icon: 'fas fa-shopping-cart',
    iconBg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  },
  {
    id: 4,
    value: '¥258,964',
    label: '今日销售额',
    changeType: 'positive',
    changeIcon: 'fas fa-arrow-up',
    changeText: '5.7% 较昨日',
    icon: 'fas fa-dollar-sign',
    iconBg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  },
])

// 最近订单数据
const recentOrders = ref([
  {
    id: '#ORD-2023-00125',
    user: '张小明',
    merchant: '精品数码旗舰店',
    amount: '¥8,999',
    status: 'active',
    date: '2023-10-15',
  },
  {
    id: '#ORD-2023-00124',
    user: '李小红',
    merchant: '时尚服饰专营店',
    amount: '¥1,258',
    status: 'pending',
    date: '2023-10-14',
  },
  {
    id: '#ORD-2023-00123',
    user: '王刚',
    merchant: '家电优选商城',
    amount: '¥3,456',
    status: 'active',
    date: '2023-10-14',
  },
  {
    id: '#ORD-2023-00122',
    user: '赵丽',
    merchant: '美妆护肤专柜',
    amount: '¥689',
    status: 'inactive',
    date: '2023-10-13',
  },
])

// Chart实例
let salesChart = null
let userChart = null

// 方法定义
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const navigateTo = (path) => {
  // 阻止默认行为（对应jQuery的e.preventDefault()）

  // 设置当前激活的菜单
  const menuMap = {
    '/store/admin/dashboard': 'dashboard',
    '/store/admin/analytics': 'analytics',
    '/store/admin/users': 'users',
    '/store/admin/users/add': 'addUser',
    '/store/admin/permissions': 'permissions',
    '/store/admin/products': 'products',
    '/store/admin/products/add': 'addProduct',
    '/store/admin/categories': 'categories',
    '/store/admin/orders': 'orders',
    '/store/admin/orders/pending': 'pendingOrders',
    '/store/admin/sales': 'salesStats',
    '/store/admin/merchants': 'merchants',
    '/store/merchant/review': 'merchantReview',
    '/store/admin/settlement': 'settlement',
    '/store/admin/system': 'systemConfig',
    '/store/admin/notifications': 'notifications',
    '/store/admin/security': 'security',
    '/store/admin/profile': '',
    '/store/admin/settings': '',
  }

  activeMenu.value = menuMap[path] || 'dashboard'

  // 使用Vue Router进行导航
  router.push(path)
}

// 对应原jQuery的商家审核菜单点击
const goToMerchantReview = () => {
  navigateTo('/store/merchant/review')
}

// 对应原jQuery的管理员首页跳转
const goToDashboard = () => {
  navigateTo('/store/admin/dashboard')
}

const toggleUserMenu = () => {
  showUserDropdown.value = !showUserDropdown.value
}

const showNotifications = () => {
  console.log('显示通知')
  // 可以跳转到通知页面或显示通知弹窗
  router.push('/store/admin/notifications')
}

const showMessages = () => {
  console.log('显示消息')
  // 可以跳转到消息页面
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    console.log('搜索关键词:', searchKeyword.value)
    // 执行搜索逻辑
  }
}

const refreshSalesChart = () => {
  if (salesChart) {
    salesChart.destroy()
    initSalesChart()
  }
}

const refreshUserChart = () => {
  if (userChart) {
    userChart.destroy()
    initUserChart()
  }
}

const showChartOptions = (chartType) => {
  console.log(`显示 ${chartType} 图表选项`)
  // 显示图表选项菜单
}

const exportData = () => {
  console.log('导出数据')
  // 实现数据导出逻辑
}

const getStatusText = (status) => {
  const statusMap = {
    active: '已完成',
    pending: '待发货',
    inactive: '已取消',
  }
  return statusMap[status] || status
}

const editOrder = (order) => {
  console.log('编辑订单:', order.id)
  // 跳转到编辑页面或打开编辑模态框
}

const deleteOrder = (order) => {
  if (confirm(`确定要删除订单 ${order.id} 吗？`)) {
    console.log('删除订单:', order.id)
    // 执行删除逻辑
  }
}

const logout = () => {
  if (confirm('确定要退出登录吗？')) {
    console.log('退出登录')
    // 执行退出逻辑
    router.push('/login')
  }
}

// 初始化图表
const initSalesChart = () => {
  const ctx = document.getElementById('salesChart')
  if (!ctx) return

  salesChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['1月', '2月', '3月', '4月', '5月', '6月'],
      datasets: [
        {
          label: '销售额',
          data: [65, 59, 80, 81, 56, 55],
          borderColor: '#667eea',
          backgroundColor: 'rgba(102, 126, 234, 0.1)',
          tension: 0.4,
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false,
        },
      },
    },
  })
}

const initUserChart = () => {
  const ctx = document.getElementById('userChart')
  if (!ctx) return

  userChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['普通用户', 'VIP用户', '商家用户', '管理员'],
      datasets: [
        {
          data: [300, 50, 100, 20],
          backgroundColor: ['#667eea', '#f093fb', '#4facfe', '#43e97b'],
        },
      ],
    },
    options: {
      responsive: true,
      plugins: {
        legend: {
          position: 'right',
        },
      },
    },
  })
}

// 生命周期钩子
onMounted(() => {
  // 初始化图表
  initSalesChart()
  initUserChart()

  // 点击外部关闭用户菜单
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  // 清理图表实例
  if (salesChart) {
    salesChart.destroy()
  }
  if (userChart) {
    userChart.destroy()
  }

  // 移除事件监听
  document.removeEventListener('click', handleClickOutside)
})

// 点击外部关闭用户菜单
const handleClickOutside = (event) => {
  const userDropdown = document.querySelector('.user-dropdown')
  const userInfo = document.querySelector('.user-info')

  if (
    showUserDropdown.value &&
    userDropdown &&
    !userDropdown.contains(event.target) &&
    !userInfo.contains(event.target)
  ) {
    showUserDropdown.value = false
  }
}

// 监听路由变化更新激活菜单
watch(
  () => route.path,
  (newPath) => {
    // 根据路径设置激活菜单
    const pathToMenu = {
      '/store/admin/dashboard': 'dashboard',
      '/store/admin/analytics': 'analytics',
      '/store/admin/users': 'users',
      '/store/admin/users/add': 'addUser',
      '/store/admin/permissions': 'permissions',
      '/store/admin/products': 'products',
      '/store/admin/products/add': 'addProduct',
      '/store/admin/categories': 'categories',
      '/store/admin/orders': 'orders',
      '/store/admin/orders/pending': 'pendingOrders',
      '/store/admin/sales': 'salesStats',
      '/store/admin/merchants': 'merchants',
      '/store/merchant/review': 'merchantReview',
      '/store/admin/settlement': 'settlement',
      '/store/admin/system': 'systemConfig',
      '/store/admin/notifications': 'notifications',
      '/store/admin/security': 'security',
    }

    activeMenu.value = pathToMenu[newPath] || 'dashboard'
  },
)
</script>

<style>
@import '@/static/css/admin/管理员首页.css';
</style>
