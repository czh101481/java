package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AvatarFile;
import org.apache.ibatis.annotations.Mapper;

/** 头像图片本体（base64 存库），供上传写入与 /uploads/** 取图读取。 */
@Mapper
public interface AvatarFileMapper extends BaseMapper<AvatarFile> {
}
