package com.atguigu.feign;

import com.atguigu.entity.PaymentInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "shop-payment")
public interface PaymentFeignClient {

    //1.关闭交易接口
    @GetMapping("/payment/closeAlipayTrade/{orderId}")
    public boolean closeAlipayTrade(@PathVariable Long orderId);

    //2.查询阿里内部交易信息
    @GetMapping("/payment/queryAlipayTrade/{orderId}")
    public boolean queryAlipayTrade(@PathVariable Long orderId);

    //3.查询paymentInfo数据接口
    @GetMapping("/payment/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);

}
