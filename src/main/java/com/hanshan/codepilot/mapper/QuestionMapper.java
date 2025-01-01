package com.hanshan.codepilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanshan.codepilot.model.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;


/**
* @author LEGION
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2024-11-18 20:07:05
* @Entity generator.domain.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 查询题目列表（包括已删除的数据）
     * @param minUpdateTime 最近时间内
     * @return 题目列表
     */
    @Select("select * from question where update_time >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);
}




