package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ReadRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 研读记录 Mapper
 */
@Mapper
public interface ReadRecordMapper extends BaseMapper<ReadRecord> {
}
