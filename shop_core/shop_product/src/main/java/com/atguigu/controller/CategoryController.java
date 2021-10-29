package com.atguigu.controller;


import com.atguigu.entity.BaseCategory1;
import com.atguigu.entity.BaseCategory2;
import com.atguigu.entity.BaseCategory3;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseCategory1Service;
import com.atguigu.service.BaseCategory2Service;
import com.atguigu.service.BaseCategory3Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-10-27
 */
@RestController
@RequestMapping("/product")
public class CategoryController {

    @Autowired
    private BaseCategory1Service baseCategory1Service;

    @Autowired
    private BaseCategory2Service baseCategory2Service;

    @Autowired
    private BaseCategory3Service baseCategory3Service;

    @GetMapping("/getCategory1")
    public RetVal getCategory1(){
        List<BaseCategory1> category1List = baseCategory1Service.list(null);
        return RetVal.ok(category1List);
    }

    @GetMapping("/getCategory2/{Category1Id}")
    public RetVal getCategory2(@PathVariable Long Category1Id){
        QueryWrapper<BaseCategory2> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category1_id",Category1Id);
        List<BaseCategory2> category2List = baseCategory2Service.list(queryWrapper);
        return RetVal.ok(category2List);
    }

    @GetMapping("/getCategory3/{Category2Id}")
    public RetVal getCategory3(@PathVariable Long Category2Id){
        QueryWrapper<BaseCategory3> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category2_id",Category2Id);
        List<BaseCategory3> category3List = baseCategory3Service.list(queryWrapper);
        return RetVal.ok(category3List);
    }


}

