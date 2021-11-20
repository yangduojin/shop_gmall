package com.atguigu.service;

import com.atguigu.entity.ProductImage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 商品图片表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
public interface ProductImageService extends IService<ProductImage> {

    List<ProductImage> queryProductImageByProductId(Long productId);
}
