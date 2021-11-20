package com.atguigu.myAnnotation;

import com.atguigu.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class ShopCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.myAnnotation.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint target){
        Object retVal = null;
        MethodSignature signature = (MethodSignature) target.getSignature();
        ShopCache shopCache = signature.getMethod().getAnnotation(ShopCache.class);
        String prefix = shopCache.prefix();
        Object[] methodParams = target.getArgs();
        String skuKey = prefix + Arrays.asList(methodParams).toString();

        retVal =  redisTemplate.opsForValue().get(skuKey);
        if(retVal == null){
            String lockKey =  skuKey + RedisConst.SKULOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            try {
                boolean acquireLock = lock.tryLock(RedisConst.WAITTIN_GET_LOCK_TIME, RedisConst.LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
                if(acquireLock){

                    retVal = target.proceed();

                    if(retVal == null){
                        Object emptySkuInfo = new Object();
                        redisTemplate.opsForValue().set(skuKey, emptySkuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return emptySkuInfo;
                    }
                    redisTemplate.opsForValue().set(skuKey, retVal, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                }else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return cacheAroundAdvice(target);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                lock.unlock();
            }
            return retVal;
        }else {
            return retVal;
        }
    }

}
