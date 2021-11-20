package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisChannelConfig {
    //用哪个类的哪个方法去接受处理消息
    @Bean
    MessageListenerAdapter messageListenerAdapter(MyRedisMessageReceiver myRedisMessageReceiver){
        return new MessageListenerAdapter(myRedisMessageReceiver,"receiveChannelMessage");
    }


    /**
     * 相当于一个Listener
     * @param connectionFactory   redis的连接工厂
     * @param messageListenerAdapter 当有消息的时候接受并处理消息的实现类
     * @return
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter messageListenerAdapter){
        RedisMessageListenerContainer container=new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅那个主题
        container.addMessageListener(messageListenerAdapter,new PatternTopic(RedisConst.PREPARE_PUB_SUB_SECKILL));
        return container;
    }
}
