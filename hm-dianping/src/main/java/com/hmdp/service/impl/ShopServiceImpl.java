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

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

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
        // 4、缓存中不存在商铺信息,根据id查询数据库中的商铺信息
        Shop shop = getById(id);
        // 5、判断数据库中是否有商铺信息
        if (shop == null){
            // 不存在，返回错误信息
            return Result.fail("店铺不存在！");
        }
        // 6、数据库中存在商铺信息，将其写入redis缓存中
        stringRedisTemplate.opsForValue().set(shop_cache_key, JSONUtil.toJsonStr(shop));
        // 7、返回数据库中的商铺信息
        return Result.ok(shop);
    }
}
