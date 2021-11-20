package com.atguigu.controller;

import com.atguigu.feign.ProductFeignClient;
import com.atguigu.feign.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private SearchFeignClient searchFeignClient;

    @GetMapping({"/", "/index.html"})
    public String getSkuDetail(HttpServletRequest request) {
        RetVal retVal = productFeignClient.getIndexCategoryInfo();
        request.setAttribute("list", retVal.getData());
        return "index/index";
    }

    @GetMapping({"/search.html"})
    public String search(SearchParam searchParam, Model model) {
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        // 搜索到的商品,品牌集合,平台属性集合的展示
        model.addAllAttributes(retVal.getData());
        //前台需要存储一个searchParam
        model.addAttribute("searchParam", searchParam);
        // 搜索路径参数的回显
        String urlParam = pageUrlParam(searchParam);
        model.addAttribute("urlParam", urlParam);
        // 页面回显品牌信息
        String brandName = pageBrandName(searchParam.getBrandName());
        model.addAttribute("brandNameParam", brandName);
        // 页面回显平台属性信息
        List<Map<String, String>> propsList = pageProps(searchParam.getProps());
        model.addAttribute("propsParamList", propsList);
        // 页面会先排序信息
        Map<String, Object> map = pageSortInfo(searchParam.getOrder());
        model.addAttribute("orderMap",map);

        return "search/index";
    }

    private Map<String, Object> pageSortInfo(String order) {
        Map<String,Object> map = new HashMap<>();
        if(!StringUtils.isEmpty(order)){
            //order=1:asc拆分数据
            String[] orderParams  = order.split(":");
            if (null != orderParams && orderParams.length == 2) {
                map.put("type",orderParams[0]);
                map.put("sort",orderParams[1]);
            }
        }else {//给一个默认的排序规则
            map.put("type", 1);
            map.put("sort", "asc");
        }
        return map;
    }

    private List<Map<String, String>> pageProps(String[] props) {
        List<Map<String, String>> list = new ArrayList<>();
        if (null != props && props.length > 0) {
            for (String prop : props) {
                String[] propPrams = prop.split(":");
                if (null != propPrams && propPrams.length == 3) {
                    Map<String, String> map = new HashMap<>();
                    map.put("propertyKeyId", propPrams[0]);
                    map.put("propertyValue", propPrams[1]);
                    map.put("propertyKey", propPrams[2]);
                    list.add(map);
                }
            }
        }
        return list;
    }

    private String pageBrandName(String brandName) {
        if (!StringUtils.isEmpty(brandName)) {
            String[] brandnameParams = brandName.split(":");
            if (null != brandnameParams && brandnameParams.length == 2) {
                return "品牌：" + brandnameParams[1];
            }
        }
        return "";
    }

    private String pageUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }

        if (null != searchParam.getCategory1Id()) {
            urlParam.append("category1Id").append(searchParam.getCategory1Id());
        }
        if (null != searchParam.getCategory2Id()) {
            urlParam.append("category2Id").append(searchParam.getCategory2Id());
        }
        if (null != searchParam.getCategory3Id()) {
            urlParam.append("category3Id").append(searchParam.getCategory3Id());
        }
        if (!StringUtils.isEmpty(searchParam.getBrandName())) {
            if (urlParam.length() > 0) {
                urlParam.append("&brandName=").append(searchParam.getBrandName());
            } else {
                urlParam.append("brandName=").append(searchParam.getBrandName());
            }
        }
        if (null != searchParam.getProps()) {
            for (String prop : searchParam.getProps()) {
                if (urlParam.length() > 0) {
                    urlParam.append("&props=").append(prop);
                } else {
                    urlParam.append("props=").append(prop);
                }
            }
        }
        return "search.html?" + urlParam.toString();
    }


}
