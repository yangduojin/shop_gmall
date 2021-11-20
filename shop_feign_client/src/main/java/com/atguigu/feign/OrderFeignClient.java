package com.atguigu.feign;

import com.atguigu.entity.OrderInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("shop-order")
public interface OrderFeignClient {

    @GetMapping("/order/confirm")
    public RetVal<Map<String, Object>> confirm();

    //3.封装保存订单信息为接口
    @PostMapping("/order/saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo);

    @GetMapping("order/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId);

}
