package com.atguigu.consumer;

import com.alibaba.fastjson.JSON;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.feign.PaymentFeignClient;
import com.atguigu.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    PaymentFeignClient paymentFeignClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 1.取消订单的监视器
     *
     */

//    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
//    public void cancelOrder(Long orderId, Message message, Channel channel) throws Exception {
//        if (orderId != null) {
//            //查询订单信息
//            OrderInfo orderInfo = orderInfoService.getById(orderId);
//            //如果订单为未支付
//            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
//                //关闭订单信息
//                orderInfoService.updateOrderStatus(orderInfo, ProcessStatus.CLOSED);
//                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
//                //如果有支付订单信息
//                if (paymentInfo!=null && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
//                    rabbitTemplate.convertAndSend(MqConst.CLOSE_PAYMENT_EXCHANGE, MqConst.CLOSE_PAYMENT_ROUTE_KEY, orderId);
//                    //如果阿里系统创建了交易 则关闭阿里内部交易
//                    boolean flag = paymentFeignClient.queryAlipayTrade(orderId);
//                    if(flag){
//                        paymentFeignClient.closeAlipayTrade(orderId);
//                    }
//                }
//            }
//        }
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//    }

    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void canCelOrder(Long orderId){
        if(orderId!=null){
            //根据订单id查询订单信息
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            //需要在订单为未支付的情况下去关闭订单
            if(orderInfo!=null&&orderInfo.getOrderStatus().equals(OrderStatus.UNPAID.name())){
                //修改订单状态为关闭状态
                orderInfoService.updateOrderStatus(orderInfo, ProcessStatus.CLOSED);
                //关闭支付订单信息
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if(paymentInfo!=null&&paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    //借助MQ实现发送消息去关闭支付订单信息
                    rabbitTemplate.convertAndSend(MqConst.CLOSE_PAYMENT_EXCHANGE,MqConst.CLOSE_PAYMENT_ROUTE_KEY,orderId);
                    //查询支付宝是否有该交易记录信息
                    boolean flag = paymentFeignClient.queryAlipayTrade(orderId);
                    if(flag){
                        //关闭支付宝交易记录
                        paymentFeignClient.closeAlipayTrade(orderId);
                    }

                }
            }
        }
    }

//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = MqConst.PAY_ORDER_EXCHANGE,durable = "false",autoDelete = "false"),
//            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE,durable = "false",autoDelete = "true"),
//            key = {MqConst.PAY_ORDER_ROUTE_KEY}
//    ))
//    public void updateOrderAfterPaySucess(Long orderId, Message message, Channel channel) throws Exception{
//        if(orderId != null){
//            OrderInfo orderInfo = orderInfoService.getById(orderId);
//            if(orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
//                    orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.PAID);
//                    orderInfoService.sendMsgToWarehouse(orderInfo);
//                }
//            }
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//        }
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE,durable = "false"),
//            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE,durable = "false",autoDelete = "true"),
//            key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}
//    ))

    //2.支付成功之后修改订单状态
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PAY_ORDER_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE,durable = "false"),
            key = {MqConst.PAY_ORDER_ROUTE_KEY}))

    public void updateOrderAfterPaySuccess(Long orderId, Message message, Channel channel) throws IOException {
        if(orderId!=null){
            //根据orderId查询订单对象
            OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);
            //如果订单为未支付
            if(orderInfo!=null&&orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.name())){
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.PAID);
                //通知库存系统去减库存
                orderInfoService.sendMsgToWareHouse(orderInfo);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


//    public void updateOrderStatus(String msgJson, Message message , Channel channel) throws Exception{
//        if(!StringUtils.isEmpty(msgJson)){
//            Map map = JSON.parseObject(msgJson, Map.class);
//            // 获取对应的数据
//            String orderId = (String) map.get("orderId");
//            String status = (String) map.get("status");
//            OrderInfo orderInfo = orderInfoService.getOrderInfo(Long.parseLong(orderId));
//            if ("DEDUCTED".equals(status)){
//                // 减库存成功！
//                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.WAITING_DELEVER);
//            }else {
//                // 减库存失败！超卖
//                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.STOCK_EXCEPTION);
//            }
//        }
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//        }

    //3.仓库系统减库存成功之后的消费代码
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE,durable = "false"),
            key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}))

    public void updateOrderStatus(String msgJson, Message message, Channel channel) throws IOException {
        if(!StringUtils.isEmpty(msgJson)){
            //把拿到的数据信息解析出订单id和状态信息
            Map<String,String> map = JSON.parseObject(msgJson, Map.class);
            String orderId = map.get("orderId");
            String status = map.get("status");
            OrderInfo orderInfo = orderInfoService.getOrderInfo(Long.parseLong(orderId));
            //如果仓库减库存成功(DEDUCTED) 这边就把状态改为已发货
            if("DEDUCTED".equals(status)){
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.WAITING_DELEVER);
            }else{
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.STOCK_EXCEPTION);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    }

