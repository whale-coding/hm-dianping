package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

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

        // 2.判断到底是关注还是取关
        if (isFollow){
            // 关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
        }else {
            // 未关注，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getUserId,userId).eq(Follow::getFollowUserId,followUserId);
            remove(queryWrapper);
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
}
