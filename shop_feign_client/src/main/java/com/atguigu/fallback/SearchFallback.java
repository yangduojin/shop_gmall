package com.atguigu.fallback;

import com.atguigu.feign.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;

public class SearchFallback implements SearchFeignClient {
    @Override
    public RetVal createIndex() {
        return null;
    }

    @Override
    public RetVal onsale(Long skuId) {
        return null;
    }

    @Override
    public RetVal offsale(Long skuId) {
        return null;
    }

    @Override
    public RetVal incrHotScore(Long skuId) {
        return null;
    }

    @Override
    public RetVal searchProduct(SearchParam searchParam) {
        return null;
    }
}
