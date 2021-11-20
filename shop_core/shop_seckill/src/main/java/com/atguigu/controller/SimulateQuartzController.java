package com.atguigu.controller;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulateQuartzController {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //1.发送上架秒杀商品的通知
    @GetMapping("sendMsgToScanSeckill")
    public String sendMsgToScanSeckill(){
        //只起到一个通知上架作用 所以在这里随便发点内容
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.SCAN_SECKILL_ROUTE_KEY,"");
        return "success";
    }


    //2.秒杀结束之后的善后工作(定时任务)
    @GetMapping("sendMsgToClearSeckill")
    public String sendMsgToClearSeckill(){
        //只起到一个通知上架作用 所以在这里随便发点内容
        rabbitTemplate.convertAndSend(MqConst.CLEAR_REDIS_EXCHANGE,MqConst.CLEAR_REDIS_ROUTE_KEY,"");
        return "success";
    }
}
