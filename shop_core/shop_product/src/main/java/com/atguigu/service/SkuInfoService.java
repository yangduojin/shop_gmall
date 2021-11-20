package com.atguigu.service;

import com.atguigu.entity.SkuInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 库存单元表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
public interface SkuInfoService extends IService<SkuInfo> {

    void saveSkuInfo(SkuInfo skuInfo);

    IPage<SkuInfo> querySkuInfoByPage(Long pageNum, Long pageSize);

//    void onSale(Long skuInfoId);
//
//    void offSale(Long skuInfoId);
}
