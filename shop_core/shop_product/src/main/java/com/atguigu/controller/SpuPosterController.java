package com.atguigu.controller;


import com.atguigu.service.SpuPosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 商品海报表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@RestController
@RequestMapping("/product/spu-poster")
public class SpuPosterController {

    @Autowired
    SpuPosterService spuPosterService;

    @GetMapping("/testRedis")
    public String testRedis(){
        spuPosterService.testRedis();
        return "success";
    }

}

