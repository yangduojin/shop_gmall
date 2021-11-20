package com.atguigu.service.impl;

import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {

    @Autowired
    SkuPlatformPropertyValueService skuPlatformPropertyValueService;

    @Autowired
    SkuSalePropertyValueService skuSalePropertyValueService;

    @Autowired
    SkuImageService skuImageService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        baseMapper.insert(skuInfo);
        Long skuId = skuInfo.getId();
        Long productId = skuInfo.getProductId();
        List<SkuPlatformPropertyValue> skuPlatformPropertyValueList = skuInfo.getSkuPlatformPropertyValueList();
        if(!CollectionUtils.isEmpty(skuPlatformPropertyValueList)){
            skuPlatformPropertyValueList.forEach(skuPlatformPropertyValue -> skuPlatformPropertyValue.setSkuId(skuId));
            skuPlatformPropertyValueService.saveBatch(skuPlatformPropertyValueList);
        }

        List<SkuSalePropertyValue> skuSalePropertyValueList = skuInfo.getSkuSalePropertyValueList();
        if(!CollectionUtils.isEmpty(skuSalePropertyValueList)){
            skuSalePropertyValueList.forEach(skuSalePropertyValue -> {
                skuSalePropertyValue.setSkuId(skuId);
                skuSalePropertyValue.setProductId(productId);
            });
            skuSalePropertyValueService.saveBatch(skuSalePropertyValueList);
        }

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(!CollectionUtils.isEmpty(skuImageList)){
            skuImageList.forEach(skuImage -> {skuImage.setSkuId(skuId);});
            skuImageService.saveBatch(skuImageList);
        }
    }

    @Override
    public IPage<SkuInfo> querySkuInfoByPage(Long pageNum, Long pageSize) {
        Page<SkuInfo> skuInfoPage = new Page<>(pageNum,pageSize);
        IPage<SkuInfo> skuInfoIPage = baseMapper.selectPage(skuInfoPage, null);
        return skuInfoIPage;
    }

//    @Override
//    public void onSale(Long skuInfoId) {
//        SkuInfo skuInfo = baseMapper.selectById(skuInfoId);
//        skuInfo.setIsSale(1);
//        baseMapper.updateById(skuInfo);
//    }
//
//    @Override
//    public void offSale(Long skuInfoId) {
//        SkuInfo skuInfo = baseMapper.selectById(skuInfoId);
//        skuInfo.setIsSale(0);
//        baseMapper.updateById(skuInfo);
//    }
}
