package com.atguigu.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MyCallback implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {

    /**
     * 交换机是否收到消息的一个回调
     *
     * @param correlationData 和消息有关的参数
     * @param ack             true代表的是交换机已经收到消息
     * @param cause           交换机没有收到消息的原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = correlationData != null ? correlationData.getId() : "";
        if (ack) {
            log.info("交换机已经收到id为:{}的消息", id);
        } else {
            log.info("交换机还未收到id为:{}消息,由于原因:{}", id, cause);
        }
    }

    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.info("消息:{}被服务器退回，退回原因:{}, 交换机是:{}, 路由key:{}", new String(message.getBody()),replyText, exchange, routingKey);

    }
}
