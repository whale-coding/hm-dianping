package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注和取关
     * @param followUserId 被关注者的id
     * @param isFollow 是否关注，True表示关注，False表示取关
     * @return 无
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    /**
     * 当前登录用户是否关注了博主
     * @param followUserId 博主的id
     * @return 无
     */
    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }


    /**
     * 共同关注
     * @param id 目标用户id
     * @return 两人共同关注的人 List<UserDTO>
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }


}
