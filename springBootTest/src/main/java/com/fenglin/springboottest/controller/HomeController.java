package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.ContentService;
import com.fenglin.springboottest.vo.AnnouncementVO;
import com.fenglin.springboottest.vo.DetailVO;
import com.fenglin.springboottest.vo.OverviewVO;
import com.fenglin.springboottest.vo.SearchItemVO;
import com.fenglin.springboottest.vo.SearchResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 魔族修行 · 首页内容接口
 * 基底址：/api/home
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private ContentService contentService;

    /** 首页概览 GET /api/home/overview → 各模块数量 + 精选 */
    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        return Result.success(contentService.overview());
    }

    /** 跨表检索 GET /api/home/search?kw=关键字 → 分组结果 */
    @GetMapping("/search")
    public Result<SearchResultVO> search(
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        return Result.success(contentService.search(kw == null ? "" : kw.trim()));
    }

    /** 魔门公告 GET /api/home/announcements → 公告列表（按时间倒序） */
    @GetMapping("/announcements")
    public Result<List<AnnouncementVO>> announcements() {
        return Result.success(contentService.announcements());
    }

    /** 热门修习 GET /api/home/highlights → 各模块最新 2 条混合推荐 */
    @GetMapping("/highlights")
    public Result<List<SearchItemVO>> highlights() {
        return Result.success(contentService.highlights());
    }

    /** 模块列表 GET /api/home/list?type=art&kw= → 该模块全部条目（kw 可选过滤） */
    @GetMapping("/list")
    public Result<List<SearchItemVO>> list(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        return Result.success(contentService.list(type, kw));
    }

    /** 内容详情 GET /api/home/detail?type=art&id=1 → 单条完整信息（含正文与属性表） */
    @GetMapping("/detail")
    public Result<DetailVO> detail(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "id", required = false) Long id) {
        DetailVO vo = contentService.detail(type, id);
        if (vo == null) {
            return Result.error(404, "魔渊之中并无此记载");
        }
        return Result.success(vo);
    }
}
