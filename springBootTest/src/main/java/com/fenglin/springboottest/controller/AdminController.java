package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.AdminLoginDTO;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.mapper.ArtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台管理系统接口
 * 基底址：/api/admin
 * 功能：管理员登录（仅登录，无注册）+ 功法阁（demon_art）数据管理 CRUD
 * 说明：管理员凭据为演示用常量（czh101481 / czh101481），令牌内存存储，重启即失效；
 *      生产环境请接入数据库账号表 + JWT。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** 演示用管理员凭据 */
    private static final String ADMIN_USERNAME = "czh101481";
    private static final String ADMIN_PASSWORD = "czh101481";

    @Autowired
    private ArtMapper artMapper;

    /** 管理员令牌表（内存，演示用） */
    private final Set<String> adminTokens = ConcurrentHashMap.newKeySet();

    /** 管理员登录 POST /api/admin/login → 仅登录，无注册 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginDTO dto) {
        if (dto == null || !ADMIN_USERNAME.equals(dto.getUsername())
                || !ADMIN_PASSWORD.equals(dto.getPassword())) {
            return Result.error(401, "管理员账号或密令有误。");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        adminTokens.add(token);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", ADMIN_USERNAME);
        return Result.success(data);
    }

    /** 功法阁列表 GET /api/admin/art/list?kw= → 全量（id 倒序，kw 过滤功法名/简介） */
    @GetMapping("/art/list")
    public Result<List<Art>> artList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Art> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            qw.like(Art::getName, kw.trim()).or().like(Art::getSummary, kw.trim());
        }
        qw.orderByDesc(Art::getId);
        return Result.success(artMapper.selectList(qw));
    }

    /** 新增功法 POST /api/admin/art（JSON body：name/category/grade/realmReq/summary/image/content） */
    @PostMapping("/art")
    public Result<?> artAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Art art) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (art == null || !StringUtils.hasText(art.getName())) {
            return Result.error(400, "功法名不可为空。");
        }
        if (!StringUtils.hasText(art.getCategory())) {
            art.setCategory("攻伐");
        }
        if (!StringUtils.hasText(art.getGrade())) {
            art.setGrade("黄");
        }
        if (!StringUtils.hasText(art.getRealmReq())) {
            art.setRealmReq("引煞");
        }
        art.setId(null);
        artMapper.insert(art);
        return Result.success(art);
    }

    /** 修改功法 PUT /api/admin/art/{id} */
    @PutMapping("/art/{id}")
    public Result<?> artUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Art art) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Art old = artMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "魔渊之中并无此功法。");
        }
        if (art == null || !StringUtils.hasText(art.getName())) {
            return Result.error(400, "功法名不可为空。");
        }
        art.setId(id);
        artMapper.updateById(art);
        return Result.success(artMapper.selectById(id));
    }

    /** 删除功法 DELETE /api/admin/art/{id} */
    @DeleteMapping("/art/{id}")
    public Result<?> artDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (artMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此功法。");
        }
        artMapper.deleteById(id);
        return Result.success("已将该功法从功法阁除名。");
    }

    /** 管理员鉴权：有效返回 null，无效返回提示 */
    private String checkAdmin(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return "未持管理令牌，请先登录后台。";
        }
        if (!adminTokens.contains(authorization.substring(7).trim())) {
            return "管理令牌已失效，请重新登录。";
        }
        return null;
    }
}
