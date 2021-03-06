package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.mapper.ProductImageMapper;
import com.atguigu.service.ProductImageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 商品图片表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {

    @Override
    public List<ProductImage> queryProductImageByProductId(Long productId) {
        QueryWrapper<ProductImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id",productId);
        List<ProductImage> imageList = baseMapper.selectList(queryWrapper);
        return imageList;
    }
}
