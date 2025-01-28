package com.hanshan.codepilot.model.dto.question;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量删除题目请求
 */
@Data
public class QuestionBatchDeleteRequest implements Serializable {

    /**
     * 题目 id 列表
     */
    List<Long> questionIdList;

    @Serial
    private static final long serialVersionUID = 1L;
}