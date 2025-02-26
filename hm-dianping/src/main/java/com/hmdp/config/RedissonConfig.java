package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Classname: RedissonConfig
 * @Date: 2025/2/26 13:35
 * @Author: 聂建强
 * @Description: Redisson的配置类
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        // 配置类
        Config config = new Config();
        // 添加redis地址，这里添加的单节点地址，也可以使用config.useClusterServers()添加集群地址
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        // config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        // 创建客户端
        return Redisson.create(config);
    }
}
