package com.atguigu.service;

import com.atguigu.entity.CartInfo;

public interface AsyncCartInfoService {
    void insertCartInfo(CartInfo existCartInfo);

    void updateCartInfo(CartInfo existCartInfo);

    void deleteCartInfo(String userId, Long skuId);

    void checkCart(String userId, Long skuId, Integer isChecked);
}
