package com.atguigu.service.impl;

import com.atguigu.constant.RedisConst;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.mapper.ProductSalePropertyKeyMapper;
import com.atguigu.mapper.SkuSalePropertyValueMapper;
import com.atguigu.myAnnotation.ShopCache;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SkuDetailServiceImpl implements SkuDetailService {

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuDetailService skuDetailService;

    @Autowired
    SkuImageService skuImageService;

    @Autowired
    ProductSalePropertyKeyMapper productSalePropertyKeyMapper;

    @Autowired
    SkuSalePropertyValueMapper skuSalePropertyValueMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public SkuInfo getSkuInfo(long skuId) {
//        SkuInfo skuInfo = ((类) AopContext.currentProxy()).要代理的方法(该方法需要的参数);
        SkuInfo skuInfo = skuDetailService.getSkuInfoFromRedission(skuId);
//        SkuInfo skuInfo = getSkuInfoFromRedis(skuId);
        return skuInfo;
    }

    @ShopCache(prefix = "sku:info:")
    @Override
    public SkuInfo getSkuInfoFromRedission(long skuId) {// redis
        SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);
        return skuInfoFromDB;
    }

    public SkuInfo getSkuInfoFromRedis(Long skuId) { // redis & lua
        String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
        if(skuInfo == null){
            String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
            String uuid = UUID.randomUUID().toString();
            Boolean acquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 3, TimeUnit.SECONDS);
            if(acquireLock){
                SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);
                if(skuInfoFromDB == null){
                    SkuInfo emptySkuInfo = new SkuInfo();
                    redisTemplate.opsForValue().set(skuKey, emptySkuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return emptySkuInfo;
                }
                redisTemplate.opsForValue().set(skuKey, skuInfoFromDB, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptText(luaScript);
                redisScript.setResultType(Long.class);
                redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);

                    return skuInfoFromDB;
            }else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuInfo(skuId);
            }
        }else {
            return skuInfo;
        }
    }

    private SkuInfo getSkuInfoFromDB(long skuId) {
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        if(skuInfo!=null) {
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<SkuImage>().eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageService.list(wrapper);
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    public List<ProductSalePropertyKey> getSkuSalePropertyKeyAndValue(long productId, long skuId) {

        return productSalePropertyKeyMapper.getSkuSalePropertyKeyAndValue(productId,skuId);
    }

    @ShopCache(prefix = "sku:idMapping:")
    @Override
    public Map getSkuSalePropertyValueId(long productId) {
        HashMap<Object, Object> resultMap = new HashMap<>();
        List<Map> skuSalePropertyValueId = skuSalePropertyValueMapper.getSkuSalePropertyValueId(productId);
        if( !CollectionUtils.isEmpty(skuSalePropertyValueId)){
            for (Map skuMap : skuSalePropertyValueId) {
                resultMap.put(skuMap.get("sale_property_value_id"),skuMap.get("sku_id"));
            }
        }
        return resultMap;
    }
}
