package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
public interface OrderInfoService extends IService<OrderInfo> {

    Long saveOrderAndDetail(OrderInfo orderInfo);

    String generateTradeNo(String userId);

    boolean checkTradeNo(String tradeNo, String userId);

    void deleteTradeNo(String userId);

    List<String> checkStockAndPrice(String userId, OrderInfo orderInfo);

    void updateOrderStatus(OrderInfo orderInfo, ProcessStatus processStatus);

    OrderInfo getOrderInfo(Long orderId);

    void sendMsgToWareHouse(OrderInfo orderInfo);

    String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson);
}
