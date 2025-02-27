package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    /**
     * 优惠券秒杀
     * @param voucherId 优惠券id
     * @return 是否秒杀成功，秒杀成功返回订单id
     */
    Result seckillVoucher(Long voucherId);


    /**
     * 创建优惠券的订单(异步的时候使用)
     * @param voucherOrder 优惠券订单信息
     */
    void createVoucherOrder(VoucherOrder voucherOrder);

    /**
     * 创建订单（同步的时候使用）
     * @param voucherId 优惠券id
     */
    void createVoucherOrder(Long voucherId);
}
