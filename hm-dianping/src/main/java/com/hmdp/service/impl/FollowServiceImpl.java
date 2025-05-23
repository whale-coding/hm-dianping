package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    /**
     * 关注和取关
     * @param followUserId 被关注者的id
     * @param isFollow 是否关注，True表示关注，False表示取关
     * @return 无
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1.获取当前登录用户的id
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;  // 关注信息保存到redis时的key
        // 2.判断到底是关注还是取关
        if (isFollow){
            // 关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess){
                // 把关注用户的id,放入redis的set集合 sadd userId followerUserId
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }

        }else {
            // 未关注，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getUserId,userId).eq(Follow::getFollowUserId,followUserId);
            boolean isSuccess = remove(queryWrapper);
            if (isSuccess){
                // 把关注用户的id从redis集合中移除
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 当前登录用户是否关注了博主
     * @param followUserId 博主的id
     * @return 无
     */
    @Override
    public Result isFollow(Long followUserId) {
        // 1.获取当前登录用户的id
        Long userId = UserHolder.getUser().getId();

        // 2.查询是否关注，select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 3.判断
        return Result.ok(count > 0);
    }

    /**
     * 共同关注
     * @param id 目标用户id
     * @return 两人共同关注的人 List<UserDTO>
     */
    @Override
    public Result followCommons(Long id) {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        String loginUserKey = "follows:" + userId;
        // 2.求当前用户和目标用户的交集
        String targetUserKey = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(loginUserKey, targetUserKey);
        if (intersect == null || intersect.isEmpty()){
            // 无交集
            return Result.ok(Collections.emptyList());
        }
        // 3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4.查询用户
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }
}
