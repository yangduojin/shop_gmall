package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MyRedisMessageReceiver {
    @Autowired
    private RedisTemplate redisTemplate;

    //具体实现如何处理消息的类
    public void receiveChannelMessage(String message){
        //拿到的消息格式 ""33:1""
        if(StringUtils.isNotEmpty(message)){
            //"33:1"
            message=message.replaceAll("\"","");
            String[] splitMessage = message.split(":");
            if(splitMessage.length==2){
                //splitMessage[0]是商品skuId,splitMessage[1]状态位
                redisTemplate.opsForValue().set(RedisConst.SECKILL_STATE_PREFIX+splitMessage[0],splitMessage[1]);
            }
        }
    }
}
