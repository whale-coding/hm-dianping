package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
        String shop_cache_key = CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shop_cache_key);
        // 2、判断缓存中是否有商铺信息
        if (StrUtil.isNotBlank(shopJson)){
            // 3、缓存中存在商铺信息，直接返回
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);  // 将Json转换为对象
            return Result.ok(shop);
        }
        /*避免缓存穿透问题*/
        // 判断命中的是否是空值
        if (shopJson != null){
            // 不为null，是空值，返回错误信息
            return Result.fail("店铺不存在！");
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
            return Result.fail("店铺不存在！");
        }
        // 6、数据库中存在商铺信息，将其写入redis缓存中
        // stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop));
        // 给缓存添加超时时间，方便后续缓存的更新
        stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7、返回数据库中的商铺信息
        return Result.ok(shop);
    }

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
