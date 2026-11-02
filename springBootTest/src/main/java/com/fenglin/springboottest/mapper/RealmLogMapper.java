package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.RealmLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秘境探索日志 Mapper
 */
@Mapper
public interface RealmLogMapper extends BaseMapper<RealmLog> {
}
