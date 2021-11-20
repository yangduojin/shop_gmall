package com.atguigu.feign;

import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "shop-seckill")
public interface SecKillFeignClient {
    //1.秒杀商品列表显示
    @GetMapping("/seckill/queryAllSecKillProduct")
    public RetVal queryAllSecKillProduct();
    //2.秒杀商品的详情页编写
    @GetMapping("/seckill//getSecKillProductBySkuId/{skuId}")
    public RetVal getSecKillProductBySkuId(@PathVariable Long skuId);

    //6.秒杀确认订单数据信息
    @GetMapping("/seckill/seckillConfirm")
    public RetVal seckillConfirm();
}
