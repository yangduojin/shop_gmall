package com.atguigu.controller;


import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {

    @Autowired
    CartFeignClient cartFeignClient;

    @Autowired
    UserFeignClient userFeignClient;

    @Autowired
    OrderInfoService orderInfoService;

    //1.订单确认信息提供的数据接口
    @GetMapping("/confirm")
    public RetVal confirm(HttpServletRequest request){
        String userId=AuthContextHolder.getUserId(request);
        //a.收货地址信息
        List<UserAddress> userAddressList = userFeignClient.queryAddressByUserId(userId);
        //b.送货清单
        List<CartInfo> selectedProductList = cartFeignClient.getSelectedProduct(userId);
        //c.商品总金额
        BigDecimal totalMoney = new BigDecimal(0);
        //d.商品总共多少件
        int totalNum=0;
        //e.送货清单改造版
        List<OrderDetail> orderDetailList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(selectedProductList)){

            for (CartInfo cartInfo : selectedProductList) {
                //把cartInfo信息转换为orderDetail
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum()+"");
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                //订单总金额
                totalMoney=totalMoney.add(cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                totalNum+=cartInfo.getSkuNum();
                orderDetailList.add(orderDetail);
            }
        }
        Map<String, Object> retMap = new HashMap<>();
        //地址信息
        retMap.put("userAddressList",userAddressList);
        //送货清单改造版
        retMap.put("detailArrayList",orderDetailList);
        //订单总金额
        retMap.put("totalMoney",totalMoney);
        //订单总数量
        retMap.put("totalNum",totalNum);
        //生成一个流水号
        String tradeNo=orderInfoService.generateTradeNo(userId);
        retMap.put("tradeNo",tradeNo);
        return RetVal.ok(retMap);
    }

    //2.提交订单信息
    @PostMapping("/submitOrder")
    public RetVal submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //a.从页面拿到传递过来的流水号
        String tradeNo = request.getParameter("tradeNo");
        //b.把页面流水号同redis里面的进行比较
        boolean flag = orderInfoService.checkTradeNo(tradeNo, userId);
        if(!flag){
            return RetVal.fail().message("不能无刷新重复提交订单");
        }
        //c.删除redis流水号
        orderInfoService.deleteTradeNo(userId);
        //d.验证库存与价格
        List<String> warningInfoList = orderInfoService.checkStockAndPrice(userId,orderInfo);
        //如果有警告信息
        if(warningInfoList.size()>0){
            return RetVal.fail().message(StringUtils.join(warningInfoList,","));
        }

        //d.保存订单信息
        orderInfo.setUserId(Long.parseLong(userId));
        Long orderId = orderInfoService.saveOrderAndDetail(orderInfo);
        return RetVal.ok(orderId);
    }

//    public static void main(String[] args) {
//        List<String> aa=new ArrayList<String>();
//        aa.add("aaa");
//        aa.add("bbb");
//        System.out.println(StringUtils.join(aa,","));
//    }

    //3.根据订单id查询订单信息
    @GetMapping("getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        return orderInfoService.getOrderInfo(orderId);
    }

    //4.拆单接口编写 http://order.gmall.com/order/splitOrder
    @PostMapping("splitOrder")
    public String splitOrder(@RequestParam Long orderId,@RequestParam String wareHouseIdSkuIdMapJson){
        return orderInfoService.splitOrder(orderId,wareHouseIdSkuIdMapJson);
    }

    //5.封装保存订单信息为接口
    @PostMapping("saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo){
        return orderInfoService.saveOrderAndDetail(orderInfo);
    }

}

