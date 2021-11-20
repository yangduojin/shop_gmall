package com.atguigu.feign;

import com.atguigu.entity.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("shop-user")
public interface UserFeignClient {
    @GetMapping("user/queryAddressByUserId/{userId}")
    public List<UserAddress> queryAddressByUserId(@PathVariable(value = "userId") String userId);
}
