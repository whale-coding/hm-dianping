package com.hmdp;


import com.hmdp.service.impl.ShopServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    /**
     * 测试往Redis中存入shop数据以及逻辑过期时间
     */
    @Test
    public void test() throws InterruptedException {
        shopService.saveShopToRedis(1L,10L);
    }


}
