package com.hanshan.codepilot.model.dto.questionBankQuestion;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新题库题目请求
 */
@Data
public class QuestionBankQuestionUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id
     */
    private Long questionId;

    @Serial
    private static final long serialVersionUID = 1L;
}