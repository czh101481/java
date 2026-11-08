package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.PasswordEncoder;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.dto.ChangePasswordDTO;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.entity.AvatarFile;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.AvatarFileMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
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

    /** 头像图片本体（base64 存库，随全量快照跨机器迁移） */
    @Autowired
    private AvatarFileMapper avatarFileMapper;

    @Autowired
    private TokenManager tokenManager;

    /** 入魔道：注册即发开局礼包 */
    @Autowired
    private NewbieService newbieService;

    /** 入魔道：校验道号 / 密令，签发魔契令牌 */
    public Result<LoginVO> login(LoginDTO dto) {
        if (!StringUtils.hasText(dto.getName()) || !StringUtils.hasText(dto.getPassword())) {
            return Result.error(400, "道号与密令皆不可为空。");
        }
        DemonUser user = userMapper.selectOne(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getName, dto.getName()));
        if (!verifyPassword(user, dto.getPassword())) {
            return Result.error(401, "道号或密令有误，魔门不纳。");
        }
        String token = tokenManager.createToken(user.getId());
        user.setPassword(null); // 密令不外泄
        return Result.success(new LoginVO(token, user));
    }

    /**
     * 校验密令：命中即返回 true。
     * 若该账号仍是历史明文存储，登录成功后顺手升级为 PBKDF2 哈希——
     * 存量用户无感，新写入的一律是哈希。
     */
    public boolean verifyPassword(DemonUser user, String raw) {
        if (user == null || !PasswordEncoder.matches(raw, user.getPassword())) {
            return false;
        }
        if (!PasswordEncoder.isEncoded(user.getPassword())) {
            DemonUser patch = new DemonUser();
            patch.setId(user.getId());
            patch.setPassword(PasswordEncoder.encode(raw));
            userMapper.updateById(patch);
        }
        return true;
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
        user.setPassword(PasswordEncoder.encode(dto.getPassword()));
        user.setPermission(1); // 注册修者皆为默认权限，管理员需另行提权
        userMapper.insert(user);
        // 入魔道：发放开局资粮并开启 14 天新兵期（数值见 common/NewbieRule）
        newbieService.grantStarterOnRegister(user.getId());
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

    /**
     * 断魔契：销毁当前令牌。
     * 令牌无效也返回成功——登出本就应该「无论如何都退出」，不给出可枚举的失败信号。
     */
    public Result<Void> logout(String token) {
        if (StringUtils.hasText(token)) {
            tokenManager.remove(token.trim());
        }
        return Result.success();
    }

    /**
     * 改密令：旧密令校验 → 新密令强度校验 → 落库 → 使该修者其余令牌失效。
     * 当前会话保留，避免改完密码立刻被踢下线。
     */
    public Result<java.util.Map<String, Object>> changePassword(String token, ChangePasswordDTO dto) {
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
        if (dto == null || !StringUtils.hasText(dto.getOldPassword())
                || !StringUtils.hasText(dto.getNewPassword())) {
            return Result.error(400, "旧密令与新密令皆需填写。");
        }
        if (!PasswordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.error(400, "旧密令有误，心魔未认。");
        }
        if (dto.getNewPassword().length() < 6) {
            return Result.error(400, "新密令需六字以上，方足以封印心魔。");
        }
        if (PasswordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            return Result.error(400, "新密令与旧密令相同，无需更改。");
        }

        DemonUser patch = new DemonUser();
        patch.setId(userId);
        patch.setPassword(PasswordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(patch);

        // 其他设备上的魔契一并作废，仅留当前这条
        int revoked = tokenManager.removeOthersOfUser(userId, token);

        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("revoked", revoked);
        data.put("msg", revoked > 0
                ? "密令已换，其余 " + revoked + " 处魔契已断。"
                : "密令已换，此契仍续。");
        return Result.success(data);
    }

    /** 更换头像：校验令牌 + 代号白名单，成功后返回最新修者信息 */    public Result<DemonUser> updateAvatar(String token, String avatar) {
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
        String old = user.getAvatar();
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
        // 若原本是上传的图片、现改为预设/默认，清理旧文件
        deleteUploaded(old);
        user.setPassword(null);
        return Result.success(user);
    }

    /** 上传自定义头像：校验图片 → 图片本体入 demon_avatar_file → 落盘备份 → 更新 avatar 路径 */
    public Result<DemonUser> uploadAvatar(String token, MultipartFile file) {
        if (!StringUtils.hasText(token)) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Long userId = tokenManager.getUserId(token);
        if (userId == null) {
            return Result.error(401, "魔契已逝，请重入魔道。");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "未择取图像，无从刻印。");
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            return Result.error(400, "图像不得超过 2MB。");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅可上传图像。");
        }
        String ext = "jpg";
        String original = file.getOriginalFilename();
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp").contains(ext)) {
            return Result.error(400, "此图格式不可取用（仅 jpg/png/gif/webp/bmp）。");
        }

        DemonUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "修者不复存在。");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            return Result.error(500, "刻印失败，图像未能读取。");
        }
        if (bytes.length == 0) {
            return Result.error(400, "图像空空如也，无从刻印。");
        }

        String filename = userId + "_" + System.currentTimeMillis() + "." + ext;
        String url = "/uploads/avatars/" + filename;

        // ① 图片本体入 demon_avatar_file —— 权威副本，随 demon_db.sql 快照一起迁移。
        //    换电脑导入快照后，即使 uploads/ 目录为空，英杰榜/道牌/传音仍能取到图。
        try {
            AvatarFile rec = new AvatarFile();
            rec.setFilePath(url);
            rec.setMime(mimeOf(ext));
            rec.setImgBase64(Base64.getEncoder().encodeToString(bytes));
            rec.setCreatedAt(LocalDateTime.now());
            AvatarFile exists = avatarFileMapper.selectOne(
                    new LambdaQueryWrapper<AvatarFile>()
                            .eq(AvatarFile::getFilePath, url).last("LIMIT 1"));
            if (exists == null) {
                avatarFileMapper.insert(rec);
            } else {
                rec.setId(exists.getId());
                avatarFileMapper.updateById(rec);
            }
        } catch (Exception e) {
            return Result.error(500, "刻印失败，图像未能入藏经阁。");
        }

        // ② 磁盘仅作本机备份，写失败不影响主流程
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars").toAbsolutePath();
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), bytes);
        } catch (IOException ignored) {
            /* 库里已有权威副本 */
        }

        String old = user.getAvatar();
        userMapper.update(null, new UpdateWrapper<DemonUser>()
                .eq("id", userId).set("avatar", url));
        // 清理上一张自定义头像（库记录 + 磁盘文件，预设 key 不动）
        deleteUploaded(old);

        user.setAvatar(url);
        user.setPassword(null);
        return Result.success(user);
    }

    /** 由扩展名定 mime（不直接信任客户端传来的 contentType） */
    private static String mimeOf(String ext) {
        switch (ext) {
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            default:
                return "image/jpeg";
        }
    }

    /** 删除旧的自定义头像（库记录 + 磁盘文件；预设 key / 空值不动） */
    private void deleteUploaded(String oldAvatar) {
        if (oldAvatar == null || !oldAvatar.startsWith("/uploads/")) {
            return;
        }
        // 库记录优先删：跨机器后磁盘可能不存在，但库里的旧图必须清掉，
        // 否则快照会一直背着一张无人引用的图片。
        try {
            avatarFileMapper.delete(new LambdaQueryWrapper<AvatarFile>()
                    .eq(AvatarFile::getFilePath, oldAvatar));
        } catch (Exception ignored) {
            /* 表尚未建立时不影响主流程 */
        }
        try {
            Path base = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            Path old = base.resolve(oldAvatar.replaceFirst("^/+", "")).normalize();
            if (old.startsWith(base.resolve("uploads")) && Files.exists(old)) {
                Files.deleteIfExists(old);
            }
        } catch (IOException ignored) {
            /* 清理失败不影响主流程 */
        }
    }
}
