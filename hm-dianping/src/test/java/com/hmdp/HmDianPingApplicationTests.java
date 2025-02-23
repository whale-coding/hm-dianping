package com.hmdp;


import com.hmdp.service.impl.ShopServiceImpl;

import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SnowflakeIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisIdWorker redisIdWorker;

    SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(0,0);

    /**
     * 测试往Redis中存入shop数据以及逻辑过期时间
     */
    @Test
    public void test() throws InterruptedException {
        shopService.saveShopToRedis(1L,10L);
    }

    private ExecutorService es = Executors.newFixedThreadPool(500);

    /**
     * 测试Redis生成分布式ID
     */
    @Test
    public void testIdWorker()throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id ="+id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = "+ (end - begin));
    }

    /**
     * 测试雪花算法生成分布式ID
     */
    @Test
    public void testSnowflakeIdWorker()throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = snowflakeIdWorker.nextId();
                System.out.println("id ="+id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = "+ (end - begin));
    }



}
