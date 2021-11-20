package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2021-11-17
 */
public interface SeckillProductService extends IService<SeckillProduct> {

    SeckillProduct getSecKillProductBySkuId(Long skuId);

    void prepareSeckill(UserSeckillSkuInfo userSeckillSkuInfo);

    RetVal hasQualified(Long skuId, String userId);
}
