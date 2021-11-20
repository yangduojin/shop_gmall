package com.atguigu.controller;

import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.ESSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    ElasticsearchRestTemplate restTemplate;

    @Autowired
    ESSearchService esSearchService;



    //1.创建索引
    @GetMapping("createIndex")
    public RetVal createIndex() {
        // 调用类中方法自动创建
        restTemplate.createIndex(Product.class);
        restTemplate.putMapping(Product.class);
        return RetVal.ok();
    }


    @GetMapping("onsale/{skuId}")
    public RetVal onsale(@PathVariable Long skuId){
        esSearchService.onsale(skuId);
        return RetVal.ok();
    }

    @GetMapping("offsale/{skuId}")
    public RetVal offsale(@PathVariable Long skuId){
        esSearchService.offsale(skuId);
        return RetVal.ok();
    }

    //4.商品的热度排名
    @GetMapping("incrHotScore/{skuId}")
    public RetVal incrHotScore(@PathVariable Long skuId){
        esSearchService.incrHotScore(skuId);
        return RetVal.ok();
    }

    //5.商品搜索
    @PostMapping
    public RetVal searchProduct(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo responseVo = esSearchService.searchProduct(searchParam);
        return RetVal.ok(responseVo);
    }


}
