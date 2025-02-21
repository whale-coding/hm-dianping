package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constants.Constants.REQUEST_HEADER_TOKEN_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @Classname: interceptor
 * @Date: 2025/2/21 14:56
 * @Author: 聂建强
 * @Description: 登录拦截器
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    // 前置拦截方法：进入Controller之前，进行登录校验

    // 注意这里不能通过注解注入！！！
    private final StringRedisTemplate stringRedisTemplate;
    // 通过构造器去注入
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO: 1、获取请求头中的token
        String token = request.getHeader(REQUEST_HEADER_TOKEN_KEY);
        // TODO: 2、判断token是否存在
        if (StrUtil.isBlank(token)){
            return true;  // 放行
        }
        // TODO: 3、基于token获取redis中的用户信息
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        // TODO: 4、判断用户是否存在
        if (userMap.isEmpty()){
            return true;  // 放行
        }
        // TODO: 5、用户存在，将查询到的Hash结构的用户数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // TODO: 6、用户存在，保存用户信息到ThreadLocal中
        UserHolder.saveUser(userDTO);

        // TODO: 7、刷新token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);

        // TODO: 8、放行
        return true;
    }

    // 后置拦截方法：用户业务执行完毕，销毁对应的用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户，防止内存泄露
        UserHolder.removeUser();
    }
}
