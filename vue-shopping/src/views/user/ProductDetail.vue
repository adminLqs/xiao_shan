<template>
  <div class="product-detail-container">
    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="skeleton-container">
      <!-- 主图骨架 -->
      <div class="skeleton-swiper"></div>

      <!-- 店铺卡片骨架 -->
      <div class="skeleton-shop-card">
        <div class="skeleton-shop-avatar"></div>
        <div class="skeleton-shop-info">
          <div class="skeleton-line short"></div>
          <div class="skeleton-line medium"></div>
        </div>
      </div>

      <!-- 价格区骨架 -->
      <div class="skeleton-price-section">
        <div class="skeleton-price"></div>
        <div class="skeleton-line short"></div>
      </div>

      <!-- 标题区骨架 -->
      <div class="skeleton-title-section">
        <div class="skeleton-line"></div>
        <div class="skeleton-line short"></div>
      </div>

      <!-- 品牌卡片骨架 -->
      <div class="skeleton-brand-card">
        <div class="skeleton-brand-icon"></div>
        <div class="skeleton-brand-info">
          <div class="skeleton-line short"></div>
          <div class="skeleton-line medium"></div>
        </div>
      </div>

      <!-- 数据看板骨架 -->
      <div class="skeleton-dashboard">
        <div class="skeleton-dashboard-header">
          <div class="skeleton-line short"></div>
        </div>
        <div class="skeleton-dashboard-grid">
          <div class="skeleton-dashboard-cell" v-for="i in 4" :key="i">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
          </div>
        </div>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- 服务行骨架 -->
      <div class="skeleton-service-row">
        <div class="skeleton-line medium"></div>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- 双栏卡片骨架 -->
      <div class="skeleton-dual-cards">
        <div class="skeleton-card"></div>
        <div class="skeleton-card"></div>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- 商品参数骨架 -->
      <div class="skeleton-params">
        <div class="skeleton-line short"></div>
        <div class="skeleton-params-body">
          <div class="skeleton-param-row" v-for="i in 3" :key="i">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line medium"></div>
          </div>
        </div>
      </div>

      <!-- 灰色间隔 -->
      <div class="gray-divider"></div>

      <!-- 评价精选骨架 -->
      <div class="skeleton-reviews">
        <div class="skeleton-reviews-header">
          <div class="skeleton-line short"></div>
        </div>
        <div class="skeleton-reviews-scroll">
          <div class="skeleton-review-card" v-for="i in 3" :key="i">
            <div class="skeleton-review-header">
              <div class="skeleton-avatar"></div>
              <div class="skeleton-review-user">
                <div class="skeleton-line short"></div>
              </div>
            </div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 底部留白 -->
      <div class="bottom-space"></div>
    </div>

    <!-- 商品详情内容 -->
    <template v-else>
    <div class="product-detail-content">
      <!-- ========== 商家信息栏（仅下架/删除状态显示，且非本人） ========== -->
      <div class="seller-bar" v-if="sellerInfo && (!product || product.status !== 1) && product?.sellerId !== currentUserId" @click="goToShop">
        <img :src="sellerInfo.storeAvatar || sellerDefaultAvatar" class="seller-avatar" @click="goToShop" />
        <div class="seller-info" @click="goToShop">
          <div class="seller-name">{{ sellerInfo.storeName || sellerInfo.name }}</div>
          <div class="seller-stats">{{ sellerInfo.fansCount || 0 }} 粉丝</div>
        </div>
        <button class="btn-contact-service" @click="contactService">
          <i class="fas fa-headset"></i>
          <span>客服</span>
        </button>
      </div>

      <!-- ========== 商品状态判断 ========== -->
      <!-- 已下架 -->
      <div v-if="product && product.status === 0" class="product-offline-notice">
        <i class="fas fa-exclamation-triangle"></i>
        <span>商品已下架</span>
      </div>
      <!-- 已删除 -->
      <div v-else-if="product && product.status === 2" class="product-deleted-notice">
        <i class="fas fa-times-circle"></i>
        <span>商品不存在</span>
      </div>
      <!-- 正常商品内容 -->
      <template v-else-if="product">

      <!-- ========== 主图 Swiper（全宽沉浸，支持左右滑动 + 自动播放） ========== -->
      <div class="product-swiper" @touchstart.passive="onTouchStart" @touchend="onTouchEnd">
        <div class="swiper-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
          <img v-for="(img, i) in productImages" :key="i" :src="img"
            class="swiper-image" @click="previewImage(i as number)" />
        </div>
        <div class="swiper-dots">
          <span v-for="(img, i) in productImages" :key="i"
                class="dot" :class="{ active: i === currentIndex }"></span>
        </div>
      </div>

      <!-- ========== 店铺卡片 - 嵌入主图下方，有重叠效果 ========== -->
      <div class="shop-card-inline" v-if="sellerInfo">
        <div class="shop-card-bg">
          <div class="shop-avatar-area" @click="goToShop">
            <img :src="sellerInfo.storeAvatar || sellerDefaultAvatar" class="shop-avatar-large" />
          </div>
          <div class="shop-info-area">
            <div class="shop-name-row">
              <span class="shop-name">{{ sellerInfo.storeName || sellerInfo.name }}</span>
              <span class="shop-level-badge">
                <i class="fas fa-crown"></i> 金牌卖家
              </span>
            </div>
            <div class="shop-stats">
              <span>{{ sellerInfo.fansCount || 0 }} 粉丝</span>
              <span class="stat-divider">|</span>
              <span>评分 {{ sellerInfo.rating > 0 ? sellerInfo.rating : '暂无' }}</span>
              <span class="stat-divider">|</span>
              <span v-if="sellerInfo.positiveRate > 0">好评率 {{ sellerInfo.positiveRate }}%</span>
            </div>
          </div>
          <div class="shop-actions">
            <button class="btn-follow-inline" :class="{ followed: isFollowed }" @click="toggleFollow">
              {{ isFollowed ? '已关注' : '+ 关注' }}
            </button>
            <button class="btn-enter-shop" @click="goToShop">
              进店逛逛 <i class="fas fa-arrow-right"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- ========== 价格区 ========== -->
      <div class="price-section">
        <div class="price-row">
          <template v-if="bestCoupon && canUseCoupon">
            <span class="coupon-price">¥{{ formatPrice(couponPrice) }}</span>
            <span class="coupon-label">({{ bestCouponDisplay }})</span>
            <span class="current-price-line">¥{{ formatPrice(displayPrice) }}</span>
          </template>
          <template v-else>
            <span class="current-price">¥{{ formatPrice(displayPrice) }}</span>
            <span v-if="displayOriginalPrice && displayOriginalPrice > displayPrice" class="original-price">¥{{ formatPrice(displayOriginalPrice) }}</span>
            <span v-if="displayOriginalPrice && displayOriginalPrice > displayPrice" class="discount-badge">{{ getDiscountPercent(displayPrice, displayOriginalPrice) }}% OFF</span>
          </template>
          <span v-if="selectedSkuInfo" class="selected-spec-hint" @click="resetSpecs">
            {{ selectedSkuInfo.skuName || '已选规格' }} <i class="fas fa-times"></i>
          </span>
        </div>
        <div class="sales-row">
          <span class="sales-count">已售 {{ product.salesCount || 0 }}件</span>
          <span class="view-count" v-if="product.viewCount">
            <i class="fas fa-eye"></i> {{ product.viewCount }} 浏览
          </span>
        </div>
      </div>

      <!-- ========== 优惠券领取区 ========== -->
      <div class="coupon-section" v-if="productCoupons.length > 0">
        <div class="coupon-section-header">
          <span class="coupon-icon">🎫</span>
          <span class="coupon-title">可用优惠券</span>
          <span class="coupon-count">{{ productCoupons.length }}张</span>
        </div>
        <div class="coupon-list">
          <div
            v-for="coupon in productCoupons"
            :key="coupon.id"
            class="coupon-card"
            :class="{ received: coupon.received, disabled: coupon.received || coupon.status !== 1 }"
          >
            <div class="coupon-left">
              <span class="coupon-amount">¥{{ coupon.discountAmount || coupon.amount || 0 }}</span>
              <span class="coupon-condition">{{ getCouponDisplayText(coupon) }}</span>
            </div>
            <div class="coupon-right">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-validity">{{ formatCouponDate(coupon.validStart || coupon.startTime) }} - {{ formatCouponDate(coupon.validEnd || coupon.endTime) }}</div>
              <button
                class="coupon-receive-btn"
                :disabled="coupon.received || coupon.status !== 1"
                @click="receiveCoupon(coupon.id)"
              >
                {{ coupon.received ? '已领取' : (coupon.status !== 1 ? '已结束' : '立即领取') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 商品标题 ========== -->
      <div class="title-section">
        <div class="product-title" :class="{ expanded: titleExpanded }" @click="titleExpanded = !titleExpanded">
          {{ product.name }}
        </div>
      </div>

      <!-- ========== 品牌行 ========== -->
      <div class="brand-row" v-if="product.brand">
        <div class="brand-card">
          <div class="brand-logo-area">
            <img src="@/static/images/云杉购图袋.png" class="brand-logo-img" />
          </div>
          <div class="brand-info">
            <div class="brand-label">品牌</div>
            <div class="brand-name">{{ product.brand }}</div>
          </div>
          <div class="brand-cert">
            <i class="fas fa-check-circle"></i>
            <span>云杉认证</span>
          </div>
        </div>
      </div>

      <!-- ========== 商品描述 ========== -->
      <div class="desc-section" v-if="product.description">
        <div class="desc-label">商品描述</div>
        <div class="desc-content">{{ product.description }}</div>
      </div>

      <!-- 无描述时的可爱提示 -->
      <div class="desc-section empty-desc" v-else>
        <div class="empty-desc-content">
          <i class="fas fa-feather-alt"></i>
          <span>商家很懒，没有写描述~</span>
        </div>
      </div>

      <!-- ========== 真实数据看板 ========== -->
      <div class="quality-dashboard" v-if="product">
        <div class="quality-header">
          <span class="quality-title">📊 真实数据看板</span>
          <span class="quality-hint">数据透明 · 放心购买</span>
        </div>
        <div class="quality-grid-4">
          <div class="quality-cell">
            <div class="q-value highlight-green">{{ qualityStats.positiveRate }}%</div>
            <div class="q-label">好评率</div>
            <div class="q-sub" v-if="qualityStats.positiveRate >= 90">超越同行</div>
          </div>
          <div class="quality-cell">
            <div class="q-value highlight-orange">{{ (previewComments || []).length > 0 ? qualityStats.avgRating : '暂无' }}</div>
            <div class="q-label">综合评分</div>
            <div class="q-stars" v-if="qualityStats.avgRating > 0">
              <i v-for="i in 5" :key="i" class="fas fa-star"
                :class="{ active: i <= Math.round(qualityStats.avgRating) }"></i>
            </div>
          </div>
          <div class="quality-cell">
            <div class="q-value">{{ formatSaleCount(qualityStats.totalSales) }}</div>
            <div class="q-label">累计销量</div>
            <div class="q-sub" v-if="qualityStats.recentSales > 0 && qualityStats.totalSales > 0">近30天 {{ qualityStats.recentSales }} 件</div>
          </div>
          <div class="quality-cell">
            <div class="q-value highlight-blue">{{ qualityStats.avgDeliveryHours || 24 }}h</div>
            <div class="q-label">平均发货</div>
            <div class="q-sub">准时率 {{ qualityStats.onTimeRate || 99 }}%</div>
          </div>
        </div>
      </div>

      <!-- ========== 优惠券/满减标签 ========== -->
      <div class="promo-tags" v-if="(promoTags || []).length > 0">
        <span class="promo-tag" v-for="tag in (promoTags || [])" :key="tag">{{ tag }}</span>
      </div>

      <!-- ========== 灰色间隔 ========== -->
      <div class="gray-divider"></div>

      <!-- ========== 选择规格/数量行 ========== -->
      <div class="service-rows">
        <div class="service-row" @click="showSku = true">
          <span>📐 选择规格/数量</span>
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <!-- ========== 已选规格参数 ========== -->
      <div class="selected-spec-params" v-if="(selectedSkus || []).length > 0 && !showSku">
        <div class="spec-params-title">已选规格</div>
        <div class="spec-params-grid">
          <div v-for="(value, key) in currentSpecParams" :key="key" class="spec-param-item">
            <span class="spec-param-label">{{ key }}</span>
            <span class="spec-param-value">{{ value }}</span>
          </div>
        </div>
      </div>



      <!-- ========== 发货 + 服务保障（双栏卡片） ========== -->
      <div class="dual-card-section">
        <div class="dual-card delivery-card">
          <div class="card-header">
            <div class="card-icon">📦</div>
            <div class="card-title">发货信息</div>
          </div>
          <div v-if="product.deliveryCity || product.weight || product.isFreeShipping !== undefined" class="card-content">
            <div class="info-row" v-if="product.deliveryCity">
              <span class="info-label">发货地</span>
              <span class="info-value">{{ product.deliveryCity }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">运费</span>
              <span class="info-value" :class="{ highlight: product.isFreeShipping }">
                {{ product.isFreeShipping ? '包邮' : '按模板计算' }}
              </span>
            </div>
            <div class="info-row" v-if="product.weight">
              <span class="info-label">重量</span>
              <span class="info-value">{{ product.weight }}kg</span>
            </div>
          </div>
          <div v-else class="card-empty">
            <i class="fas fa-truck"></i>
            <span>商家未填写发货信息</span>
          </div>
        </div>
        <div class="dual-card service-card">
          <div class="card-header">
            <div class="card-icon">🛡️</div>
            <div class="card-title">服务保障</div>
          </div>
          <div class="card-content">
            <div v-if="(guaranteeList || []).length > 0" class="guarantee-list">
              <div class="guarantee-item" v-for="item in guaranteeList.slice(0, 4)" :key="item">
                <i class="fas fa-check"></i>
                <span>{{ item }}</span>
              </div>
            </div>
            <div v-else class="card-empty">
              <i class="fas fa-shield-alt"></i>
              <span>商家未提供服务保障</span>
            </div>
          </div>
        </div>
      </div>

      <div class="gray-divider"></div>

      <!-- ========== 商品参数（时间轴样式） ========== -->
      <div class="params-timeline" v-if="(productParams || []).length > 0">
        <div class="timeline-header" @click="paramsExpanded = !paramsExpanded">
          <span>📋 商品参数</span>
          <i class="fas" :class="paramsExpanded ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
        </div>
        <div class="timeline-body" v-show="paramsExpanded">
          <div class="timeline-row" v-for="param in (productParams || [])" :key="param.id">
            <div class="timeline-dot"></div>
            <div class="timeline-line" v-if="(productParams || []).indexOf(param) < (productParams || []).length - 1"></div>
            <div class="timeline-content">
              <span class="timeline-label">{{ param.paramName }}</span>
              <span class="timeline-value">{{ param.paramValue }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 灰色间隔 ========== -->
      <div class="gray-divider"></div>

      <!-- ========== 评价精选（横向滑动） ========== -->
      <div class="reviews-strip" v-if="(previewComments || []).length > 0">
        <div class="strip-header">
          <span>💬 买家怎么说</span>
          <span class="strip-all" @click="openComments">全部 {{ commentTotal }} 条 →</span>
        </div>
        <div class="strip-scroll">
          <div class="review-mini-card" v-for="comment in previewComments" :key="comment.id">
            <div class="rmc-header">
              <img :src="comment.avatar || defaultAvatar" class="rmc-avatar" />
              <div>
                <div class="rmc-name">{{ comment.userName || '匿名用户' }}</div>
                <div class="rmc-stars">
                  <i v-for="i in 5" :key="i" class="fas fa-star" :class="{ active: i <= (comment.rating || 5) }"></i>
                </div>
              </div>
            </div>
            <div class="rmc-content">{{ comment.content }}</div>
            <div class="rmc-images" v-if="comment.images && comment.images.length > 0">
              <img v-for="(img, idx) in comment.images.slice(0, 2)" :key="idx" :src="img" class="rmc-img" />
            </div>
            <div class="rmc-videos" v-if="comment.videos && comment.videos.length > 0">
              <div class="rmc-video-thumb" v-for="(video, vIdx) in comment.videos.slice(0, 1)" :key="'v-'+vIdx" @click.stop="previewVideo(video.videoUrl)">
                <img v-if="video.coverUrl" :src="video.coverUrl" class="rmc-video-cover" />
                <div v-else class="rmc-video-placeholder">
                  <i class="fas fa-video"></i>
                </div>
                <div class="rmc-video-play">
                  <i class="fas fa-play"></i>
                </div>
                <span class="rmc-video-dur" v-if="video.duration">{{ formatDuration(video.duration) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 评价精选 - 无评论时 -->
      <div class="reviews-strip empty-reviews" v-else>
        <div class="empty-reviews-content">
          <div class="empty-reviews-icon">💬</div>
          <p class="empty-reviews-title">暂无评价</p>
          <p class="empty-reviews-desc">这款商品还没有人评价，快来成为第一个吧~</p>
        </div>
      </div>

      <!-- ========== 灰色间隔 ========== -->
      <div class="gray-divider"></div>

      <!-- ========== 商品详情（富文本） ========== -->
      <div class="detail-section">
        <div class="section-title" @click="detailExpanded = !detailExpanded">
          <i class="fas fa-file-alt"></i> 商品详情
          <i class="fas" :class="detailExpanded ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
        </div>
        <div class="detail-content" v-show="detailExpanded">
          <!-- 有详情 -->
          <div v-if="product.detailHtml" v-html="product.detailHtml"></div>
          <!-- 无详情 -->
          <div v-else class="empty-detail">
            <div class="empty-detail-icon">📦</div>
            <p class="empty-detail-title">暂无商品详情</p>
            <p class="empty-detail-desc">商家正在努力编辑中，敬请期待~</p>
          </div>
        </div>
      </div>

      <!-- 底部留空 -->
      <div class="bottom-space"></div>

      </template>
    </div>

    <!-- 空状态（product 为 null） -->
    <div v-if="!product" class="empty-state">
      <i class="fas fa-box-open"></i>
      <p>商品不存在或已下架</p>
    </div>

    <!-- ========== 固定返回按钮（悬浮胶囊隐藏时显示） ========== -->
    <div class="back-btn-fixed" v-if="!showDetailCapsule" @click="router.back()">
      <i class="fas fa-chevron-left"></i>
    </div>

    <!-- ========== 悬浮胶囊导航（长方形置顶渐隐渐显） ========== -->
    <div class="detail-floating-capsule" :class="{ visible: showDetailCapsule }">
      <!-- 左侧：返回 -->
      <button class="capsule-back" @click="router.back()">
        <i class="fas fa-chevron-left"></i>
      </button>

      <!-- 中间：店铺头像+名称 -->
      <div class="capsule-center" v-if="sellerInfo">
        <img :src="sellerInfo?.storeAvatar || sellerDefaultAvatar" class="capsule-avatar" />
        <span class="capsule-shop-name">{{ sellerInfo?.storeName || '' }}</span>
      </div>
    </div>

    <!-- ========== 底部固定操作栏 ========== -->
    <div class="bottom-bar" v-if="product && product.status === 1 && product?.sellerId !== currentUserId">
      <!-- 收藏按钮：非本人时才显示 -->
      <div class="bottom-icon" @click="toggleFavorite">
        <i :class="isFavorited ? 'fas fa-heart' : 'far fa-heart'" :style="{ color: isFavorited ? '#ff4757' : '#666' }"></i>
        <span>收藏</span>
      </div>
      <!-- 客服按钮 -->
      <div class="bottom-icon" @click="contactService">
        <i class="fas fa-headset"></i>
        <span>客服</span>
      </div>
      <!-- 加购按钮 -->
      <button v-if="totalStock <= 0 || isSingleSpecSoldOut" class="btn-cart" disabled>已售罄</button>
      <button v-else class="btn-cart" @click="handleAddToCart">加入购物车</button>
      <!-- 购买按钮 -->
      <button v-if="totalStock <= 0 || isSingleSpecSoldOut" class="btn-buy" disabled>已售罄</button>
      <button v-else class="btn-buy" @click="handleBuyNow">立即购买</button>
    </div>

    <!-- ========== SKU 选择弹窗 ========== -->
    <div class="sku-modal" v-if="showSku" @click.self="showSku = false">
      <div class="sku-content">
        <div class="sku-header">
          <img :src="currentSkuPreviewImage" class="sku-image" @click="previewSkuImage" />
          <div class="sku-info">
            <div class="sku-name">{{ product?.name }}</div>
            <span class="sku-price">
              ¥{{ formatPrice(skuModalPrice) }}
            </span>
          </div>
          <button class="sku-close" @click="showSku = false">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="sku-body">
          <!-- 已选规格标签 -->
          <div v-if="(selectedSkus || []).length > 0" class="selected-skus">
            <div class="selected-label">已选规格：</div>
            <div class="selected-tags">
              <span
                v-for="item in (selectedSkus || [])"
                :key="item.skuId"
                class="selected-tag"
              >
                {{ getSkuName(item.skuId) }}
                <i class="fas fa-times" @click="removeSku(item.skuId)"></i>
              </span>
            </div>
          </div>

          <!-- SKU 选择区域 -->
          <div v-if="(skuSpecGroups || []).length > 0" class="sku-spec-groups">
            <div v-for="group in (skuSpecGroups || [])" :key="group.name" class="spec-group">
              <div class="spec-group-title">{{ group.name }}</div>
              <div class="spec-group-values">
                <span
                  v-for="value in group.values"
                  :key="value"
                  class="spec-value-tag"
                  :class="{
                    active: selectedSpecs[group.name] === value,
                    'sold-out': !isSpecValueAvailable(group.name, value)
                  }"
                  @click="isSpecValueAvailable(group.name, value) ? handleSpecClick(group.name, value) : null"
                >
                  {{ value }}
                  <span v-if="!isSpecValueAvailable(group.name, value)" class="sold-out-text">售罄</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 传统 SKU 列表（多选模式或无规格分组时显示） -->
          <div v-if="((skuSpecGroups || []).length === 0 || isMultiSelect) && (skuList || []).length > 0" class="sku-list">
            <div class="sku-list-label">规格选择：</div>
            <div v-for="sku in (skuList || [])" :key="sku.id" class="sku-item">
              <img :src="sku.skuImage || (productImages || [])[0]" class="sku-item-image" />
              <div class="sku-item-info">
                <div class="sku-item-name">{{ sku.skuName || '默认' }}</div>
                <div class="sku-item-price">
                  ¥{{ formatPrice(sku.price) }}
                  <span v-if="sku.originalPrice" class="sku-item-original-price">¥{{ formatPrice(sku.originalPrice) }}</span>
                </div>
                <div class="sku-item-stock">库存 {{ sku.stock }} 件</div>
              </div>
              <div class="sku-item-control">
                <div v-if="isMultiSelect" class="quantity-control">
                  <button
                    @click="decreaseSkuQuantity(sku.id)"
                    :disabled="getSkuQuantity(sku.id) <= 0"
                  >-</button>
                  <span>{{ getSkuQuantity(sku.id) }}</span>
                  <button
                    @click="increaseSkuQuantity(sku.id)"
                    :disabled="getSkuQuantity(sku.id) >= sku.stock"
                  >+</button>
                </div>
                <div v-else class="select-btn" @click="toggleSingleSku(sku.id)">
                  <span v-if="getSkuQuantity(sku.id) > 0">已选</span>
                  <span v-else>选择</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 多选开关 -->
          <div class="multi-select-switch">
            <label class="checkbox-label">
              <input type="checkbox" v-model="isMultiSelect" @change="onMultiSelectChange" />
              <span>多选购买（可同时选多个规格）</span>
            </label>
          </div>
        </div>

        <div class="sku-footer">
          <div v-if="(selectedSkus || []).length > 0" class="total-info">
            合计：<span class="total-amount">¥{{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="btn-group">
            <div v-if="(skuSpecGroups || []).length > 0 && !allSpecsSelected" class="select-spec-notice">
              <i class="fas fa-hand-point-right"></i> 请选择完整规格
            </div>
            <div v-else-if="currentSku && currentSku.stock <= 0" class="sold-out-notice">
              <i class="fas fa-exclamation-circle"></i> 该规格已售罄
            </div>
            <template v-else>
              <button
                class="btn-sku-cart"
                @click="addToCart"
                :disabled="!canAddToCart"
                :class="{ 'btn-disabled': !canAddToCart }"
              >加入购物车</button>
              <button
                class="btn-sku-buy"
                @click="buyNow"
                :disabled="!canAddToCart"
                :class="{ 'btn-disabled': !canAddToCart }"
              >立即购买</button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== SKU 多选弹窗 ========== -->
    <div class="multi-sku-modal" v-if="showMultiSku" @click.self="closeMultiSku">
      <div class="multi-sku-content">
        <div class="multi-sku-header">
          <span class="multi-sku-title">多选购买</span>
          <button class="multi-sku-close" @click="closeMultiSku">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <div class="multi-sku-body">
          <div v-if="(skuList || []).length > 0" class="multi-sku-list">
            <div v-for="sku in (skuList || [])" :key="sku.id" class="multi-sku-item">
              <img :src="sku.skuImage || (productImages || [])[0]" class="multi-sku-image" @click.stop="previewSkuImage(sku)" />
              <div class="multi-sku-info">
                <div class="multi-sku-name">{{ sku.skuName || '默认' }}</div>
                <div class="multi-sku-price">¥{{ formatPrice(sku.price) }}</div>
                <div class="multi-sku-stock">库存 {{ sku.stock }} 件</div>
              </div>
              <div class="multi-sku-control">
                <button
                  @click="decreaseSkuQuantity(sku.id)"
                  :disabled="getSkuQuantity(sku.id) <= 0"
                  class="qty-btn qty-minus"
                >-</button>
                <span class="qty-value">{{ getSkuQuantity(sku.id) }}</span>
                <button
                  @click="increaseSkuQuantity(sku.id)"
                  :disabled="getSkuQuantity(sku.id) >= sku.stock"
                  class="qty-btn qty-plus"
                >+</button>
              </div>
            </div>
          </div>
        </div>

        <div class="multi-sku-footer">
          <div class="multi-sku-total">
            合计：<span class="total-price">¥{{ formatPrice(totalAmount) }}</span>
            <span class="total-qty">（共{{ totalQuantity }}件）</span>
          </div>
          <div class="multi-sku-actions">
            <button
              class="btn-multi-cart"
              @click="addToCartFromMulti"
              :disabled="totalQuantity <= 0"
              :class="{ 'btn-disabled': totalQuantity <= 0 }"
            >加入购物车</button>
            <button
              class="btn-multi-buy"
              @click="buyNowFromMulti"
              :disabled="totalQuantity <= 0"
              :class="{ 'btn-disabled': totalQuantity <= 0 }"
            >立即购买</button>
          </div>
        </div>
      </div>
    </div>
    </template>

    <!-- 全局媒体预览 -->
    <ImagePreview
      v-if="showMediaPreview"
      :mediaList="allMediaList"
      :currentIndex="mediaPreviewIndex"
      @close="closeMediaPreview"
      @update:index="mediaPreviewIndex = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch  } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authAPI } from '@/api/authAPI'
import Message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import ImagePreview from '@/components/ImagePreview.vue'
import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'
import defaultAvatar from '@/static/images/user-avatar.jpg'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const productId = computed(() => Number(route.params.productId))

// ==================== 响应式数据 ====================
const loading = ref(true)
const product = ref<any>(null)
const isFavorited = ref(false)
const currentUserId = ref(authStore.userId)
const titleExpanded = ref(false)
const detailExpanded = ref(true)
const descExpanded = ref(true)
const showSku = ref(false)
const showMultiSku = ref(false)

// 媒体预览相关
const showMediaPreview = ref(false)
const mediaPreviewIndex = ref(0)

// Swiper相关
const currentIndex = ref(0)
const touchStartX = ref(0)
const touchEndX = ref(0)
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

// 评价相关
const commentTotal = ref(0)
const previewComments = ref<any[]>([])

// 店铺信息（真实接口数据）
const sellerInfo = ref<any>(null)
const isFollowed = ref(false)

// 悬浮胶囊导航相关
const showDetailCapsule = ref(false)
const mainImageHeight = 375 // 主图高度（全屏宽度，约375px）

// 优惠券标签（真实接口数据）
const promoTags = ref<string[]>([])

// 商品可用优惠券
const productCoupons = ref<any[]>([])
const receivingCouponIds = ref<Set<number>>(new Set())

// 质量数据统计
const qualityStats = computed(() => {
  const totalSales = product.value?.salesCount || 0

  // 从评论数据计算（不用 sellerInfo 的数据）
  let avgRating = 0
  let positiveRate = 0

  if (previewComments.value.length > 0) {
    const totalRating = previewComments.value.reduce((sum, c) => sum + (c.rating || 0), 0)
    avgRating = Math.round((totalRating / previewComments.value.length) * 10) / 10

    const goodComments = previewComments.value.filter(c => (c.rating || 0) >= 4).length
    positiveRate = Math.round((goodComments / previewComments.value.length) * 100)
  }

  // 计算平均发货时间（从店铺信息，暂无则默认24h）
  const avgDeliveryHours = sellerInfo.value?.avgDeliveryHours || 24

  return {
    positiveRate: positiveRate,
    avgRating: avgRating,
    totalSales: totalSales || 0,
    recentSales: product.value?.recentSales || 0,
    avgDeliveryHours: avgDeliveryHours,
    onTimeRate: sellerInfo.value?.onTimeRate || 99
  }
})

// SKU 相关
const skuList = ref<any[]>([])
const skuCache = ref<Record<number, any[]>>({})
const isMultiSelect = ref(false)
interface SelectedSku {
  skuId: number
  quantity: number
}
const selectedSkus = ref<SelectedSku[]>([])

// 当前选中的规格值
const selectedSpecs = ref<Record<string, string>>({})

// 用户在弹窗中确认选择的 SKU（持久化，不随弹窗关闭而重置）
const selectedSkuInfo = ref<any>(null)

// 按规格类型分组
const skuSpecGroups = computed(() => {
  if (skuList.value.length === 0) return []
  const firstSku = skuList.value[0]
  if (!firstSku.specInfo) return []

  const specInfo = typeof firstSku.specInfo === 'string'
    ? JSON.parse(firstSku.specInfo)
    : firstSku.specInfo

  const groups: { name: string; values: string[] }[] = []
  for (const [name, _value] of Object.entries(specInfo)) {
    const values = [...new Set(skuList.value.map(sku => {
      const info = typeof sku.specInfo === 'string' ? JSON.parse(sku.specInfo) : sku.specInfo
      return info?.[name]
    }).filter(Boolean))]
    groups.push({ name, values })
  }
  return groups
})

// 汇总所有 SKU 总库存
const totalStock = computed(() => {
  return skuList.value.reduce((sum, sku) => sum + (sku.stock || 0), 0)
})

// 根据选中的规格查找对应 SKU
const currentSku = computed(() => {
  // 必须所有规格分组都选中
  if (skuSpecGroups.value.length === 0) return null
  if (!allSpecsSelected.value) return null

  return skuList.value.find(sku => {
    const info = typeof sku.specInfo === 'string' ? JSON.parse(sku.specInfo) : sku.specInfo
    if (!info) return false
    return Object.entries(selectedSpecs.value).every(([key, val]) => info[key] === val)
  })
})

// 判断是否所有规格分组都已选择
const allSpecsSelected = computed(() => {
  if (skuSpecGroups.value.length === 0) return true
  return skuSpecGroups.value.every(group => selectedSpecs.value[group.name])
})

// 是否可以加购/购买
const canAddToCart = computed(() => {
  // 无 SKU 时直接允许
  if (skuList.value.length === 0) return true
  // 多选模式：至少选中一个即可
  if (isMultiSelect.value) {
    return selectedSkus.value.length > 0 && selectedSkus.value.every(item => item.quantity > 0)
  }
  // 单选模式：有规格分组时需要选完所有规格，无规格分组时选中一个即可
  if (skuSpecGroups.value.length > 0) {
    return allSpecsSelected.value && selectedSkus.value.length > 0
  }
  return selectedSkus.value.length > 0 && selectedSkus.value.every(item => item.quantity > 0)
})

// 规格值对应的 SKU 是否有库存
const isSpecValueAvailable = (groupName: string, value: string) => {
  // 单级规格：直接判断
  if (skuSpecGroups.value.length === 1) {
    return skuList.value.some(sku => {
      const info = typeof sku.specInfo === 'string' ? JSON.parse(sku.specInfo) : sku.specInfo
      return info?.[groupName] === value && (sku.stock || 0) > 0
    })
  }

  // 多级规格：规格1始终可用
  const firstGroupName = skuSpecGroups.value[0]?.name
  if (groupName === firstGroupName) return true

  // 规格2+：规格1未选 → 不可用
  if (!firstGroupName) return false
  const firstValue = selectedSpecs.value[firstGroupName]
  if (!firstValue) return false

  // 规格2+：规格1已选 → 判断组合是否有库存
  return skuList.value.some(sku => {
    const info = typeof sku.specInfo === 'string' ? JSON.parse(sku.specInfo) : sku.specInfo
    if (!info) return false
    return info[firstGroupName] === firstValue
      && info[groupName] === value
      && (sku.stock || 0) > 0
  })
}

// 是否当前选中的是单级规格且已售罄
const isSingleSpecSoldOut = computed(() => {
  if (skuSpecGroups.value.length !== 1) return false
  if (!currentSku.value) return false
  return currentSku.value.stock <= 0
})

// 规格值到图片 URL 的映射
const specImageMap = computed(() => {
  const map: Record<string, string> = {}
  skuList.value.forEach(sku => {
    if (!sku.skuImage) return
    const info = typeof sku.specInfo === 'string' ? JSON.parse(sku.specInfo) : sku.specInfo
    if (!info) return
    for (const [key, val] of Object.entries(info)) {
      const imgKey = `${String(key)}:${String(val)}`
      map[imgKey] = sku.skuImage
    }
  })
  return map
})

// SKU ID 到图片 URL 的映射（兜底逻辑）
const skuImageMapping = computed(() => {
  const mapping: Record<string, string> = {}
  const productImgList = productImages.value

  skuList.value.forEach((sku, index) => {
    if (sku.skuImage) {
      mapping[sku.id] = sku.skuImage
      return
    }

    const specInfo = typeof sku.specInfo === 'string'
      ? JSON.parse(sku.specInfo)
      : sku.specInfo

    for (const [_key, val] of Object.entries(specInfo || {})) {
      const matchedImg = productImgList.find((img: string) =>
        img.toLowerCase().includes(String(val).toLowerCase())
      )
      if (matchedImg) {
        mapping[sku.id] = matchedImg
        break
      }
    }

    if (!mapping[sku.id]) {
      const imgIndex = index % productImgList.length
      mapping[sku.id] = productImgList[imgIndex] || productImgList[0] || ''
    }
  })

  const hasNullImages = skuList.value.some(s => !s.skuImage)

  return mapping
})

// 当前选中 SKU 的预览图片
const currentSkuPreviewImage = computed(() => {
  // 完整选中 SKU 时，返回该 SKU 的图片
  if (currentSku.value) {
    return currentSku.value.skuImage || productImages.value[0] || ''
  }
  // 未完整选中时，保持商品主图
  return productImages.value[0] || ''
})

// 弹窗内显示的 SKU 价格
const skuModalPrice = computed(() => {
  if (currentSku.value) {
    return currentSku.value.price
  }
  return minPrice.value
})

// 点击规格标签
const handleSpecClick = (groupName: string, value: string) => {
  if (!isSpecValueAvailable(groupName, value)) {
    Message.warning('该规格已售罄')
    return
  }
  selectSpec(groupName, value)
}

// 选择规格值
const selectSpec = (groupName: string, value: string) => {
  if (selectedSpecs.value[groupName] === value) {
    delete selectedSpecs.value[groupName]
  } else {
    selectedSpecs.value = { ...selectedSpecs.value, [groupName]: value }
  }

  // 同步到 selectedSkus（用于加购/购买）
  if (currentSku.value) {
    selectedSkus.value = [{ skuId: currentSku.value.id, quantity: 1 }]
  } else {
    selectedSkus.value = []
  }
}

// 商品参数相关
const paramsExpanded = ref(true)
const productParams = computed(() => {
  return product.value?.params || []
})

// ==================== 计算属性 ====================
const productImages = computed(() => {
  if (!product.value) return []
  // 新字段：productImages 数组
  if (product.value.productImages && Array.isArray(product.value.productImages)) {
    return product.value.productImages.map((img: any) => img.image || img)
  }
  // 旧字段：images 逗号分隔字符串
  if (product.value.images && typeof product.value.images === 'string') {
    return product.value.images.split(',').filter((img: string) => img.trim())
  }
  return []
})

// ==================== SKU 相关计算属性 ====================
const totalQuantity = computed(() => {
  return selectedSkus.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return selectedSkus.value.reduce((sum, item) => {
    const sku = skuList.value.find(s => s.id === item.skuId)
    return sum + (sku?.price || 0) * item.quantity
  }, 0)
})

const currentImage = computed(() => productImages.value[currentIndex.value] || '')

const minPrice = computed(() => {
  if (skuList.value.length === 0) return 0
  return Math.min(...skuList.value.map(sku => sku.price || 0))
})

const maxPrice = computed(() => {
  if (skuList.value.length === 0) return 0
  return Math.max(...skuList.value.map(sku => sku.price || 0))
})

const displayPrice = computed(() => {
  // 优先使用用户在弹窗中已确认选择的 SKU
  if (selectedSkuInfo.value) {
    return selectedSkuInfo.value.price || minPrice.value
  }
  // 有规格分组且已选中完整规格，返回当前 SKU 价格
  if (skuSpecGroups.value.length > 0 && currentSku.value) {
    return currentSku.value.price || minPrice.value
  }
  // 无规格分组
  if (skuList.value.length === 0) {
    return product.value?.price || 0
  }
  // 有多规格但未选完，使用最低价
  return minPrice.value
})

const displayOriginalPrice = computed(() => {
  // 优先使用用户在弹窗中已确认选择的 SKU 原价
  if (selectedSkuInfo.value) {
    return selectedSkuInfo.value.originalPrice || null
  }
  // 有规格分组且已选中完整规格，返回当前 SKU 原价
  if (skuSpecGroups.value.length > 0 && currentSku.value) {
    return currentSku.value.originalPrice || null
  }
  if (skuList.value.length === 0) return null
  // 取所有 SKU 中最低的原价作为"划线价"展示
  const minOriginal = skuList.value
    .filter(s => s.originalPrice && s.originalPrice > (s.price || 0))
    .map(s => s.originalPrice)
  return minOriginal.length > 0 ? Math.min(...minOriginal) : null
})

const discountPercent = computed(() => {
  if (!displayOriginalPrice.value || !displayPrice.value) return ''
  const percent = Math.round((displayPrice.value / displayOriginalPrice.value) * 10)
  return percent.toString()
})

const calculateCouponDiscount = (coupon: any, price: number): { canUse: boolean; discount: number } => {
  if (!coupon) return { canUse: false, discount: 0 }
  const type = coupon.type || coupon.couponType
  if (!type) return { canUse: false, discount: 0 }

  switch (type) {
    case 'FULL_REDUCTION':
    case 1: {
      const minAmount = Number(coupon.minAmount) || 0
      const discountAmount = Number(coupon.discountAmount) || 0
      return {
        canUse: price >= minAmount,
        discount: price >= minAmount ? discountAmount : 0
      }
    }
    case 'DISCOUNT':
    case 2: {
      const discountRate = Number(coupon.discountRate) || 0
      if (discountRate <= 0 || discountRate >= 10) return { canUse: false, discount: 0 }
      return {
        canUse: true,
        discount: price * (1 - discountRate / 10)
      }
    }
    case 'NO_THRESHOLD':
    case 3: {
      const discountAmount = Number(coupon.discountAmount) || 0
      const canUse = price > discountAmount
      return {
        canUse: canUse,
        discount: canUse ? discountAmount : 0
      }
    }
    default:
      return { canUse: false, discount: 0 }
  }
}

const getCouponDisplayText = (coupon: any): string => {
  const type = coupon.type || coupon.couponType
  if (!type) return ''

  switch (type) {
    case 'FULL_REDUCTION':
    case 1:
      return `满${coupon.minAmount}减${coupon.discountAmount}`
    case 'DISCOUNT':
    case 2:
      return `${coupon.discountRate}折`
    case 'NO_THRESHOLD':
    case 3:
      return `无门槛减${coupon.discountAmount}元`
    default:
      return ''
  }
}

const bestCoupon = computed(() => {
  if (productCoupons.value.length === 0) return null
  
  let best = null
  let maxDiscount = 0

  productCoupons.value.forEach(coupon => {
    const { canUse, discount } = calculateCouponDiscount(coupon, displayPrice.value)
    if (canUse && discount > maxDiscount) {
      maxDiscount = discount
      best = coupon
    }
  })

  return best
})

const canUseCoupon = computed(() => {
  if (!bestCoupon.value) return false
  const { canUse } = calculateCouponDiscount(bestCoupon.value, displayPrice.value)
  return canUse
})

const couponPrice = computed(() => {
  if (!bestCoupon.value || !canUseCoupon.value) return displayPrice.value
  const { discount } = calculateCouponDiscount(bestCoupon.value, displayPrice.value)
  return Math.max(displayPrice.value - discount, 0)
})

const bestCouponDisplay = computed(() => {
  if (!bestCoupon.value) return ''
  return getCouponDisplayText(bestCoupon.value)
})

const guaranteeList = computed(() => {
  if (!product.value?.serviceGuarantee) return []
  return product.value.serviceGuarantee.split(',').filter((s: string) => s.trim())
})

const currentSpecParams = computed(() => {
  if (selectedSkus.value.length === 0) return {}
  const firstSku = skuList.value.find(s => s.id === selectedSkus.value[0]?.skuId)
  if (!firstSku?.specInfo) return {}
  try {
    return typeof firstSku.specInfo === 'string'
      ? JSON.parse(firstSku.specInfo)
      : firstSku.specInfo
  } catch {
    return {}
  }
})

// ==================== 图片预加载 ====================
const preloadImages = (urls: string[]) => {
  urls.forEach((url: any) => {
    const img = new Image()
    img.src = url
  })
}

// ==================== 商品详情加载 ====================
const loadProductDetail = async () => {
  loading.value = true
  try {
    const response = await authAPI.getProduct(productId.value)

    if (response.success && response.data?.product) {
      const productData = response.data.product

      if (productData.status === 2) {
        product.value = null
        loading.value = false
        return
      }

      product.value = productData
      commentTotal.value = product.value.commentCount || 0

      // 图片预加载
      const images = productImages.value
      if (images.length > 0) {
        preloadImages(images)
      }

      // 加载关注状态
      if (product.value.sellerId) {
        await checkFollowStatus()
      }

      // 加载收藏状态
      await checkFavoriteStatus()

      await loadSkuList(true)

      // 加载商品可用优惠券
      await loadProductCoupons()

      // 从评论跳转时自动选择规格
      await autoSelectSkuFromUrl()
    }
  } catch (error: any) {
    Message.error(error.message || '加载商品失败')
  } finally {
    loading.value = false
  }
}

// ==================== 从 URL 参数自动选择 SKU ====================
const autoSelectSkuFromUrl = async () => {
  const skuSpec = route.query.skuSpec as string
  if (!skuSpec || skuList.value.length === 0) return

  // 先尝试按 skuName 直接匹配（如 "皓月白-23L"）
  const matchedByName = skuList.value.find(sku => sku.skuName === skuSpec)
  if (matchedByName) {
    selectedSkus.value = [{ skuId: matchedByName.id, quantity: 1 }]
    selectedSpecs.value = typeof matchedByName.specInfo === 'string'
      ? JSON.parse(matchedByName.specInfo) : matchedByName.specInfo
    selectedSkuInfo.value = matchedByName
    Message.success('已自动选择：' + matchedByName.skuName)
    return
  }

  // 再尝试按冒号格式解析（如 "颜色:红色,内存:128GB"）
  const specMap = parseSkuSpec(skuSpec)
  if (Object.keys(specMap).length === 0) return

  // 查找匹配的 SKU
  const matchedSku = skuList.value.find(sku => {
    const info = typeof sku.specInfo === 'string' ? JSON.parse(sku.specInfo) : sku.specInfo
    return Object.keys(specMap).every(key => info[key] === specMap[key])
  })

  if (matchedSku) {
    selectedSkus.value = [{ skuId: matchedSku.id, quantity: 1 }]
    selectedSpecs.value = specMap
    selectedSkuInfo.value = matchedSku
    Message.success('已自动选择同款规格')
  }
}

// 解析 skuSpec 字符串
const parseSkuSpec = (specStr: string): Record<string, string> => {
  const result: Record<string, string> = {}
  const pairs = specStr.split(',')
  for (const pair of pairs) {
    const [key, value] = pair.split(':')
    if (key && value) {
      result[key.trim()] = value.trim()
    }
  }
  return result
}

// ==================== 加载商品可用优惠券 ====================
const loadProductCoupons = async () => {
  try {
    const response = await authAPI.getProductCoupons(productId.value)
    if (response.success && response.data?.coupons) {
      productCoupons.value = response.data.coupons
    }
  } catch (error: any) {
    console.warn('加载优惠券失败:', error)
  }
}

// ==================== 领取优惠券 ====================
const receiveCoupon = async (couponId: number) => {
  if (!authStore.isLoggedIn) {
    Message.warning('请先登录')
    router.push({ name: 'Login' })
    return
  }

  if (receivingCouponIds.value.has(couponId)) return
  receivingCouponIds.value.add(couponId)

  try {
    const response = await authAPI.receiveCoupon(couponId)
    if (response.success) {
      Message.success('领取成功')
      const coupon = productCoupons.value.find(c => c.id === couponId)
      if (coupon) {
        coupon.received = true
      }
    } else {
      Message.error(response.message || '领取失败')
    }
  } catch (error: any) {
    Message.error(error.message || '领取失败')
  } finally {
    receivingCouponIds.value.delete(couponId)
  }
}

// ==================== 格式化优惠券日期 ====================
const formatCouponDate = (dateStr: string) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${date.getMonth() + 1}/${date.getDate()}`
  } catch {
    return ''
  }
}

// ==================== 加载 SKU 列表（带缓存） ====================
const loadSkuList = async (forceRefresh = false) => {
  const cached = skuCache.value[productId.value]
  if (!forceRefresh && cached) {
    skuList.value = cached
    return
  }

  try {
    const response = await authAPI.getProductSkus(productId.value, { _t: Date.now() })
    if (response.success && response.data?.skus) {
      skuList.value = response.data.skus
      skuCache.value[productId.value] = response.data.skus
    }
  } catch {}
}



// ==================== SKU 选择逻辑 ====================
const getSkuName = (skuId: number): string => {
  const sku = skuList.value.find(s => s.id === skuId)
  return sku?.skuName || ''
}

const getSkuQuantity = (skuId: number): number => {
  const item = selectedSkus.value.find(s => s.skuId === skuId)
  return item?.quantity || 0
}

const increaseSkuQuantity = (skuId: number) => {
  const sku = skuList.value.find(s => s.id === skuId)
  if (!sku) return

  const existing = selectedSkus.value.find(s => s.skuId === skuId)
  if (existing) {
    if (existing.quantity < sku.stock) {
      existing.quantity++
    }
  } else {
    selectedSkus.value.push({ skuId, quantity: 1 })
  }
}

const decreaseSkuQuantity = (skuId: number) => {
  const existing = selectedSkus.value.find(s => s.skuId === skuId)
  if (existing) {
    if (existing.quantity <= 1) {
      removeSku(skuId)
    } else {
      existing.quantity--
    }
  }
}

const toggleSingleSku = (skuId: number) => {
  if (isMultiSelect.value) return   // 多选模式不走这个

  const existing = selectedSkus.value.find(s => s.skuId === skuId)
  if (existing) {
    if (existing.quantity > 1) {
      existing.quantity = 1
    } else {
      selectedSkus.value = []
    }
  } else {
    selectedSkus.value = [{ skuId, quantity: 1 }]
  }
}

const removeSku = (skuId: number) => {
  const index = selectedSkus.value.findIndex(s => s.skuId === skuId)
  if (index > -1) {
    selectedSkus.value.splice(index, 1)
  }
  // 同步清空规格选择
  selectedSpecs.value = {}
}

const onMultiSelectChange = () => {
  if (isMultiSelect.value) {
    // 打开多选弹窗前先清空选中状态
    selectedSkus.value = []
    // 关闭SKU弹窗，打开多选弹窗
    showSku.value = false
    showMultiSku.value = true
  } else {
    // 取消多选，保留第一个选中项
    if (selectedSkus.value.length > 1) {
      selectedSkus.value = selectedSkus.value.slice(0, 1)
    }
  }
}

const closeMultiSku = () => {
  showMultiSku.value = false
  isMultiSelect.value = false
  selectedSkus.value = []
}

const addToCartFromMulti = async () => {
  if (selectedSkus.value.length === 0) {
    Message.error('请选择规格')
    return
  }

  try {
    for (const item of selectedSkus.value) {
      await authAPI.addToCart({
        productId: product.value.id,
        skuId: item.skuId,
        quantity: item.quantity
      })
    }
    Message.success(`已添加 ${totalQuantity.value} 件商品到购物车`)
    closeMultiSku()
  } catch (error: any) {
    Message.error(error.message || '添加失败')
  }
}

const buyNowFromMulti = () => {
  if (selectedSkus.value.length === 0) {
    Message.error('请选择规格')
    return
  }

  const skuIds = selectedSkus.value.map(item => `${item.skuId}:${item.quantity}`).join(',')
  router.push({
    name: 'Checkout',
    query: {
      source: 'product',
      productId: product.value.id,
      skuIds
    }
  })
  closeMultiSku()
}

const resetSpecs = () => {
  selectedSkus.value = []
  isMultiSelect.value = false
  selectedSpecs.value = {}
  selectedSkuInfo.value = null
}

// 监听弹窗关闭，自动保存选中的 SKU
watch(showSku, (newVal, oldVal) => {
  if (oldVal === true && newVal === false && currentSku.value) {
    // 弹窗关闭时，如果已选中完整规格，保存 SKU 信息
    selectedSkuInfo.value = currentSku.value
  }
})

// 统一管理弹窗状态，控制页面滚动
const isAnyModalOpen = computed(() => showSku.value || showMultiSku.value)

watch(isAnyModalOpen, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})

// ==================== 加载店铺信息 ====================
const loadSellerInfo = async () => {
  try {
    const response = await authAPI.getSellerByProduct(productId.value)
    if (response.success && response.data?.seller) {
      sellerInfo.value = response.data.seller
    }
  } catch (error: any) {
    if (error.status !== 403) {
      Message.error('店铺信息加载失败')
    }
  }
}

// ==================== 检查关注状态 ====================
const checkFollowStatus = async () => {
  if (!product.value?.sellerId) return
  try {
    const response = await authAPI.checkFollowSeller(product.value.sellerId)
    if (response.success) {
      isFollowed.value = response.data?.isFollowed || false
    }
  } catch {}
}

// ==================== 加载优惠券 ====================
const loadCoupons = async () => {
  try {
    const response = await authAPI.getProductCoupons(productId.value)
    if (response.success && response.data?.coupons) {
      promoTags.value = response.data.coupons.map((coupon: any) => coupon.name || coupon.title)
    }
  } catch (error: any) {
    promoTags.value = []
  }
}

// ==================== 加载评价预览 ====================
const loadPreviewComments = async () => {
  try {
    const response = await authAPI.getProductReviews({
      productId: productId.value,
      page: 1,
      pageSize: 3
    })
    if (response.success && response.data?.records?.length > 0) {
      previewComments.value = response.data.records.map((comment: any) => {
        const userReviewVO = comment.review ? comment : { review: comment, userProfile: null, reviewImages: [], reviewVideos: [] }
        const review = userReviewVO.review || {}
        const userProfile = userReviewVO.userProfile || {}
        const reviewImages = userReviewVO.reviewImages || []
        const reviewVideos = userReviewVO.reviewVideos || userReviewVO.videos || []

        return {
          id: review.id,
          avatar: userProfile.avatar || defaultAvatar,
          userName: userProfile.nickname || '匿名用户',
          rating: review.rating || 5,
          content: review.comment || '',
          time: formatTime(review.createdAt),
          images: reviewImages.map((img: any) => img.image),
          videos: reviewVideos.map((v: any) => ({
            id: v.id,
            videoUrl: v.videoUrl || v.video_url,
            coverUrl: v.coverUrl || v.cover_url,
            duration: v.duration || 0
          }))
        }
      })
    }
  } catch (error: any) {
    Message.error(error.message || '加载评论预览失败')
  }
}

// 格式化时间
const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 30 * day) return Math.floor(diff / day) + '天前'

  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// ==================== 收藏相关 ====================
const checkFavoriteStatus = async () => {
  try {
    const response = await authAPI.checkFavorite(productId.value)
    if (response.success) {
      isFavorited.value = response.data?.isFavorited || false
    }
  } catch (error) {
  }
}

const contactService = () => {
  const sellerId = product.value?.sellerId
  if (!sellerId) {
    Message.info('暂无商家信息')
    return
  }
  // 不能和自己聊天
  if (sellerId === currentUserId.value) {
    Message.warning('不能和自己聊天')
    return
  }
  if (!authStore.isLoggedIn) {
    Message.error('请先登录')
    router.push({ name: 'Login' })
    return
  }
  router.push({ name: 'Chat', params: { targetId: sellerId } })
}

const toggleFavorite = async () => {
  try {
    if (isFavorited.value) {
      const response = await authAPI.removeFavorite(productId.value)
      if (response.success) {
        isFavorited.value = false
        Message.success('已取消收藏')
      }
    } else {
      const response = await authAPI.addFavorite(productId.value)
      if (response.success) {
        isFavorited.value = true
        Message.success('收藏成功')
      }
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  }
}

// ==================== 关注店铺 ====================
const toggleFollow = async () => {
  if (!product.value?.sellerId) return
  try {
    if (isFollowed.value) {
      await authAPI.unfollowSeller(product.value.sellerId)
      isFollowed.value = false
      Message.success('已取消关注')
    } else {
      await authAPI.followSeller(product.value.sellerId)
      isFollowed.value = true
      Message.success('关注成功')
    }
  } catch (error: any) {
    Message.error(error.message || '操作失败')
  }
}

// ==================== 底部栏操作 ====================
const handleAddToCart = () => {
  if (skuList.value.length > 0) {
    loadSkuList().then(() => {
      showSku.value = true
    })
  } else {
    addToCart()
  }
}

const handleBuyNow = () => {
  if (skuList.value.length > 0) {
    loadSkuList().then(() => {
      showSku.value = true
    })
  } else {
    buyNow()
  }
}

// ==================== 购物车和购买 ====================
const addToCart = async () => {
  if (skuList.value.length === 0) {
    try {
      const response = await authAPI.addToCart({
        productId: product.value.id,
        quantity: 1
      })
      if (response.success) {
        Message.success('已添加到购物车')
      }
    } catch (error: any) {
      Message.error(error.message || '添加失败')
    }
    return
  }

  if (skuSpecGroups.value.length > 0 && !allSpecsSelected.value) {
    Message.warning('请选择完整规格')
    return
  }

  if (selectedSkus.value.length === 0) {
    Message.error('请选择规格')
    return
  }

  try {
    for (const item of selectedSkus.value) {
      await authAPI.addToCart({
        productId: product.value.id,
        skuId: item.skuId,
        quantity: item.quantity
      })
    }
    // 保存用户选中的 SKU 信息，用于外部价格显示
    if (currentSku.value) {
      selectedSkuInfo.value = currentSku.value
    }
    Message.success(`已添加 ${totalQuantity.value} 件商品到购物车`)
    showSku.value = false
    resetSpecs()
  } catch (error: any) {
    Message.error(error.message || '添加失败')
  }
}

const buyNow = () => {
  if (skuList.value.length === 0) {
    router.push({
      name: 'Checkout',
      query: {
        source: 'product',
        productId: product.value.id,
        quantity: 1
      }
    })
    return
  }

  if (skuSpecGroups.value.length > 0 && !allSpecsSelected.value) {
    Message.warning('请选择完整规格')
    return
  }

  if (selectedSkus.value.length === 0) {
    Message.error('请选择规格')
    return
  }

  // 保存用户选中的 SKU 信息，用于外部价格显示
  if (currentSku.value) {
    selectedSkuInfo.value = currentSku.value
  }
  const skuIds = selectedSkus.value.map(item => `${item.skuId}:${item.quantity}`).join(',')
  router.push({
    name: 'Checkout',
    query: {
      source: 'product',
      productId: product.value.id,
      skuIds
    }
  })
  showSku.value = false
}

// ==================== Swiper滑动 + 自动播放 ====================
const onTouchStart = (e: TouchEvent) => {
  stopAutoPlay()
  const touch = e.touches[0]
  if (touch) {
    touchStartX.value = touch.clientX
  }
}

const onTouchEnd = (e: TouchEvent) => {
  const touch = e.changedTouches[0]
  if (touch) {
    touchEndX.value = touch.clientX
  }
  const diff = touchStartX.value - touchEndX.value
  if (Math.abs(diff) > 50) {
    if (diff > 0 && currentIndex.value < productImages.value.length - 1) {
      currentIndex.value++
    } else if (diff < 0 && currentIndex.value > 0) {
      currentIndex.value--
    }
  }
  startAutoPlay()
}

const startAutoPlay = () => {
  stopAutoPlay()
  autoPlayTimer = setInterval(() => {
    currentIndex.value = (currentIndex.value + 1) % productImages.value.length
  }, 3000)
}

// ==================== 媒体预览 ====================
const allMediaList = computed(() => {
  const list: { type: 'image' | 'video'; url: string; cover?: string }[] = []
  
  // 商品主图
  productImages.value.forEach((img: any) => list.push({ type: 'image', url: img }))
  
  // 评论图片和视频
  previewComments.value.forEach((comment: any) => {
    if (comment.images) {
      comment.images.forEach((img: string) => list.push({ type: 'image', url: img }))
    }
    if (comment.videos) {
      comment.videos.forEach((video: any) => 
        list.push({ type: 'video', url: video.videoUrl, cover: video.coverUrl })
      )
    }
  })
  
  return list
})

const previewImage = (index: number) => {
  mediaPreviewIndex.value = index
  showMediaPreview.value = true
  stopAutoPlay()
}

const closeMediaPreview = () => {
  showMediaPreview.value = false
  startAutoPlay()
}

const openReviewImagePreview = (images: string[], index: number) => {
  const targetImage = images[index]
  if (targetImage) {
    const foundIndex = allMediaList.value.findIndex(item => item.url === targetImage)
    if (foundIndex !== -1) {
      mediaPreviewIndex.value = foundIndex
      showMediaPreview.value = true
    }
  }
}

const previewSkuImage = (sku?: any) => {
  let url = ''
  if (sku) {
    url = sku.skuImage || (productImages.value || [])[0]
  } else {
    url = currentSkuPreviewImage.value
  }
  if (url) {
    const foundIndex = allMediaList.value.findIndex(item => item.url === url)
    if (foundIndex !== -1) {
      mediaPreviewIndex.value = foundIndex
    } else {
      mediaPreviewIndex.value = 0
    }
    showMediaPreview.value = true
  }
}

const previewVideo = (url: string) => {
  const foundIndex = allMediaList.value.findIndex(item => item.url === url)
  if (foundIndex !== -1) {
    mediaPreviewIndex.value = foundIndex
  } else {
    mediaPreviewIndex.value = 0
  }
  showMediaPreview.value = true
}

// 格式化视频时长
const formatDuration = (seconds: number): string => {
  if (!seconds || seconds <= 0) return ''
  const min = Math.floor(seconds / 60)
  const sec = Math.floor(seconds % 60)
  return `${min}:${String(sec).padStart(2, '0')}`
}

const stopAutoPlay = () => {
  if (autoPlayTimer) {
    clearInterval(autoPlayTimer)
    autoPlayTimer = null
  }
}

// ==================== 滚动监听 ====================
const handleScroll = () => {
  const scrollTop = window.scrollY

  // 悬浮胶囊：滚动超过主图高度后出现
  showDetailCapsule.value = scrollTop > mainImageHeight
}

// ==================== 页面跳转 ====================
const goToShop = () => {
  // 优先使用 sellerInfo.userId，其次使用 product.sellerId
  const shopId = sellerInfo.value?.userId || product.value?.sellerId
  if (shopId) {
    router.push({ name: 'Shop', params: { sellerId: shopId } })
  }
}

const openComments = () => {
  router.push({
    name: 'ProductReviews',
    params: { productId: productId.value }
  })
}

// ==================== 工具函数 ====================
const formatPrice = (price: number | null | undefined): string => {
  if (!price && price !== 0) return '0.00'
  return Number(price).toFixed(2)
}

// 近30天销量（可从接口获取或估算）
const recentSales = computed(() => {
  const total = product.value?.salesCount || 0
  return Math.floor(total * 0.3)  // 假设30天占30%
})

const formatSaleCount = (count: number): string => {
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return String(count)
}

const getDiscountPercent = (price: number | null | undefined, originalPrice: number | null | undefined): number => {
  if (!originalPrice || originalPrice <= (price || 0)) return 0
  return Math.round((1 - (price || 0) / originalPrice) * 100)
}

// ==================== 浏览记录 ====================
const recordBrowse = async () => {
  if (!authStore.isLoggedIn) return
  const item = {
    id: productId.value,
    productName: product.value?.name,
    productImage: productImages.value[0] || '',
    productPrice: product.value?.price
  }
  authAPI.recordBrowse(item).catch(() => {})
  const local = JSON.parse(localStorage.getItem('browseHistory') || '[]')
  const filtered = local.filter((h: any) => h.id !== item.id)
  filtered.unshift(item)
  localStorage.setItem('browseHistory', JSON.stringify(filtered.slice(0, 20)))
}

// ==================== 生命周期 ====================
onMounted(() => {
  if (!authStore.validateUserPermission()) return

  window.addEventListener('scroll', handleScroll, { passive: true })

  Promise.all([
    loadProductDetail(),
    loadPreviewComments(),
    loadSellerInfo(),
    loadCoupons(),
    startAutoPlay()
  ]).then(() => {
    recordBrowse()
    // 如果是从评论页跳转来的，自动打开 SKU 弹窗并选中对应规格
    const autoOpenSku = route.query.autoOpenSku
    const skuName = route.query.skuName as string

    if (autoOpenSku === 'true' && skuName) {
      loadSkuList().then(() => {
        const matchedSku = skuList.value.find((s: any) => s.skuName === skuName)
        if (matchedSku) {
          selectedSkus.value = [{ skuId: matchedSku.id, quantity: 1 }]
        }
        showSku.value = true
      })
    }
  })
})

onUnmounted(() => {
  stopAutoPlay()
  window.removeEventListener('scroll', handleScroll)
  document.body.style.overflow = ''
})
</script>

<style scoped>
@import url("@/static/css/user/商品详情.css");
</style>
