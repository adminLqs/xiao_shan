<template>
  <div class="order-card" @click="$emit('view-detail', orderWrapper.order.id)">
    <div class="order-header">
      <div class="order-header-left">
        <img :src="getSellerAvatar(orderWrapper)" class="seller-avatar" />
        <span class="seller-name">{{ getSellerName(orderWrapper) }}</span>
      </div>
      <span class="order-status" :class="getStatusClass(orderWrapper.order.status)">
        {{ getStatusText(orderWrapper.order.status) }}
      </span>
    </div>

    <div class="order-items">
      <div
        v-for="item in orderWrapper.orderItems"
        :key="item.id"
        class="order-item"
      >
        <img :src="item.productImage" class="item-image">
        <div class="item-info">
          <div class="item-name">{{ item.productName }}</div>
          <div class="tags-group">
            <span v-if="item.skuName" class="tag-spec">{{ item.skuName }}</span>
            <span class="tag-quantity">x{{ item.quantity }}</span>
            <span v-if="item.refundStatus === 'PROCESSING'" class="tag-refund-text">退款中</span>
            <span v-if="item.refundStatus === 'WAITING_RETURN'" class="tag-refund-text">待退货</span>
            <span v-if="item.refundStatus === 'RETURNING'" class="tag-refund-text">退货中</span>
            <span v-if="item.refundStatus === 'SUCCESS'" class="tag-refund-text">已退款</span>
            <span v-if="item.refundStatus === 'FAILED'" class="tag-refund-text">已拒绝</span>
          </div>
        </div>
        <div class="item-price">¥{{ formatPrice(item.price) }}</div>
      </div>
    </div>

    <div class="order-footer" @click.stop>
      <div class="order-actions">
        <template v-if="hasRefundingItem(orderWrapper)">
          <button class="btn-primary btn-refund" @click="$emit('handle-refund', orderWrapper)">查看售后</button>
        </template>

        <template v-if="orderWrapper.order.status === 'PAID'">
          <button class="btn-outline" @click="$emit('view-detail', orderWrapper.order.id)">查看详情</button>
          <button class="btn-outline" @click="$emit('process-order', orderWrapper.order.id)">处理订单</button>
        </template>

        <template v-if="orderWrapper.order.status === 'PROCESSING'">
          <button class="btn-outline" @click="$emit('view-detail', orderWrapper.order.id)">查看详情</button>
          <button class="btn-outline" @click="$emit('ship-order', orderWrapper.order.id)">发货</button>
        </template>

        <template v-if="orderWrapper.order.status === 'SHIPPED'">
          <button class="btn-outline" @click="$emit('view-detail', orderWrapper.order.id)">查看详情</button>
          <button class="btn-outline" @click="$emit('view-logistics', orderWrapper.order.id)">查看物流</button>
        </template>

        <template v-if="orderWrapper.order.status !== 'PAID' && orderWrapper.order.status !== 'PROCESSING' && orderWrapper.order.status !== 'SHIPPED' && !hasRefundingItem(orderWrapper)">
          <button class="btn-outline" @click="$emit('view-detail', orderWrapper.order.id)">查看详情</button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import sellerDefaultAvatar from '@/static/images/seller-avatar.jpg'

  interface OrderItem {
    id: number
    productId: number
    productName: string
    productImage: string
    quantity: number
    price: number
    refundStatus?: string
    refundType?: string
    skuName?: string
    sellerName?: string
    sellerAvatar?: string
  }

  interface Order {
    id: number
    orderNumber: string
    totalAmount: number
    status: string
    createdAt: string
    userId?: number
    addressId?: number
    paymentMethod?: string
    paidAt?: string
    shippedAt?: string
    completedAt?: string
    trackingNumber?: string
    logisticsCode?: string
    logisticsName?: string
    remark?: string
  }

  interface OrderWithItems {
    order: Order
    orderItems: OrderItem[]
    sellerName?: string
    sellerAvatar?: string
  }

  defineProps<{
    orderWrapper: OrderWithItems
  }>()

  defineEmits<{
    (e: 'view-detail', orderId: number): void
    (e: 'process-order', orderId: number): void
    (e: 'ship-order', orderId: number): void
    (e: 'view-logistics', orderId: number): void
    (e: 'handle-refund', orderWrapper: OrderWithItems): void
  }>()

  const formatPrice = (price: number): string => {
    if (price == null) return '0.00'
    return price.toFixed(2)
  }

  const getStatusClass = (status: string): string => {
    const map: Record<string, string> = {
      PENDING: 'status-pending',
      PAID: 'status-paid',
      PROCESSING: 'status-paid',
      SHIPPED: 'status-shipped',
      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled'
    }
    return map[status] || ''
  }

  const getStatusText = (status: string): string => {
    const map: Record<string, string> = {
      PENDING: '待付款',
      PAID: '待发货',
      PROCESSING: '待发货',
      SHIPPED: '待收货',
      COMPLETED: '已完成',
      CANCELLED: '已取消'
    }
    return map[status] || status
  }

  const hasRefundingItem = (orderWrapper: OrderWithItems): boolean => {
    return orderWrapper.orderItems.some(item =>
      item.refundStatus === 'PROCESSING' ||
      item.refundStatus === 'WAITING_RETURN' ||
      item.refundStatus === 'RETURNING' ||
      item.refundStatus === 'SUCCESS' ||
      item.refundStatus === 'FAILED'
    )
  }

  const getSellerName = (orderWrapper: OrderWithItems): string => {
    return orderWrapper.sellerName || orderWrapper.orderItems[0]?.sellerName || '商家'
  }

  const getSellerAvatar = (orderWrapper: OrderWithItems): string => {
    return orderWrapper.sellerAvatar || orderWrapper.orderItems[0]?.sellerAvatar || sellerDefaultAvatar
  }
</script>

<style scoped>
  @import url('@/static/css/seller/订单管理页.css');
</style>
