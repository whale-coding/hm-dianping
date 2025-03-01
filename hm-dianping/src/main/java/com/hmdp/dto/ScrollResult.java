package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * 滚动分页的结果
 */
@Data
public class ScrollResult {
    private List<?> list;  // 内容
    private Long minTime;  // 上一次时间（最小时间）
    private Integer offset;  // 偏移量
}
