package com.hmdp.utils.redisLock;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;


/**
 * @Classname: SimpleRedisLock
 * @Date: 2025/2/26 10:51
 * @Author: 聂建强
 * @Description: 使用Redis实现简单的分布式锁
 */
public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate){
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识
        // long threadId = Thread.currentThread().getId();  // 获取当前线程的id
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        // 避免拆箱时发生空指针异常
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 调用lua脚本释放锁
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }

    // @Override
    // public void unlock() {
    //     // 获取线程标识
    //     String threadId = ID_PREFIX + Thread.currentThread().getId();
    //     // 获取锁中的标识
    //     String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    //     // 判断标识是否一致
    //     if (threadId.equals(lockId)){
    //         // 释放锁
    //         stringRedisTemplate.delete(KEY_PREFIX + name);
    //     }
    // }
}
