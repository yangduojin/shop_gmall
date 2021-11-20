package com.atguigu.controller;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@RestController
@RequestMapping("/cart")
public class CartInfoController {

    @Autowired
    private CartInfoService cartInfoService;

    @PostMapping("addToCart/{skuId}/{skuNum}")
    public RetVal addToCart(@RequestParam Long skuId, @RequestParam Integer skuNum, HttpServletRequest request) {
        // 如何获取到userId，在网关中将用户Id{登录，未登录}传递到后台 从header中获取登录的userId
        String userId = AuthContextHolder.getUserId(request);
        // 说明用户未登录
        if (StringUtils.isEmpty(userId)) {
            // 获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }

        cartInfoService.addToCart(skuId, userId, skuNum);
        return RetVal.ok();
    }

    @GetMapping("getCartList")
    public RetVal getCartList(HttpServletRequest request) {

        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfos = cartInfoService.getCartList(userId,userTempId);
        return RetVal.ok(cartInfos);
    }

    //3.商品的选中 http://api.gmall.com/cart/checkCart/30/1
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public RetVal checkCart(@PathVariable Long skuId,@PathVariable Integer isChecked, HttpServletRequest request){
        //拿到用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            userId=AuthContextHolder.getUserTempId(request);
        }
        cartInfoService.checkCart(userId,skuId,isChecked);
        return RetVal.ok();
    }
    //4.购物车的删除
    @DeleteMapping("deleteCart/{skuId}")
    public RetVal deleteCart(@PathVariable Long skuId, HttpServletRequest request){
        //拿到用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            userId=AuthContextHolder.getUserTempId(request);
        }
        cartInfoService.deleteCart(userId,skuId);
        return RetVal.ok();
    }
    //5.查询选中的商品信息
    @GetMapping("getSelectedProduct/{userId}")
    public List<CartInfo> getSelectedProduct(@PathVariable String userId){
        return cartInfoService.getSelectedProduct(userId);
    }

    //6.从数据库中查询出最新的购物车信息到redis中
    @GetMapping("queryFromDbToRedis/{userId}")
    public List<CartInfo> queryFromDbToRedis(@PathVariable String userId){
        return cartInfoService.queryFromDbToRedis(userId);
    }

    @PostMapping("getSkusPrice/{userId}")
    public List<Long> getSkusPrice(@RequestBody Map<Long, BigDecimal> map, @PathVariable Long userId){
        return cartInfoService.getSkusPrice(map,userId);
    }

}

