package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constants.Constants;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /*
    // 发送手机验证码(基于session实现验证码登录功能)
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1、对用户提交的手机号进行校验
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 注意这个isPhoneInvalid()函数是校验手机号是否是非法的手机号，true表示是非法手机号！
            // 2、如果手机号非法，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3、手机号合法，生成验证码
        String code = RandomUtil.randomNumbers(6);  // 使用hutool工具包的工具生成6位数字验证码
        // 4、保存验证码到session中
        session.setAttribute(Constants.PHONE_KEY, code);
        // 保存手机号到session中！！！目的是解决【当获取验证码的手机号和登录手机号不一致时，也可以登录成功】的bug
        session.setAttribute(Constants.CACHE_PHONE_KEY, phone); // 保存手机号

        // 5、发送短信验证码
        // TODO:这里并没有去真正实现，可以自己去使用阿里云的短信服务实现发送验证码功能！！！
        log.debug("发送短信验证码成功，验证码:{}", code);

        // 返回成功信息
        return Result.ok();
    }
    */



    /*
    // 登录(基于session实现验证码登录功能)
    // TODO:该接口还存在着一个bug:使用A手机号发送验证码，B手机号登录，会发现也能登录成功！！
    // TODO：已经解决了：解决办法是在发送验证码的时候同时将手机号存储到session中，在登录时验证发送验证码的手机号和登录的手机号是否相同。
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1、校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果手机号非法，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 2、校验验证码
        Object cacheCode = session.getAttribute(Constants.PHONE_KEY);  // 从session获取的code
        // 获取保存在session中的手机号
        Object cachePhone = session.getAttribute(Constants.CACHE_PHONE_KEY);
        // 用户传递过来的手机验证码code
        String code = loginForm.getCode();

        // 对登录的手机号进行校验，确保与发送验证码的手机号是一致的！！
        if (cachePhone == null || !cachePhone.toString().equals(phone)) {
            return Result.fail("手机号不匹配或未发送验证码！");
        }

        // 对验证码进行校验
        if (cacheCode == null || ! cacheCode.toString().equals(code)){
            // 验证码不一致
            return Result.fail("验证码错误!");
        }

        // 3、手机号和验证码校验完成，根据手机号查询用户信息 select * from tb_user where phone = ?
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user = getOne(queryWrapper);

        // 4、判断用户是否存在
        if (user == null) {
            // 5、用户不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        // 5、用户存在，保存用户信息到session中
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);  // 将user中的属性拷贝到UserDTO中
        session.setAttribute(Constants.SESSION_USER_KEY, userDTO);  // 这里存储userDTO而不是user是为了不暴漏敏感信息以及减轻服务器压力

        // 基于session去做登录，不需要返回登录凭证,session的原理是基于cookie
        // 每一个session都会有一个唯一的ID，在服务端访问时，session会自动写入cookie当中携带过来
        return Result.ok();
    }
    */

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    // 发送手机验证码(基于redis实现验证码登录功能)
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1、对用户提交的手机号进行校验
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 注意这个isPhoneInvalid()函数是校验手机号是否是非法的手机号，true表示是非法手机号！
            // 2、如果手机号非法，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3、手机号合法，生成验证码
        String code = RandomUtil.randomNumbers(6);  // 使用hutool工具包的工具生成6位数字验证码
        // TODO: 4、保存验证码到redis中  set key value ex 120
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5、发送短信验证码
        // TODO:这里并没有去真正实现，可以自己去使用阿里云的短信服务实现发送验证码功能！！！
        log.debug("发送短信验证码成功，验证码:{}", code);

        // 返回成功信息
        return Result.ok();
    }

    // 登录(基于redis实现验证码登录功能)
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1、校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果手机号非法，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 2、校验验证码
        // TODO: 从redis中获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);  // 从Redis中获取的手机验证码
        String code = loginForm.getCode();  // 用户传递过来的手机验证码code

        // 对验证码进行校验
        if (cacheCode == null || ! cacheCode.equals(code)){
            // 验证码不一致
            return Result.fail("验证码错误!");
        }

        // 3、手机号和验证码校验完成，根据手机号查询用户信息 select * from tb_user where phone = ?
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user = getOne(queryWrapper);

        // 4、判断用户是否存在
        if (user == null) {
            // 5、用户不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        // 5、用户存在，保存用户信息到redis中
        // TODO 5.1 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // TODO 5.2 将User对象转为UserDTO对象，再转为HashMap，方便后面使用redis的hash结构
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);  // 将user中的属性拷贝到UserDTO中
        // 但是这里会报错，因为userDTO中的id类型是Long类型，stringRedisTemplate只能处理String类型的，因此还需要手动去转换为Map
        // Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);  // 将对象转换为Map
        // TODO 手动将对象转换为Map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userDTO.getId().toString());  // 转换为 String！之后stringRedisTemplate就能处理了
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());

        // TODO 5.3 保存到redis中
        String tokenKey = LOGIN_USER_KEY + token;  // 为了区分业务，建议在token前面加上前缀作为tokenKey
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);  // 这里建议使用putAll()而不是put()方法，因为put()方法会多次交互，效率低
        // TODO 5.4 设置token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);

        // TODO 6 返回token
        return Result.ok(token);
    }


    // 根据手机号创建用户信息
    private User createUserWithPhone(String phone) {
        // 1、创建用户
        User user = new User();
        user.setPhone(phone);  // 手机号赋值为注册用户的手机号
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));  // 昵称使用随机生成user_xxx

        // 2、保存用户并返回
        save(user);
        return user;
    }


    /**
     * 用户签到
     * @return 无
     */
    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    /**
     * 签到统计（统计当前用户截止当前时间在本月的连续签到天数）
     * @return 连续签到的天数
     */
    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }
}
