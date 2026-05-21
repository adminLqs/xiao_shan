package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器（独立）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final AlipayService alipayService;

    /**
     * 统一支付回调入口
     */
    @PostMapping("/payment/callback")
    public String alipayCallback(HttpServletRequest request) {
        try {

            // 打印请求信息
            log.info("========== 收到回调请求 ==========");
            log.info("请求方法: {}", request.getMethod());
            log.info("Content-Type: {}", request.getContentType());
            log.info("请求URL: {}", request.getRequestURL());

            // 打印所有请求头
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                log.info("Header - {}: {}", headerName, request.getHeader(headerName));
            });

            // 获取所有回调参数
            Map<String, String> params = getParams(request);
            log.info("收到支付宝回调，参数: {}", params);

            // 处理回调
            boolean success = alipayService.handleAlipayCallback(params);

            // 必须返回 "success"，支付宝才会停止回调
            return success ? "success" : "failure";

        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "failure";
        }
    }

    private Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            params.put(name, requestParams.get(name)[0]);
        }
        return params;
    }
}