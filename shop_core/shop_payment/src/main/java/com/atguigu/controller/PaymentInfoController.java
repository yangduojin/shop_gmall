package com.atguigu.controller;


import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.config.AlipayConfig;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {

    @Autowired
    private PaymentInfoService paymentInfoService;

    //1.创建支付二维码
    @RequestMapping("createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.createQrCode(orderId);
    }
    //2.支付宝发起的异步通知  http://127.0.0.1:8005/payment/async/notify
    @PostMapping("async/notify")
    public String asyncNotify(@RequestParam Map<String,String> aliParamMap) throws Exception {
        //调用支付宝sdk验签
        boolean signVerified = AlipaySignature.rsaCheckV1(aliParamMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        if (signVerified) {
            String tradeStatus = aliParamMap.get("trade_status");
            //交易成功
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                //拿到商户系统的订单号
                String outTradeNo = aliParamMap.get("out_trade_no");
                PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(outTradeNo);
                //如果支付订单的状态是关闭和已支付 就不需要支付宝再调用我们
                String paymentStatus = paymentInfo.getPaymentStatus();
                if (paymentStatus.equals(PaymentStatus.ClOSED) || paymentStatus.equals(PaymentStatus.PAID)) {
                    return "success";
                }
                //修改支付订单信息
                paymentInfoService.updatePaymentInfo(aliParamMap);
            }
            return "failure";
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
    }
    //3.退款接口编写
    @RequestMapping("refund/{orderId}")
    public boolean refund(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.refund(orderId);
    }
    //4.查询支付宝中是否有交易记录
    @RequestMapping("queryAlipayTrade/{orderId}")
    public boolean queryAlipayTrade(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.queryAlipayTrade(orderId);
    }
    //5.关闭交易
    @RequestMapping("closeAlipayTrade/{orderId}")
    public boolean closeAlipayTrade(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.closeAlipayTrade(orderId);
    }

    //6.查询支付订单信息 shop-payment
    @GetMapping("getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        return paymentInfoService.getPaymentInfo(outTradeNo);
    }


}

