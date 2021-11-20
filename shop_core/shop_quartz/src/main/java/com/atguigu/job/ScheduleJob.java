package com.atguigu.job;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@EnableScheduling
@Component
public class ScheduleJob {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //https://cron.qqe2.com/   0 0 5 * * ?
    @Scheduled(cron = "0 0 5 * * ?")
    public void taskEveryNight05(){
        System.out.println("开始上架商品");
        //只起到一个通知上架作用 所以在这里随便发点内容
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.SCAN_SECKILL_ROUTE_KEY,"");
    }
    @Scheduled(cron = "0 0 2 * * ?")
    public void taskEveryNight03(){
        System.out.println("开始下架商品");
        //只起到一个通知上架作用 所以在这里随便发点内容
        rabbitTemplate.convertAndSend(MqConst.CLEAR_REDIS_EXCHANGE,MqConst.CLEAR_REDIS_ROUTE_KEY,"");
    }
}
