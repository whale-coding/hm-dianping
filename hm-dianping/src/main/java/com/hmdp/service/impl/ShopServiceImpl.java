package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 根据id查询商铺信息
    @Override
    public Result queryById(Long id) {
        /*
         // 以下代码是缓存穿透时的逻辑。
        String shop_cache_key = CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shop_cache_key);
        // 2、判断缓存中是否有商铺信息
        if (StrUtil.isNotBlank(shopJson)){
            // 3、缓存中存在商铺信息，直接返回
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);  // 将Json转换为对象
            return Result.ok(shop);
        }
        //**避免缓存穿透问题**
        // 判断命中的是否是空值
        if (shopJson != null){
            // 不为null，是空值，返回错误信息
            return Result.fail("店铺不存在！");
        }
        //**避免缓存穿透问题**

        // 4、缓存中不存在商铺信息,根据id查询数据库中的商铺信息
        Shop shop = getById(id);
        // 5、判断数据库中是否有商铺信息
        if (shop == null){
            //**避免缓存穿透问题**
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(shop_cache_key, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
            //**避免缓存穿透问题**
            // 不存在，返回错误信息
            return Result.fail("店铺不存在！");
        }
        // 6、数据库中存在商铺信息，将其写入redis缓存中
        // stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop));
        // 给缓存添加超时时间，方便后续缓存的更新
        stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7、返回数据库中的商铺信息
        return Result.ok(shop);

        */

        // 缓存穿透问题的解决
        // Shop shop = queryWithPassThrough(id);

        // 缓存击穿问题的解决:基于互斥锁
        // Shop shop = queryWithMutex(id);

        // 缓存击穿问题的解决：基于逻辑过期
        Shop shop = queryWithLogicalExpire(id);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);
    }

    // 缓存穿透解决办法的逻辑封装,基于缓存空对象
    public Shop queryWithPassThrough(Long id){
        String shop_cache_key = CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shop_cache_key);
        // 2、判断缓存中是否有商铺信息
        if (StrUtil.isNotBlank(shopJson)){
            // 3、缓存中存在商铺信息，直接返回
            // 将Json转换为对象
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        /*避免缓存穿透问题*/
        // 判断命中的是否是空值
        if (shopJson != null){
            // 不为null，是空值，返回错误信息
            return null;
        }
        /*避免缓存穿透问题*/

        // 4、缓存中不存在商铺信息,根据id查询数据库中的商铺信息
        Shop shop = getById(id);
        // 5、判断数据库中是否有商铺信息
        if (shop == null){
            /*避免缓存穿透问题*/
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(shop_cache_key, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
            /*避免缓存穿透问题*/
            // 不存在，返回错误信息
            return null;
        }
        // 6、数据库中存在商铺信息，将其写入redis缓存中
        // stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop));
        // 给缓存添加超时时间，方便后续缓存的更新
        stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7、返回数据库中的商铺信息
        return shop;
    }

    // 缓存击穿解决办法的逻辑封装,基于互斥锁解决。
    public Shop queryWithMutex(Long id){
        String shop_cache_key = CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shop_cache_key);
        // 2、判断缓存中是否有商铺信息
        if (StrUtil.isNotBlank(shopJson)) {
            // 3、缓存中存在商铺信息，直接返回
            // 将Json转换为对象
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        /*避免缓存穿透问题*/
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 不为null，是空值，返回错误信息
            return null;
        }
        // TODO: 实现缓存重建
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            // TODO: 1、获取互斥锁
            boolean isLock = tryLock(lockKey);
            // TODO: 2、判断锁是否获取成功
            if (!isLock) {
                // TODO: 3、获取锁失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // TODO: 4、获取锁成功，则根据id查询数据库
            // 4、缓存中不存在商铺信息,根据id查询数据库中的商铺信息
            shop = getById(id);
            Thread.sleep(200);  // 模拟缓存重建的延时
            // 5、判断数据库中是否有商铺信息
            if (shop == null) {
                // 将空值写入redis，避免缓存穿透问题
                stringRedisTemplate.opsForValue().set(shop_cache_key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 不存在，返回错误信息
                return null;
            }
            // 6、数据库中存在商铺信息，将其写入redis缓存中
            stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // TODO: 5、释放互斥锁
            unlock(lockKey);
        }
        // 7、返回数据库中的商铺信息
        return shop;
    }

    // 缓存击穿解决办法的逻辑封装,基于逻辑过期解决。
    public Shop queryWithLogicalExpire(Long id){
        String shop_cache_key = CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shop_cache_key);
        // 2、判断缓存中是否有商铺信息
        if (StrUtil.isBlank(shopJson)){
            // 3、不存在，直接返回null
            return null;
        }
        // TODO: 1、缓存命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson,RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // TODO: 2、判断逻辑过期时间是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // TODO: 2.1、未过期，直接返回店铺信息
            return shop;
        }
        // TODO: 2.2、已过期，需要缓存重建
        // TODO: 3、缓存重建
        // TODO: 3.1、获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // TODO: 3.2、判断是否获取锁成功
        if (isLock){
            // TODO: 4、DoubleCheck，再次检查redis逻辑过期时间是否过期
            if (expireTime.isAfter(LocalDateTime.now())) {
                // TODO: 4.1、未过期，直接返回店铺信息
                return shop;
            }
            // TODO: 4、获取锁成功,已过期，需要缓存重建，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    // 重建缓存
                    this.saveShopToRedis(id,20L);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }

        // 返回商铺信息
        return shop;
    }

    // 缓存重建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    // 获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    // 释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    // 存储店铺信息和逻辑过期时间
    public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        // 1、查询店铺数据
        Shop shop = getById(id);

        Thread.sleep(200);  // 模拟缓存重建延迟

        // 2、封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3、写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY +id,JSONUtil.toJsonStr(redisData));

    }
    //

    // 更新商铺信息
    @Override
    @Transactional  // 添加事务，更新数据库和删除缓存保证原子性。（但是这种方式只使用与单体项目！！！）
    public Result update(Shop shop) {
        Long shopId = shop.getId();
        String shop_cache_key = CACHE_SHOP_KEY + shopId;
        // 确保shopId不为空，为空后面会出现问题
        if (shopId == null){
            return Result.fail("店铺id不能为空");
        }
        /*更新数据库和删除缓存需要保证原子性。在单体项目中可以使用事务@Transactional，分布式情况下需要使用TCC保证强一致性*/
        // 1、更新数据库
        updateById(shop);
        // 2、删除缓存
        stringRedisTemplate.delete(shop_cache_key);

        return Result.ok();
    }
}
