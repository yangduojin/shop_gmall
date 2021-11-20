package com.atguigu.service;

import com.atguigu.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
public interface PaymentInfoService extends IService<PaymentInfo> {

    String createQrCode(Long orderId) throws Exception;

    PaymentInfo getPaymentInfo(String outTradeNo);

    void updatePaymentInfo(Map<String, String> aliParamMap);

    boolean refund(Long orderId) throws Exception;

    boolean queryAlipayTrade(Long orderId) throws Exception;

    boolean closeAlipayTrade(Long orderId) throws Exception;
}
