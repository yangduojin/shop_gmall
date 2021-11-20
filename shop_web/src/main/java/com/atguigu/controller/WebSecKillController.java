package com.atguigu.controller;

import com.atguigu.feign.SecKillFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class WebSecKillController {
    @Autowired
    private SecKillFeignClient secKillFeignClient;

    //1.秒杀商品列表显示
    @GetMapping("/seckill-index.html")
    public String seckillIndex(Model model){
        RetVal retVal = secKillFeignClient.queryAllSecKillProduct();
        model.addAttribute("list",retVal.getData());
        return "seckill/index";
    }
    //2.秒杀商品的详情页编写
    @GetMapping("/seckill-detail/{skuId}.html")
    public String seckillDetail(@PathVariable Long skuId, Model model){
        RetVal retVal = secKillFeignClient.getSecKillProductBySkuId(skuId);
        model.addAttribute("item",retVal.getData());
        return "seckill/detail";
    }
    //3.获取抢购码成功之后跳转的页面
    @GetMapping("/seckill-queue.html")
    public String seckillQueue(Long skuId,String seckillCode,Model model){
        model.addAttribute("skuId",skuId);
        model.addAttribute("seckillCode",seckillCode);
        return "seckill/queue";
    }

    //4.显示秒杀确认订单页面
    @GetMapping("/seckill-confirm.html")
    public String seckillConfirm(Model model){
        RetVal retVal = secKillFeignClient.seckillConfirm();
        if(retVal.isOk()){
            Map<String, Object> retMap =(Map<String, Object>) retVal.getData();
            model.addAllAttributes(retMap);
            return "seckill/confirm";
        }else{
            model.addAttribute("message",retVal.getMessage());
            return "seckill/confirm";
        }

    }


}