package com.xiaoshan.springbootdemo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.vo.LogisticsVO;
import com.xiaoshan.springbootdemo.service.LogisticsService;
import com.xiaoshan.springbootdemo.service.OrderService;
import com.xiaoshan.springbootdemo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LogisticsController {

    private final UserService userService;
    private final OrderService orderService;
    private final LogisticsService logisticsService;
    private final ObjectMapper objectMapper;

    // 用户ID（EBusinessID）
    @Value("${kdniao.app.key:}")
    private String appKey;

    // API密钥（AppKey）
    @Value("${kdniao.app.secret:}")
    private String appSecret;

    /**
     * 获取物流信息
     * GET /api/v1/seller/orders/{orderId}/logistics
     */
    @GetMapping("/seller/orders/{orderId}/logistics")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getLogistics(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            // 获取当前商家ID
            Long sellerId = userService.getCurrentUserId(authentication);

            // 查询订单信息（获取物流单号和物流公司代码）
            Order order = orderService.getOrderBySellerId(sellerId, orderId);

            // 订单不存在时返回错误
            if (order == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "订单不存在"
                ));
            }

            // 订单未发货时返回错误
            if (order.getTrackingNumber() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "该订单尚未发货"
                ));
            }

            // 调用物流服务查询物流轨迹
            LogisticsVO logistics = logisticsService.queryLogistics(
                    order.getTrackingNumber(),
                    order.getLogisticsCode(),
                    order.getId()
            );

            // 返回物流信息
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "logistics", logistics,
                            "order", order
                    )
            ));

        } catch (Exception e) {
            log.error("获取物流信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 刷新物流信息
     * POST /api/v1/seller/orders/{orderId}/logistics/refresh
     */
    @PostMapping("/seller/orders/{orderId}/logistics/refresh")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> refreshLogistics(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            // 获取当前商家ID
            Long sellerId = userService.getCurrentUserId(authentication);

            // 查询订单信息（获取物流单号和物流公司代码）
            Order order = orderService.getOrderBySellerId(sellerId, orderId);

            // 订单不存在时返回错误
            if (order == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "订单不存在"
                ));
            }

            // 订单未发货时返回错误
            if (order.getTrackingNumber() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "该订单尚未发货"
                ));
            }

            // 刷新物流信息（调用物流服务）
            LogisticsVO logistics = logisticsService.queryLogistics(
                    order.getTrackingNumber(),
                    order.getLogisticsCode(),
                    orderId
            );

            // 返回物流信息
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", logistics
            ));

        } catch (Exception e) {
            log.error("刷新物流信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 用户端获取物流信息
     * GET /api/v1/orders/{orderId}/logistics
     *
     * 根据订单ID查询物流轨迹（用户端使用）
     * 返回订单信息和物流轨迹
     *
     * @param authentication 认证信息
     * @param orderId 订单ID
     * @return 订单信息和物流信息
     */
    @GetMapping("/orders/{orderId}/logistics")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getUserLogistics(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单信息（校验订单属于当前用户）
            Order order = orderService.getOrderDetail(userId, orderId);

            // 订单不存在时返回错误
            if (order == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "订单不存在"
                ));
            }

            // 订单未发货时返回错误
            if (order.getTrackingNumber() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "该订单尚未发货",
                        "data", Map.of("order", order)
                ));
            }

            // 调用物流服务查询物流轨迹
            LogisticsVO logistics = logisticsService.queryLogistics(
                    order.getTrackingNumber(),
                    order.getLogisticsCode(),
                    orderId
            );

            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("logistics", logistics);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));

        } catch (Exception e) {
            log.error("获取物流信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 用户端刷新物流信息
     * POST /api/v1/orders/{orderId}/logistics/refresh
     *
     * @param authentication 认证信息
     * @param orderId 订单ID
     * @return 最新物流信息
     */
    @PostMapping("/orders/{orderId}/logistics/refresh")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> refreshUserLogistics(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        try {
            // 获取当前登录用户ID
            Long userId = userService.getCurrentUserId(authentication);

            // 查询订单信息（校验订单属于当前用户）
            Order order = orderService.getOrderDetail(userId, orderId);

            // 订单不存在时返回错误
            if (order == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "订单不存在"
                ));
            }

            // 订单未发货时返回错误
            if (order.getTrackingNumber() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "该订单尚未发货"
                ));
            }

            // 刷新物流信息（调用快递鸟API）
            LogisticsVO logistics = logisticsService.queryLogistics(
                    order.getTrackingNumber(),
                    order.getLogisticsCode(),
                    orderId
            );

            // 返回物流信息
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", logistics
            ));

        } catch (Exception e) {
            log.error("刷新物流信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    /**
     * 快递鸟物流状态推送回调接口
     * POST /api/v1/logistics/callback
     */
    @PostMapping("/logistics/callback")
    public Map<String, Object> receivePush(HttpServletRequest request) {
        try {
            // 获取快递鸟推送的请求参数（RequestData：JSON格式的物流数据）
            String requestData = request.getParameter("RequestData");

            // 获取快递鸟推送的签名（用于验证数据真实性）
            String dataSign = request.getParameter("DataSign");

            // 获取快递鸟的用户ID（用于返回响应）
            String eBusinessId = request.getParameter("EBusinessID");

            // 计算本地签名：MD5(RequestData + AppSecret) 然后 Base64编码
            String localSign = encrypt(requestData + appSecret);

            // 验证签名是否一致，防止伪造请求
            if (!localSign.equals(dataSign)) {
                log.warn("签名验证失败");
                return buildCallbackResponse(eBusinessId, false, "签名验证失败");
            }

            // 使用Jackson解析RequestData JSON字符串
            JsonNode rootNode = objectMapper.readTree(requestData);

            // 获取物流数据数组（一次可能推送多个物流单号）
            JsonNode dataArray = rootNode.get("Data");

            // 遍历处理每个物流单号
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode item : dataArray) {
                    // 获取物流单号
                    String logisticCode = item.get("LogisticCode").asText();

                    // 获取快递公司编码（SF、YTO、ZTO等）
                    String shipperCode = item.get("ShipperCode").asText();

                    // 获取物流状态（2-在途中，3-已签收，4-问题件）
                    String state = item.get("State").asText();

                    // 获取轨迹列表
                    JsonNode traces = item.get("Traces");

                    // 记录物流更新日志
                    log.info("物流更新: 单号={}, 公司={}, 状态={}, 轨迹数={}",
                            logisticCode, shipperCode, state, traces != null ? traces.size() : 0);

                    // 如果状态为已签收（3），更新订单送达时间
                    if ("3".equals(state)) {
                        // 遍历轨迹列表，查找签收记录
                        if (traces != null && traces.isArray()) {
                            for (JsonNode trace : traces) {
                                // 获取轨迹描述
                                String acceptStation = trace.get("AcceptStation").asText();

                                // 轨迹描述包含"签收"或"已签收"时，说明包裹已送达
                                if (acceptStation.contains("签收") || acceptStation.contains("已签收")) {
                                    // 获取签收时间
                                    String acceptTime = trace.get("AcceptTime").asText();
                                    log.info("订单已签收: 单号={}, 签收时间={}", logisticCode, acceptTime);

                                    // 调用服务层更新订单送达时间
                                    orderService.updateDeliveredTime(logisticCode, acceptTime);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // 返回快递鸟要求的成功响应格式
            return buildCallbackResponse(eBusinessId, true, "success");

        } catch (Exception e) {
            // 记录错误日志
            log.error("处理物流推送失败: {}", e.getMessage(), e);

            // 返回失败响应
            return buildCallbackResponse(null, false, e.getMessage());
        }
    }



    /**
     * 构建快递鸟回调响应
     *
     * @param eBusinessId 快递鸟用户ID
     * @param success 是否成功
     * @param reason 失败原因
     * @return 响应Map
     */
    private Map<String, Object> buildCallbackResponse(String eBusinessId, boolean success, String reason) {
        Map<String, Object> response = new HashMap<>();
        // 快递鸟用户ID（从请求参数获取，如果没有则使用配置的appKey）
        response.put("EBusinessID", eBusinessId != null ? eBusinessId : appKey);
        // 更新时间（格式：yyyy-MM-dd HH:mm:ss）
        response.put("UpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 处理结果（true-成功，false-失败）
        response.put("Success", success);
        // 失败原因（成功时为空字符串）
        response.put("Reason", reason != null ? reason : "");
        return response;
    }

    /**
     * MD5加密并转为Base64（快递鸟签名规则）
     * 规则：MD5(RequestData + AppKey) 得到32位小写字符串，再对这个字符串做Base64编码
     */
    private String encrypt(String content) throws Exception {
        // MD5加密
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content.getBytes("UTF-8"));

        // 转为十六进制字符串（小写）
        StringBuilder hexSb = new StringBuilder();
        for (byte b : digest) {
            hexSb.append(String.format("%02x", b));
        }
        String md5Hex = hexSb.toString();

        // Base64编码
        return Base64.getEncoder().encodeToString(md5Hex.getBytes(StandardCharsets.UTF_8));
    }
}