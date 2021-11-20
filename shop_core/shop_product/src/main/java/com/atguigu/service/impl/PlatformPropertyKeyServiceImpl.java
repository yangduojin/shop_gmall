package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.mapper.PlatformPropertyKeyMapper;
import com.atguigu.mapper.PlatformPropertyValueMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 属性表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-27
 */
@Service
public class PlatformPropertyKeyServiceImpl extends ServiceImpl<PlatformPropertyKeyMapper, PlatformPropertyKey> implements PlatformPropertyKeyService {

    @Autowired
    private PlatformPropertyValueService propertyValueService;

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id) {
        List<PlatformPropertyKey> platformPropertyKeyList = baseMapper.getPlatformPropertyByCategoryId(category1Id,category2Id,category3Id);

        //for多次io数据库，数据库压力大。  不如多表查询 一次出结果
//        for (PlatformPropertyKey platformPropertyKey : platformPropertyKeyList) {
//            List<PlatformPropertyValue> platformPropertyValueList = propertyValueService.getPlatformPropertyByPropertyKeyId(platformPropertyKey.getId());
//            platformPropertyKey.setPropertyValueList(platformPropertyValueList);
//        }
        return platformPropertyKeyList;
    }

    @Override
    public void savePlatformProperty(PlatformPropertyKey platformPropertyKey) {
        if(platformPropertyKey.getId()!=null){
            baseMapper.updateById(platformPropertyKey);
            QueryWrapper<PlatformPropertyValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("property_key_id",platformPropertyKey.getId());
            propertyValueService.remove(queryWrapper);
        }else{
            baseMapper.insert(platformPropertyKey);
        }

        Long keyId = platformPropertyKey.getId();

        List<PlatformPropertyValue> propertyValueList = platformPropertyKey.getPropertyValueList();
        for (PlatformPropertyValue platformPropertyValue : propertyValueList) {
            platformPropertyValue.setPropertyKeyId(keyId);
        }
        propertyValueService.saveBatch(propertyValueList);
    }

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId) {
        return baseMapper.getPlatformPropertyBySkuId(skuId);
    }
}
