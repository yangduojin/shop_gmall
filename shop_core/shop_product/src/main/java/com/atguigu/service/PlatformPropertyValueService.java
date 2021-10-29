package com.atguigu.service;

import com.atguigu.entity.PlatformPropertyValue;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 属性值表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-10-27
 */
public interface PlatformPropertyValueService extends IService<PlatformPropertyValue> {

    List<PlatformPropertyValue> getPlatformPropertyByPropertyKeyId(Long propertyKeyId);
}
