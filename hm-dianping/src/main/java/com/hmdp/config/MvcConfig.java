package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @Classname: MvcConfig
 * @Date: 2025/2/21 15:16
 * @Author: 聂建强
 * @Description: SpringBoot的MVC配置
 */
@Configuration  // 表明这是一个配置类
public class MvcConfig implements WebMvcConfigurer {

    // 添加拦截器
    /*
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())  // 注册自定义的登录拦截器
                .excludePathPatterns(  // 配置拦截规则。excludePathPatterns是配置要放行哪些接口
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                );
    }
    */

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))  // 注册自定义的登录拦截器
                .excludePathPatterns(  // 配置拦截规则。excludePathPatterns是配置要放行哪些接口
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                );
    }
}
