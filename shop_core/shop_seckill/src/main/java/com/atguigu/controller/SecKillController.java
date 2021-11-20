package com.atguigu.controller;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.*;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.feign.UserFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MD5;
import com.atguigu.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/seckill")
public class SecKillController {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SeckillProductService seckillProductService;

    //1.秒杀商品列表显示
    @GetMapping("/queryAllSecKillProduct")
    public RetVal queryAllSecKillProduct(){
        List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();
        return RetVal.ok(seckillProductList);
    }

    //2.秒杀商品的详情页编写
    @GetMapping("/getSecKillProductBySkuId/{skuId}")
    public RetVal getSecKillProductBySkuId(@PathVariable Long skuId){
        SeckillProduct seckillProduct=seckillProductService.getSecKillProductBySkuId(skuId);
        return RetVal.ok(seckillProduct);
    }

    //3.生成抢购码  防止用户直接跳过商品详情页面进入秒杀地址
    @GetMapping("/generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request){
        //a.判断用户是否登录
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isNotEmpty(userId)){
            //b.从缓存中查询秒杀的商品
            SeckillProduct secKillProduct = seckillProductService.getSecKillProductBySkuId(skuId);
            //c.当前时间如果在秒杀时间范围内 则生成抢购码
            Date currentTime = new Date();
            if(DateUtil.dateCompare(secKillProduct.getStartTime(),currentTime)&&
            DateUtil.dateCompare(currentTime,secKillProduct.getEndTime())){
                //d.抢购码--对用户id进行加密
                String secKillCode = MD5.encrypt(userId);
                return RetVal.ok(secKillCode);
            }
        }
        return RetVal.fail().message("获取抢购码失败请先登录");
    }

    //4.秒杀商品预下单前奏
    @PostMapping("/prepareSeckill/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId,String seckillCode, HttpServletRequest request){
        //a.抢购码是否正确
        String userId = AuthContextHolder.getUserId(request);
        if(!MD5.encrypt(userId).equals(seckillCode)){
            //预下单用户抢购码不合法 报异常
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }
        //b.秒杀商品是否可以进行秒杀 状态为是否为1
        String state =(String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId);
        if(StringUtils.isEmpty(state)){
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }
        //可以进行秒杀
        if(RedisConst.CAN_SECKILL.equals(state)){
            //c.把秒杀用户id和商品skuId发送给rabbitmq
            UserSeckillSkuInfo userSeckillSkuInfo = new UserSeckillSkuInfo();
            userSeckillSkuInfo.setUserId(userId);
            userSeckillSkuInfo.setSkuId(skuId);
            rabbitTemplate.convertAndSend(MqConst.PREPARE_SECKILL_EXCHANGE,MqConst.PREPARE_SECKILL_ROUTE_KEY,userSeckillSkuInfo);
        }else{
            //秒杀商品已售罄
            return RetVal.build(null,RetValCodeEnum.SECKILL_FINISH);
        }
        return RetVal.ok();
    }

    //5.判断用户是否有秒杀资格
    @GetMapping("/hasQualified/{skuId}")
    public RetVal hasQualified(@PathVariable Long skuId,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        return seckillProductService.hasQualified(skuId,userId);
    }

    //6.秒杀确认订单数据信息
    @GetMapping("seckillConfirm")
    public RetVal seckillConfirm(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //a.从redis里面拿到预订单信息
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder)redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if(prepareSeckillOrder==null){
            return RetVal.fail().message("请求非法");
        }
        //b.获取用户的收货地址
        List<UserAddress> userAddressList = userFeignClient.queryAddressByUserId(userId);
        //c.用户秒杀到的商品信息
        SeckillProduct seckillProduct = prepareSeckillOrder.getSeckillProduct();
        //d.把秒杀商品订单信息转换为订单详情对象信息
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillProduct.getSkuId());
        orderDetail.setSkuName(seckillProduct.getSkuName());
        orderDetail.setImgUrl(seckillProduct.getSkuDefaultImg());
        orderDetail.setSkuNum(prepareSeckillOrder.getBuyNum()+"");
        orderDetail.setOrderPrice(seckillProduct.getCostPrice());
        List<OrderDetail> orderDetailList = new ArrayList<>();
        orderDetailList.add(orderDetail);
        Map<String, Object> retMap = new HashMap<>();
        //地址信息
        retMap.put("userAddressList",userAddressList);
        //送货清单改造版
        retMap.put("orderDetailList",orderDetailList);
        //订单总金额
        retMap.put("totalMoney",seckillProduct.getCostPrice());
        return RetVal.ok(retMap);
    }

    //7.提交秒杀订单信息
    @PostMapping("/submitSecKillOrder")
    public RetVal submitSecKillOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //a.判断用户是否有预下单信息
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder)redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if(prepareSeckillOrder==null){
            return RetVal.fail().message("请求非法");
        }
        //b.远程调用shop-order下订单
        Long orderId = orderFeignClient.saveOrderAndDetail(orderInfo);
        if(orderId==null){
            return RetVal.fail().message("下单失败");
        }
        //c.删除redis里面的临时订单信息
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).delete(userId);
        //d.在redis当中把用户购买的商品信息 放到Redis里面 用于判断用户是否已经购买
        redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).put(userId,orderId);
        return RetVal.ok(orderId);
    }

}
