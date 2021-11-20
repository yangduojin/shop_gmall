package com.atguigu.fallback;

import com.atguigu.entity.*;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ProductFallback implements ProductFeignClient {
    @Override
    public SkuInfo getSkuInfo(long skuId) {
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(long category3Id) {
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(long skuId) {
        return null;
    }

    @Override
    public List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(long productId, long skuId) {
        return null;
    }

    @Override
    public Map getSkuSalePropertyValueId(long productId) {
        return null;
    }

    @Override
    public RetVal getIndexCategoryInfo() {
        return null;
    }

    @Override
    public BaseBrand getBrandById(Long id) {
        return null;
    }

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId) {
        return null;
    }
}
