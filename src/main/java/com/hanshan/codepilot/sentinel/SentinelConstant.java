package com.hanshan.codepilot.sentinel;

/**
 * Sentinel 限流熔断常量
 */
public interface SentinelConstant {

    /**
     * 分页获取题库列表接口限流熔断
     */
    String listQuestionBankVOByPage = "listQuestionBankVOByPage";

    /**
     * 分页获取题目列表接口限流熔断
     */
    String listQuestionVOByPage = "listQuestionVOByPage";
}
