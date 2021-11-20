package com.atguigu.feign;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient("shop-cart")
public interface CartFeignClient {

    @PostMapping("cart/addToCart/{skuId}/{skuNum}")
    public RetVal addToCart(@RequestParam Long skuId, @RequestParam Integer skuNum);

    @GetMapping("/cart/getSelectedProduct/{userId}")
    public List<CartInfo> getSelectedProduct(@PathVariable String userId);

    @PostMapping("/cart/getSkusPrice/{userId}")
    public List<Long> getSkusPrice(@RequestBody Map<Long, BigDecimal> map, @PathVariable Long userId);

    //2.从数据库中查询出最新的购物车信息到redis中
    @GetMapping("/cart/queryFromDbToRedis/{userId}")
    public List<CartInfo> queryFromDbToRedis(@PathVariable String userId);
}
