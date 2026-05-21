package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Order;
import com.xiaoshan.springbootdemo.entity.RefundRecord;
import com.xiaoshan.springbootdemo.entity.dto.RefundRequestDTO;
import com.xiaoshan.springbootdemo.mapper.OrderMapper;
import com.xiaoshan.springbootdemo.mapper.RefundRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 退款处理服务
 *
 * @author xiaoshan
 * @date 2026-05-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRecordMapper refundRecordMapper;
    private final OrderMapper orderMapper;
    private final AlipayService alipayService;

    // ==================== 查询方法 ====================

    /**
     * 根据订单号查询退款记录
     *
     * @param orderNumber 订单号
     * @return 退款记录列表
     */
    public List<RefundRecord> findByOrderNumber(String orderNumber) {
        return refundRecordMapper.findByOrderNumber(orderNumber);
    }

    /**
     * 批量查询退款记录并转为 Map
     *
     * @param orderNumbers 订单号列表
     * @return Map<订单号, 退款记录>
     */
    public Map<String, RefundRecord> findRefundMapByOrderNumbers(List<String> orderNumbers) {
        return refundRecordMapper.findByOrderNumbers(orderNumbers)
                .stream()
                .collect(Collectors.toMap(
                        RefundRecord::getOrderNumber,
                        r -> r,
                        (a, b) -> a
                ));
    }

    /**
     * 获取订单已退款总额
     *
     * @param orderNumber 订单号
     * @return 已退款总额
     */
    public BigDecimal getTotalRefundedAmount(String orderNumber) {
        List<RefundRecord> records = refundRecordMapper.findByOrderNumber(orderNumber);
        return records.stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()))
                .map(RefundRecord::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== 创建退款记录 ====================

    /**
     * 创建退款记录
     *
     * @param request 退款请求DTO，包含订单号、退款金额、退款原因
     * @return 退款记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public RefundRecord processRefundRequest(RefundRequestDTO request) {
        // 提取请求参数
        String orderNumber = request.getOrderNumber();
        BigDecimal refundAmount = request.getRefundAmount();
        String refundReason = request.getRefundReason();

        // 参数业务校验
        validateRefundParams(refundAmount, refundReason);

        // 查询并校验订单
        Order order = validateAndGetOrder(orderNumber);

        // 校验退款金额是否合理
        validateRefundAmount(order, refundAmount);

        // 更新订单状态为退款中
        orderMapper.updateToRefunding(orderNumber);

        // 创建退款记录
        RefundRecord refund = createRefundRecord(orderNumber, refundAmount, refundReason);

        log.info("退款申请处理完成 - 订单号: {}, 退款金额: {}",
                orderNumber, refundAmount);

        return refund;
    }

    /**
     * 校验退款参数
     * <p>
     * 对退款金额和退款原因进行业务层面的校验。
     * 注意：基础的非空校验已在DTO的注解中完成，此处进行业务规则校验。
     *
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @throws IllegalArgumentException 当参数不符合业务规则时抛出
     */
    private void validateRefundParams(BigDecimal refundAmount, String refundReason) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额必须大于0");
        }

        if (refundReason == null || refundReason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写退款原因");
        }
    }

    /**
     * 校验并获取订单
     * <p>
     * 根据订单号查询订单，并校验订单是否存在以及状态是否为"已支付"。
     * 只有状态为PAID的订单才允许申请退款。
     *
     * @param orderNumber 订单号
     * @return 校验通过的订单对象
     * @throws IllegalArgumentException 当订单不存在时抛出
     * @throws IllegalStateException    当订单状态不允许退款时抛出
     */
    private Order validateAndGetOrder(String orderNumber) {
        // 查询订单
        Order order = orderMapper.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在，订单号: " + orderNumber);
        }

        // 检查订单状态（只有已支付的订单才能退款）
        if (!"PAID".equals(order.getStatus())) {
            throw new IllegalStateException(
                    "订单状态异常，无法退款。当前状态：" + order.getStatus()
            );
        }

        return order;
    }

    /**
     * 校验退款金额是否合理
     * <p>
     * 检查退款金额是否超过订单可退金额。
     * 可退金额 = 订单总金额 - 已退款总额
     *
     * @param order        订单对象
     * @param refundAmount 申请退款金额
     * @throws IllegalArgumentException 当退款金额超过可退金额时抛出
     */
    private void validateRefundAmount(Order order, BigDecimal refundAmount) {
        // 查询已退款总额
        BigDecimal alreadyRefunded = getTotalRefundedAmount(order.getOrderNumber());

        // 计算最大可退款金额
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal maxRefundable = totalAmount.subtract(alreadyRefunded);

        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new IllegalArgumentException(
                    String.format("退款金额不能超过可退金额。已退款: %s, 可退最大: %s",
                            alreadyRefunded, maxRefundable)
            );
        }
    }

    /**
     * 创建退款记录
     * <p>
     * 将退款信息持久化到数据库，创建一条待处理的退款记录。
     * 该方法在事务中执行，确保数据一致性。
     *
     * @param orderNumber  订单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 退款记录ID
     */
    @Transactional
    public RefundRecord createRefundRecord(String orderNumber, BigDecimal refundAmount, String refundReason) {
        RefundRecord refundRecord = new RefundRecord(orderNumber, refundAmount, refundReason);
        refundRecordMapper.insert(refundRecord);

        return refundRecord;
    }



    // ==================== 退款处理 ====================

    /**
     * 异步处理退款
     *
     * @param refundId 退款记录ID
     */
    @Async
    public void processRefundAsync(Long refundId) {
        log.info("开始异步处理退款，退款记录ID: {}", refundId);
        processRefund(refundId);
    }

    /**
     * 处理退款（同步）
     *
     * @param refundId 退款记录ID
     */
    @Transactional
    public void processRefund(Long refundId) {
        RefundRecord refundRecord = refundRecordMapper.findById(refundId);
        if (refundRecord == null) {
            log.error("退款记录不存在，ID: {}", refundId);
            return;
        }

        // 只处理处理中的记录
        if (!"PROCESSING".equals(refundRecord.getStatus())) {
            log.info("退款记录状态不是处理中，无需处理。状态: {}", refundRecord.getStatus());
            return;
        }

        String orderNumber = refundRecord.getOrderNumber();
        BigDecimal refundAmount = refundRecord.getRefundAmount();
        String refundReason = refundRecord.getRefundReason();

        try {
            Order order = orderMapper.findByOrderNumber(orderNumber);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            // 再次验证订单状态
            if (!"PAID".equals(order.getStatus())) {
                String failReason = "订单状态异常，无法退款。当前状态：" + order.getStatus();
                refundRecordMapper.updateToFailed(refundId, failReason, LocalDateTime.now());
                log.error("退款处理失败 - 订单号: {}, 原因: {}", orderNumber, failReason);
                return;
            }

            // 验证退款金额
            BigDecimal alreadyRefunded = getTotalRefundedAmount(orderNumber);
            if (alreadyRefunded.add(refundAmount).compareTo(order.getTotalAmount()) > 0) {
                String failReason = String.format("退款金额超限。已退款：%s，申请退款：%s，订单总额：%s",
                        alreadyRefunded, refundAmount, order.getTotalAmount());
                refundRecordMapper.updateToFailed(refundId, failReason, LocalDateTime.now());
                log.error("退款处理失败 - 订单号: {}, 原因: {}", orderNumber, failReason);
                return;
            }

            // 根据支付方式调用不同的退款接口
            boolean success = false;
            String refundTransactionId = null;
            String failReason = null;

            if ("ALIPAY".equals(order.getPaymentMethod())) {
                try {
                    success = alipayService.refund(orderNumber, refundAmount, refundReason);
                    refundTransactionId = generateRefundTransactionId(orderNumber);
                    if (!success) {
                        failReason = "支付宝退款接口返回失败";
                    }
                } catch (Exception e) {
                    failReason = "支付宝退款异常: " + e.getMessage();
                    log.error(failReason, e);
                }
            } else if ("WECHAT".equals(order.getPaymentMethod())) {
                failReason = "微信退款暂未实现";
                log.warn(failReason);
            } else {
                failReason = "不支持的支付方式: " + order.getPaymentMethod();
            }

            if (success) {
                // 更新退款记录为成功
                refundRecordMapper.updateToSuccess(refundId, refundTransactionId, LocalDateTime.now());

                // 更新订单状态为已退款
                orderMapper.updateToRefunded(orderNumber);

                log.info("退款处理成功 - 订单号: {}, 退款金额: {}, 退款交易号: {}",
                        orderNumber, refundAmount, refundTransactionId);
            } else {
                if (failReason == null) {
                    failReason = "退款失败，原因未知";
                }
                refundRecordMapper.updateToFailed(refundId, failReason, LocalDateTime.now());
                log.error("退款处理失败 - 订单号: {}, 退款金额: {}, 失败原因: {}",
                        orderNumber, refundAmount, failReason);
            }

        } catch (Exception e) {
            log.error("退款处理异常", e);
            refundRecordMapper.updateToFailed(refundId, "系统异常: " + e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * 商家处理退款结果
     *
     * @param orderNumber 订单号
     * @param result      处理结果 SUCCESS/FAIL
     * @param failReason  失败原因
     */
    @Transactional
    public void handleRefundResult(String orderNumber, String result, String failReason) {
        // 查退款记录
        List<RefundRecord> records = refundRecordMapper.findByOrderNumber(orderNumber);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("退款记录不存在");
        }
        RefundRecord record = records.get(0);

        if (!"PROCESSING".equals(record.getStatus())) {
            throw new IllegalStateException("退款已处理过");
        }

        if ("SUCCESS".equals(result)) {
            // 退款成功
            refundRecordMapper.updateToSuccess(record.getId(), record.getRefundTransactionId(), LocalDateTime.now());
            orderMapper.updateToRefunded(orderNumber);
        } else {
            // 退款失败
            if (failReason == null || failReason.trim().isEmpty()) {
                throw new IllegalArgumentException("请填写失败原因");
            }
            refundRecordMapper.updateToFailed(record.getId(), failReason, LocalDateTime.now());
            orderMapper.updateToRefundFailed(orderNumber);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 生成退款交易号
     */
    private String generateRefundTransactionId(String orderNumber) {
        return "REF_" + orderNumber + "_" + System.currentTimeMillis();
    }
}