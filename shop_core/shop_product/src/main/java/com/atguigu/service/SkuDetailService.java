package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author 90362
 */
public interface SkuDetailService {
    SkuInfo getSkuInfo(long skuId);

    List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(long productId, long skuId);

    Map getSkuSalePropertyValueId(long productId);

    SkuInfo getSkuInfoFromRedission(long skuId);
}
