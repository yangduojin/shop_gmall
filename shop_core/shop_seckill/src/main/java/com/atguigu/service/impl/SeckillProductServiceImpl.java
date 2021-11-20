package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-17
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public SeckillProduct getSecKillProductBySkuId(Long skuId) {
        return (SeckillProduct) redisTemplate.opsForHash().get(RedisConst.SECKILL_PRODUCT, skuId.toString());
    }

    @Override
    public void prepareSeckill(UserSeckillSkuInfo userSeckillSkuInfo) {
        Long skuId = userSeckillSkuInfo.getSkuId();
        String userId = userSeckillSkuInfo.getUserId();
        //a.校验秒杀商品状态位
        String state =(String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId);
        //商品已经售罄
        if(RedisConst.CAN_NOT_SECKILL.equals(state)){
            //可以返回一个信息给调用方
            return;
        }
        //b.要先判断之前是否已经加入过 把用户抢购的预订单放入缓存
        boolean flag=redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID+":"+userId+":"+skuId,skuId,RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        //true代表之前还没有放入预订单 false代表已经放入过
        if(!flag){
           return;
        }
        //c.校验是否还有库存 如果有库存 需要减库存
        String stockSkuId = (String)redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        //没有库存了
        if(StringUtils.isEmpty(stockSkuId)){
            //没有库存的时候 需要更新秒杀状态位 24:0
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_NOT_SECKILL);
            return;
        }
        //d.生成临时订单数据存储redis
        PrepareSeckillOrder prepareSeckillOrder = new PrepareSeckillOrder();
        prepareSeckillOrder.setUserId(userId);
        prepareSeckillOrder.setBuyNum(1);
        SeckillProduct seckillProduct = getSecKillProductBySkuId(skuId);
        prepareSeckillOrder.setSeckillProduct(seckillProduct);
        //设置订单码
        prepareSeckillOrder.setPrepareOrderCode(MD5.encrypt(userId+skuId));
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId,prepareSeckillOrder);
        //e.更新库存量
        updateSecKillStockCount(skuId);
    }



    /**
     * 第一种异步优化 异步编排结合线程池 并发量太大 导致创建线程也很多
     * 第二种同步
     * @param skuId
     */
    private void updateSecKillStockCount(Long skuId) {
        //锁定库存量=总的数量-剩余的队列个数
        Long leftCount = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //更新频次 自定义规则
        if(leftCount%2==0){
            SeckillProduct seckillProduct = getSecKillProductBySkuId(skuId);
            Integer totalNum = seckillProduct.getNum();
            int leftCountInt = Integer.parseInt(leftCount + "");
            int stockCount=totalNum-leftCountInt;
            seckillProduct.setStockCount(stockCount);
            //更新数据库是为了持久化 防止数据丢失
            baseMapper.updateById(seckillProduct);
            //更新redis里面的已售商品数量信息 目的是给客户看还有多少
            redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId,seckillProduct);
        }

    }

    @Override
    public RetVal hasQualified(Long skuId, String userId) {
        //a.如果预下单中有用户信息 就代表用户有资格
        boolean isExist = redisTemplate.hasKey(RedisConst.PREPARE_SECKILL_USERID_SKUID +":"+userId+":"+skuId);
        if(isExist){
            /**
             * prepareSeckillOrder如果不为空 说明用户预抢单成功 具备抢购资格
             * prepareSeckillOrder如果为空 代表用户已经购买过相同的商品 因为购买成功之后会删除预订单的信息
             */
            PrepareSeckillOrder prepareSeckillOrder=(PrepareSeckillOrder)redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
            if(prepareSeckillOrder!=null){
                return RetVal.build(prepareSeckillOrder, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
            }
        }
        //如果prepareSeckillOrder如果为空走这里
        Integer orderId =(Integer) redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).get(userId);
        if(orderId!=null){
            return RetVal.build(orderId,RetValCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        return RetVal.build(null,RetValCodeEnum.SECKILL_RUN);
    }
}
