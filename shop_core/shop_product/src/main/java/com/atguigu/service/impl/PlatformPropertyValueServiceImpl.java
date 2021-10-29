package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.mapper.PlatformPropertyValueMapper;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 属性值表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-27
 */
@Service
public class PlatformPropertyValueServiceImpl extends ServiceImpl<PlatformPropertyValueMapper, PlatformPropertyValue> implements PlatformPropertyValueService {

    @Override
    public List<PlatformPropertyValue> getPlatformPropertyByPropertyKeyId(Long propertyKeyId) {

        //根据PropertyKeyId查询PlatformPropertyValue,但是多次io数据库 不好，所以连接查询 一次返回结果好
        QueryWrapper<PlatformPropertyValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("property_key_id",propertyKeyId);

        List<PlatformPropertyValue> platformPropertyValueList = baseMapper.selectList(queryWrapper);
        return platformPropertyValueList;
    }
}
