package com.hanshan.codepilot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hanshan.codepilot.common.ErrorCode;
import com.hanshan.codepilot.constant.CommonConstant;
import com.hanshan.codepilot.exception.BusinessException;
import com.hanshan.codepilot.exception.ThrowUtils;
import com.hanshan.codepilot.mapper.QuestionBankQuestionMapper;
import com.hanshan.codepilot.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.hanshan.codepilot.model.entity.Question;
import com.hanshan.codepilot.model.entity.QuestionBank;
import com.hanshan.codepilot.model.entity.QuestionBankQuestion;
import com.hanshan.codepilot.model.entity.User;
import com.hanshan.codepilot.model.vo.QuestionBankQuestionVO;
import com.hanshan.codepilot.model.vo.UserVO;
import com.hanshan.codepilot.service.QuestionBankQuestionService;
import com.hanshan.codepilot.service.QuestionBankService;
import com.hanshan.codepilot.service.QuestionService;
import com.hanshan.codepilot.service.UserService;
import com.hanshan.codepilot.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题库题目关联服务实现
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    @Lazy
    private QuestionService questionService;

    /**
     * 校验数据
     *
     * @param questionBankQuestion 题库题目关联
     * @param add 对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        // 题库和题目必须存在
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        Long questionId = questionBankQuestion.getQuestionId();
        if (questionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }
        if (questionId != null) {
            Question question = questionService.getById(questionId);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest 获取题库题目关联请求
     * @return 题库题目关联 QueryWrapper
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(
            QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest
    ) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "question_id", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "question_bank_id", questionBankId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目关联（封装类）
     *
     * @param questionBankQuestion 获取题库题目关联请求
     * @param request HttpServletRequest
     * @return 封装后的题库题目关联
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题库题目关联列表（封装类）
     *
     * @param questionBankQuestionPage 获取题库题目关联请求
     * @param request HttpServletRequest
     * @return 封装后的题库题目关联分页列表
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(
            Page<QuestionBankQuestion> questionBankQuestionPage,
            HttpServletRequest request
    ) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(
                questionBankQuestionPage.getCurrent(),
                questionBankQuestionPage.getSize(),
                questionBankQuestionPage.getTotal()
        );
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream()
                .map(QuestionBankQuestionVO::objToVo)
                .collect(Collectors.toList());

        // 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream()
                .map(QuestionBankQuestion::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
        });

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    /**
     * 批量向题库添加题目
     *
     * @param questionIdList 题目 id 列表
     * @param questionBankId 题库 id
     * @param loginUser 当前登录用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionsToQuestionBank(List<Long> questionIdList, Long questionBankId, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 检查题目 id 是否存在
        LambdaQueryWrapper<Question> questionIdLambdaQueryWrapper = Wrappers.lambdaQuery(Question.class)
                .select(Question::getId)
                .in(Question::getId, questionIdList);
        // 合法的题目 id
        List<Long> validQuestionIdList = questionService.listObjs(questionIdLambdaQueryWrapper, obj -> (Long) obj);
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "合法的题目列表不存在");
        // 检查题库 id 是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR, "题库不存在");

        // 检查哪些题目还不在题库中，避免重复插入
        LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, validQuestionIdList);
        List<QuestionBankQuestion> existQuestionList = this.list(lambdaQueryWrapper);
        // 已存在于题库中的题目 id
        Set<Long> existQuestionIdSet = existQuestionList.stream()
                .map(QuestionBankQuestion::getQuestionId)
                .collect(Collectors.toSet());
        // 已存在于题库中的题目 id，不需要再次添加
        validQuestionIdList = validQuestionIdList.stream()
                .filter(questionId -> !existQuestionIdSet.contains(questionId))
                .collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "所有的题目都已存在于题库中");

        // 自定义线程池（IO 密集型）
        ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                20,    // 核心线程数
                50,                 // 最大线程数
                60L,                // 线程空闲存活时间
                TimeUnit.SECONDS,   // 存活时间单位
                new LinkedBlockingDeque<>(10000),  // 阻塞队列容量
                new ThreadPoolExecutor.CallerRunsPolicy()   // 拒绝策略：由调用线程处理任务
        );

        // 保留所有批次任务
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        // 分批处理避免长事务，每次处理 1000 条数据
        int batchSize = 1000;
        int totalQuestionListSize = validQuestionIdList.size();
        for (int i = 0; i < totalQuestionListSize; i += batchSize) {
            // 生成每批次的数据
            List<Long> subList = validQuestionIdList.subList(i, Math.min(i + batchSize, totalQuestionListSize));
            List<QuestionBankQuestion> questionBankQuestionList = subList.stream()
                    .map(questionId -> {
                        QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                        questionBankQuestion.setQuestionBankId(questionBankId);
                        questionBankQuestion.setQuestionId(questionId);
                        questionBankQuestion.setUserId(loginUser.getId());
                        return questionBankQuestion;
                    }).toList();
            // 获取代理，Spring 事务依赖于代理，内部直接使用 this 调用方法，不会触发事务
            QuestionBankQuestionService questionBankQuestionService = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();
            // 异步处理每批次数据，将任务添加到异步任务列表
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 使用事务处理每批数据
                questionBankQuestionService.batchAddQuestionsToQuestionBankInner(questionBankQuestionList);
            }, customExecutor).exceptionally(ex -> {
                log.error("批处理任务执行失败", ex);
                return null;
            });
            futureList.add(future);
        }
        // 等待所有批次完成操作
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        // 关闭线程池
        customExecutor.shutdown();
    }

    /**
     * 批量向题库添加题目（事务，仅供内部使用）
     *
     * @param questionBankQuestionList 题库题目关联列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionsToQuestionBankInner(List<QuestionBankQuestion> questionBankQuestionList) {
        try {
            boolean result = this.saveBatch(questionBankQuestionList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
        } catch (DataIntegrityViolationException e) {
            log.error("数据库唯一键冲突或违反其他完整性约束，错误信息：{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题库已存在与该题库，无法重复添加");
        } catch (DataAccessException e) {
            log.error("数据库连接问题、事物问题等导致操作失败，错误信息：{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
        } catch (Exception e) {
            // 捕获其他异常，做通用处理
            log.error("添加题目到题库时发生未知错误，错误信息：{}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
        }
    }

    /**
     * 批量从题库移除题目
     *
     * @param questionIdList 题目 id 列表
     * @param questionBankId 题库 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionsFromQuestionBank(List<Long> questionIdList, Long questionBankId) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "题目列表为空");
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库非法");
        // 执行移除关联
        for (Long questionId : questionIdList) {
            // 构造查询
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            boolean result = this.remove(lambdaQueryWrapper);
            ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "从题库移除题目失败");
        }
    }

}
