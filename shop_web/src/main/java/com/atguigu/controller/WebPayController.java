package com.atguigu.controller;

import com.atguigu.entity.OrderInfo;
import com.atguigu.feign.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebPayController {

    @Autowired
    private OrderFeignClient orderFeignClient;


    @GetMapping("pay.html")
    public String payment(@RequestParam Long orderId, HttpServletRequest request) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        request.setAttribute("orderInfo", orderInfo);
        return "payment/pay";
    }

    // 同步给用户展示支付成功页面
    @GetMapping("alipay/success.html")
    public String success(){
        return "payment/success";
    }


}
