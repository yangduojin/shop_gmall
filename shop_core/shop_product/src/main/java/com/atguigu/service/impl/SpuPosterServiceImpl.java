package com.atguigu.service.impl;

import com.atguigu.entity.SpuPoster;
import com.atguigu.mapper.SpuPosterMapper;
import com.atguigu.service.SpuPosterService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 商品海报表 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@Service
public class SpuPosterServiceImpl extends ServiceImpl<SpuPosterMapper, SpuPoster> implements SpuPosterService {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    //@Override
    public synchronized void testRedis01() {
        String num = (String) redisTemplate.opsForValue().get("num");
        if(StringUtils.isEmpty(num)){
            redisTemplate.opsForValue().set("num","1");
            return;
        }
        int newNum = Integer.parseInt(num);
        redisTemplate.opsForValue().set("num",String.valueOf(++newNum));
    }

//    @Override
    public void testRedis02() {
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent("lock", "111");
        if(aBoolean){
            String num = (String) redisTemplate.opsForValue().get("num");
            if(StringUtils.isEmpty(num)){
                num = "1";
                redisTemplate.opsForValue().set("num",num);
                redisTemplate.delete("lock");
                return;
            }
            int newNum = Integer.parseInt(num);
            redisTemplate.opsForValue().set("num",String.valueOf(++newNum));
            redisTemplate.delete("lock");
        }else {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testRedis();
        }
    }


    // uuid 有可能在equals后，未删除key之前  时线程1的key到期，线程1删除了线程2的key。判断和删除不具有原子性，导致容易出问题
    // @Override
    public void testRedis3() {
        String uuid = UUID.randomUUID().toString();
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent("lock", "uuid",3, TimeUnit.SECONDS);
        if(aBoolean){
            String num = (String) redisTemplate.opsForValue().get("num");
            if(StringUtils.isEmpty(num)){
                num = "1";
                redisTemplate.opsForValue().set("num",num);
                redisTemplate.delete("lock");
                return;
            }
            int newNum = Integer.parseInt(num);
            redisTemplate.opsForValue().set("num",String.valueOf(++newNum));
            String redisUuid = (String) redisTemplate.opsForValue().get(num);
            if(uuid.equals(redisUuid)){
            redisTemplate.delete("lock");
            }
        }else {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testRedis();
        }
    }


    // @Override
    public void testRedis4() {
        String uuid = UUID.randomUUID().toString();
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent("lock", "uuid",3, TimeUnit.SECONDS);
        if(aBoolean){
            String num = (String) redisTemplate.opsForValue().get("num");
            if(StringUtils.isEmpty(num)){
                num = "1";
                redisTemplate.opsForValue().set("num",num);
                redisTemplate.delete("lock");
                return;
            }
            int newNum = Integer.parseInt(num);
            redisTemplate.opsForValue().set("num",String.valueOf(++newNum));

//            String redisUuid = (String) redisTemplate.opsForValue().get(num);
//            if(uuid.equals(redisUuid)){
//            redisTemplate.delete("lock");
//            }

            // 这一段是对上面的三行代码的强化，增强原子性，判断和删除在同一个时间片里执行
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);// 执行成功后返回的类型值
            redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);

        }else {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testRedis();
        }
    }


    @Override
    public void testRedis() {//redission
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        }
        lock.unlock();
    }

    public void testRedission22() {//redission
        RLock lock = redissonClient.getLock("lock");
        lock.lock(10, TimeUnit.SECONDS);// 看门狗 超时自动释放
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        }
        lock.unlock();
    }

    public void testRedission33() {//redission
        RLock lock = redissonClient.getLock("lock");
        try {
            /**
             * 尝试获取锁的最大等待时间，超过这个值，则认为获取锁失败
             * 锁的持有时间,超过这个时间锁会自动失效（值应设置为大于业务处理的时间，确保在锁有效期内业务能处理完）
             */
            boolean acquireLock = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if(acquireLock){
                String value = (String) redisTemplate.opsForValue().get("num");
                if (StringUtils.isEmpty(value)) {
                    redisTemplate.opsForValue().set("num", "1");
                } else {
                    int num = Integer.parseInt(value);
                    redisTemplate.opsForValue().set("num", String.valueOf(++num));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

}
