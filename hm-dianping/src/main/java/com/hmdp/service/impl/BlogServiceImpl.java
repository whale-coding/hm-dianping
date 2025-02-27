package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查看热门探店笔记
     * @param current 页码
     * @return 探店笔记
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        // records.forEach(this::queryBlogUser);
        records.forEach(blog -> {
            this.queryBlogUser(blog);  // 用户信息
            this.isBlogLiked(blog);  // 是否被点赞过
        });
        return Result.ok(records);
    }

    // 查询与博客相关的用户信息，并将其设置在Blog实体中
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    // 查询Blog是否被点赞过
    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:"+ blog.getId();  // 存入redis时的key
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key,userId.toString());
        blog.setIsLike(BooleanUtil.isTrue(isMember));  // 设置是否被点赞过
    }

    /**
     * 查看探店笔记
     * @param id 笔记的id
     * @return 笔记的内容
     */
    @Override
    public Result queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null){
            return Result.fail("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞过
        isBlogLiked(blog);

        // 返回
        return Result.ok(blog);
    }

    /***
     * 点赞功能
     * @param id 探店笔记id
     * @return 无
     */
    @Override
    public Result likeBlog(Long id) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:"+ id;  // 存入redis时的key
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key,userId.toString());
        if (BooleanUtil.isFalse(isMember)){
            // 2.1 未点赞，该用户可以点赞
            // 数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id",id).update();
            if (isSuccess){
                // 保存用户到Redis的set集合
                stringRedisTemplate.opsForSet().add(key,userId.toString());  // 将当前用户的id记录在redis中
            }
        }else{
            // 2.2 用户已经点过赞，取消点赞
            // 数据库点赞数-1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id",id).update();
            if (isSuccess){
                // 把用户从redis的set集合中移除
                stringRedisTemplate.opsForSet().remove(key,userId.toString());  // 将当前用户的id记录在redis中
            }
        }

        return Result.ok();
    }


}
