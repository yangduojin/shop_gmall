package com.atguigu.service;

import com.atguigu.entity.ProductSpu;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
public interface ProductSpuService extends IService<ProductSpu> {

    IPage<ProductSpu> queryProductSpuByPage(Long pageNum, Long pageSize, Long category3Id);

    void saveProductSpu(ProductSpu productSpu);
}
