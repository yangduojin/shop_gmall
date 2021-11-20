package com.atguigu.service.impl;


import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuInfo;
import com.atguigu.feign.ProductFeignClient;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.AsyncCartInfoService;
import com.atguigu.service.CartInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-11-09
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private AsyncCartInfoService asyncCartInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId);
        wrapper.eq("user_id", userId);
        CartInfo existCartInfo = baseMapper.selectOne(wrapper);
        if (existCartInfo == null) {
            existCartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            existCartInfo.setUserId(userId);
            existCartInfo.setSkuId(skuId);
            existCartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
            existCartInfo.setSkuNum(skuNum);
            existCartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            existCartInfo.setSkuName(skuInfo.getSkuName());
            existCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            existCartInfo.setIsChecked(1);
            // baseMapper.insert(existCartInfo); 异步优化
            asyncCartInfoService.insertCartInfo(existCartInfo);
        }else{
            existCartInfo.setSkuNum(existCartInfo.getSkuNum() + skuNum);
            existCartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
            asyncCartInfoService.updateCartInfo(existCartInfo);
        }
        String cartKey = getUserCartKey(userId);
        //把信息也存一份到redis缓存中
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(), existCartInfo);
        //设置购物车过期时间
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //1.当用户没有登录
        if(!StringUtils.isEmpty(userTempId)){
            cartInfoList=getUserCartList(userTempId);
        }
        //2.如果用户登录了
        if(!StringUtils.isEmpty(userId)){
            //查询未登录购物车信息
            List<CartInfo> noLoginCartInfoList =getUserCartList(userTempId);
            if(!CollectionUtils.isEmpty(noLoginCartInfoList)){
                //合并未登录和已登录购物车信息
                cartInfoList=mergeCartInfoList(noLoginCartInfoList,userId);
                //合并之后删除未登录信息
                deleteNoLoginCartInfoList(userTempId);
            }else{
                cartInfoList=queryFromDbToRedis(userId);
            }
        }
        return cartInfoList;
    }

    //设置购物车的过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
    //拼接购物车的key
    private String getUserCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }

    public List<CartInfo> getUserCartList(String oneOfUserId){
        List<CartInfo> cartInfoList = new ArrayList<>();
        if(StringUtils.isEmpty(oneOfUserId)){
            return cartInfoList;
        }
        //从数据库中查询数据到数据库
        cartInfoList=queryFromDbToRedis(oneOfUserId);
        return cartInfoList;
    }

    @Override
    public List<CartInfo> queryFromDbToRedis(String userTempId) {
        //根据临时id查询购物车信息
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userTempId);
        List<CartInfo> dbCartInfoList = baseMapper.selectList(wrapper);
        //如果数据库中不存在用户添加的购物车信息
        if(CollectionUtils.isEmpty(dbCartInfoList)){
            return dbCartInfoList;
        }
        String userCartKey = getUserCartKey(userTempId);
        HashMap<String, CartInfo> cartInfoMap = new HashMap<>();
        for (CartInfo cartInfo : dbCartInfoList) {
            //方式一
            //redisTemplate.opsForHash().put(userCartKey,cartInfo.getSkuId().toString(),cartInfo);
            //更新实时价格
            cartInfo.setCartPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            cartInfoMap.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        redisTemplate.opsForHash().putAll(userCartKey,cartInfoMap);
        //设置过期时间
        setCartKeyExpire(userCartKey);
        return dbCartInfoList;
    }

    private void deleteNoLoginCartInfoList(String userTempId) {
        //a.删除数据库里面的购物车信息
        asyncCartInfoService.deleteCartInfo(userTempId, null);

        //b.删除redis里面的信息
        String userCartKey = getUserCartKey(userTempId);
        Boolean flag = redisTemplate.hasKey(userCartKey);
        if(flag){
            redisTemplate.delete(userCartKey);
        }
    }

    private List<CartInfo> mergeCartInfoList(List<CartInfo> noLoginCartInfoList, String userId) {
        //a.取出登录了和未登录的购物车信息
        List<CartInfo> loginUserCartList = getUserCartList(userId);
        //把其中一个集合转换为map结构
        Map<Long, CartInfo> loginCartInfoMap = loginUserCartList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            Long skuId = noLoginCartInfo.getSkuId();
            //b.对比skuId是否相同
            if(loginCartInfoMap.containsKey(skuId)){
                //sku数量进行相加操作
                CartInfo loginCartInfo = loginCartInfoMap.get(skuId);
                loginCartInfo.setSkuNum(loginCartInfo.getSkuNum()+noLoginCartInfo.getSkuNum());
                //当未登录的时候如果商品是勾选状态 合并之后也要进行勾选
                if(noLoginCartInfo.getIsChecked()==1){
                    loginCartInfo.setIsChecked(1);
                }
                //更新数据库信息
                //baseMapper.updateById(loginCartInfo);
                asyncCartInfoService.updateCartInfo(loginCartInfo);

            }else{
                //把临时用户id改为登录之后的userId
                noLoginCartInfo.setUserId(userId);
                //baseMapper.updateById(noLoginCartInfo);
                asyncCartInfoService.updateCartInfo(noLoginCartInfo);
            }
        }
        //c.合并之后需要更新一下缓存
        List<CartInfo> cartInfoList = queryFromDbToRedis(userId);
        return cartInfoList;
    }

    @Override
    public void deleteCart(String userId, Long skuId) {
        //a.删除redis的商品信息
        String userCartKey = getUserCartKey(userId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(userCartKey);
        //看是否有该skuid所对于的商品信息
        if(boundHashOps.hasKey(skuId.toString())){
            //删除数据
            boundHashOps.delete(skuId.toString());
        }
        //b.在数据库中需要删除商品信息
        asyncCartInfoService.deleteCartInfo(userId,skuId);
    }

    @Override
    public void checkCart(String userId, Long skuId, Integer isChecked) {
        String userCartKey = getUserCartKey(userId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(userCartKey);

        if (boundHashOps.hasKey(skuId.toString())) {
            // todo 问老师 为什么是这样 而不是 jsonobject转换
            CartInfo redisCartInfo = (CartInfo)boundHashOps.get(skuId.toString());
            redisCartInfo.setIsChecked(isChecked);
            //更新到redis当中
            boundHashOps.put(skuId.toString(),redisCartInfo);
            //设置过期时间
            setCartKeyExpire(userCartKey);
        }
        //b.在数据库中需要修改选中或者未选中状态
        asyncCartInfoService.checkCart(userId,skuId,isChecked);
    }

    @Override
    public List<CartInfo> getSelectedProduct(String userId) {
        //创建一个新的集合 装勾选了的商品信息
        List<CartInfo> retCartInfoList = new ArrayList<>();
        String userCartKey = getUserCartKey(userId);
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(userCartKey);
        if(!CollectionUtils.isEmpty(cartInfoList)){
            retCartInfoList =  cartInfoList.stream().filter(item -> item.getIsChecked() == 1).collect(Collectors.toList());
        }
        return retCartInfoList;
    }

    @Override
    public List<Long> getSkusPrice(Map<Long, BigDecimal> map, Long userId) {
        Set<Long> skus = map.keySet();
        List<CartInfo> cartInfos = baseMapper.selectList(new QueryWrapper<CartInfo>().eq("user_id", userId).in("sku_id", skus));
        List<Long> priceChangeList = cartInfos.stream().filter(item -> {
            BigDecimal mapPrice = map.get(item.getSkuId());
            return mapPrice.compareTo(item.getCartPrice()) != 0;
        }).map(item -> item.getSkuId()).collect(Collectors.toList());

        return priceChangeList;
    }

}
