package com.atguigu.controller;


import com.atguigu.entity.UserAddress;
import com.atguigu.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户地址表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-11-08
 */
@RestController
@RequestMapping("/user")
public class UserAddressController {

    @Autowired
    UserAddressService userAddressService;

    @GetMapping("queryAddressByUserId/{userId}")
    public List<UserAddress> queryAddressByUserId(@PathVariable(value = "userId") String userId){
        return userAddressService.list(new QueryWrapper<UserAddress>().eq("user_id",userId));

    }


}

