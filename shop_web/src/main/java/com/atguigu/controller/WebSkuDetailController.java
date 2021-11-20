package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.exector.MyExecutor;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.SearchFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
public class WebSkuDetailController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    //编写访问的控制器！
    @RequestMapping("{skuId}.html")
    public String getSkuDetail(@PathVariable Long skuId, Model model){
        Map<String, Object> map = new HashMap<>();
        CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        }, MyExecutor.getInstance());
//         两种线程池都一样
        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        },MyExecutor.getInstance());

        CompletableFuture<Void> skuInfoCompletableFuture = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView",categoryView);
        }, threadPoolExecutor);

        CompletableFuture<Void> spuSalePropertyListFuture = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            List<ProductSalePropertyKey> skuSalePropertyKeyAndValueList = productFeignClient.getSkuSalePropertyKeyAndValue(skuInfo.getProductId(), skuId);
            map.put("spuSalePropertyList", skuSalePropertyKeyAndValueList);
        }, threadPoolExecutor);

        CompletableFuture<Void> salePropertyValueIdJson = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            Map salePropertyValueIdMap = productFeignClient.getSkuSalePropertyValueId(skuInfo.getProductId());
            map.put("salePropertyValueIdJson", JSON.toJSONString(salePropertyValueIdMap));
        }, threadPoolExecutor);

//        CompletableFuture<Void> hotScoreFuture = CompletableFuture.runAsync(() -> {
//            searchFeignClient.incrHotScore(skuId);
//        }, MyExecutor.getInstance());

        CompletableFuture.allOf(priceFuture,skuInfoFuture,spuSalePropertyListFuture,skuInfoCompletableFuture,salePropertyValueIdJson
//                ,hotScoreFuture
        ).join();

        model.addAllAttributes(map);

        return "detail/index";
    }
}