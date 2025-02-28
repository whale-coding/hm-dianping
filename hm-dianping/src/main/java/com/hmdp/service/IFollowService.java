package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注和取关
     * @param followUserId 被关注者的id
     * @param isFollow 是否关注，True表示关注，False表示取关
     * @return 无
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 当前登录用户是否关注了博主
     * @param followUserId 博主的id
     * @return 无
     */
    Result isFollow(Long followUserId);


}
