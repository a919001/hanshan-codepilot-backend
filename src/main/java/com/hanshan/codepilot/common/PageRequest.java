package com.hanshan.codepilot.common;

import cn.hutool.core.util.StrUtil;
import com.hanshan.codepilot.constant.CommonConstant;
import lombok.Data;

/**
 * 分页请求
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

    /**
     * 获取排序字段
     * @return 排序字段（下划线）
     */
    public String getSortField() {
        return StrUtil.toUnderlineCase(this.sortField);
    }
}
