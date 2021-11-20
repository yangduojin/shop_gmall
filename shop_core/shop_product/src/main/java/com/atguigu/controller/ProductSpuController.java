package com.atguigu.controller;


import com.atguigu.constant.MqConst;
import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSpu;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSpuService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    ProductImageService productImageService;

    @Autowired
    ProductSalePropertyKeyService productSalePropertyKeyService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SearchFeignClient searchFeignClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

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



    // queryProductImageByProductId/12
    @GetMapping("/queryProductImageByProductId/{productId}")
    public RetVal queryProductImageByProductId(@PathVariable Long productId){
        List<ProductImage> imageList = productImageService.queryProductImageByProductId(productId);
        return RetVal.ok(imageList);
    }

    //querySalePropertyByProductId/12
    @GetMapping("/querySalePropertyByProductId/{productId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long productId){
        List<ProductSalePropertyKey> salePropertyKeyList = productSalePropertyKeyService.querySalePropertyByProductId(productId);
        return RetVal.ok(salePropertyKeyList);
    }

    //http://127.0.0.1/product/saveSkuInfo
    @PostMapping("/saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo){
        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }

    // http://127.0.0.1/    product/querySkuInfoByPage/1/10

    @GetMapping("/querySkuInfoByPage/{pageNum}/{pageSize}")
    public RetVal querySkuInfoByPage(@PathVariable Long pageNum,
                                        @PathVariable Long pageSize){
        IPage<SkuInfo> skuInfoIPage = skuInfoService.querySkuInfoByPage(pageNum,pageSize);
        return RetVal.ok(skuInfoIPage);
    }

    //http://127.0.0.1/product/onSale/38 上架
    @GetMapping("/onSale/{skuInfoId}")
    public RetVal onSale(@PathVariable Long skuInfoId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuInfoId);
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);
//        searchFeignClient.onsale(skuInfoId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.ON_SALE_ROUTING_KEY,skuInfoId);
        return RetVal.ok();
    }

    //http://127.0.0.1/product/offSale/37 下架
    @GetMapping("/offSale/{skuInfoId}")
    public RetVal offSale(@PathVariable Long skuInfoId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuInfoId);
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);
//        searchFeignClient.offsale(skuInfoId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.OFF_SALE_ROUTING_KEY,skuInfoId);
        return RetVal.ok();
    }

}

