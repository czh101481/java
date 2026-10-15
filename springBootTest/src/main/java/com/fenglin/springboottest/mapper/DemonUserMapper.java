package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DemonUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 魔族修者 Mapper（MyBatis-Plus）
 */
@Mapper
public interface DemonUserMapper extends BaseMapper<DemonUser> {
}
