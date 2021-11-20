package com.atguigu.service;

import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;

import java.io.IOException;

public interface ESSearchService {
    void onsale(Long skuId);

    void offsale(Long skuId);

    void incrHotScore(Long skuId);

    SearchResponseVo searchProduct(SearchParam searchParam) throws IOException;
}
