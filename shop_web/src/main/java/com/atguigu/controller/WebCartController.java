package com.atguigu.controller;

import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebCartController {

    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private ProductFeignClient productFeignClient;

    @RequestMapping("addCart.html")
    public String addCart(@RequestParam Long skuId, @RequestParam Integer skuNum, HttpServletRequest request) {
        //并没有传入用户Id？
        cartFeignClient.addToCart(skuId, skuNum);
        //页面需要skuNum, skuInfo
        request.setAttribute("skuInfo", productFeignClient.getSkuInfo(skuId));
        request.setAttribute("skuNum", skuNum);
        return "cart/addCart";
    }

    @RequestMapping("cart.html")
    public String cart() {
        //并没有传入用户Id？
//        List<CartInfo> cartInfos = cartFeignClient.getCartList();
//        request.setAttribute("",cartInfos);
        return "cart/index";
    }
}


