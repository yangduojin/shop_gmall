package com.atguigu.service.impl;

import com.atguigu.entity.CartInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.AsyncCartInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Async
@Service
public class AsyncCartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements AsyncCartInfoService {

    @Override
    public void insertCartInfo(CartInfo existCartInfo) {
        baseMapper.insert(existCartInfo);
    }

    @Override
    public void updateCartInfo(CartInfo existCartInfo) {
        baseMapper.updateById(existCartInfo);
    }

    @Override
    public void deleteCartInfo(String oneOfUserId, Long skuId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(oneOfUserId)){
            wrapper.eq("user_id",oneOfUserId);
        }
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id",skuId);
        }
        baseMapper.delete(wrapper);
    }

    @Override
    public void checkCart(String userId, Long skuId, Integer isChecked) {
    /**
     * 根据条件进行修改
     * update cart_info set is_checked=1 where user_id =xxx and sku_id=xxx
     */
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        wrapper.eq("sku_id",skuId);
        baseMapper.update(cartInfo,wrapper);
    }


}
