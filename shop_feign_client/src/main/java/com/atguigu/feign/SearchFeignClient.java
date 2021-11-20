package com.atguigu.feign;


import com.atguigu.fallback.ProductFallback;
import com.atguigu.fallback.SearchFallback;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "shop-search",fallback = SearchFallback.class)
public interface SearchFeignClient {


    @GetMapping("/search/createIndex")
    public RetVal createIndex();

    @GetMapping("/search/onsale/{skuId}")
    public RetVal onsale(@PathVariable Long skuId);

    @GetMapping("/search/offsale/{skuId}")
    public RetVal offsale(@PathVariable Long skuId);

    @GetMapping("/search/incrHotScore/{skuId}")
    public RetVal incrHotScore(@PathVariable Long skuId);

    @PostMapping("/search")
    public RetVal searchProduct(@RequestBody SearchParam searchParam);
}