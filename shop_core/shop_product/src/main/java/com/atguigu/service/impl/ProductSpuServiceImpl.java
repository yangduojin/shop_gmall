package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.mapper.ProductSpuMapper;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {

    @Autowired
    ProductSalePropertyKeyService productSalePropertyKeyService;

    @Autowired
    ProductSalePropertyValueService productSalePropertyValueService;

    @Autowired
    ProductImageService productImageService;

    @Override
    public IPage<ProductSpu> queryProductSpuByPage(Long pageNum, Long pageSize, Long category3Id) {

        Page<ProductSpu> productSpuPage = new Page<>(pageNum,pageSize);
        QueryWrapper<ProductSpu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        IPage<ProductSpu> productSpuIPage = baseMapper.selectPage(productSpuPage, queryWrapper);
        return productSpuIPage;
    }

    @Transactional
    @Override
    public void saveProductSpu(ProductSpu productSpu) {

        if (productSpu.getId()!= null){
            baseMapper.updateById(productSpu);
        }else {
            baseMapper.insert(productSpu);
        }

        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        List<ProductSalePropertyValue> productSalePropertyValues = new ArrayList<>();
        for (ProductSalePropertyKey productSalePropertyKey : salePropertyKeyList) {
            productSalePropertyKey.setProductId(productSpu.getId());
            List<ProductSalePropertyValue> salePropertyValueList = productSalePropertyKey.getSalePropertyValueList();
            for (ProductSalePropertyValue productSalePropertyValue : salePropertyValueList) {
                productSalePropertyValue.setSalePropertyKeyName(productSalePropertyKey.getSalePropertyKeyName());
                productSalePropertyValue.setProductId(productSpu.getId());
                productSalePropertyValues.add(productSalePropertyValue);
            }
        }
        productSalePropertyValueService.saveBatch(productSalePropertyValues);

        productSalePropertyKeyService.saveBatch(salePropertyKeyList);

        List<ProductImage> productImageList = productSpu.getProductImageList();
        for (ProductImage productImage : productImageList) {
            productImage.setProductId(productSpu.getId());
        }
        productImageService.saveBatch(productImageList);
    }



}