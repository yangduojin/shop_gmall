package com.atguigu.service;

import com.atguigu.entity.BaseSaleProperty;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 基本销售属性表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
public interface BaseSalePropertyService extends IService<BaseSaleProperty> {

    List<BaseSaleProperty> queryAllSaleProperty();
}
