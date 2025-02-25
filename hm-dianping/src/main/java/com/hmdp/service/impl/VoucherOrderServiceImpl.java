package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.val;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    // 优惠券秒杀
    @Override
    // @Transactional
    public Result seckillVoucher(Long voucherId) {
        // 1、查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2、判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            // 尚未开始
            return Result.fail("秒杀尚未开始");
        }
        // 3、判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }
        // 4、判断库存是否充足
        if (voucher.getStock() < 1){
            // 库存不足
            return Result.fail("库存不足");
        }

        /*

        Long userId = UserHolder.getUser().getId();  // 用户id

        // TODO: 一人一单
        // 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 判断是否已经购买过了
        if (count > 0){
            // 用户已经购买过了
            return Result.fail("用户已经购买过一次！");
        }

        // 5、扣减库存
        // boolean success =  seckillVoucherService.update()
        //         .setSql("stock = stock -1")
        //         .eq("voucher_id", voucherId).update();

        // boolean success =  seckillVoucherService.update()
        //         .setSql("stock = stock -1")  // set stock = stock -1
        //         .eq("voucher_id", voucherId).eq("stock",voucher.getStock())  // where id = ? and stock = ?
        //         .update();

        // 乐观锁解决超卖问题
        boolean success =  seckillVoucherService.update()
                .setSql("stock = stock -1")  // set stock = stock -1
                .eq("voucher_id", voucherId).gt("stock",0)  // where id = ? and stock > 0
                .update();
        if (!success){
            // 扣减失败
            return Result.fail("库存不足！");
        }

        // 6、创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2 用户id
        voucherOrder.setUserId(userId);
        // 6.3 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 7、订单写入数据库
        save(voucherOrder);

        // 8、返回订单id
        return Result.ok(orderId);

         */

        Long userId = UserHolder.getUser().getId();  // 用户id
        synchronized (userId.toString().intern()){
            // 解决事务失效的问题
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();  // 获取代理对象（事务）
            return proxy.createVoucherOrder(voucherId);
        }
    }

    /**
     * 秒杀创建订单的代码封装（但是也仅仅只是适用于单机模式下）
     * @param voucherId 优惠券id
     * @return 订单id
     */
    @Transactional
    public synchronized Result createVoucherOrder(Long voucherId){
        Long userId = UserHolder.getUser().getId();  // 用户id

        // 5、一人一单
        // 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 判断是否已经购买过了
        if (count > 0){
            // 用户已经购买过了
            return Result.fail("用户已经购买过一次！");
        }

        // 6、扣减库存
        // 乐观锁解决超卖问题
        boolean success =  seckillVoucherService.update()
                .setSql("stock = stock -1")  // set stock = stock -1
                .eq("voucher_id", voucherId).gt("stock",0)  // where id = ? and stock > 0
                .update();
        if (!success){
            // 扣减失败
            return Result.fail("库存不足！");
        }

        // 7、创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2 用户id
        voucherOrder.setUserId(userId);
        // 7.3 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 8、订单写入数据库
        save(voucherOrder);

        // 9、返回订单id
        return Result.ok(orderId);

    }
}
