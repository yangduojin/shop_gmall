package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PaymentConsumer {
    @Autowired
    private PaymentInfoService paymentInfoService;

    //1.关闭支付订单信息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.CLOSE_PAYMENT_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.CLOSE_PAYMENT_EXCHANGE,durable = "false"),
            key = {MqConst.CLOSE_PAYMENT_ROUTE_KEY}))

    public void closePaymentInfo(Long orderId, Message message, Channel channel) throws IOException {
        if(orderId!=null){
            QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("order_id",orderId);
            PaymentInfo paymentInfo = paymentInfoService.getOne(wrapper);
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            paymentInfoService.updateById(paymentInfo);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}