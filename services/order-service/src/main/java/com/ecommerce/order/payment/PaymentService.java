package com.ecommerce.order.payment;

import com.ecommerce.core.model.PageResult;
import com.ecommerce.order.payment.model.entity.Refund;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    /** 创建支付单并获取支付链接，返回 {paymentNo, payUrl} */
    Map<String, String> createPayment(Long userId, Long orderId, String orderNo, BigDecimal amount, String channel);

    void handleCallback(String channel, String paymentNo, String transactionNo);

    /** 模拟支付成功（用于前端测试） */
    void simulatePayment(String orderNo);

    // ==================== 退款 ====================

    /**
     * 用户提交退款申请（仅创建退款记录，不实际退款）
     * <p>
     * 将订单状态改为"退款中"、支付状态改为"退款中"，
     * 生成退款单，等待管理员审核。
     */
    void requestRefund(Long userId, Long orderId, BigDecimal amount, String reason);

    /**
     * 用户提交退款/退货退款申请（完整参数版）
     *
     * @param refundType   退款类型: 1=仅退款, 2=退货退款
     * @param refundReason 退款原因分类: not_received/wrong_item/quality/dont_like/other
     * @param refundImages 退款凭证图片(JSON数组)
     * @return 退款单ID
     */
    Long requestRefund(Long userId, Long orderId, BigDecimal amount, String reason,
                       Integer refundType, String refundReason, String refundImages);

    /**
     * 管理员审核通过，执行实际退款
     * <p>
     * 调用支付渠道退款 → 更新退款单/支付单/订单状态 → 发送 MQ 事件
     */
    void processRefund(Long refundId);

    /**
     * 管理员拒绝退款，回退订单和支付状态
     */
    void declineRefund(Long refundId);

    /** 填写退货物流单号 */
    void submitReturnLogistics(Long refundId, String logisticsNo);

    /** 管理员确认收到退货 */
    void confirmReturnReceived(Long refundId);

    /** 查询订单退款信息 */
    Map<String, Object> getRefundInfo(Long orderId);

    /** 退款分页列表（管理端） */
    PageResult<Refund> refundPage(Integer page, Integer size, Integer status);

    Integer getStatus(String orderNo);
}