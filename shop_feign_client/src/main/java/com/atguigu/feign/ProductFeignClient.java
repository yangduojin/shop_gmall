package com.atguigu.feign;

import com.atguigu.entity.*;
import com.atguigu.fallback.ProductFallback;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "shop-product",fallback = ProductFallback.class)
public interface ProductFeignClient {
    @GetMapping("/sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable long skuId);

    @GetMapping("/sku/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable long category3Id);

    // 根据skuId 查询商品的价格
    @GetMapping("/sku/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable long skuId);

    // 根据spuId,skuId 查询数据
    @GetMapping("/sku/getSkuSalePropertyKeyAndValue/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(@PathVariable long productId, @PathVariable long skuId);

    // 通过spuId 获取对应的Json 字符串。
    @GetMapping("/sku/getSkuSalePropertyValueId/{productId}")
    public Map getSkuSalePropertyValueId(@PathVariable long productId);

    @GetMapping("/product/getIndexCategoryInfo")
    public RetVal getIndexCategoryInfo();

    @GetMapping("/product/brand/getBrandById/{id}")
    public BaseBrand getBrandById(@PathVariable Long id);

    @GetMapping("/product/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId);
}