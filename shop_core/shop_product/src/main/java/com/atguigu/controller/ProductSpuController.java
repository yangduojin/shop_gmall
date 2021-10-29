package com.atguigu.controller;


import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@RestController
@RequestMapping("/product")
public class ProductSpuController {

    @Autowired
    ProductSpuService productSpuService;

    @GetMapping("/queryProductSpuByPage/{pageNum}/{pageSize}/{category3Id}")
    public RetVal queryProductSpuByPage(@PathVariable Long pageNum,
                                        @PathVariable Long pageSize,
                                        @PathVariable Long category3Id){
        IPage<ProductSpu> productSpuList = productSpuService.queryProductSpuByPage(pageNum,pageSize,category3Id);

        return RetVal.ok(productSpuList);

    }

    @PostMapping("/saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu){

        productSpuService.saveProductSpu(productSpu);


        return RetVal.ok();
    }
}

