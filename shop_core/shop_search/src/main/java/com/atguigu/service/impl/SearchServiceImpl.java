package com.atguigu.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.dao.SearchDao;
import com.atguigu.entity.BaseBrand;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.search.*;
import com.atguigu.service.ESSearchService;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements ESSearchService {

    @Autowired
    ProductFeignClient productFeignClient;

    @Autowired
    SearchDao searchDao;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RestHighLevelClient restHighLevelClient;


    @Override
    public void onsale(Long skuId) {
        Product product = new Product();
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo != null) {
            product.setId(skuInfo.getId());
            product.setProductName(skuInfo.getSkuName());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setCreateTime(new Date());
        }

        BaseBrand brand = productFeignClient.getBrandById(skuInfo.getBrandId());
        if (brand != null) {
            product.setBrandId(brand.getId());
            product.setBrandLogoUrl(brand.getBrandLogoUrl());
            product.setBrandName(brand.getBrandName());
        }

        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());

        if (categoryView != null) {
            product.setCategory1Id(categoryView.getCategory1Id());
            product.setCategory1Name(categoryView.getCategory1Name());
            product.setCategory2Id(categoryView.getCategory2Id());
            product.setCategory2Name(categoryView.getCategory2Name());
            product.setCategory3Id(categoryView.getCategory3Id());
            product.setCategory3Name(categoryView.getCategory3Name());
        }

        List<PlatformPropertyKey> platformPropertyBySkuId = productFeignClient.getPlatformPropertyBySkuId(skuId);
        List<SearchPlatformProperty> searchPlatformProperties = platformPropertyBySkuId.stream().map(item -> {
            SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
            searchPlatformProperty.setPropertyKeyId(item.getId());
            searchPlatformProperty.setPropertyKey(item.getPropertyKey());
            searchPlatformProperty.setPropertyValue(item.getPropertyValueList().get(0).getPropertyValue());
            return searchPlatformProperty;
        }).collect(Collectors.toList());

        product.setPlatformProperty(searchPlatformProperties);
        searchDao.save(product);
    }

    @Override
    public void offsale(Long skuId) {
        searchDao.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        String hotKey = "hotScore";
        Double count = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);

        if (count % 6 == 0) {
            Optional<Product> optional = searchDao.findById(skuId);
            Product product = optional.get();
            product.setHotScore(product.getHotScore() + Math.round(count));
            searchDao.save(product);
        }
    }

    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) throws IOException {
        //1.??????DSL????????????
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);

        //2.???????????????????????????
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //3.??????????????????????????????????????????
        SearchResponseVo searchResponseVo = this.parseSearchResult(searchResponse);

        //4.???????????????????????????
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());

        //5.???????????????
        boolean addPageFlag=searchResponseVo.getTotal()%searchParam.getPageSize()==0;
        long totalPage=0;
        if(addPageFlag){
            totalPage=searchResponseVo.getTotal()/searchParam.getPageSize();
        }else{
            totalPage=searchResponseVo.getTotal()/searchParam.getPageSize()+1;
        }
        searchResponseVo.setTotalPages(totalPage);
        return searchResponseVo;
    }

    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {

        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //1.???????????????????????????
        SearchHits firstHists = searchResponse.getHits();
        SearchHit[] secondHits = firstHists.getHits();
        List<Product> productList = new ArrayList<>();
        if (null != secondHits && secondHits.length > 0) {
            for (SearchHit totalHit : secondHits) {
                //?????????????????????????????????product??????
                Product product = JSONObject.parseObject(totalHit.getSourceAsString(), Product.class);
                //????????????????????????????????????
                if(null != totalHit.getHighlightFields().get("productName")){
                    Text highlightProductName = totalHit.getHighlightFields().get("productName").getFragments()[0];
                    product.setProductName(highlightProductName.toString());
                }
                //??????????????????
                productList.add(product);
            }

        }
        //???????????????????????????searchResponseVo???????????????
        searchResponseVo.setProductList(productList);
        searchResponseVo.setTotal(firstHists.getTotalHits());
        //2.????????????brandIdAgg??????????????????
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        //?????????????????????bucket
        if(!CollectionUtils.isEmpty(brandIdAgg.getBuckets())){
            List<SearchBrandVo> searchBrandVoList = brandIdAgg.getBuckets().stream().map(bucket ->{
                SearchBrandVo searchBrandVo = new SearchBrandVo();

                String brandId = bucket.getKeyAsString();
                searchBrandVo.setBrandId(Long.parseLong(brandId));

                ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
                searchBrandVo.setBrandName(brandNameAgg.getBuckets().get(0).getKeyAsString());

                ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
                searchBrandVo.setBrandLogoUrl(brandLogoUrlAgg.getBuckets().get(0).getKeyAsString());

                return searchBrandVo;
            }).collect(Collectors.toList());

            searchResponseVo.setBrandVoList(searchBrandVoList);
        }


        //3.??????????????????????????????-platformPropertyAgg
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyVos = null;
        if(!CollectionUtils.isEmpty(propertyKeyIdAgg.getBuckets())){
           platformPropertyVos = propertyKeyIdAgg.getBuckets().stream().map(bucket -> {
                SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();

                Number propertyKeyId = bucket.getKeyAsNumber();
                searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId.longValue());

                ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
                String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
                searchPlatformPropertyVo.setPropertyKey(propertyKey);

                ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
                List<String> propertyValueList = propertyValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
                return searchPlatformPropertyVo;
            }).collect(Collectors.toList());
        }
        searchResponseVo.setPlatformPropertyList(platformPropertyVos);
        return searchResponseVo;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1.??????bool??????
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //2.????????????Id?????????
        if (searchParam.getCategory3Id() != null) {
            firstBool.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }

        if (searchParam.getCategory2Id() != null) {
            firstBool.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }

        if (searchParam.getCategory1Id() != null) {
            firstBool.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        //3.????????????id???????????? ????????????brandName=2:??????
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandParam = brandName.split(":");
            if (null != brandParam && brandParam.length == 2) {
                firstBool.filter(QueryBuilders.termQuery("brandId", brandParam[0]));
            }
        }
        //4.??????????????????????????????
        //????????????????????????http://search.gmall.com/search.html?keyword=??????&props=23:4G:????????????&props=24:128G:????????????
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            for (String prop : props) {
                String[] platformParams = prop.split(":");
                if (null != platformParams && platformParams.length == 3) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder childBoolQuery = QueryBuilders.boolQuery();
                    childBoolQuery.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    childBoolQuery.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("platformProperty", childBoolQuery, ScoreMode.None));
                    firstBool.filter(boolQuery);
                }
            }
        }
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            firstBool.must(QueryBuilders.matchQuery("productName", searchParam.getKeyword()).operator(Operator.AND));
        }

        //5 ??????dsl?????????{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //6.???query????????????????????????
        searchSourceBuilder.query(firstBool);
        //7 ???????????? ??????????????????(pageNo-1)*pageSize
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        /**
         * 8.????????????
         * ??????http://search.gmall.com/search.html?keyword=??????&props=23:4G:????????????&order=2:desc
         * ??????=1 ??????=2 ---> hotScore=1 price=2
         * &order=1:desc??????????????????????????????
         * ????????????????????????http://search.gmall.com/search.html?keyword=??????&props=23:4G:????????????&order=2
         * ?????????????????????
         */
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] orderParams = order.split(":");
            if (null != orderParams && orderParams.length == 2) {
                String field = null;
                switch (orderParams[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + orderParams[0]);
                }
                //??????????????????
                searchSourceBuilder.sort(field, "asc".equals(orderParams[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        } else {
            //??????????????????
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        //9.????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //10.??????????????????
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl")));

        //11.???????????????????????????
        // ??????????????????????????? subAggregation ???????????????????????????
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("platformPropertyAgg", "platformProperty")
                        .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                                .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                                .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));


        //12.??????????????????????????????

        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImage", "productName", "price","hotScore"},null);
        //13.?????????????????????index???type

        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");

        //14.???????????????????????????????????????????????????????????????
        searchRequest.source(searchSourceBuilder);
        System.out.println("????????????DSL??????:"+searchSourceBuilder.toString());
        return searchRequest;
    }
}