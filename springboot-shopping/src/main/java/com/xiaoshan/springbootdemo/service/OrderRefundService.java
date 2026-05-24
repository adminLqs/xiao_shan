package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.OrderItem;
import com.xiaoshan.springbootdemo.entity.OrderRefund;
import com.xiaoshan.springbootdemo.entity.RefundImage;
import com.xiaoshan.springbootdemo.entity.RefundVideo;
import com.xiaoshan.springbootdemo.mapper.OrderItemMapper;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import com.xiaoshan.springbootdemo.mapper.OrderRefundMapper;
import com.xiaoshan.springbootdemo.mapper.RefundImageMapper;
import com.xiaoshan.springbootdemo.mapper.RefundVideoMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import com.xiaoshan.springbootdemo.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRefundService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    // 快递鸟API配置
    @Value("${kdniao.api.url:https://api.kdniao.com/api/dist}")
    private String kdniaoApiUrl;

    @Value("${kdniao.eorder.url:https://api.kdniao.com/api/EOrderService}")
    private String kdniaoEorderUrl;

    @Value("${kdniao.app.key:1918612}")
    private String kdniaoAppKey;

    @Value("${kdniao.app.secret:37691e1f-fa2b-4ce8-b0aa-1e5c3fd7dae3}")
    private String kdniaoAppSecret;

    // 请求类型：1007表示电子面单接口（上门取件）
    private static final String REQUEST_TYPE_EORDER = "1007";

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 30 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/webm"
    );

    private final OrderRefundMapper orderRefundMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundImageMapper refundImageMapper;
    private final RefundVideoMapper refundVideoMapper;
    private final AlipayService alipayService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final AddressService addressService;
    private final SellerProfileService sellerProfileService;
    private final WebSocketService webSocketService;

    /**
     * 根据ID获取退款记录
     * @param id 退款ID
     * @return 退款记录
     */
    public OrderRefund getById(Long id) {
        return orderRefundMapper.findById(id).orElse(null);
    }

    /**
     * 用户申请退款/售后（支持订单项级别的退款）
     * @param userId 用户ID
     * @param orderItemId 订单项ID
     * @param refundType 退款类型: REFUND-退款, AFTER_SALE-售后
     * @param refundReason 退款原因
     * @param refundAmount 退款金额
     * @param description 退款描述
     * @param evidenceImages 证据图片（逗号分隔）
     * @return 退款记录
     */
    @Transactional
    public OrderRefund applyRefund(Long userId, Long orderItemId, String refundType, 
                                   String refundReason, BigDecimal refundAmount, 
                                   String description, String evidenceImages) {
        log.info("用户 {} 申请{}，订单项ID: {}, 原因: {}, 金额: {}", 
                userId, refundType.equals("AFTER_SALE") ? "售后" : "退款", 
                orderItemId, refundReason, refundAmount);

        // 查询订单项
        OrderItem orderItem = orderItemMapper.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 查询订单
        Order order = orderMapper.findById(orderItem.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证用户权限
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 根据退款类型区分状态判断
        if ("REFUND".equals(refundType)) {
            // 退款（未发货）：仅允许已付款、处理中状态
            Set<Order.OrderStatus> refundableStatuses = Set.of(
                    Order.OrderStatus.PAID,
                    Order.OrderStatus.PROCESSING
            );
            if (!refundableStatuses.contains(order.getStatus())) {
                throw new RuntimeException("当前订单状态不允许申请退款");
            }
        } else if ("AFTER_SALE".equals(refundType)) {
            // 售后（已发货）：允许已发货、已送达、已完成状态
            Set<Order.OrderStatus> refundableStatuses = Set.of(
                    Order.OrderStatus.SHIPPED,
                    Order.OrderStatus.DELIVERED,
                    Order.OrderStatus.COMPLETED
            );
            if (!refundableStatuses.contains(order.getStatus())) {
                throw new RuntimeException("当前订单状态不允许申请售后");
            }
        }

        // 检查该订单项是否已有退款申请
        List<OrderRefund> existingRefunds = orderRefundMapper.findByOrderItemId(orderItemId);
        if (!existingRefunds.isEmpty()) {
            throw new RuntimeException("该商品已有退款申请");
        }

        // 创建退款记录
        OrderRefund refund = new OrderRefund();
        refund.setId(snowflakeIdGenerator.nextId()); // 生成雪花ID
        refund.setOrderId(order.getId());
        refund.setOrderItemId(orderItemId);
        refund.setOrderNumber(order.getOrderNumber());
        refund.setUserId(userId);
        refund.setSellerId(orderItem.getSellerId()); // 设置商家ID
        refund.setRefundAmount(refundAmount);
        refund.setRefundStatus(OrderRefund.RefundStatus.PROCESSING);
        refund.setRefundReason(refundReason);
        refund.setRefundType(refundType);
        refund.setDescription(description);
        refund.setApplyTime(LocalDateTime.now());
        refund.setCommunicationRound(0); // 初始轮次为0（表示还没开始沟通）

        orderRefundMapper.insert(refund);

        // 保存证据图片到 refund_images 表
        saveRefundImages(refund.getId(), evidenceImages, RefundImage.ImageType.EVIDENCE);

        // 更新订单项的售后状态和退款ID
        String itemRefundStatus = "REFUND".equals(refundType) ?
                com.xiaoshan.springbootdemo.entity.OrderItem.RefundStatus.REFUNDING :
                com.xiaoshan.springbootdemo.entity.OrderItem.RefundStatus.AFTER_SALE;
        orderItemMapper.updateRefundStatusWithId(orderItemId, itemRefundStatus, refund.getId());

        log.info("{}申请成功，退款ID: {}", refundType.equals("AFTER_SALE") ? "售后" : "退款", refund.getId());

        // 发送退款申请通知给商家
        if (refund.getSellerId() != null) {
            webSocketService.sendRefundApplication(refund.getSellerId(), order.getOrderNumber(), refundAmount);
        }

        return refund;
    }

    /**
     * 用户申请退款（旧版本，兼容订单级别的退款）
     */
    @Transactional
    public OrderRefund applyRefund(Long userId, Long orderId, String refundReason) {
        log.info("用户 {} 申请退款，订单ID: {}, 原因: {}", userId, orderId, refundReason);

        // 查询订单
        Order order = orderMapper.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证用户权限
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 检查订单状态（已付款、处理中、已发货、已送达、已完成状态可以申请退款/售后）
        Set<Order.OrderStatus> refundableStatuses = Set.of(
                Order.OrderStatus.PAID,
                Order.OrderStatus.PROCESSING,
                Order.OrderStatus.SHIPPED,
                Order.OrderStatus.DELIVERED,
                Order.OrderStatus.COMPLETED
        );
        if (!refundableStatuses.contains(order.getStatus())) {
            throw new RuntimeException("当前状态不允许退款");
        }

        // 检查是否已有退款申请
        if (orderRefundMapper.existsByOrderId(orderId)) {
            throw new RuntimeException("该订单已有退款申请");
        }

        // 创建退款记录
        OrderRefund refund = new OrderRefund();
        refund.setOrderId(orderId);
        refund.setOrderNumber(order.getOrderNumber());
        refund.setUserId(userId);
        refund.setRefundAmount(order.getTotalAmount());
        refund.setRefundStatus(OrderRefund.RefundStatus.PROCESSING);
        refund.setRefundReason(refundReason);
        refund.setApplyTime(LocalDateTime.now());
        refund.setCommunicationRound(0); // 初始轮次为0（表示还没开始沟通）

        orderRefundMapper.insert(refund);

        log.info("退款申请成功，退款ID: {}", refund.getId());
        return refund;
    }

    /**
     * 商家处理退款申请 - 同意
     * @param refundId 退款ID
     * @param sellerId 商家ID
     * @param notes 审核备注
     * @return 处理结果
     */
    @Transactional
    public Map<String, Object> approveRefund(Long refundId, Long sellerId, String notes) {
        log.info("商家 {} 处理退款申请，ID: {}", sellerId, refundId);

        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        if (refund.getRefundStatus() != OrderRefund.RefundStatus.PROCESSING) {
            throw new RuntimeException("退款状态不允许操作");
        }

        // 查询订单
        Order order = orderMapper.findById(refund.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证商家权限（订单属于该商家）
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
        if (orderItems.isEmpty() || !orderItems.get(0).getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        // 更新订单项退款状态
        OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 根据退款类型区分处理
        if ("AFTER_SALE".equals(refund.getRefundType())) {
            // 售后类型：商家同意后需要退货
            // 1. 更新退款状态为 APPROVED（商家已同意，待退货）
            refund.setRefundStatus(OrderRefund.RefundStatus.APPROVED);
            refund.setReviewTime(LocalDateTime.now());
            refund.setReviewedBy(sellerId);
            refund.setReviewNotes(notes);
            orderRefundMapper.updateStatus(refund);

            // 2. 更新订单项状态为 WAITING_RETURN（待退货）
            orderItem.setRefundStatus("WAITING_RETURN");
            orderItemMapper.updateById(orderItem);
            log.info("售后申请已同意，更新订单项退款状态为 WAITING_RETURN，订单项ID: {}", orderItem.getId());

            // 3. 订单状态保持不变，等待退货完成

            log.info("售后申请已同意，等待用户退货，退款ID: {}", refundId);

            // 发送退款结果通知给用户
            webSocketService.sendUserRefundResult(refund.getUserId(), "商家已同意售后申请，请尽快退货");

            return Map.of(
                    "refundId", refundId,
                    "status", "APPROVED",
                    "message", "已同意售后申请，请等待用户退货",
                    "refundType", "AFTER_SALE"
            );

        } else {
            // 仅退款类型：直接退款
            // 调用支付宝退款接口
            boolean refundSuccess = false;
            try {
                refundSuccess = alipayService.refund(
                        order.getTransactionId(),
                        refund.getRefundAmount(),
                        refund.getRefundReason()
                );
            } catch (Exception e) {
                log.error("支付宝退款失败: {}", e.getMessage());
                throw new RuntimeException("退款失败：" + e.getMessage());
            }

            if (!refundSuccess) {
                throw new RuntimeException("支付宝退款失败");
            }

            // 使用订单交易号作为退款交易参考
            String refundTransactionId = order.getTransactionId();

            // 更新退款状态为成功
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            orderRefundMapper.markAsSuccess(refundId, now, refundTransactionId);

            // 更新订单项退款状态为已完成
            orderItem.setRefundStatus("COMPLETED");
            orderItemMapper.updateById(orderItem);
            log.info("更新订单项退款状态为 COMPLETED，订单项ID: {}", orderItem.getId());

            // 检查该订单下所有订单项是否都已退款
            int unrefundCount = orderItemMapper.countUnrefundedByOrderId(refund.getOrderId());
            if (unrefundCount == 0) {
                // 全部退款 → 订单状态改为已退款
                orderMapper.updateStatus(refund.getOrderId(), "REFUNDED");
                log.info("订单所有订单项已退款，更新订单状态为 REFUNDED，订单ID: {}", refund.getOrderId());
            } else {
                log.info("订单还有 {} 个订单项未退款，订单状态保持不变", unrefundCount);
            }

            // 恢复库存
            restoreStock(refund.getOrderId());

            log.info("退款处理成功，退款ID: {}", refundId);

            // 发送退款成功通知给用户
            webSocketService.sendUserRefundResult(refund.getUserId(), "退款已成功，款项将在1-3个工作日内到账");

            return Map.of(
                    "refundId", refundId,
                    "status", "SUCCESS",
                    "message", "退款成功",
                    "refundType", "REFUND"
            );
        }
    }

    /**
     * 商家处理退款申请 - 拒绝
     * @param refundId 退款ID
     * @param sellerId 商家ID
     * @param notes 拒绝原因
     * @return 处理结果
     */
    @Transactional
    public Map<String, Object> rejectRefund(Long refundId, Long sellerId, String notes) {
        log.info("商家 {} 拒绝退款申请，ID: {}", sellerId, refundId);

        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        if (refund.getRefundStatus() != OrderRefund.RefundStatus.PROCESSING) {
            throw new RuntimeException("退款状态不允许操作");
        }

        // 查询订单验证商家权限
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
        if (orderItems.isEmpty() || !orderItems.get(0).getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        // 更新退款状态为失败
        refund.setRefundStatus(OrderRefund.RefundStatus.FAILED);
        refund.setReviewTime(LocalDateTime.now());
        refund.setReviewedBy(sellerId);
        refund.setReviewNotes(notes);

        orderRefundMapper.updateStatus(refund);

        // 更新订单项的退款状态（清除退款中状态，使订单列表不再显示"退款中"）
        OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId()).orElse(null);
        if (orderItem == null) {
            log.warn("订单项不存在，订单ID: {}", refund.getOrderItemId());
        } else {
            orderItem.setRefundStatus(null);
            orderItemMapper.updateById(orderItem);
        }

        log.info("退款申请已拒绝，退款ID: {}", refundId);

        // 发送退款拒绝通知给用户
        String rejectMessage = notes != null ? "商家拒绝了您的退款申请，原因：" + notes : "商家拒绝了您的退款申请";
        webSocketService.sendUserRefundResult(refund.getUserId(), rejectMessage);

        return Map.of(
                "refundId", refundId,
                "status", "REJECTED",
                "message", "退款申请已拒绝"
        );
    }

    /**
     * 获取用户的退款记录列表
     * @param userId 用户ID
     * @return 退款记录列表
     */
    public List<OrderRefund> getUserRefunds(Long userId) {
        return orderRefundMapper.findByUserId(userId);
    }

    /**
     * 分页查询用户退款记录
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 包含分页信息的Map
     */
    public Map<String, Object> getUserRefundsWithPage(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<OrderRefund> refunds = orderRefundMapper.findByUserIdWithPage(userId, offset, pageSize);
        
        int total = orderRefundMapper.countByUserId(userId);
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (OrderRefund refund : refunds) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", refund.getId());
            record.put("orderItemId", refund.getOrderItemId());
            record.put("orderNumber", refund.getOrderNumber());
            record.put("refundAmount", refund.getRefundAmount());
            record.put("refundStatus", refund.getRefundStatus().name());
            record.put("refundType", refund.getRefundType());
            record.put("returnStatus", refund.getReturnStatus());
            record.put("returnTrackingNumber", refund.getReturnTrackingNumber());
            record.put("returnLogisticsName", refund.getReturnLogisticsName());
            record.put("applyTime", refund.getApplyTime() != null ? 
                    refund.getApplyTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            
            // 查询订单项获取商品信息
            OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId()).orElse(null);
            if (orderItem != null) {
                record.put("productName", orderItem.getProductName());
                record.put("productImage", orderItem.getProductImage());
                record.put("skuName", orderItem.getSkuName());
                record.put("quantity", orderItem.getQuantity());
            }
            
            records.add(record);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return result;
    }

    /**
     * 获取用户进行中的退款记录数量
     * @param userId 用户ID
     * @return 进行中的退款数量
     */
    public int getPendingRefundCount(Long userId) {
        return orderRefundMapper.countPendingByUserId(userId);
    }

    /**
     * 获取订单的退款记录
     * @param orderId 订单ID
     * @return 退款记录列表
     */
    public List<OrderRefund> getOrderRefunds(Long orderId) {
        return orderRefundMapper.findByOrderId(orderId);
    }

    /**
     * 获取退款详情（包含图片和视频列表）
     * @param refundId 退款ID
     * @return 退款记录（包含证据图片、申诉图片和视频列表）
     */
    public Map<String, Object> getRefundDetailWithImages(Long refundId) {
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 查询证据图片
        List<RefundImage> evidenceImageList = refundImageMapper.findByRefundIdAndType(refundId, RefundImage.ImageType.EVIDENCE.name());
        List<String> evidenceImages = evidenceImageList.stream()
                .map(RefundImage::getImage)
                .toList();

        // 查询申诉图片
        List<RefundImage> appealImageList = refundImageMapper.findByRefundIdAndType(refundId, RefundImage.ImageType.APPEAL.name());
        List<String> appealImages = appealImageList.stream()
                .map(RefundImage::getImage)
                .toList();

        // 查询退款视频
        List<RefundVideo> refundVideoList = refundVideoMapper.findByRefundId(refundId);
        List<Map<String, Object>> videos = refundVideoList.stream()
                .map(video -> {
                    Map<String, Object> videoMap = new HashMap<>();
                    videoMap.put("id", video.getId());
                    videoMap.put("videoUrl", video.getVideoUrl());
                    videoMap.put("coverUrl", video.getCoverUrl());
                    videoMap.put("duration", video.getDuration());
                    videoMap.put("size", video.getSize());
                    return videoMap;
                })
                .toList();

        // 查询订单信息，获取买家和卖家ID
        Long buyerId = null;
        Long sellerId = null;
        String productName = null;
        String productImage = null;
        String skuName = null;
        Integer quantity = null;
        BigDecimal price = null;
        try {
            Order order = orderMapper.findById(refund.getOrderId()).orElse(null);
            if (order != null) {
                buyerId = order.getUserId();
            }
            OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId()).orElse(null);
            if (orderItem != null) {
                sellerId = orderItem.getSellerId();
                productName = orderItem.getProductName();
                productImage = orderItem.getProductImage();
                skuName = orderItem.getSkuName();
                quantity = orderItem.getQuantity();
                price = orderItem.getPrice();
            }
        } catch (Exception e) {
            log.warn("获取订单信息失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", refund.getId());
        result.put("orderId", refund.getOrderId());
        result.put("orderItemId", refund.getOrderItemId());
        result.put("orderNumber", refund.getOrderNumber());
        result.put("userId", refund.getUserId());
        result.put("buyerId", buyerId);
        result.put("sellerId", sellerId);
        result.put("refundAmount", refund.getRefundAmount());
        result.put("refundStatus", refund.getRefundStatus());
        result.put("refundReason", refund.getRefundReason());
        result.put("refundType", refund.getRefundType());
        result.put("description", refund.getDescription());
        result.put("returnMethod", refund.getReturnMethod());
        result.put("returnTrackingNumber", refund.getReturnTrackingNumber());
        result.put("returnLogisticsName", refund.getReturnLogisticsName());
        result.put("returnStatus", refund.getReturnStatus());
        result.put("applyTime", refund.getApplyTime());
        result.put("reviewTime", refund.getReviewTime());
        result.put("completeTime", refund.getCompleteTime());
        result.put("reviewNotes", refund.getReviewNotes());
        result.put("reviewedBy", refund.getReviewedBy());
        result.put("communicationRound", refund.getCommunicationRound());
        result.put("refundTransactionId", refund.getRefundTransactionId());
        result.put("evidenceImages", evidenceImages);
        result.put("appealImages", appealImages);
        result.put("videos", videos);
        result.put("productName", productName);
        result.put("productImage", productImage);
        result.put("skuName", skuName);
        result.put("quantity", quantity);
        result.put("price", price);

        return result;
    }

    /**
     * 获取退款详情（旧版本，兼容原有接口）
     * @param refundId 退款ID
     * @return 包含商品信息的退款详情Map
     */
    public Map<String, Object> getRefundDetail(Long refundId) {
        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", refund.getId());
        result.put("orderId", refund.getOrderId());
        result.put("orderItemId", refund.getOrderItemId());
        result.put("orderNumber", refund.getOrderNumber());
        result.put("userId", refund.getUserId());
        result.put("sellerId", refund.getSellerId());
        result.put("refundAmount", refund.getRefundAmount());
        result.put("refundStatus", refund.getRefundStatus());
        result.put("refundReason", refund.getRefundReason());
        result.put("refundType", refund.getRefundType());
        result.put("description", refund.getDescription());
        result.put("returnMethod", refund.getReturnMethod());
        result.put("returnTrackingNumber", refund.getReturnTrackingNumber());
        result.put("returnLogisticsName", refund.getReturnLogisticsName());
        result.put("returnStatus", refund.getReturnStatus());
        result.put("applyTime", refund.getApplyTime());
        result.put("reviewTime", refund.getReviewTime());
        result.put("completeTime", refund.getCompleteTime());
        result.put("reviewNotes", refund.getReviewNotes());
        result.put("reviewedBy", refund.getReviewedBy());
        result.put("communicationRound", refund.getCommunicationRound());
        result.put("refundTransactionId", refund.getRefundTransactionId());

        // 根据 orderItemId 查询 order_items 表获取商品信息
        if (refund.getOrderItemId() != null) {
            OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId()).orElse(null);
            if (orderItem != null) {
                result.put("productName", orderItem.getProductName());
                result.put("productImage", orderItem.getProductImage());
                result.put("skuName", orderItem.getSkuName());
                result.put("quantity", orderItem.getQuantity());
            }
        }

        return result;
    }

    /**
     * 根据订单项ID获取退款记录
     * @param orderItemId 订单项ID
     * @param userId 用户ID
     * @return 退款记录
     */
    public OrderRefund getRefundByOrderItemId(Long orderItemId, Long userId) {
        List<OrderRefund> refunds = orderRefundMapper.findByOrderItemId(orderItemId);
        if (refunds.isEmpty()) {
            throw new RuntimeException("暂无退款记录");
        }
        // 返回最新的退款记录
        return refunds.get(0);
    }

    /**
     * 获取商家的待处理退款列表
     * @param sellerId 商家ID
     * @return 待处理退款列表
     */
    public List<Map<String, Object>> getPendingRefunds(Long sellerId) {
        // 查询商家所有订单中的退款申请
        List<OrderRefund> allRefunds = orderRefundMapper.findAll();
        List<Map<String, Object>> pendingRefunds = new ArrayList<>();

        for (OrderRefund refund : allRefunds) {
            if (refund.getRefundStatus() != OrderRefund.RefundStatus.PROCESSING) {
                continue;
            }

            List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
            if (!orderItems.isEmpty() && orderItems.get(0).getSellerId().equals(sellerId)) {
                pendingRefunds.add(Map.of(
                        "refund", refund,
                        "orderItems", orderItems
                ));
            }
        }

        return pendingRefunds;
    }

    /**
     * 用户提交申诉
     * @param refundId 退款ID
     * @param userId 用户ID
     * @param appealContent 申诉内容
     * @param appealEvidence 申诉凭证
     * @return 处理结果
     */
    @Transactional
    public Map<String, Object> submitAppeal(Long refundId, Long userId, String appealContent, String appealEvidence) {
        log.info("用户 {} 提交申诉，退款ID: {}", userId, refundId);

        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 验证用户权限
        if (!refund.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        // 验证退款状态（只有被拒绝的才能申诉）
        if (refund.getRefundStatus() != OrderRefund.RefundStatus.FAILED) {
            throw new RuntimeException("当前状态不允许申诉");
        }

        // 检查沟通轮次
        if (refund.getCommunicationRound() == null) {
            refund.setCommunicationRound(1);
        }
        if (refund.getCommunicationRound() >= 3) {
            throw new RuntimeException("已达到最大申诉次数");
        }

        // 更新申诉信息
        refund.setCommunicationRound(refund.getCommunicationRound() + 1);

        // 将退款状态改回处理中
        refund.setRefundStatus(OrderRefund.RefundStatus.PROCESSING);

        orderRefundMapper.updateStatus(refund);

        // 保存申诉图片到 refund_images 表
        saveRefundImages(refundId, appealEvidence, RefundImage.ImageType.APPEAL);

        log.info("用户申诉提交成功，退款ID: {}, 沟通轮次: {}", refundId, refund.getCommunicationRound());

        return Map.of(
                "refundId", refundId,
                "communicationRound", refund.getCommunicationRound(),
                "message", "申诉提交成功"
        );
    }

    /**
     * 保存退款图片到 refund_images 表
     * @param refundId 退款ID
     * @param images 图片URL（逗号分隔）
     * @param imageType 图片类型
     */
    private void saveRefundImages(Long refundId, String images, RefundImage.ImageType imageType) {
        if (images == null || images.trim().isEmpty()) {
            return;
        }

        String[] imageUrls = images.split(",");
        List<RefundImage> refundImages = new ArrayList<>();

        for (int i = 0; i < imageUrls.length; i++) {
            String url = imageUrls[i].trim();
            if (!url.isEmpty()) {
                RefundImage refundImage = RefundImage.builder()
                        .id(snowflakeIdGenerator.nextId())
                        .refundId(refundId)
                        .image(url)
                        .imageType(imageType.name())
                        .sortOrder(i)
                        .createdAt(LocalDateTime.now())
                        .build();
                refundImages.add(refundImage);
            }
        }

        if (!refundImages.isEmpty()) {
            refundImageMapper.batchInsert(refundImages);
            log.info("保存{}张{}图片到退款ID: {}", refundImages.size(), imageType.name(), refundId);
        }
    }

    /**
     * 恢复订单项库存
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            orderItemMapper.restoreStock(item.getId());
        }
    }

    /**
     * 用户申请退款/售后（支持图片和视频上传）
     * @param userId 用户ID
     * @param orderItemId 订单项ID
     * @param refundType 退款类型: REFUND-退款, AFTER_SALE-售后
     * @param refundReason 退款原因
     * @param refundAmount 退款金额
     * @param description 退款描述
     * @param images 证据图片列表
     * @param videos 证据视频列表（最多3个）
     * @param videoCovers 视频封面列表（与视频一一对应）
     * @return 退款结果
     */
    @Transactional
    public Map<String, Object> submitRefundWithMedia(Long userId, Long orderItemId, String refundType,
                                                     String refundReason, BigDecimal refundAmount,
                                                     String description, List<MultipartFile> images,
                                                     List<MultipartFile> videos, List<MultipartFile> videoCovers) {
        log.info("用户 {} 申请{}，订单项ID: {}, 原因: {}, 金额: {}, 图片数: {}, 视频数: {}",
                userId, refundType.equals("AFTER_SALE") ? "售后" : "退款",
                orderItemId, refundReason, refundAmount,
                images != null ? images.size() : 0,
                videos != null ? videos.size() : 0);

        // 查询订单项
        OrderItem orderItem = orderItemMapper.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 查询订单
        Order order = orderMapper.findById(orderItem.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证用户权限
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 根据退款类型区分状态判断
        if ("REFUND".equals(refundType)) {
            // 退款（未发货）：仅允许已付款、处理中状态
            Set<Order.OrderStatus> refundableStatuses = Set.of(
                    Order.OrderStatus.PAID,
                    Order.OrderStatus.PROCESSING
            );
            if (!refundableStatuses.contains(order.getStatus())) {
                throw new RuntimeException("当前订单状态不允许申请退款");
            }
        } else if ("AFTER_SALE".equals(refundType)) {
            // 售后（已发货）：允许已发货、已送达、已完成状态
            Set<Order.OrderStatus> refundableStatuses = Set.of(
                    Order.OrderStatus.SHIPPED,
                    Order.OrderStatus.DELIVERED,
                    Order.OrderStatus.COMPLETED
            );
            if (!refundableStatuses.contains(order.getStatus())) {
                throw new RuntimeException("当前订单状态不允许申请售后");
            }
        }

        // 检查该订单项是否已有退款申请
        List<OrderRefund> existingRefunds = orderRefundMapper.findByOrderItemId(orderItemId);
        if (!existingRefunds.isEmpty()) {
            throw new RuntimeException("该商品已有退款申请");
        }

        // 校验图片
        validateImages(images);

        // 校验视频
        validateVideos(videos);

        // 创建退款记录
        OrderRefund refund = new OrderRefund();
        refund.setId(snowflakeIdGenerator.nextId());
        refund.setOrderId(order.getId());
        refund.setOrderItemId(orderItemId);
        refund.setOrderNumber(order.getOrderNumber());
        refund.setUserId(userId);
        refund.setSellerId(orderItem.getSellerId()); // 设置商家ID
        refund.setRefundAmount(refundAmount);
        refund.setRefundStatus(OrderRefund.RefundStatus.PROCESSING);
        refund.setRefundReason(refundReason);
        refund.setRefundType(refundType);
        refund.setDescription(description);
        refund.setApplyTime(LocalDateTime.now());
        refund.setCommunicationRound(0);

        orderRefundMapper.insert(refund);

        // 保存证据图片
        if (images != null && !images.isEmpty()) {
            saveRefundImagesWithFiles(refund.getId(), images);
        }

        // 保存证据视频
        if (videos != null && !videos.isEmpty()) {
            saveRefundVideos(refund.getId(), videos, videoCovers);
        }

        // 更新订单项的售后状态和退款ID
        String itemRefundStatus = "REFUND".equals(refundType) ?
                com.xiaoshan.springbootdemo.entity.OrderItem.RefundStatus.REFUNDING :
                com.xiaoshan.springbootdemo.entity.OrderItem.RefundStatus.AFTER_SALE;
        orderItemMapper.updateRefundStatusWithId(orderItemId, itemRefundStatus, refund.getId());

        log.info("{}申请成功，退款ID: {}", refundType.equals("AFTER_SALE") ? "售后" : "退款", refund.getId());

        return Map.of(
                "refundId", refund.getId(),
                "orderItemId", refund.getOrderItemId(),
                "orderId", refund.getOrderId()
        );
    }

    /**
     * 校验图片文件列表
     * @param images 图片文件列表
     */
    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        if (images.size() > 6) {
            throw new RuntimeException("凭证图片最多上传6张");
        }

        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            if (image.isEmpty()) {
                throw new RuntimeException("第" + (i + 1) + "张图片不能为空");
            }
            if (image.getSize() > MAX_IMAGE_SIZE) {
                throw new RuntimeException("第" + (i + 1) + "张图片大小不能超过5MB");
            }
            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new RuntimeException("第" + (i + 1) + "张图片格式不支持");
            }
        }
    }

    /**
     * 校验视频文件列表
     * @param videos 视频文件列表
     */
    private void validateVideos(List<MultipartFile> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        if (videos.size() > 3) {
            throw new RuntimeException("凭证视频最多上传3个");
        }

        for (int i = 0; i < videos.size(); i++) {
            MultipartFile video = videos.get(i);
            if (video.isEmpty()) {
                throw new RuntimeException("第" + (i + 1) + "个视频不能为空");
            }
            if (video.getSize() > MAX_VIDEO_SIZE) {
                throw new RuntimeException("第" + (i + 1) + "个视频大小不能超过30MB");
            }
            String contentType = video.getContentType();
            if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
                throw new RuntimeException("第" + (i + 1) + "个视频格式不支持，请上传 MP4、MOV、AVI 或 WEBM 格式");
            }
        }
    }

    /**
     * 上传退款媒体文件到服务器
     * @param file 媒体文件
     * @param refundId 退款ID
     * @param index 文件序号
     * @param isVideo 是否为视频
     * @return 文件URL
     */
    private String uploadRefundMedia(MultipartFile file, Long refundId, int index, boolean isVideo) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = index + "_" + UUID.randomUUID() + extension;

            String mediaType = isVideo ? "videos" : "images";
            Path uploadPath = Paths.get(uploadDir, "refunds", String.valueOf(refundId), mediaType).toAbsolutePath();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath);

            String url = "/uploads/refunds/" + refundId + "/" + mediaType + "/" + fileName;
            log.info("退款媒体文件上传成功: refundId={}, url={}", refundId, url);

            return url;

        } catch (IOException e) {
            log.error("退款媒体文件上传失败: refundId={}", refundId, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存退款图片
     * @param refundId 退款ID
     * @param images 图片文件列表
     */
    private void saveRefundImagesWithFiles(Long refundId, List<MultipartFile> images) {
        List<RefundImage> refundImages = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            String url = uploadRefundMedia(images.get(i), refundId, i, false);
            RefundImage refundImage = RefundImage.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .refundId(refundId)
                    .image(url)
                    .imageType(RefundImage.ImageType.EVIDENCE.name())
                    .sortOrder(i)
                    .createdAt(LocalDateTime.now())
                    .build();
            refundImages.add(refundImage);
        }

        if (!refundImages.isEmpty()) {
            refundImageMapper.batchInsert(refundImages);
            log.info("保存{}张退款图片到退款ID: {}", refundImages.size(), refundId);
        }
    }

    /**
     * 保存退款视频
     * @param refundId 退款ID
     * @param videos 视频文件列表
     * @param videoCovers 视频封面文件列表
     */
    private void saveRefundVideos(Long refundId, List<MultipartFile> videos, List<MultipartFile> videoCovers) {
        List<RefundVideo> refundVideos = new ArrayList<>();

        for (int i = 0; i < videos.size(); i++) {
            MultipartFile videoFile = videos.get(i);

            String videoUrl = uploadRefundMedia(videoFile, refundId, i, true);

            String coverUrl = null;
            if (videoCovers != null && videoCovers.size() > i && videoCovers.get(i) != null && !videoCovers.get(i).isEmpty()) {
                coverUrl = uploadRefundMedia(videoCovers.get(i), refundId, i + 1000, false);
                log.info("使用前端上传的视频封面: refundId={}, coverUrl={}", refundId, coverUrl);
            }

            RefundVideo refundVideo = RefundVideo.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .refundId(refundId)
                    .videoUrl(videoUrl)
                    .coverUrl(coverUrl)
                    .size(videoFile.getSize())
                    .sortOrder(i)
                    .createdAt(LocalDateTime.now())
                    .build();
            refundVideos.add(refundVideo);
        }

        if (!refundVideos.isEmpty()) {
            refundVideoMapper.batchInsert(refundVideos);
            log.info("保存{}个退款视频到退款ID: {}", refundVideos.size(), refundId);
        }
    }

    /**
     * 用户提交退货信息
     * @param refundId 退款ID
     * @param userId 用户ID
     * @param returnMethod 退货方式
     * @param addressId 用户地址ID（上门取件时必填）
     * @param pickupDate 取件日期（上门取件时必填）
     * @param pickupTime 取件时间段（上门取件时必填）
     * @param returnLogisticsName 退货物流公司（自寄时必填）
     * @param returnTrackingNumber 退货单号（自寄时必填）
     * @return 退货信息
     */
    @Transactional
    public Map<String, Object> submitReturn(Long refundId, Long userId, String returnMethod,
                                           Long addressId, String pickupDate, String pickupTime,
                                           String returnLogisticsName, String returnTrackingNumber) {
        log.info("用户 {} 提交退货信息，退款ID: {}, 方式: {}", userId, refundId, returnMethod);

        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 验证用户权限
        if (!refund.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        // 验证退款类型（只有售后才需要退货）
        if (!"AFTER_SALE".equals(refund.getRefundType())) {
            throw new RuntimeException("当前退款类型不需要退货");
        }

        // 验证退款状态（商家已同意或已同意待退货的才能提交退货）
        if (refund.getRefundStatus() != OrderRefund.RefundStatus.APPROVED &&
            refund.getRefundStatus() != OrderRefund.RefundStatus.PROCESSING) {
            throw new RuntimeException("当前状态不允许提交退货");
        }

        // 更新退货信息
        refund.setReturnMethod(returnMethod);
        refund.setReturnStatus("RETURNING");
        refund.setReturnApplyTime(LocalDateTime.now());

        if ("SELF".equals(returnMethod)) {
            // 自行寄回：直接保存用户填写的物流信息
            if (returnLogisticsName == null || returnLogisticsName.trim().isEmpty()) {
                throw new RuntimeException("请选择物流公司");
            }
            if (returnTrackingNumber == null || returnTrackingNumber.trim().isEmpty()) {
                throw new RuntimeException("请填写物流单号");
            }
            
            refund.setReturnLogisticsName(returnLogisticsName);
            refund.setReturnTrackingNumber(returnTrackingNumber);
            
            log.info("用户选择自行寄回，物流公司: {}, 单号: {}", returnLogisticsName, returnTrackingNumber);
            
        } else if ("PICKUP".equals(returnMethod)) {
            // 上门取件：需要调用快递鸟接口
            if (addressId == null) {
                throw new RuntimeException("请选择取件地址");
            }
            if (pickupDate == null || pickupDate.isEmpty()) {
                throw new RuntimeException("请选择取件日期");
            }
            if (pickupTime == null || pickupTime.isEmpty()) {
                throw new RuntimeException("请选择取件时间段");
            }

            // 根据 addressId 查询用户地址
            Address pickupAddress = null;
            try {
                pickupAddress = addressService.getAddressById(addressId);
                log.info("取件地址查询成功: {}, {}", pickupAddress.getRecipientName(), pickupAddress.getRecipientPhone());
            } catch (Exception e) {
                log.error("查询取件地址失败: {}", e.getMessage());
                throw new RuntimeException("取件地址不存在");
            }

            // 查询商家地址（从 seller_profiles 获取）
            Map<String, Object> sellerInfo = null;
            try {
                if (refund.getSellerId() != null) {
                    sellerInfo = sellerProfileService.getByUserId(refund.getSellerId());
                }
            } catch (Exception e) {
                log.warn("获取商家信息失败: {}", e.getMessage());
            }

            // 调用快递鸟接口创建上门取件订单
            Map<String, Object> logisticsResult = createPickupOrder(
                    refundId,
                    pickupAddress,
                    sellerInfo,
                    pickupDate,
                    pickupTime
            );

            // 如果快递鸟调用成功，更新退款记录
            if (logisticsResult != null && "success".equals(logisticsResult.get("status"))) {
                refund.setReturnTrackingNumber((String) logisticsResult.get("trackingNumber"));
                refund.setReturnLogisticsName((String) logisticsResult.get("logisticsName"));
                log.info("快递鸟上门取件创建成功，单号: {}, 快递公司: {}", 
                        logisticsResult.get("trackingNumber"), logisticsResult.get("logisticsName"));
            } else {
                // 快递鸟调用失败，返回错误提示
                String errorMsg = (String) logisticsResult.get("message");
                throw new RuntimeException(errorMsg != null ? errorMsg : "上门取件订单创建失败，请稍后重试");
            }
        }

        orderRefundMapper.updateReturnInfo(refund);

        // 更新 order_items 表的 refund_status 为 RETURNING
        if (refund.getOrderItemId() != null) {
            orderItemMapper.updateRefundStatus(refund.getOrderItemId(), "RETURNING");
            log.info("更新订单项状态为 RETURNING，订单项ID: {}", refund.getOrderItemId());
        }

        log.info("用户退货信息提交成功，退款ID: {}", refundId);

        return Map.of(
                "refundId", refundId,
                "returnMethod", returnMethod,
                "returnStatus", "RETURNING",
                "message", "退货信息已提交"
        );
    }

    /**
     * 调用快递鸟接口创建上门取件订单
     * @param refundId 退款ID
     * @param pickupAddress 用户取件地址
     * @param sellerInfo 商家信息（包含商家地址）
     * @param pickupDate 取件日期
     * @param pickupTime 取件时间段
     * @return 快递鸟返回结果
     */
    private Map<String, Object> createPickupOrder(Long refundId, Address pickupAddress, 
                                                  Map<String, Object> sellerInfo,
                                                  String pickupDate, String pickupTime) {
        log.info("开始创建上门取件订单（模拟），退款ID: {}", refundId);

        try {
            // 寄件人信息（用户地址）
            String senderName = pickupAddress.getRecipientName();
            String senderPhone = pickupAddress.getRecipientPhone();
            String senderProvince = pickupAddress.getProvince();
            String senderCity = pickupAddress.getCity();
            String senderDistrict = pickupAddress.getDistrict();
            String senderAddress = pickupAddress.getDetailAddress();

            // 收件人信息（商家地址）
            String receiverName = sellerInfo != null && sellerInfo.get("shopName") != null ? 
                    (String) sellerInfo.get("shopName") : "商家店铺";
            String receiverPhone = sellerInfo != null && sellerInfo.get("phone") != null && !((String) sellerInfo.get("phone")).isEmpty() ? 
                    (String) sellerInfo.get("phone") : "13800000000";
            String receiverProvince = sellerInfo != null && sellerInfo.get("province") != null && !((String) sellerInfo.get("province")).isEmpty() ? 
                    (String) sellerInfo.get("province") : "广东省";
            String receiverCity = sellerInfo != null && sellerInfo.get("city") != null && !((String) sellerInfo.get("city")).isEmpty() ? 
                    (String) sellerInfo.get("city") : "广州市";
            String receiverDistrict = sellerInfo != null && sellerInfo.get("district") != null && !((String) sellerInfo.get("district")).isEmpty() ? 
                    (String) sellerInfo.get("district") : "天河区";
            String receiverAddress = sellerInfo != null && sellerInfo.get("address") != null && !((String) sellerInfo.get("address")).isEmpty() ? 
                    (String) sellerInfo.get("address") : "请与商家联系获取详细地址";

            // 取件时间格式化
            String pickupTimeDesc = formatPickupTime(pickupDate, pickupTime);

            log.info("寄件人: {} {}, 地址: {} {} {} {}", 
                    senderName, senderPhone, senderProvince, senderCity, senderDistrict, senderAddress);
            log.info("收件人: {} {}, 地址: {} {} {} {}", 
                    receiverName, receiverPhone, receiverProvince, receiverCity, receiverDistrict, receiverAddress);
            log.info("取件时间: {}", pickupTimeDesc);

            // ========== 模拟数据返回 ==========
            String mockTrackingNumber = "465144307875876";
            String mockLogisticsName = "韵达快递";

            log.info("上门取件订单创建成功（模拟），单号: {}", mockTrackingNumber);

            return Map.of(
                    "status", "success",
                    "trackingNumber", mockTrackingNumber,
                    "logisticsName", mockLogisticsName,
                    "message", "上门取件订单创建成功"
            );

        } catch (Exception e) {
            log.error("上门取件订单创建失败: {}", e.getMessage(), e);
            return Map.of(
                    "status", "error",
                    "message", "上门取件订单创建失败: " + e.getMessage()
            );
        }
    }



    /**
     * 构建电子面单请求数据JSON字符串
     * 根据快递鸟1007接口文档构建请求参数
     */
    private String buildPickupRequestData(String shipperCode, Long refundId,
                                          String senderName, String senderPhone,
                                          String senderProvince, String senderCity, String senderDistrict, String senderAddress,
                                          String receiverName, String receiverPhone,
                                          String receiverProvince, String receiverCity, String receiverDistrict, String receiverAddress,
                                          String startDate, String endDate) {
        // 构建订单编号（唯一，用退款ID生成）
        String orderCode = "REFUND_" + refundId;

        // 构建寄件人JSON
        String senderJson = String.format(
                "{\"Name\":\"%s\",\"Mobile\":\"%s\",\"ProvinceName\":\"%s\",\"CityName\":\"%s\",\"ExpAreaName\":\"%s\",\"Address\":\"%s\"}",
                senderName, senderPhone, senderProvince, senderCity, senderDistrict, senderAddress
        );

        // 构建收件人JSON
        String receiverJson = String.format(
                "{\"Name\":\"%s\",\"Mobile\":\"%s\",\"ProvinceName\":\"%s\",\"CityName\":\"%s\",\"ExpAreaName\":\"%s\",\"Address\":\"%s\"}",
                receiverName, receiverPhone, receiverProvince, receiverCity, receiverDistrict, receiverAddress
        );

        // 构建商品信息JSON
        String commodityJson = "[{\"GoodsName\":\"退货商品\",\"Goodsquantity\":1,\"GoodsWeight\":1.0,\"GoodsDesc\":\"\"}]";

        return String.format(
                "{\"ShipperCode\":\"%s\",\"OrderCode\":\"%s\",\"PayType\":1,\"ExpType\":\"1\",\"Sender\":%s,\"Receiver\":%s,\"Quantity\":1,\"Commodity\":%s,\"IsNotice\":0,\"StartDate\":\"%s\",\"EndDate\":\"%s\"}",
                shipperCode, orderCode, senderJson, receiverJson, commodityJson, startDate, endDate
        );
    }

    /**
     * 发送POST请求到快递鸟API
     * @param requestData 请求数据JSON
     * @param requestType 请求类型
     * @param apiUrl API地址
     * @return 快递鸟返回的响应字符串
     */
    private String sendPostRequestToKdniao(String requestData, String requestType, String apiUrl) throws Exception {
        // 计算签名（快递鸟要求Base64编码的MD5）
        String dataSign = encryptKdniao(requestData);

        // 构建POST请求参数（按照官方文档格式）
        StringBuilder params = new StringBuilder();
        params.append("RequestData=").append(URLEncoder.encode(requestData, "UTF-8"));
        params.append("&EBusinessID=").append(URLEncoder.encode(kdniaoAppKey, "UTF-8"));
        params.append("&RequestType=").append(requestType);
        params.append("&DataSign=").append(URLEncoder.encode(dataSign, "UTF-8"));
        params.append("&DataType=2");

        // 创建URL对象
        URL url = new URL(apiUrl);

        // 打开HTTP连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方法为POST
        connection.setRequestMethod("POST");

        // 设置连接超时时间（30秒）
        connection.setConnectTimeout(30000);

        // 设置读取超时时间（30秒）
        connection.setReadTimeout(30000);

        // 允许输出数据
        connection.setDoOutput(true);

        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        // 发送请求参数
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
            writer.write(params.toString());
            writer.flush();
        }

        // 获取HTTP响应状态码
        int responseCode = connection.getResponseCode();
        log.info("快递鸟API响应状态码: {}", responseCode);

        // 读取响应内容
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    /**
     * 快递鸟签名算法
     * 规则：MD5(content) 得到32位小写字符串，再对这个字符串做Base64编码
     *
     * @param content 待签名内容（IP + RequestData + AppSecret）
     * @return Base64编码后的签名
     */
    private String encrypt(String content) throws Exception {
        String charset = "UTF-8";

        // MD5加密（得到32位小写）
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content.getBytes(charset));

        // 转为十六进制字符串（小写）
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String md5Hex = hexString.toString().toLowerCase();

        // 对MD5结果进行Base64编码
        String result = Base64.getEncoder().encodeToString(md5Hex.getBytes(charset));

        return result;
    }

    /**
     * 快递鸟签名算法（旧版本，用于其他接口）
     * 规则：MD5(RequestData + AppSecret) 得到32位小写字符串，再对这个字符串做Base64编码
     */
    private String encryptKdniao(String requestData) throws Exception {
        // 拼接字符串（请求数据 + AppSecret）
        String toEncrypt = requestData + kdniaoAppSecret;

        // MD5加密（得到32位小写）
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(toEncrypt.getBytes("UTF-8"));

        // 转为十六进制字符串（小写）
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String md5Hex = hexString.toString().toLowerCase();

        // 对MD5结果进行Base64编码
        String result = Base64.getEncoder().encodeToString(md5Hex.getBytes("UTF-8"));

        return result;
    }

    /**
     * 解析电子面单接口返回结果（1007接口）
     */
    private Map<String, Object> parsePickupResponse(String response) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response);

        boolean success = jsonNode.has("Success") && "true".equals(jsonNode.get("Success").asText());

        if (success) {
            // 电子面单接口返回格式：{"Success":true,"Order":{"OrderCode":"xxx","ShipperCode":"SF","LogisticCode":"SF1234567890"}}
            JsonNode orderNode = jsonNode.get("Order");
            String trackingNumber = "";
            String logisticsName = "";
            
            if (orderNode != null) {
                trackingNumber = orderNode.has("LogisticCode") ? orderNode.get("LogisticCode").asText() : "";
                
                // 根据快递公司编码获取名称
                String shipperCode = orderNode.has("ShipperCode") ? orderNode.get("ShipperCode").asText() : "";
                logisticsName = getLogisticsName(shipperCode);
            }

            log.info("快递鸟电子面单创建成功，单号: {}, 快递公司: {}", trackingNumber, logisticsName);

            return Map.of(
                    "status", "success",
                    "trackingNumber", trackingNumber,
                    "logisticsName", logisticsName,
                    "message", "电子面单创建成功"
            );
        } else {
            String reason = jsonNode.has("Reason") ? jsonNode.get("Reason").asText() : "创建失败";
            log.error("快递鸟电子面单创建失败: {}", reason);

            return Map.of(
                    "status", "error",
                    "message", reason
            );
        }
    }

    /**
     * 根据物流公司代码获取中文名称
     */
    private String getLogisticsName(String code) {
        Map<String, String> nameMap = Map.of(
                "SF", "顺丰速运",
                "YTO", "圆通速递",
                "ZTO", "中通快递",
                "EMS", "邮政EMS",
                "YD", "韵达快递",
                "STO", "申通快递",
                "JT", "极兔速递",
                "JD", "京东物流",
                "HTKY", "百世快递"
        );
        return nameMap.getOrDefault(code, code);
    }

    /**
     * 取消取件预约（调用快递鸟接口）
     * @param refundId 退款ID
     * @param userId 用户ID
     * @param logisticCode 物流单号
     * @param shipperCode 快递公司编码
     * @param orderCode 订单编号
     * @return 取消结果
     */
    public Map<String, Object> cancelPickupOrder(Long refundId, Long userId, 
                                                  String logisticCode, String shipperCode, String orderCode) {
        log.info("开始取消取件预约，退款ID: {}, 物流单号: {}", refundId, logisticCode);

        try {
            // 验证退款记录是否存在
            OrderRefund refund = orderRefundMapper.findById(refundId).orElse(null);
            if (refund == null) {
                return Map.of("status", "error", "message", "退款记录不存在");
            }

            // 验证用户权限
            if (!refund.getUserId().equals(userId)) {
                return Map.of("status", "error", "message", "您无权取消此预约");
            }

            // 构建取消请求数据
            String requestData = buildCancelPickupRequestData(shipperCode, logisticCode, orderCode);

            log.info("快递鸟取消取件请求数据: {}", requestData);

            // 调用快递鸟接口取消订单（使用电子面单接口地址）
            String response = sendPostRequestToKdniao(requestData, REQUEST_TYPE_EORDER, kdniaoEorderUrl);

            log.info("快递鸟取消取件响应数据: {}", response);

            // 解析取消结果
            Map<String, Object> result = parseCancelPickupResponse(response);

            if ("success".equals(result.get("status"))) {
                // 取消成功，更新数据库状态
                refund.setReturnStatus(null);
                refund.setReturnTrackingNumber(null);
                refund.setReturnLogisticsName(null);
                refund.setReturnMethod(null);
                orderRefundMapper.updateReturnInfo(refund);
                
                log.info("取件预约已取消，退款ID: {}", refundId);
            }

            return result;

        } catch (Exception e) {
            log.error("取消取件预约失败: {}", e.getMessage(), e);
            return Map.of(
                    "status", "error",
                    "message", "取消取件预约失败: " + e.getMessage()
            );
        }
    }

    /**
     * 构建取消取件请求数据
     * 快递鸟电子面单接口取消订单需要传 CancelOrder: "1"
     */
    private String buildCancelPickupRequestData(String shipperCode, String logisticCode, String orderCode) {
        return String.format(
                "{\"ShipperCode\":\"%s\",\"LogisticCode\":\"%s\",\"OrderCode\":\"%s\",\"CancelOrder\":\"1\"}",
                shipperCode, logisticCode, orderCode
        );
    }

    /**
     * 解析取消取件接口返回结果
     */
    private Map<String, Object> parseCancelPickupResponse(String response) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response);

        boolean success = jsonNode.has("Success") && "true".equals(jsonNode.get("Success").asText());

        if (success) {
            log.info("快递鸟取消取件成功");
            return Map.of(
                    "status", "success",
                    "message", "取消成功"
            );
        } else {
            String reason = jsonNode.has("Reason") ? jsonNode.get("Reason").asText() : "取消失败";
            log.error("快递鸟取消取件失败: {}", reason);

            return Map.of(
                    "status", "error",
                    "message", reason
            );
        }
    }

    /**
     * 格式化取件时间
     * @param pickupDate 取件日期
     * @param pickupTime 取件时间段
     * @return 格式化后的时间描述
     */
    private String formatPickupTime(String pickupDate, String pickupTime) {
        StringBuilder sb = new StringBuilder();
        sb.append(pickupDate);
        sb.append(" ");
        
        switch (pickupTime) {
            case "WEEKDAY_MORNING":
                sb.append("工作日上午 9:00-12:00");
                break;
            case "WEEKDAY_AFTERNOON":
                sb.append("工作日下午 14:00-18:00");
                break;
            case "WEEKEND_MORNING":
                sb.append("周末上午 9:00-12:00");
                break;
            case "WEEKEND_AFTERNOON":
                sb.append("周末下午 14:00-18:00");
                break;
            default:
                sb.append(pickupTime);
        }
        
        return sb.toString();
    }

    /**
     * 格式化取件时间范围（电子面单接口1007需要）
     * @param pickupDate 取件日期
     * @param pickupTime 取件时间段
     * @return 返回数组 [startDate, endDate]，格式为 "yyyy-MM-dd HH:mm:ss"
     */
    private String[] formatPickupTimeRange(String pickupDate, String pickupTime) {
        String startDate = pickupDate + " ";
        String endDate = pickupDate + " ";
        
        switch (pickupTime) {
            case "WEEKDAY_MORNING":
            case "WEEKEND_MORNING":
                startDate += "09:00:00";
                endDate += "12:00:00";
                break;
            case "WEEKDAY_AFTERNOON":
            case "WEEKEND_AFTERNOON":
                startDate += "14:00:00";
                endDate += "18:00:00";
                break;
            default:
                // 默认使用上午时间
                startDate += "09:00:00";
                endDate += "12:00:00";
        }
        
        return new String[]{startDate, endDate};
    }

    /**
     * 商家确认收货
     * @param refundId 退款ID
     * @param sellerId 商家ID
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> confirmReturn(Long refundId, Long sellerId) {
        log.info("商家 {} 确认收货，退款ID: {}", sellerId, refundId);

        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 通过 orderItem 获取 sellerId，而不是退款记录
        OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 验证商家权限
        if (!orderItem.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        // 验证退货状态（只有退货中才能确认收货）
        if (!"RETURNING".equals(refund.getReturnStatus())) {
            throw new RuntimeException("当前状态不允许确认收货");
        }

        // 更新退款记录
        refund.setReturnStatus("RECEIVED");
        refund.setReturnReceiveTime(LocalDateTime.now());
        refund.setRefundStatus(OrderRefund.RefundStatus.SUCCESS);
        refund.setCompleteTime(LocalDateTime.now());

        orderRefundMapper.confirmReceive(refund);

        // 更新订单项状态
        if (refund.getOrderItemId() != null) {
            orderItemMapper.updateRefundStatus(refund.getOrderItemId(), "COMPLETED");
            log.info("更新订单项状态为 COMPLETED，订单项ID: {}", refund.getOrderItemId());
        }

        // 更新订单状态为 REFUNDED（只有当所有订单项都已退款完成时）
        if (refund.getOrderId() != null) {
            List<OrderItem> orderItems = orderItemMapper.findByOrderId(refund.getOrderId());
            boolean allRefunded = orderItems.stream().allMatch(
                item -> "COMPLETED".equals(item.getRefundStatus())
            );
            
            if (allRefunded) {
                orderMapper.updateStatus(refund.getOrderId(), "REFUNDED");
                log.info("更新订单状态为 REFUNDED，订单ID: {}", refund.getOrderId());
            } else {
                log.info("订单部分退款完成，保持原状态，订单ID: {}", refund.getOrderId());
            }
        }

        // 触发退款到账
        try {
            String refundReason = refund.getRefundReason() != null ? refund.getRefundReason() : "退货退款";
            alipayService.refund(refund.getOrderNumber(), refund.getRefundAmount(), refundReason);
            log.info("退款到账成功，订单号: {}, 金额: {}", refund.getOrderNumber(), refund.getRefundAmount());
        } catch (Exception e) {
            log.error("退款到账失败: {}", e.getMessage());
            // 退款失败不回滚，记录日志并继续
        }

        log.info("商家确认收货成功，退款ID: {}", refundId);

        return Map.of(
                "refundId", refundId,
                "returnStatus", "RECEIVED",
                "refundStatus", "SUCCESS",
                "message", "确认收货成功，退款已处理"
        );
    }

    /**
     * 查询退货物流信息
     * @param refundId 退款ID
     * @param sellerId 商家ID
     * @return 物流信息
     */
    public Map<String, Object> queryReturnLogistics(Long refundId, Long sellerId) {
        log.info("商家 {} 查询退货物流，退款ID: {}", sellerId, refundId);

        OrderRefund refund = orderRefundMapper.findById(refundId)
                .orElseThrow(() -> new RuntimeException("退款记录不存在"));

        // 通过 orderItem 获取 sellerId，而不是退款记录
        OrderItem orderItem = orderItemMapper.findById(refund.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        // 验证商家权限
        if (!orderItem.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此退款申请");
        }

        String trackingNumber = refund.getReturnTrackingNumber();
        String logisticsName = refund.getReturnLogisticsName();

        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new RuntimeException("暂无物流单号");
        }

        // 调用快递鸟接口查询物流
        return queryLogisticsFromKdniao(trackingNumber, logisticsName);
    }

    /**
     * 调用快递鸟接口查询物流信息
     * @param trackingNumber 物流单号
     * @param logisticsName 物流公司名称
     * @return 物流信息
     */
    private Map<String, Object> queryLogisticsFromKdniao(String trackingNumber, String logisticsName) {
        log.info("查询快递鸟物流信息，单号: {}, 公司: {}", trackingNumber, logisticsName);

        try {
            // TODO: 集成快递鸟接口
            // 快递鸟接口文档：https://www.kdniao.com/api-track
            // 接口地址：https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx
            // 接口类型：1002（查询物流）

            // 模拟物流轨迹数据
            List<Map<String, String>> logisticsTraces = new ArrayList<>();
            logisticsTraces.add(Map.of(
                    "time", LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "status", "快件正在派送中"
            ));
            logisticsTraces.add(Map.of(
                    "time", LocalDateTime.now().minusHours(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "status", "快件到达派送网点"
            ));
            logisticsTraces.add(Map.of(
                    "time", LocalDateTime.now().minusHours(8).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "status", "快件已发出"
            ));
            logisticsTraces.add(Map.of(
                    "time", LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "status", "快件已揽收"
            ));

            log.info("快递鸟物流查询成功");

            return Map.of(
                    "status", "success",
                    "trackingNumber", trackingNumber,
                    "logisticsName", logisticsName,
                    "traces", logisticsTraces,
                    "message", "查询成功"
            );

        } catch (Exception e) {
            log.error("快递鸟物流查询失败: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", "物流查询失败: " + e.getMessage()
            );
        }
    }
}
