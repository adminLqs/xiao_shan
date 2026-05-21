package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单项服务层
 * 负责订单项的增删改查等业务逻辑
 *
 * @author xiaoshan
 * @date 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemMapper orderItemMapper;

    // ==================== 插入方法 ====================

    /**
     * 插入单个订单项
     *
     * @param orderItem 订单项实体
     * @return 是否插入成功
     */
    @Transactional
    public boolean insert(OrderItem orderItem) {
        if (orderItem == null) {
            log.warn("订单项为空");
            return false;
        }
        int inserted = orderItemMapper.insert(orderItem);
        if (inserted > 0) {
            log.info("订单项插入成功 - 订单ID: {}, 商品名: {}", orderItem.getOrderId(), orderItem.getProductName());
            return true;
        }
        log.warn("订单项插入失败");
        return false;
    }

    /**
     * 批量插入订单项
     *
     * @param orderItems 订单项列表
     * @return 是否插入成功
     */
    @Transactional
    public boolean batchInsert(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("订单项列表为空");
            return false;
        }
        int inserted = orderItemMapper.batchInsert(orderItems);
        if (inserted > 0) {
            log.info("批量插入订单项成功 - 数量: {}", inserted);
            return true;
        }
        log.warn("批量插入订单项失败");
        return false;
    }

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询订单项
     *
     * @param id 订单项ID
     * @return 订单项实体
     */
    public OrderItem findById(Long id) {
        if (id == null) {
            log.warn("订单项ID为空");
            return null;
        }
        return orderItemMapper.findById(id);
    }

    /**
     * 根据订单ID查询所有订单项
     *
     * @param orderId 订单ID
     * @return 订单项列表
     */
    public List<OrderItem> findByOrderId(Long orderId) {
        if (orderId == null) {
            log.warn("订单ID为空");
            return List.of();
        }
        return orderItemMapper.findByOrderId(orderId);
    }

    /**
     * 根据商品ID查询所有订单项
     *
     * @param productId 商品ID
     * @return 订单项列表
     */
    public List<OrderItem> findByProductId(Long productId) {
        if (productId == null) {
            log.warn("商品ID为空");
            return List.of();
        }
        return orderItemMapper.findByProductId(productId);
    }

    /**
     * 查询订单的所有商品总数
     *
     * @param orderId 订单ID
     * @return 商品总数
     */
    public int countTotalItemsByOrderId(Long orderId) {
        if (orderId == null) {
            log.warn("订单ID为空");
            return 0;
        }
        Integer count = orderItemMapper.countTotalItemsByOrderId(orderId);
        return count != null ? count : 0;
    }

    /**
     * 查询订单的商品种类数
     *
     * @param orderId 订单ID
     * @return 商品种类数
     */
    public int countProductTypesByOrderId(Long orderId) {
        if (orderId == null) {
            log.warn("订单ID为空");
            return 0;
        }
        Integer count = orderItemMapper.countProductTypesByOrderId(orderId);
        return count != null ? count : 0;
    }

    /**
     * 查询订单的总金额
     *
     * @param orderId 订单ID
     * @return 订单总金额
     */
    public BigDecimal calculateOrderTotal(Long orderId) {
        if (orderId == null) {
            log.warn("订单ID为空");
            return BigDecimal.ZERO;
        }
        BigDecimal total = orderItemMapper.calculateOrderTotal(orderId);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ==================== 更新方法 ====================

    /**
     * 更新订单项数量
     *
     * @param id       订单项ID
     * @param quantity 新数量
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateQuantity(Long id, Integer quantity) {
        if (id == null) {
            log.warn("订单项ID为空");
            return false;
        }
        if (quantity == null || quantity <= 0) {
            log.warn("数量无效: {}", quantity);
            return false;
        }
        int updated = orderItemMapper.updateQuantity(id, quantity);
        if (updated > 0) {
            log.info("订单项数量更新成功 - ID: {}, 数量: {}", id, quantity);
            return true;
        }
        log.warn("订单项数量更新失败 - ID: {}", id);
        return false;
    }

    /**
     * 更新订单项价格
     *
     * @param id    订单项ID
     * @param price 新价格
     * @return 是否更新成功
     */
    @Transactional
    public boolean updatePrice(Long id, BigDecimal price) {
        if (id == null) {
            log.warn("订单项ID为空");
            return false;
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("价格无效: {}", price);
            return false;
        }
        int updated = orderItemMapper.updatePrice(id, price);
        if (updated > 0) {
            log.info("订单项价格更新成功 - ID: {}, 价格: {}", id, price);
            return true;
        }
        log.warn("订单项价格更新失败 - ID: {}", id);
        return false;
    }

    /**
     * 更新订单项
     *
     * @param orderItem 订单项实体
     * @return 是否更新成功
     */
    @Transactional
    public boolean update(OrderItem orderItem) {
        if (orderItem == null || orderItem.getId() == null) {
            log.warn("订单项或ID为空");
            return false;
        }
        int updated = orderItemMapper.update(orderItem);
        if (updated > 0) {
            log.info("订单项更新成功 - ID: {}", orderItem.getId());
            return true;
        }
        log.warn("订单项更新失败 - ID: {}", orderItem.getId());
        return false;
    }

    // ==================== 删除方法 ====================

    /**
     * 根据ID删除订单项
     *
     * @param id 订单项ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteById(Long id) {
        if (id == null) {
            log.warn("订单项ID为空");
            return false;
        }
        int deleted = orderItemMapper.deleteById(id);
        if (deleted > 0) {
            log.info("订单项删除成功 - ID: {}", id);
            return true;
        }
        log.warn("订单项删除失败 - ID: {}", id);
        return false;
    }

    /**
     * 根据订单ID删除所有订单项
     *
     * @param orderId 订单ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteByOrderId(Long orderId) {
        if (orderId == null) {
            log.warn("订单ID为空");
            return false;
        }
        int deleted = orderItemMapper.deleteByOrderId(orderId);
        if (deleted > 0) {
            log.info("删除订单所有订单项成功 - 订单ID: {}, 数量: {}", orderId, deleted);
            return true;
        }
        log.warn("删除订单所有订单项失败 - 订单ID: {}", orderId);
        return false;
    }

    /**
     * 根据订单ID和商品ID删除指定订单项
     *
     * @param orderId   订单ID
     * @param productId 商品ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteByOrderIdAndProductId(Long orderId, Long productId) {
        if (orderId == null || productId == null) {
            log.warn("订单ID或商品ID为空");
            return false;
        }
        int deleted = orderItemMapper.deleteByOrderIdAndProductId(orderId, productId);
        if (deleted > 0) {
            log.info("删除订单项成功 - 订单ID: {}, 商品ID: {}", orderId, productId);
            return true;
        }
        log.warn("删除订单项失败 - 订单ID: {}, 商品ID: {}", orderId, productId);
        return false;
    }
}