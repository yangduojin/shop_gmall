package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.exector.MyExecutor;
import com.atguigu.feign.CartFeignClient;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.HttpClientUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${cancel.order.delay}")
    private Integer cancelOrderDelay;

    @Transactional
    @Override
    public Long saveOrderAndDetail(OrderInfo orderInfo) {
        //a.保存订单基本信息
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //商品对外订单号 out_trade_no 给支付宝或者微信
        String outTradeNo = "atguigu" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //订单主体信息
        orderInfo.setTradeBody("购买的商品");
        orderInfo.setCreateTime(new Date());
        //订单支付过期时间 默认30分钟过期 设置一天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        //订单进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        baseMapper.insert(orderInfo);
        //b.保存订单详情信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
        }
        orderDetailService.saveBatch(orderDetailList);
        //发送一个延迟消息 定时取消订单
        rabbitTemplate.convertAndSend(
                MqConst.CANCEL_ORDER_EXCHANGE,
                MqConst.CANCEL_ORDER_ROUTE_KEY,
                orderInfo.getId(),
                correlationData -> {
                    correlationData.getMessageProperties().setDelay(cancelOrderDelay);
                    return correlationData;
                }
        );
        return orderInfo.getId();
    }

    @Override
    public String generateTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        //往redis里面存放流水号
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    @Override
    public boolean checkTradeNo(String uiTradeNo, String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return uiTradeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    //方式二
    @Override
    public List<String> checkStockAndPrice(String userId, OrderInfo orderInfo) {
        //新建一个list用于提示用户商品库存情况
        List<String> warningInfoList = new ArrayList<>();
        //异步编排的集合
        List<CompletableFuture> multiFutureList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            //看看每个商品是否足够 调用下面的接口
            for (OrderDetail orderDetail : orderDetailList) {
                //http://localhost:8100/hasStock?skuId=24&num=200
                Long skuId = orderDetail.getSkuId();
                String skuNum = orderDetail.getSkuNum();
                CompletableFuture<Void> checkStockFuture = CompletableFuture.runAsync(() -> {
                    String result = HttpClientUtil.doGet("http://localhost:8100/hasStock?skuId=" + skuId + "&num=" + skuNum);
                    //0：无库存   1：有库存
                    if ("0".equals(result)) {
                        warningInfoList.add(orderDetail.getSkuName() + "库存不足!!");
                    }
                }, MyExecutor.getInstance());
                multiFutureList.add(checkStockFuture);

                CompletableFuture<Void> checkPriceFuture = CompletableFuture.runAsync(() -> {
                    //验证价格是否为最新价格
                    BigDecimal realTimePrice = productFeignClient.getSkuPrice(skuId);
                    //判断当前送货清单商品价格与实时价格是否相同
                    if (orderDetail.getOrderPrice().compareTo(realTimePrice) != 0) {
                        warningInfoList.add(orderDetail.getSkuName() + "价格有变化");
                        //更新缓存里面的价格信息
                        cartFeignClient.queryFromDbToRedis(userId);
                    }
                }, MyExecutor.getInstance());
                multiFutureList.add(checkPriceFuture);
            }
        }
        //需要以上两个都做完了之后才返回
        CompletableFuture[] completableFuture = new CompletableFuture[multiFutureList.size()];
        CompletableFuture.allOf(multiFutureList.toArray(completableFuture)).join();
        return warningInfoList;
    }

    @Override
    public void updateOrderStatus(OrderInfo orderInfo, ProcessStatus processStatus) {
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        baseMapper.updateById(orderInfo);
    }



    //方式一
//    @Override
//    public List<String> checkStockAndPrice(String userId, OrderInfo orderInfo) {
//        //新建一个list用于提示用户商品库存情况
//        List<String> warningInfoList = new ArrayList<>();
//        //
//        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
//        if(!CollectionUtils.isEmpty(orderDetailList)){
//            //看看每个商品是否足够 调用下面的接口
//            for (OrderDetail orderDetail : orderDetailList) {
//                //http://localhost:8100/hasStock?skuId=24&num=200
//                Long skuId = orderDetail.getSkuId();
//                String skuNum = orderDetail.getSkuNum();
//                String result = HttpClientUtil.doGet("http://localhost:8100/hasStock?skuId=" + skuId + "&num=" + skuNum);
//                //0：无库存   1：有库存
//                if("0".equals(result)){
//                    warningInfoList.add(orderDetail.getSkuName()+"库存不足!!");
//                }
//                //验证价格是否为最新价格
//                BigDecimal realTimePrice = productFeignClient.getSkuPrice(skuId);
//                //判断当前送货清单商品价格与实时价格是否相同
//                if(orderDetail.getOrderPrice().compareTo(realTimePrice)!=0){
//                    warningInfoList.add(orderDetail.getSkuName()+"价格有变化");
//                    //更新缓存里面的价格信息
//                    cartFeignClient.queryFromDbToRedis(userId);
//                }
//            }
//        }
//        return warningInfoList;
//    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //1.查询订单的基本信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //2.查询订单的详情信息
        if(orderInfo!=null){
            QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("order_id",orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }

    @Override
    public void sendMsgToWareHouse(OrderInfo orderInfo) {
        //a.将订单状态改为已通知仓库
        updateOrderStatus(orderInfo,ProcessStatus.NOTIFIED_WARE);
        //b.需要组织数据为json字符串传递给仓库系统
        Map<String, Object> dataMap = assembleWareHouseData(orderInfo);
        String jsonData = JSON.toJSONString(dataMap);
        //c.发送消息给仓库系统
        rabbitTemplate.convertAndSend(MqConst.DECREASE_STOCK_EXCHANGE,MqConst.DECREASE_STOCK_ROUTE_KEY,jsonData);
    }



    private Map<String, Object> assembleWareHouseData(OrderInfo orderInfo) {
        //构造一个map用于封装数据
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("orderId",orderInfo.getId());
        dataMap.put("consignee",orderInfo.getConsignee());
        dataMap.put("consigneeTel",orderInfo.getConsigneeTel());
        dataMap.put("orderComment",orderInfo.getOrderComment());
        dataMap.put("orderBody",orderInfo.getTradeBody());
        dataMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        dataMap.put("paymentWay",2);
        //TODO 这里有一个非常重要的字段先不写
        dataMap.put("wareId",orderInfo.getWareHouseId());

        List<Map> orderDetailMapList=new ArrayList<>();
        //商品清单
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            orderDetailMapList.add(orderDetailMap);
        }
        dataMap.put("details",orderDetailMapList);
        return dataMap;
    }

    @Override
    public String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson) {
        //a.获取原始订单
        OrderInfo parentOrder = getOrderInfo(orderId);
        //把接受到的参数转换为List [{"wareHouseId":"1","skuIdList":["24"]},{"wareHouseId":"2","skuIdList":["25"]}]
        List<Map> wareHouseIdSkuIdMapList = JSON.parseArray(wareHouseIdSkuIdMapJson, Map.class);

        List<Map> assembleDataMapList = new ArrayList<>();
        for (Map wareHouseIdSkuIdMap : wareHouseIdSkuIdMapList) {
            String wareHouseId=(String)wareHouseIdSkuIdMap.get("wareHouseId");
            List<String> childSkuIdList=(List<String>)wareHouseIdSkuIdMap.get("skuIdList");
            //b.设置子订单信息
            OrderInfo childOrder = new OrderInfo();
            //以下操作累的不行
            //childOrder.setConsignee(parentOrder.getConsignee());
            //childOrder.setConsigneeTel(parentOrder.getConsigneeTel());
            BeanUtils.copyProperties(parentOrder,childOrder);
            childOrder.setParentOrderId(orderId);
            childOrder.setId(null);
            //设置仓库id
            childOrder.setWareHouseId(wareHouseId);
            //c.设置子订单详情信息
            ArrayList<OrderDetail> childOrderDetailList = new ArrayList<>();
            BigDecimal totalMoney = new BigDecimal(0);
            List<OrderDetail> parentOrderDetailList = parentOrder.getOrderDetailList();
            for (OrderDetail parentOrderDetail : parentOrderDetailList) {
                for (String childSkuId : childSkuIdList) {
                    if(parentOrderDetail.getSkuId()==Long.parseLong(childSkuId)){
                        childOrderDetailList.add(parentOrderDetail);
                        //子订单的总金额
                        BigDecimal orderPrice = parentOrderDetail.getOrderPrice();
                        String skuNum = parentOrderDetail.getSkuNum();
                        totalMoney=totalMoney.add(orderPrice.multiply(new BigDecimal(skuNum)));
                    }
                }
            }
            childOrder.setOrderDetailList(childOrderDetailList);
            childOrder.setTotalMoney(totalMoney);
            //d.保存子订单及详情信息
            saveOrderAndDetail(childOrder);
            Map<String, Object> assembleDataMap = assembleWareHouseData(childOrder);
            assembleDataMapList.add(assembleDataMap);
        }
        //原始订单需要修改状态 修改为split
        updateOrderStatus(parentOrder,ProcessStatus.SPLIT);
        //e.把订单信息返回给库存系统
        return JSON.toJSONString(assembleDataMapList);
    }
}
