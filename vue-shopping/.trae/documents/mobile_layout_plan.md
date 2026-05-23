
# 移动端底部导航栏布局组件实现计划

## 一、需求分析

根据用户需求，需要创建一个移动端底部导航栏布局组件，包含以下功能：

### 1. 创建 MobileLayout.vue 组件
- 包含 4 个 Tab：首页、分类、消息、我的
- 底部栏固定，高度 50px，背景白色，顶部阴影
- 主内容区留底部空间
- Tab 图标在上，文字在下，居中对齐
- 当前路由高亮（active-class="active"）
- 角标显示未读消息数
- 安全区适配

### 2. 路由修改
- 将首页、分类、消息、个人中心作为 MobileLayout 的子路由
- 所有页面路径前加 `/user`

### 3. Dashboard.vue 简化
- 移除底部导航栏、顶部导航、抽屉菜单
- 只保留首页业务内容

### 4. 新增页面
- Categories.vue：分类页面
- Messages.vue：消息页面

## 二、文件结构

```
src/
├── views/
│   └── user/
│       ├── Layout/
│       │   └── MobileLayout.vue   # 新增：移动端布局组件
│       ├── Dashboard.vue          # 修改：简化首页
│       ├── Center.vue             # 已存在：个人中心
│       ├── Categories.vue         # 新增：分类页面
│       └── Messages.vue           # 新增：消息页面
└── router/
    └── modules/
        └── user.ts                # 修改：路由配置
```

## 三、实现步骤

### 步骤 1：创建 MobileLayout.vue
```typescript
// src/views/user/Layout/MobileLayout.vue
// 包含：
// - RouterView 主内容区
// - 底部 TabBar（首页、分类、消息、我的）
// - 消息角标
// - 安全区适配
```

### 步骤 2：修改路由配置 user.ts
```typescript
// 将以下路由改为 MobileLayout 的子路由：
// - UserDashboard (/user/dashboard)
// - UserCategories (/user/categories)
// - UserMessages (/user/messages)
// - UserCenter (/user/center)
```

### 步骤 3：简化 Dashboard.vue
```typescript
// 移除：
// - 顶部导航栏
// - 底部导航栏
// - 用户抽屉菜单
// 保留：
// - 搜索栏
// - 分类导航
// - 商品轮播图
// - 商品列表
```

### 步骤 4：创建 Categories.vue
```typescript
// src/views/user/Categories.vue
// 简单分类页面，展示分类列表
```

### 步骤 5：创建 Messages.vue
```typescript
// src/views/user/Messages.vue
// 消息页面占位组件
```

## 四、样式规范

### TabBar 样式
```css
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: white;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.08);
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  text-decoration: none;
}

.tab-bar-item.active {
  color: #4a6491;
}

.badge {
  position: absolute;
  background: #ff4757;
  color: white;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
```

### 主内容区样式
```css
.main-content {
  min-height: 100vh;
  padding-bottom: calc(50px + env(safe-area-inset-bottom));
}
```

## 五、数据来源

- 消息角标：从后端 API 获取未读消息数
- 路由高亮：Vue Router 自动匹配

## 六、风险与注意事项

1. **路由冲突**：确保所有子路由路径正确，避免与其他路由冲突
2. **样式覆盖**：注意 Dashboard.vue 中原有的底部栏样式需要移除
3. **安全区适配**：确保在不同设备上正确显示
4. **组件引用**：确保所有新增页面的组件路径正确

## 七、测试要点

1. 底部导航栏在所有页面都显示
2. 点击 Tab 切换页面，当前 Tab 高亮
3. 消息角标正确显示
4. 在不同屏幕尺寸下布局正常
5. 安全区适配正常（iOS 刘海屏、底部安全区）
