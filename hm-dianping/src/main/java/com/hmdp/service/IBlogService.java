package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 查看热门探店笔记
     * @param current 页码
     * @return 笔记的内容
     */
    Result queryHotBlog(Integer current);

    /**
     * 查看探店笔记
     * @param id 笔记的id
     * @return 笔记的内容
     */
    Result queryBlogById(Long id);

    /***
     * 点赞功能
     * @param id 探店笔记id
     * @return 无
     */
    Result likeBlog(Long id);

    /**
     * 查看博客点赞的TopN的用户集合
     * @param id 笔记的id
     * @return 笔记的内容
     */
    Result queryBlogLikes(Long id);

    /**
     * 保存博客
     * @param blog 要保存的博客
     * @return 无
     */
    Result saveBlog(Blog blog);
}
