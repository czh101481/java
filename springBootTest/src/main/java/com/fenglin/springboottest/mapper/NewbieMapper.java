package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Newbie;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewbieMapper extends BaseMapper<Newbie> {
}
