package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 魔族修行 · 认证业务
 */
@Service
public class AuthService {

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private TokenManager tokenManager;

    /** 入魔道：校验道号 / 密令，签发魔契令牌 */
    public Result<LoginVO> login(LoginDTO dto) {
        if (!StringUtils.hasText(dto.getName()) || !StringUtils.hasText(dto.getPassword())) {
            return Result.error(400, "道号与密令皆不可为空。");
        }
        DemonUser user = userMapper.selectOne(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getName, dto.getName()));
        if (user == null || !dto.getPassword().equals(user.getPassword())) {
            return Result.error(401, "道号或密令有误，魔门不纳。");
        }
        String token = tokenManager.createToken(user.getId());
        user.setPassword(null); // 密令不外泄
        return Result.success(new LoginVO(token, user));
    }

    /** 开辟魔渊：注册新修者，含道号 / 邮箱唯一性校验 */
    public Result<Void> register(RegisterDTO dto) {
        if (!StringUtils.hasText(dto.getName())
                || !StringUtils.hasText(dto.getEmail())
                || !StringUtils.hasText(dto.getRealm())
                || !StringUtils.hasText(dto.getPassword())) {
            return Result.error(400, "道号、印记、魔种、密令皆需填写。");
        }
        if (dto.getPassword().length() < 6) {
            return Result.error(400, "密令需六字以上，方足以封印心魔。");
        }
        long nameCount = userMapper.selectCount(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getName, dto.getName()));
        if (nameCount > 0) {
            return Result.error(409, "道号已被刻入魔碑，不可重立。");
        }
        long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getEmail, dto.getEmail()));
        if (emailCount > 0) {
            return Result.error(409, "灵识印记已有主人，不可复用。");
        }

        DemonUser user = new DemonUser();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRealm(dto.getRealm());
        user.setPassword(dto.getPassword());
        userMapper.insert(user);
        return Result.success();
    }

    /** 持令问询：校验令牌，返回当前修者信息 */
    public Result<DemonUser> me(String token) {
        if (!StringUtils.hasText(token)) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Long userId = tokenManager.getUserId(token);
        if (userId == null) {
            return Result.error(401, "魔契已逝，请重入魔道。");
        }
        DemonUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "修者不复存在。");
        }
        user.setPassword(null);
        return Result.success(user);
    }
}
