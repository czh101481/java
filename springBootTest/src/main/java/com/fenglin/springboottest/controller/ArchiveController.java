package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.Archive;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.ArchiveService;
import com.fenglin.springboottest.vo.ArchiveCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 魔枢典册 · 典籍库接口
 * 基底址：/api/archive
 * 境界门槛：开卷阅册需修至「魔将」境（分类卡片公开）
 */
@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private DemonUserMapper userMapper;

    /** 分类卡片 GET /api/archive/categories → 4 张入口卡片（含条目数；公开） */
    @GetMapping("/categories")
    public Result<List<ArchiveCategoryVO>> categories() {
        return Result.success(archiveService.categories());
    }

    /** 条目列表 GET /api/archive/list?cat=lineage&kw= → 该分类全部条目（含正文；需魔将境） */
    @GetMapping("/list")
    public Result<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "cat", required = false, defaultValue = "") String cat,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkRealm(authorization);
        if (denied != null) {
            return Result.error(403, denied);
        }
        return Result.success(archiveService.list(cat, kw));
    }

    /** 单条详情 GET /api/archive/detail?id=1（需魔将境） */
    @GetMapping("/detail")
    public Result<?> detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "id", required = false) Long id) {
        String denied = checkRealm(authorization);
        if (denied != null) {
            return Result.error(403, denied);
        }
        Archive a = archiveService.detail(id);
        if (a == null) {
            return Result.error(404, "典册之中并无此篇");
        }
        return Result.success(a);
    }

    /** 境界门槛校验：需修至魔将境方可开卷 */
    private String checkRealm(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return "未持魔契，请先入魔道。";
        }
        Long userId = tokenManager.getUserId(authorization.substring(7).trim());
        if (userId == null) {
            return "魔契已逝，请重入魔道。";
        }
        DemonUser user = userMapper.selectById(userId);
        if (user == null) {
            return "修者不复存在。";
        }
        if (!Realm.satisfied(user.getCultivation(), "archive")) {
            return String.format(Realm.DENY_MSG, Realm.name(Realm.requirementOf("archive")));
        }
        return null;
    }
}
