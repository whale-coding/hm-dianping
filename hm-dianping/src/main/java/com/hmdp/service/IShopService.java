package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    /**
     * 根据商铺类型分页查询商铺信息(附近商铺)
     * @param typeId 商铺类型
     * @param current 页码，滚动查询
     * @param x 经度，非必传参数
     * @param y 纬度，非必传参数
     * @return 符合要求的商铺列表
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
