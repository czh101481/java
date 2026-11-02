package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.QuizRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论道答题记录 Mapper
 */
@Mapper
public interface QuizRecordMapper extends BaseMapper<QuizRecord> {
}
