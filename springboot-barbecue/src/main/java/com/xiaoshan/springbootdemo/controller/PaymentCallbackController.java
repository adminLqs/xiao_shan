package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.service.PaymentCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调统一控制器
 * 处理来自支付宝、微信等支付渠道的异步通知
 *
 * @author xiaoshan
 * @date 2026-05-06
 */
@Slf4j
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentCallbackService paymentCallbackService;

    /**
     * 统一支付回调接口
     * <p>
     * 支付宝、微信等支付渠道的异步通知都会发送到此接口。
     * 通过请求参数和请求头自动识别支付渠道。
     * <p>
     * 注意：
     * - 支付宝：POST form表单格式，参数在 request.getParameterMap() 中
     * - 微信：POST application/xml 格式或 application/json，参数在请求体中
     * - 必须返回 specific 字符串（如 "success"）表示接收成功，否则支付平台会重复通知
     *
     * @param request HTTP请求对象
     * @return 处理结果，根据支付渠道返回对应的成功标识
     */
    @PostMapping("/payment/callback")
    public String callbackPayment(HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("收到支付回调请求，来源IP: {}, 请求方式: {}",
                getClientIp(request), request.getMethod());

        try {
            // 识别支付渠道
            String channel = identifyPaymentChannel(request);
            log.info("识别支付渠道: {}", channel);

            // 根据渠道获取参数
            Map<String, String> params = extractParams(request, channel);

            // 打印回调参数（调试用，生产环境请脱敏）
            log.info("【{}】回调参数: {}", channel, sanitizeParams(params, channel));

            // 调用Service层处理业务逻辑
            boolean success = paymentCallbackService.handleCallback(channel, params);

            // 根据渠道返回对应的成功标识
            String response = getSuccessResponse(channel);
            long duration = System.currentTimeMillis() - startTime;
            log.info("【{}】回调处理完成，耗时: {}ms，结果: {}", channel, duration, success ? "成功" : "失败");

            return success ? response : "fail";

        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            return "fail";
        }
    }

    /**
     * 识别支付渠道
     * 通过请求参数、请求头、请求体特征来判断是哪个支付平台的回调
     *
     * @param request HTTP请求
     * @return 渠道标识：alipay / wechat
     */
    private String identifyPaymentChannel(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();

        // 判断是否为支付宝（沙箱或正式）
        if (paramMap.containsKey("app_id") && paramMap.containsKey("trade_status")) {
            String appId = getFirstParamValue(paramMap, "app_id");

            // 检查是否为沙箱环境
            if (isAlipaySandbox(appId)) {
                return "alipay_sandbox";
            }
            return "alipay";
        }

        // 判断是否为微信支付
        // 微信支付回调特征：包含 out_trade_no、transaction_id、sign 等字段
        if (paramMap.containsKey("out_trade_no") || paramMap.containsKey("transaction_id")) {
            // 检查是否有微信特有的 sign_type（MD5/HMAC-SHA256）
            if (paramMap.containsKey("sign") &&
                    (paramMap.containsKey("sign_type") || paramMap.containsKey("mch_id"))) {
                return "wechat";
            }
        }

        // 检查请求体内容（微信XML格式）
        try {
            String body = getRequestBody(request);
            if (body != null && (body.contains("<xml>") || body.contains("</xml>"))) {
                if (body.contains("out_trade_no") && body.contains("transaction_id")) {
                    return "wechat";
                }
            }
        } catch (Exception e) {
            log.warn("读取请求体失败", e);
        }

        // 默认按支付宝处理
        log.warn("无法识别支付渠道，将按支付宝处理");
        return "alipay";
    }

    /**
     * 判断是否为支付宝沙箱环境
     * 沙箱APPID通常以902100开头（根据你配置的沙箱APPID特征）
     *
     * @param appId 支付宝APPID
     * @return true:沙箱环境, false:正式环境
     */
    private boolean isAlipaySandbox(String appId) {
        // 沙箱APPID通常以特定前缀开头，如 902100（沙箱专用）
        // 正式环境APPID通常是 2014/2015/2016 开头
        if (appId == null) {
            return false;
        }
        // 你的沙箱APPID: 9021000162629440
        return appId.startsWith("902100") || appId.equals("9021000162629440");
    }

    /**
     * 根据支付渠道提取参数
     *
     * @param request HTTP请求
     * @param channel 支付渠道
     * @return 参数Map
     */
    private Map<String, String> extractParams(HttpServletRequest request, String channel) {
        Map<String, String> params = new HashMap<>();

        if ("wechat".equals(channel)) {
            // 微信支付：尝试从请求体XML中解析
            String body = getRequestBody(request);
            if (body != null && body.contains("<xml>")) {
                params = parseXmlToMap(body);
            } else {
                // 降级：从参数中获取
                Map<String, String[]> paramMap = request.getParameterMap();
                for (String key : paramMap.keySet()) {
                    params.put(key, getFirstParamValue(paramMap, key));
                }
            }
        } else {
            // 支付宝：从请求参数中获取
            Map<String, String[]> paramMap = request.getParameterMap();
            for (String key : paramMap.keySet()) {
                params.put(key, getFirstParamValue(paramMap, key));
            }
        }

        return params;
    }

    /**
     * 根据渠道返回对应的成功响应字符串
     *
     * @param channel 支付渠道
     * @return 成功响应字符串
     */
    private String getSuccessResponse(String channel) {
        if ("wechat".equals(channel)) {
            // 微信支付要求返回 "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>"
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>";
        }
        // 支付宝要求返回 "success"
        return "success";
    }

    /**
     * 获取请求参数的第一个值
     */
    private String getFirstParamValue(Map<String, String[]> paramMap, String key) {
        String[] values = paramMap.get(key);
        return (values != null && values.length > 0) ? values[0] : null;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取请求体内容
     */
    private String getRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("读取请求体失败", e);
            return null;
        }
    }

    /**
     * 简单解析XML为Map（微信支付回调使用）
     * 生产环境建议使用 XStream 或 JAXB
     */
    private Map<String, String> parseXmlToMap(String xml) {
        Map<String, String> map = new HashMap<>();
        try {
            String[] pairs = xml.split("<");
            for (String pair : pairs) {
                if (pair.contains(">")) {
                    String key = pair.substring(0, pair.indexOf(">"));
                    String value = pair.substring(pair.indexOf(">") + 1);
                    if (value.contains("<")) {
                        value = value.substring(0, value.indexOf("<"));
                    }
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            log.warn("解析XML失败", e);
        }
        return map;
    }

    /**
     * 脱敏参数，避免日志泄露敏感信息
     */
    private Map<String, String> sanitizeParams(Map<String, String> params, String channel) {
        Map<String, String> sanitized = new HashMap<>(params);

        // 脱敏手机号
        if (sanitized.containsKey("buyer_logon_id")) {
            String phone = sanitized.get("buyer_logon_id");
            if (phone != null && phone.length() >= 7) {
                sanitized.put("buyer_logon_id", phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
            }
        }

        // 脱敏签名（太长）
        if (sanitized.containsKey("sign")) {
            sanitized.put("sign", sanitized.get("sign").substring(0, Math.min(20, sanitized.get("sign").length())) + "...");
        }

        return sanitized;
    }
}