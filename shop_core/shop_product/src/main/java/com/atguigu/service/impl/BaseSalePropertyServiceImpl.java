package com.atguigu.service.impl;

import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.mapper.BaseSalePropertyMapper;
import com.atguigu.service.BaseSalePropertyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 基本销售属性表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@Service
public class BaseSalePropertyServiceImpl extends ServiceImpl<BaseSalePropertyMapper, BaseSaleProperty> implements BaseSalePropertyService {

    @Override
    public List<BaseSaleProperty> queryAllSaleProperty() {
        List<BaseSaleProperty> baseSalePropertyList = baseMapper.selectList(null);

        return baseSalePropertyList;
    }
}
