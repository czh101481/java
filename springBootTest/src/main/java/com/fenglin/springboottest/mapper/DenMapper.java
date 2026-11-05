package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Den;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DenMapper extends BaseMapper<Den> {
}
