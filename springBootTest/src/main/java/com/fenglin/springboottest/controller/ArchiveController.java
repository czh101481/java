package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Archive;
import com.fenglin.springboottest.service.ArchiveService;
import com.fenglin.springboottest.vo.ArchiveCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 魔枢典册 · 典籍库接口
 * 基底址：/api/archive
 */
@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    /** 分类卡片 GET /api/archive/categories → 4 张入口卡片（含条目数） */
    @GetMapping("/categories")
    public Result<List<ArchiveCategoryVO>> categories() {
        return Result.success(archiveService.categories());
    }

    /** 条目列表 GET /api/archive/list?cat=lineage&kw= → 该分类全部条目（含正文） */
    @GetMapping("/list")
    public Result<List<Archive>> list(
            @RequestParam(value = "cat", required = false, defaultValue = "") String cat,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        return Result.success(archiveService.list(cat, kw));
    }

    /** 单条详情 GET /api/archive/detail?id=1 */
    @GetMapping("/detail")
    public Result<Archive> detail(@RequestParam(value = "id", required = false) Long id) {
        Archive a = archiveService.detail(id);
        if (a == null) {
            return Result.error(404, "典册之中并无此篇");
        }
        return Result.success(a);
    }
}
