package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;  // 分布式ID生成器，用于生成订单ID

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;  // redisson

    // 自己注入自己是为了获取代理对象， @Lazy延迟注入，避免形成循环依赖
    @Resource
    @Lazy
    private IVoucherOrderService voucherOrderService;

    // @Override
    // // @Transactional
    // public Result seckillVoucher(Long voucherId) {
    //     // 1、查询优惠券
    //     SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //     // 2、判断秒杀是否开始
    //     if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
    //         // 尚未开始
    //         return Result.fail("秒杀尚未开始");
    //     }
    //     // 3、判断秒杀是否结束
    //     if (voucher.getEndTime().isBefore(LocalDateTime.now())){
    //         // 尚未开始
    //         return Result.fail("秒杀已经结束！");
    //     }
    //     // 4、判断库存是否充足
    //     if (voucher.getStock() < 1){
    //         // 库存不足
    //         return Result.fail("库存不足");
    //     }
    //
    //     /*
    //
    //     Long userId = UserHolder.getUser().getId();  // 用户id
    //
    //     // TODO: 一人一单
    //     // 查询订单
    //     Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    //     // 判断是否已经购买过了
    //     if (count > 0){
    //         // 用户已经购买过了
    //         return Result.fail("用户已经购买过一次！");
    //     }
    //
    //     // 5、扣减库存
    //     // boolean success =  seckillVoucherService.update()
    //     //         .setSql("stock = stock -1")
    //     //         .eq("voucher_id", voucherId).update();
    //
    //     // boolean success =  seckillVoucherService.update()
    //     //         .setSql("stock = stock -1")  // set stock = stock -1
    //     //         .eq("voucher_id", voucherId).eq("stock",voucher.getStock())  // where id = ? and stock = ?
    //     //         .update();
    //
    //     // 乐观锁解决超卖问题
    //     boolean success =  seckillVoucherService.update()
    //             .setSql("stock = stock -1")  // set stock = stock -1
    //             .eq("voucher_id", voucherId).gt("stock",0)  // where id = ? and stock > 0
    //             .update();
    //     if (!success){
    //         // 扣减失败
    //         return Result.fail("库存不足！");
    //     }
    //
    //     // 6、创建订单
    //     VoucherOrder voucherOrder = new VoucherOrder();
    //     // 6.1 订单id
    //     long orderId = redisIdWorker.nextId("order");
    //     voucherOrder.setId(orderId);
    //     // 6.2 用户id
    //     voucherOrder.setUserId(userId);
    //     // 6.3 代金券id
    //     voucherOrder.setVoucherId(voucherId);
    //
    //     // 7、订单写入数据库
    //     save(voucherOrder);
    //
    //     // 8、返回订单id
    //     return Result.ok(orderId);
    //
    //      */
    //
    //     Long userId = UserHolder.getUser().getId();  // 用户id
    //     /*
    //     synchronized (userId.toString().intern()){
    //         // 解决事务失效的问题
    //         IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();  // 获取代理对象（事务）
    //         return proxy.createVoucherOrder(voucherId);
    //     }
    //
    //      */
    //
    //     // 创建锁对象
    //     // SimpleRedisLock lock = new SimpleRedisLock("order:"+userId,stringRedisTemplate);
    //     RLock lock = redissonClient.getLock("lock:order:" + userId);  // 获取锁（可重入），指定锁的名称
    //     // 获取锁
    //     // boolean isLock = lock.tryLock(1200);
    //     boolean isLock = lock.tryLock();
    //     // 判断是否获取锁成功
    //     if (!isLock){
    //         // 获取锁失败，返回错误或重试
    //         return Result.fail("不允许重复下单");
    //     }
    //     // 获取锁成功
    //     try {
    //         IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();  // 获取代理对象（事务）
    //         return proxy.createVoucherOrder(voucherId);
    //     }finally {
    //         // 释放锁
    //         lock.unlock();
    //     }
    // }

    /**
     * 秒杀创建订单的代码封装（但是也仅仅只是适用于单机模式下）
     *
     * @param voucherId 优惠券id
     */
    @Transactional
    public synchronized void createVoucherOrder(Long voucherId){
        Long userId = UserHolder.getUser().getId();  // 用户id

        // 5、一人一单
        // 查询订单
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 判断是否已经购买过了
        if (count > 0){
            // 用户已经购买过了
            Result.fail("用户已经购买过一次！");
            return;
        }

        // 6、扣减库存
        // 乐观锁解决超卖问题
        boolean success =  seckillVoucherService.update()
                .setSql("stock = stock -1")  // set stock = stock -1
                .eq("voucher_id", voucherId).gt("stock",0)  // where id = ? and stock > 0
                .update();
        if (!success){
            // 扣减失败
            Result.fail("库存不足！");
            return;
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
        Result.ok(orderId);
    }

/* ********************************以下代码是最后的使用stream消息队列的最终版本********************************************* */
    /**
     * 优惠券秒杀（最终版本）
     * @param voucherId 优惠券id
     * @return 是否秒杀成功，成功返回订单id
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户id
        Long userId = UserHolder.getUser().getId();
        // 获取订单id
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );
        int r = result.intValue();  // 得到lua脚本的结果，是一个数字
        // 2.对lua的结果进行判断，判断是否为0
        if (r != 0){
            // 2.1.结果不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足！":"不能重复下单！");
        }
        // 有购买资格
        // 4.返回订单id
        return Result.ok(orderId);
    }

    // 执行lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    // 初始化
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));  // 从文件中读取lua命令
        SECKILL_SCRIPT.setResultType(Long.class);  // 设置返回类型
    }

    // 线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 初始化线程池，提交任务
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    // 线程任务
    /*使用消息队列的时候一定记得要先手动用命令在redis中创建队列和消费者组：XGROUP CREATE stream.orders g1 0 MKSTREAM  */
    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while (true){
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.order >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                                    Consumer.from("g1", "c1"),
                                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                    StreamOffset.create(queueName, ReadOffset.lastConsumed())
                            );
                    // 2.判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 如果获取失败，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 4.获取成功，可以创建订单
                    voucherOrderService.createVoucherOrder(voucherOrder);
                    // 5.ACK确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                    handlePendingList();
                }
            }
        }
        // 处理pending-list
        /*使用消息队列的时候一定记得要先手动用命令在redis中创建队列和消费者组：XGROUP CREATE stream.orders g1 0 MKSTREAM  */
        private void handlePendingList() {
            while (true){
                try {
                    // 1.获取Pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 STREAMS streams.order 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                                    Consumer.from("g1", "c1"),
                                    StreamReadOptions.empty().count(1),
                                    StreamOffset.create(queueName, ReadOffset.from("0"))
                            );
                    // 2.判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 如果获取失败，说明pending-list没有异常消息，结束循环
                        break;
                    }
                    // 3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 4.获取成功，可以创建订单
                    voucherOrderService.createVoucherOrder(voucherOrder);
                    // 5.ACK确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pending-list订单异常",e);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }


    /**
     * 创建订单，用于消息队列下单
     * @param voucherOrder 订单信息
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        //  TODO: 1、再次使用分布式锁用于兜底
        // 创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);  // 使用的是redisson中的锁
        // 尝试获取锁
        boolean isLock = redisLock.tryLock();
        // 判断
        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            log.error("不允许重复下单！");
            return;
        }
        //  TODO: 解决一人一单问题
        try {
            // 查询订单
            Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            // 判断是否已经购买过了
            if (count > 0){
                // 订单已经存在，说明该用户已经购买过了
                log.error("用户已经购买过了！");
                return;
            }
            // 未购买过，扣减库存
            // 乐观锁解决超卖问题.
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1") // set stock = stock - 1
                    .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                    .update();
            // 判断库存是否扣减成功
            if (! success){
                // 扣减失败
                log.error("库存不足！");
                return;
            }
            // 库存构建成功
            // 保存订单，订单写入数据库
            save(voucherOrder);
        }finally {
            // 释放锁
            redisLock.unlock();
        }
    }


    // /**
    //  * 秒杀创建订单的代码封装（但是也仅仅只是适用于单机模式下）
    //  *
    //  * @param voucherId 优惠券id
    //  */
    // @Transactional
    // public synchronized void createVoucherOrder(Long voucherId){
    //     Long userId = UserHolder.getUser().getId();  // 用户id
    //
    //     // 5、一人一单
    //     // 查询订单
    //     Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    //     // 判断是否已经购买过了
    //     if (count > 0){
    //         // 用户已经购买过了
    //         Result.fail("用户已经购买过一次！");
    //         return;
    //     }
    //
    //     // 6、扣减库存
    //     // 乐观锁解决超卖问题
    //     boolean success =  seckillVoucherService.update()
    //             .setSql("stock = stock -1")  // set stock = stock -1
    //             .eq("voucher_id", voucherId).gt("stock",0)  // where id = ? and stock > 0
    //             .update();
    //     if (!success){
    //         // 扣减失败
    //         Result.fail("库存不足！");
    //         return;
    //     }
    //
    //     // 7、创建订单
    //     VoucherOrder voucherOrder = new VoucherOrder();
    //     // 7.1 订单id
    //     long orderId = redisIdWorker.nextId("order");
    //     voucherOrder.setId(orderId);
    //     // 7.2 用户id
    //     voucherOrder.setUserId(userId);
    //     // 7.3 代金券id
    //     voucherOrder.setVoucherId(voucherId);
    //
    //     // 8、订单写入数据库
    //     save(voucherOrder);
    //
    //     // 9、返回订单id
    //     Result.ok(orderId);
    // }







    // 线程任务
    /*使用消息队列的时候一定记得要先手动用命令在redis中创建队列和消费者组：XGROUP CREATE stream.orders g1 0 MKSTREAM  */
    // private class VoucherOrderHandler implements Runnable{
    //     @Override
    //     public void run() {
    //         while (true){
    //             try {
    //                 // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
    //                 List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
    //                         Consumer.from("g1", "c1"),
    //                         StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
    //                         StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
    //                 );
    //                 // 2.判断消息获取是否为空
    //                 if (list == null || list.isEmpty()) {
    //                     // 如果为null，说明没有消息，继续下一次循环
    //                     continue;
    //                 }
    //                 // 3.解析消息中的订单信息
    //                 MapRecord<String, Object, Object> record = list.get(0);
    //                 Map<Object, Object> value = record.getValue();
    //                 VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
    //                 // 4.如果获取成功，可以创建订单
    //                 createVoucherOrder(voucherOrder.getVoucherId());
    //                 // 5.ACK确认 SACK stream.orders g1 id
    //                 stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
    //             } catch (Exception e) {
    //                 log.error("处理订单异常", e);
    //                 // 处理pending状态信息
    //                 handlePendingList();
    //             }
    //
    //         }
    //     }
    // 处理pending-list
    /*使用消息队列的时候一定记得要先手动用命令在redis中创建队列和消费者组：XGROUP CREATE stream.orders g1 0 MKSTREAM  */
    //     private void handlePendingList() {
    //         while (true) {
    //             try {
    //                 // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
    //                 List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
    //                         Consumer.from("g1", "c1"),
    //                         StreamReadOptions.empty().count(1),
    //                         StreamOffset.create("stream.orders", ReadOffset.from("0"))
    //                 );
    //                 // 2.判断订单信息是否为空
    //                 if (list == null || list.isEmpty()) {
    //                     // 如果为null，说明没有异常消息，结束循环
    //                     break;
    //                 }
    //                 // 3.解析消息中的订单信息
    //                 MapRecord<String, Object, Object> record = list.get(0);
    //                 Map<Object, Object> value = record.getValue();
    //                 VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
    //                 // 4.创建订单
    //                 createVoucherOrder(voucherOrder.getVoucherId());
    //                 // 5.ACK确认 SACK stream.orders g1 id
    //                 stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
    //             } catch (Exception e) {
    //                 log.error("处理pending-list订单异常", e);
    //             }
    //         }
    //     }
    // }

    // 优惠券秒杀
    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     // 获取用户
    //     Long userId = UserHolder.getUser().getId();
    //     // 获取订单id
    //     long orderId = redisIdWorker.nextId("order");
    //     // 1、执行lua脚本
    //     Long result = stringRedisTemplate.execute(
    //             SECKILL_SCRIPT,
    //             Collections.emptyList(),
    //             voucherId.toString(), userId.toString(),String.valueOf(orderId)
    //
    //     );
    //     // 2、判断结果是否为0
    //     int r = result.intValue();
    //     if (r != 0){
    //         // 2.1 不为0，代表没有购买资格
    //         return Result.fail(r ==1 ? "库存不足": "不能重复下单");
    //     }
    //     // 2.2 为0，有购买资格
    //     // 3、返回订单id
    //     return Result.ok(orderId);
    // }








    // 消息队列下单
    // private void handleVoucherOrder(VoucherOrder voucherOrder) {
    //     // 获取用户
    //     Long userId = voucherOrder.getUserId();
    //     // 创建锁对象
    //     RLock lock = redissonClient.getLock("lock:order:" + userId);
    //     // 获取锁
    //     boolean isLock = lock.tryLock();
    //     if (!isLock) {
    //         // 获取锁失败，返回错误或重试
    //         log.error("不允许重复下单！");
    //         return;
    //     }
    //     try {
    //         // 创建订单
    //         proxy.createVoucherOrder(voucherOrder);
    //     } finally {
    //         // 释放锁
    //         lock.unlock();
    //     }
    // }

    // private IVoucherOrderService proxy;

    // 优惠券秒杀
    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     // 获取用户
    //     Long userId = UserHolder.getUser().getId();
    //     // 获取订单id
    //     long orderId = redisIdWorker.nextId("order");
    //     System.out.println("userId:"+userId);  //  1010
    //     System.out.println("orderId:"+orderId);  // 428096308268498945
    //     System.out.println("voucherId:"+voucherId);  // 10
    //
    //     // 1.执行lua脚本
    //     Long result = stringRedisTemplate.execute(
    //             SECKILL_SCRIPT,
    //             Collections.emptyList(),
    //             voucherId.toString(),
    //             userId.toString(),
    //             String.valueOf(orderId)
    //     );
    //     // 2.判断结果是否为0
    //     System.out.println("Lua script result: " + result);
    //     assert result != null;
    //     int r = result.intValue();
    //     System.out.println("r: " + r);
    //     if (r != 0){
    //         // 2.1.结果不为0，代表没有购买资格
    //         return Result.fail(r == 1 ? "库存不足！":"不能重复下单！");
    //     }
    //     // 3.获取代理对象
    //     // 获取代理对象(事务)
    //     proxy = (IVoucherOrderService) AopContext.currentProxy();
    //     // 4.返回订单id
    //     return Result.ok(orderId);
    // }



    // @Transactional
    // public void createVoucherOrder(VoucherOrder voucherOrder) {
    //     // 5.一人一单
    //     Long userId = voucherOrder.getUserId();
    //
    //     // 5.1.查询订单
    //     int count = Math.toIntExact(query()
    //             .eq("user_id", userId)
    //             .eq("voucher_id", voucherOrder.getVoucherId()).count());
    //     // 5.2.判断是否存在
    //     if (count > 0) {
    //         log.error("用户已经购买过了！");
    //         return;
    //     }
    //     // 6.扣减库存
    //     boolean success = seckillVoucherService.update()
    //             .setSql("stock = stock - 1")    // set stock = stock - 1
    //             .eq("voucher_id", voucherOrder.getVoucherId())
    //             .gt("stock", 0)    // where voucher_id = ? and stock > 0
    //             .update();
    //     if (!success) {
    //         // 扣减失败
    //         log.error("库存不足！");
    //         return;
    //     }
    //     // 7.创建订单
    //     save(voucherOrder);
    // }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//
//        // 1.查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2.判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 尚未开始
//            return Result.fail("秒杀尚未开始！");
//        }
//        // 3.判断秒杀是否结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            // 秒杀结束
//            return Result.fail("秒杀已经结束！");
//        }
//        // 4.判断库存是否充足
//        if (voucher.getStock() < 1) {
//            // 库存不足
//            return Result.fail("库存不足！");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//        // 创建锁对象
////        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        // 获取锁
//        boolean isLock = lock.tryLock();
//        if (!isLock) {
//            // 获取锁失败，返回错误或重试
//            return Result.fail("购买失败，请重试！");
//        }
//        try {
//            // 获取代理对象(事务)
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            // 释放锁
//            lock.unlock();
//        }
//    }
}
