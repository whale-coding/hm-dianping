package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 查询店铺类型列表并添加缓存
    @Override
    public Result selectTypeList() {
        // 1、从redis中查询商铺类型缓存
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY);
        // 2、判断缓存中是否有商铺类型信息
        if (StrUtil.isNotBlank(shopTypeListJson)){
            // 3、缓存中存在商铺类型信息，直接返回
            List<ShopType> shopTypes = JSONUtil.toList(shopTypeListJson, ShopType.class);  // 将Json转换为List集合对象
            // 根据sort字段排序（为了确保数据顺序一致，即使缓存中已排序也重新排序）
            shopTypes.sort(Comparator.comparingInt(ShopType::getSort));
            return Result.ok(shopTypes);
        }
        // 4、缓存中不存在商铺类型信息,根据id查询数据库中的商铺类型列表并进行排序
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 5、判断数据库中是否有商铺类型列表
        if (CollectionUtils.isEmpty(shopTypes)){
            // 不存在，返回错误信息
            return Result.fail("店铺分类不存在！");
        }
        // 6、数据库中存在商铺分类数据，将其写入redis缓存中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypes));
        // 7、返回数据库中的商铺分类列表
        return Result.ok(shopTypes);
    }
}
