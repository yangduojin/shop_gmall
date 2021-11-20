package com.atguigu.service.impl;

import com.atguigu.entity.OrderInfo;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

}
