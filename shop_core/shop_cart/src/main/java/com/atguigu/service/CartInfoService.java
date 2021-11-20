package com.atguigu.service;

import com.atguigu.entity.CartInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
public interface CartInfoService extends IService<CartInfo> {

    void addToCart(Long skuId, String userId, Integer skuNum);

    List<CartInfo> getCartList(String userId, String userTempId);

    List<CartInfo> queryFromDbToRedis(String userTempId);

    void deleteCart(String userId, Long skuId);


    void checkCart(String userId, Long skuId, Integer isChecked);

    List<CartInfo> getSelectedProduct(String userId);

    List<Long> getSkusPrice(Map<Long, BigDecimal> map, Long userId);
}
