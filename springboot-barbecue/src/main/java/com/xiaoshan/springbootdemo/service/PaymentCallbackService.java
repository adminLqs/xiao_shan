package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付回调统一处理服务
 * 职责：识别支付渠道，路由到对应的支付服务处理
 *
 * 不包含具体的支付逻辑，只做路由和协调
 *
 * @author xiaoshan
 * @date 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private final AlipayService alipayService;
    private final OrderMapper orderMapper;

    /**
     * 统一处理支付回调
     *
     * @param channel 支付渠道：alipay / alipay_sandbox / wechat
     * @param params  回调参数
     * @return 处理是否成功
     */
    public boolean handleCallback(String channel, Map<String, String> params) {
        log.info("开始处理【{}】回调", channel);

        switch (channel) {
            case "alipay":
            case "alipay_sandbox":
                // 调用支付宝服务处理回调
                AlipayService.CallbackResult result = alipayService.handleCallback(params);

                if (result.isSuccess()) {
                    log.info("【{}】回调处理成功 - 订单号: {}, 消息: {}",
                            channel, result.getOrderNumber(), result.getMessage());
                } else {
                    log.error("【{}】回调处理失败 - 消息: {}", channel, result.getMessage());
                }
                return result.isSuccess();

            case "wechat":
                // TODO: 微信支付回调处理
                // return wechatPayService.handleCallback(params);
                log.warn("微信支付回调暂未实现");
                return false;

            default:
                log.error("不支持的支付渠道: {}", channel);
                return false;
        }
    }
}