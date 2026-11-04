package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 魔族修行 · 认证业务
 */
@Service
public class AuthService {

    /** 允许的头像代号（与前端 src/config/avatars.js 保持一致） */
    private static final Set<String> AVATAR_KEYS = new HashSet<>(Arrays.asList(
            "blood", "ghost", "soul", "bone", "flame", "moon",
            "shadow", "thunder", "curse", "spider", "abyss", "nightmare"));

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
        user.setCultivation(com.fenglin.springboottest.common.Realm.NAMES.get(0)); // 新修者皆从引煞起步
        user.setPassword(dto.getPassword());
        user.setPermission(1); // 注册修者皆为默认权限，管理员需另行提权
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

    /** 更换头像：校验令牌 + 代号白名单，成功后返回最新修者信息 */
    public Result<DemonUser> updateAvatar(String token, String avatar) {
        if (!StringUtils.hasText(token)) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Long userId = tokenManager.getUserId(token);
        if (userId == null) {
            return Result.error(401, "魔契已逝，请重入魔道。");
        }
        // 允许置空（回退为道号首字）；非空则须在白名单内
        if (StringUtils.hasText(avatar) && !AVATAR_KEYS.contains(avatar)) {
            return Result.error(400, "此头像乃虚妄之相，不可取用。");
        }
        DemonUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "修者不复存在。");
        }
        if (StringUtils.hasText(avatar)) {
            DemonUser patch = new DemonUser();
            patch.setId(userId);
            patch.setAvatar(avatar);
            userMapper.updateById(patch);
            user.setAvatar(avatar);
        } else {
            // 置空须用 UpdateWrapper，updateById 会跳过 null 字段
            userMapper.update(null, new UpdateWrapper<DemonUser>()
                    .eq("id", userId).set("avatar", null));
            user.setAvatar(null);
        }
        user.setPassword(null);
        return Result.success(user);
    }
}
