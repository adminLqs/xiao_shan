package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.OrderRefund;
import com.xiaoshan.springbootdemo.service.AddressService;
import com.xiaoshan.springbootdemo.service.OrderRefundService;
import com.xiaoshan.springbootdemo.service.SellerProfileService;
import com.xiaoshan.springbootdemo.service.UserService;
import com.xiaoshan.springbootdemo.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退款售后控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderRefundController {

    private final OrderRefundService orderRefundService;
    private final UserService userService;
    private final AddressService addressService;
    private final SellerProfileService sellerProfileService;

    /**
     * 用户申请退款/售后
     * POST /api/v1/orders/{orderId}/refund
     */
    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> applyRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            
            Long orderItemId = requestBody.get("orderItemId") != null ? 
                    ((Number) requestBody.get("orderItemId")).longValue() : null;
            String refundType = (String) requestBody.get("refundType");
            String refundReason = (String) requestBody.get("refundReason");
            BigDecimal refundAmount = requestBody.get("refundAmount") != null ? 
                    new BigDecimal(requestBody.get("refundAmount").toString()) : null;
            String description = (String) requestBody.get("description");
            String evidenceImages = (String) requestBody.get("evidenceImages");

            // 参数校验
            if (orderItemId == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择要退款的商品"
                ));
            }
            if (refundType == null || (!refundType.equals("REFUND") && !refundType.equals("AFTER_SALE"))) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择退款类型"
                ));
            }
            if (refundReason == null || refundReason.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择退款原因"
                ));
            }
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "退款金额必须大于 0"
                ));
            }

            // 校验证据图片格式和数量
            if (evidenceImages != null && !evidenceImages.isEmpty()) {
                String[] imageUrls = evidenceImages.split(",");
                if (imageUrls.length > 6) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "凭证图片最多上传 6 张"
                    ));
                }
                // 验证每个 URL 格式（允许 http://、https:// 或 /uploads/ 开头）
                for (String url : imageUrls) {
                    if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/uploads/")) {
                        return ResponseEntity.ok(Map.of(
                                "success", false,
                                "message", "凭证图片格式不正确"
                        ));
                    }
                }
            }

            // 调用服务申请退款
            OrderRefund refund = orderRefundService.applyRefund(
                    userId, orderItemId, refundType, refundReason, 
                    refundAmount, description, evidenceImages);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款申请提交成功",
                    "data", Map.of(
                            "refundId", refund.getId(),
                            "orderItemId", refund.getOrderItemId()
                    )
            ));

        } catch (RuntimeException e) {
            log.warn("退款申请失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("退款申请系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 查询订单退款状态
     * GET /api/v1/orders/{orderId}/refund/status
     */
    @GetMapping("/orders/{orderId}/refund/status")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getRefundStatus(
            Authentication authentication,
            @PathVariable Long orderId) {
        try {
            List<OrderRefund> refunds = orderRefundService.getOrderRefunds(orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refunds
            ));

        } catch (Exception e) {
            log.error("查询退款状态失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "查询失败"
            ));
        }
    }

    /**
     * 商家同意退款
     * PUT /api/v1/seller/orders/{orderId}/refund/{refundId}/approve
     */
    @PutMapping("/seller/orders/{orderId}/refund/{refundId}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> approveRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            String notes = requestBody != null ? (String) requestBody.get("notes") : null;

            Map<String, Object> result = orderRefundService.approveRefund(refundId, sellerId, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款已同意",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("同意退款失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("同意退款系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 商家拒绝退款
     * PUT /api/v1/seller/orders/{orderId}/refund/{refundId}/reject
     */
    @PutMapping("/seller/orders/{orderId}/refund/{refundId}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> rejectRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);
            String reviewNotes = (String) requestBody.get("reviewNotes");

            if (reviewNotes == null || reviewNotes.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请填写拒绝原因"
                ));
            }

            Map<String, Object> result = orderRefundService.rejectRefund(refundId, sellerId, reviewNotes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款已拒绝",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("拒绝退款失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("拒绝退款系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 获取用户的退款记录列表
     * GET /api/v1/user/refunds
     */
    @GetMapping("/user/refunds")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> getUserRefunds(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Map<String, Object> result = orderRefundService.getUserRefundsWithPage(userId, page, pageSize);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));

        } catch (Exception e) {
            log.error("获取退款记录失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "获取失败"
            ));
        }
    }

    /**
     * 获取用户进行中的退款数量
     * GET /api/v1/user/refunds/pending-count
     */
    @GetMapping("/user/refunds/pending-count")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> getPendingRefundCount(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            int count = orderRefundService.getPendingRefundCount(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("count", count)
            ));

        } catch (Exception e) {
            log.error("获取退款数量失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "获取失败",
                    "data", Map.of("count", 0)
            ));
        }
    }

    /**
     * 获取退款详情
     * GET /api/v1/refunds/{refundId}
     */
    @GetMapping("/refunds/{refundId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getRefundDetail(
            Authentication authentication,
            @PathVariable Long refundId) {
        try {
            // 调用新方法获取包含图片列表的退款详情
            Map<String, Object> refundDetail = orderRefundService.getRefundDetailWithImages(refundId);

            // 权限校验：用户只能查看自己的退款，商家只能查看自己订单的退款
            Long currentUserId = userService.getCurrentUserId(authentication);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refundDetail
            ));

        } catch (RuntimeException e) {
            log.warn("查询退款详情失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("查询退款详情系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 用户提交申诉
     * PUT /api/v1/refunds/{refundId}/appeal
     */
    @PutMapping("/refunds/{refundId}/appeal")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> submitAppeal(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestBody Map<String, String> requestBody) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            String appealContent = requestBody.get("appealContent");
            String evidenceImages = requestBody.get("evidenceImages");

            if (appealContent == null || appealContent.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请填写申诉理由"
                ));
            }

            Map<String, Object> result = orderRefundService.submitAppeal(refundId, userId, appealContent, evidenceImages);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "申诉提交成功",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("提交申诉失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("提交申诉系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 用户申请退款/售后（支持视频上传）
     * POST /api/v1/refunds/submit/{orderItemId}
     */
    @PostMapping("/refunds/submit/{orderItemId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> submitRefundWithMedia(
            Authentication authentication,
            @PathVariable Long orderItemId,
            @RequestParam(required = false) MultipartFile[] images,
            @RequestParam(required = false) MultipartFile[] videos,
            @RequestParam(required = false) MultipartFile[] videoCovers,
            @RequestParam String refundType,
            @RequestParam String refundReason,
            @RequestParam BigDecimal refundAmount,
            @RequestParam(required = false) String description) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            // 参数校验
            if (!refundType.equals("REFUND") && !refundType.equals("AFTER_SALE")) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择退款类型"
                ));
            }
            if (refundReason == null || refundReason.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择退款原因"
                ));
            }
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "退款金额必须大于 0"
                ));
            }

            // 转换文件数组为列表
            List<MultipartFile> imageList = images != null ? List.of(images) : new ArrayList<>();
            List<MultipartFile> videoList = videos != null ? List.of(videos) : new ArrayList<>();
            List<MultipartFile> coverList = videoCovers != null ? List.of(videoCovers) : new ArrayList<>();

            // 调用服务申请退款
            Map<String, Object> result = orderRefundService.submitRefundWithMedia(
                    userId, orderItemId, refundType, refundReason,
                    refundAmount, description, imageList, videoList, coverList);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退款申请提交成功",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("退款申请失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("退款申请系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 根据订单项ID获取退款记录
     * GET /api/v1/refunds/order-item/{orderItemId}
     */
    @GetMapping("/refunds/order-item/{orderItemId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> getRefundByOrderItemId(
            Authentication authentication,
            @PathVariable Long orderItemId) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            OrderRefund refund = orderRefundService.getRefundByOrderItemId(orderItemId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refund
            ));

        } catch (RuntimeException e) {
            log.warn("查询退款记录失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("查询退款记录系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 获取退货信息（商家地址、用户地址列表）
     * GET /api/v1/refunds/{refundId}/return-info
     */
    @GetMapping("/refunds/{refundId}/return-info")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> getReturnInfo(
            Authentication authentication,
            @PathVariable Long refundId) {
        try {
            // 查询退款记录
            OrderRefund refund = orderRefundService.getById(refundId);
            if (refund == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "退款记录不存在"
                ));
            }

            // 权限校验：用户只能查看自己的退款
            Long currentUserId = userService.getCurrentUserId(authentication);
            if (!refund.getUserId().equals(currentUserId)) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "您无权查看此退款"
                ));
            }

            // 查询商家收货地址（自寄时需要）
            Map<String, Object> sellerAddress = new HashMap<>();
            if (refund.getSellerId() != null) {
                try {
                    Map<String, Object> seller = sellerProfileService.getByUserId(refund.getSellerId());
                    if (seller != null) {
                        sellerAddress.put("recipientName", seller.get("shopName") != null ? seller.get("shopName") : "商家店铺");
                        sellerAddress.put("recipientPhone", seller.get("phone"));
                        sellerAddress.put("province", seller.get("province"));
                        sellerAddress.put("city", seller.get("city"));
                        sellerAddress.put("detailAddress", seller.get("address"));
                    }
                } catch (Exception e) {
                    log.warn("获取商家信息失败: {}", e.getMessage());
                }
            }

            // 查询用户地址列表（上门取件时需要）
            List<Map<String, Object>> userAddresses = new ArrayList<>();
            try {
                userAddresses = addressService.getByUserId(refund.getUserId());
            } catch (Exception e) {
                log.warn("获取用户地址列表失败: {}", e.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("returnMethod", refund.getReturnMethod());
            result.put("sellerAddress", sellerAddress);
            result.put("userAddresses", userAddresses);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));

        } catch (Exception e) {
            log.error("获取退货信息失败", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "获取失败"
            ));
        }
    }

    /**
     * 取消取件预约
     * POST /api/v1/refunds/cancel-pickup
     */
    @PostMapping("/refunds/cancel-pickup")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> cancelPickup(
            Authentication authentication,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            
            String refundIdStr = (String) requestBody.get("refundId");
            String logisticCode = (String) requestBody.get("logisticCode");
            String shipperCode = (String) requestBody.get("shipperCode");
            String orderCode = (String) requestBody.get("orderCode");

            if (refundIdStr == null || refundIdStr.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "退款ID不能为空"
                ));
            }
            if (logisticCode == null || logisticCode.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "物流单号不能为空"
                ));
            }

            Long refundId = Long.parseLong(refundIdStr);
            
            Map<String, Object> result = orderRefundService.cancelPickupOrder(refundId, userId, logisticCode, shipperCode, orderCode);

            if ("success".equals(result.get("status"))) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "取消预约成功"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", result.get("message")
                ));
            }

        } catch (RuntimeException e) {
            log.warn("取消取件预约失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("取消取件预约系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 用户提交退货信息
     * POST /api/v1/refunds/{refundId}/return-submit
     */
    @PostMapping("/refunds/{refundId}/return-submit")
    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> submitReturn(
            Authentication authentication,
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            String returnMethod = (String) requestBody.get("returnMethod");
            Long addressId = requestBody.get("addressId") != null ?
                    ((Number) requestBody.get("addressId")).longValue() : null;
            String pickupDate = (String) requestBody.get("pickupDate");
            String pickupTime = (String) requestBody.get("pickupTime");
            String returnLogisticsName = (String) requestBody.get("returnLogisticsName");
            String returnTrackingNumber = (String) requestBody.get("returnTrackingNumber");

            if (returnMethod == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "请选择退货方式"
                ));
            }

            if ("SELF".equals(returnMethod)) {
                // 自行寄回：验证物流公司和单号
                if (returnLogisticsName == null || returnLogisticsName.trim().isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "请选择物流公司"
                    ));
                }
                if (returnTrackingNumber == null || returnTrackingNumber.trim().isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "请填写物流单号"
                    ));
                }
            } else if ("PICKUP".equals(returnMethod)) {
                // 上门取件：验证地址和取件时间
                if (addressId == null) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "请选择取件地址"
                    ));
                }
                if (pickupDate == null || pickupDate.trim().isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "请选择取件日期"
                    ));
                }
                if (pickupTime == null || pickupTime.trim().isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "请选择取件时间段"
                    ));
                }
            }

            Map<String, Object> result = orderRefundService.submitReturn(
                    refundId, userId, returnMethod,
                    addressId, pickupDate, pickupTime,
                    returnLogisticsName, returnTrackingNumber);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退货信息已提交",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("提交退货信息失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("提交退货信息系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 商家确认收货
     * POST /api/v1/seller/refunds/{refundId}/confirm-receive
     */
    @PostMapping("/seller/refunds/{refundId}/confirm-receive")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER')")
    public ResponseEntity<?> confirmReturn(
            Authentication authentication,
            @PathVariable Long refundId) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);

            Map<String, Object> result = orderRefundService.confirmReturn(refundId, sellerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "确认收货成功",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("确认收货失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("确认收货系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }

    /**
     * 商家查询退货物流信息
     * GET /api/v1/seller/refunds/{refundId}/logistics
     */
    @GetMapping("/seller/refunds/{refundId}/logistics")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER')")
    public ResponseEntity<?> queryReturnLogistics(
            Authentication authentication,
            @PathVariable Long refundId) {
        try {
            Long sellerId = userService.getCurrentUserId(authentication);

            Map<String, Object> result = orderRefundService.queryReturnLogistics(refundId, sellerId);

            if ("error".equals(result.get("status"))) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", result.get("message")
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "查询成功",
                    "data", result
            ));

        } catch (RuntimeException e) {
            log.warn("查询物流失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("查询物流系统异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "系统错误，请稍后重试"
            ));
        }
    }
}
