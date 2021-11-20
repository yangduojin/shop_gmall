package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.service.ESSearchService;
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
public class EsConsumer {
    @Autowired
    private ESSearchService searchService;

    //接受上架消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.ON_SALE_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
            key = {MqConst.ON_SALE_ROUTING_KEY}))
    public void onSale(Long skuId, Message message, Channel channel) throws IOException {
        if(skuId!=null){
            searchService.onsale(skuId);
        }
        /**
         * 手动签收一把
         *  deliveryTag签收那个消息
         *  multiple 是否应答多个消息 true应答多个消息 false只应答当前消息
         */
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
    //接受下架消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.OFF_SALE_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
            key = {MqConst.OFF_SALE_ROUTING_KEY}))
    public void offSale(Long skuId, Message message, Channel channel) throws IOException {
        if(skuId!=null){
            searchService.offsale(skuId);
        }
        /**
         * 手动签收一把
         *  deliveryTag签收那个消息
         *  multiple 是否应答多个消息 true应答多个消息 false只应答当前消息
         */
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}