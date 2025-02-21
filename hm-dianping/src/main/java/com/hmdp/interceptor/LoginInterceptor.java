package com.hmdp.interceptor;

import com.hmdp.constants.Constants;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import lombok.val;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @Classname: interceptor
 * @Date: 2025/2/21 14:56
 * @Author: 聂建强
 * @Description: 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {
    // 前置拦截方法：进入Controller之前，进行登录校验
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1、获取session
        HttpSession session = request.getSession();
        // 2、获取session中的用户
        UserDTO userDTO = (UserDTO) session.getAttribute(Constants.SESSION_USER_KEY);
        // 3、判断用户是否存在
        if (userDTO == null){
            // 4、用户不存在，进行拦截并返回401未授权状态码
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;  // 拦截
        }

        // 5、用户存在，保存用户信息到ThreadLocal中
        UserHolder.saveUser(userDTO);
        // 6、放行
        return true;
    }

    // 后置拦截方法：用户业务执行完毕，销毁对应的用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户，防止内存泄露
        UserHolder.removeUser();
    }
}
