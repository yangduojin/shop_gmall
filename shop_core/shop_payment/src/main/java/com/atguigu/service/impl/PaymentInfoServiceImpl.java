package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.feign.OrderFeignClient;
import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public String createQrCode(Long orderId) throws Exception {
        //1.根据订单id查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //2.保存支付信息
        savePaymentInfo(orderInfo);
        //3.调用支付宝返回二维码的工具类
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //设置支付成功之后的异步通知
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //设置支付成功之后的同步通知
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();
        //商户订单号
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        //订单总金额
        bizContent.put("total_amount", orderInfo.getTotalMoney());
        //订单标题
        bizContent.put("subject", "天气凉了 买个暖宝宝");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if(response.isSuccess()){
            return response.getBody();
        } else {
            System.out.println("调用失败");
        }
        return null;
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        //判断数据库中是否存在该订单的支付信息
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        wrapper.eq("payment_type", PaymentType.ALIPAY.name());
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void updatePaymentInfo(Map<String, String> aliParamMap) {
        String outTradeNo = aliParamMap.get("out_trade_no");
        PaymentInfo paymentInfo=getPaymentInfo(outTradeNo);
        //修改支付订单信息
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(aliParamMap.toString());
        //获取支付宝那边的交易号
        String tradeNo = aliParamMap.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        baseMapper.updateById(paymentInfo);
        //发一条消息给shop-order修改订单信息
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE,MqConst.PAY_ORDER_ROUTE_KEY,paymentInfo.getOrderId());
    }

    @Override
    public boolean refund(Long orderId) throws Exception {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", orderInfo.getTotalMoney());
        bizContent.put("refund_reason", "已经炸了 漏水啦 诈尸了");
        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            //如果退款成功修改支付订单状态为已关闭
            PaymentInfo paymentInfo = getPaymentInfo(orderInfo.getOutTradeNo());
            paymentInfo.setPaymentStatus(ProcessStatus.CLOSED.name());
            baseMapper.updateById(paymentInfo);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean queryAlipayTrade(Long orderId) throws Exception {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean closeAlipayTrade(Long orderId) throws Exception {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        }else{
            return false;
        }

    }

    private void savePaymentInfo(OrderInfo orderInfo) {
        //判断数据库中是否存在该订单的支付信息
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderInfo.getId());
        wrapper.eq("payment_type", PaymentType.ALIPAY.name());
        Integer count = baseMapper.selectCount(wrapper);
        if(count>0){
            return;
        }
        //没有就要创建一个支付订单
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId()+"");
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setPaymentMoney(orderInfo.getTotalMoney());
        paymentInfo.setPaymentContent(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        baseMapper.insert(paymentInfo);
    }
}
