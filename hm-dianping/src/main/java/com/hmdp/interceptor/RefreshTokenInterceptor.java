package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.constants.JwtClaimsConstant;
import com.hmdp.dto.UserDTO;
import com.hmdp.properties.JwtProperties;
import com.hmdp.utils.JwtUtil;
import com.hmdp.utils.UserHolder;
import io.jsonwebtoken.Claims;
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
    private final JwtProperties jwtProperties; // 直接通过构造器注入


    // 通过构造器去注入
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate,JwtProperties jwtProperties){
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties; // 手动接收依赖
    }

    /*
    // 基于普通token来拦截
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

     */


    // 基于jwt来拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO: 1、获取请求头中的jwt token
        String token = request.getHeader(jwtProperties.getUserTokenName());
        // TODO: 2、判断jwt token是否存在
        if (StrUtil.isBlank(token)){
            return true;  // 不存在，放行
        }

        // 判断jwt是否过期
        if(JwtUtil.isExpiration(token,jwtProperties.getUserSecretKey())){
            return true;  // 存在，过期，放行
        }

        // TODO: 3、解析 jwt token，解析中其中的userId
        Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(),token);
        Long userId =  claims.get(JwtClaimsConstant.USER_ID,Long.class);

        // TODO: 4、基于userId获取redis中的用户信息
        String userIdKey = LOGIN_USER_KEY + userId;   // 这个是redis中存登录用户信息的key
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userIdKey);
        // TODO: 5、判断用户是否存在
        if (userMap.isEmpty()){
            return true;  // 不存在，放行
        }
        // TODO: 6、判断前端携带过来的token和redis中存储的userMap中的token是否一致，防止有以前生成的jwt，仍然能登录！
        String jwttoken = userMap.get("jwttoken").toString();
        if (!jwttoken.equals(token)){
            return true;  // token不一致，放行
        }

        // TODO: 7、用户存在，将查询到的Hash结构的用户数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // TODO: 8、用户存在，保存用户信息到ThreadLocal中
        UserHolder.saveUser(userDTO);

        // TODO: 9、刷新redis中存储的用户信息的有效期，不需要刷新jwt本身的过期时间！！！
        stringRedisTemplate.expire(userIdKey,LOGIN_USER_TTL, TimeUnit.MINUTES);

        // TODO: 10、放行
        return true;
    }

    // 后置拦截方法：用户业务执行完毕，销毁对应的用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户，防止内存泄露
        UserHolder.removeUser();
    }
}
