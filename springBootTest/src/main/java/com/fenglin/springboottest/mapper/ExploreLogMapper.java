package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ExploreLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参悟日志 Mapper，对应表 demon_explore_log
 */
@Mapper
public interface ExploreLogMapper extends BaseMapper<ExploreLog> {
}
