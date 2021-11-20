package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
public class SeckillConsumer {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;

    //1.扫描符合条件的秒杀商品到redis当中
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SCAN_SECKILL_EXCHANGE,durable = "false"),
            key = {MqConst.SCAN_SECKILL_ROUTE_KEY}))
    public void scanSeckillProductToRedis(Message message, Channel channel) throws IOException {
        //channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        //a.扫描符合条件的秒杀商品
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //该商品状态为已审核通过
        wrapper.eq("status",1);
        //剩余库存>0
        wrapper.gt("num",0);
        //取出当天日期的商品 select * from seckill_product where DATE_FORMAT(start_time,'%Y-%m-%d') ='2021-11-17'
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        //b.把秒杀到的商品放入redis
        if(!CollectionUtils.isEmpty(seckillProductList)){
            for (SeckillProduct seckillProduct : seckillProductList) {
                //以hash结构存放秒杀商品信息
                redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(seckillProduct.getSkuId().toString(),seckillProduct);
                //利用list集合的数据结构存储商品的剩余数量 减库存的时候从list里面取出一个
                for (int i = 0; i <seckillProduct.getNum() ; i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillProduct.getSkuId())
                            .leftPush(seckillProduct.getSkuId().toString());
                }
                //d.通知redis集群其他节点该商品可以进行秒杀啦
                redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,seckillProduct.getSkuId()+":"+RedisConst.CAN_SECKILL);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //2.秒杀商品预下单消费端编写
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE,durable = "false"),
            key = {MqConst.PREPARE_SECKILL_ROUTE_KEY}))
    public void prepareSeckill(UserSeckillSkuInfo userSeckillSkuInfo, Message message, Channel channel) throws Exception {
        if(userSeckillSkuInfo!=null){
            //开始处理预下单
            seckillProductService.prepareSeckill(userSeckillSkuInfo);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //3.秒杀结束之后的善后工作(定时任务)
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.CLEAR_REDIS_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.CLEAR_REDIS_EXCHANGE,durable = "false"),
            key = {MqConst.CLEAR_REDIS_ROUTE_KEY}))
    public void clearSeckill(Message message, Channel channel) throws Exception {
        //a.把小于等于当前时间的商品给下架
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //该商品状态为已审核通过
        wrapper.eq("status",1);
        //当前时间大于等于end_time
        wrapper.le("end_time", new Date());
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        if(!CollectionUtils.isEmpty(seckillProductList)){
            for (SeckillProduct seckillProduct : seckillProductList) {
                //b.删除该商品的状态位/库存信息
                redisTemplate.delete(RedisConst.SECKILL_STATE_PREFIX+seckillProduct.getSkuId());
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillProduct.getSkuId());
                //把秒杀商品更新为 2为已结束
                seckillProduct.setStatus("2");
                seckillProductService.updateById(seckillProduct);

            }
            //把所有商品从redis当中删除 每天晚上跑
            redisTemplate.delete(RedisConst.SECKILL_PRODUCT);
            //c.该商品的预订单信息
            redisTemplate.delete(RedisConst.PREPARE_SECKILL_USERID_ORDER);
            //d.删除已经抢到的订单信息(持久化到数据库了)
            redisTemplate.delete(RedisConst.BOUGHT_SECKILL_USER_ORDER);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
