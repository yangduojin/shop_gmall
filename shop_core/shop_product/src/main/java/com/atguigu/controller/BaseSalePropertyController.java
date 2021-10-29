package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.mapper.BaseBrandMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 基本销售属性表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@RestController
@RequestMapping("/product")
public class BaseSalePropertyController {

    @Autowired
    BaseSalePropertyService baseSalePropertyService;

    @GetMapping("/queryAllSaleProperty")
    public RetVal queryAllSaleProperty(){
        List<BaseSaleProperty> baseSalePropertyList = baseSalePropertyService.queryAllSaleProperty();
        return RetVal.ok(baseSalePropertyList);
    }
}

