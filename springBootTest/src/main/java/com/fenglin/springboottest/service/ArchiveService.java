package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Archive;
import com.fenglin.springboottest.mapper.ArchiveMapper;
import com.fenglin.springboottest.vo.ArchiveCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔枢典册 · 典籍库业务（4 大分类：谱系 / 道途 / 法则 / 秘录）
 */
@Service
public class ArchiveService {

    @Autowired
    private ArchiveMapper archiveMapper;

    /** 分类定义：键、名、说明、图标、封面、量词 */
    private static final Object[][] CATEGORIES = {
            {"lineage", "魔族谱系", "各大魔族种族、血脉、天赋", "⛧", "/images/codex/codex-6.jpg", "脉"},
            {"path",    "魔修道途", "魔修境界、修行法门、魔力运转规则", "✦", "/images/codex/codex-1.jpg", "篇"},
            {"law",     "魔域法则", "禁忌、天道枷锁、魔域独有的规则", "⚖", "/images/codex/codex-5.jpg", "条"},
            {"ancient", "上古秘录", "远古大战、旧枢机覆灭传说", "✠", "/images/codex/codex-4.jpg", "卷"}
    };

    /** 分类卡片列表（含各分类条目数） */
    public List<ArchiveCategoryVO> categories() {
        List<ArchiveCategoryVO> list = new ArrayList<>();
        for (Object[] c : CATEGORIES) {
            ArchiveCategoryVO vo = new ArchiveCategoryVO();
            vo.setKey((String) c[0]);
            vo.setName((String) c[1]);
            vo.setDesc((String) c[2]);
            vo.setIcon((String) c[3]);
            vo.setCover((String) c[4]);
            vo.setUnit((String) c[5]);
            vo.setCount(archiveMapper.selectCount(
                    new LambdaQueryWrapper<Archive>().eq(Archive::getCategory, c[0])));
            list.add(vo);
        }
        return list;
    }

    /**
     * 条目列表：cat 为空返回全部，否则按分类过滤
     * kw 非空时在标题 / 副题 / 提要中模糊匹配
     */
    public List<Archive> list(String cat, String kw) {
        LambdaQueryWrapper<Archive> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cat)) {
            w.eq(Archive::getCategory, cat.trim());
        }
        if (StringUtils.hasText(kw)) {
            String like = "%" + kw.trim() + "%";
            w.and(x -> x.like(Archive::getTitle, like)
                    .or().like(Archive::getSubtitle, like)
                    .or().like(Archive::getSummary, like));
        }
        w.orderByAsc(Archive::getCategory).orderByAsc(Archive::getSortOrder);
        return archiveMapper.selectList(w);
    }

    /** 单条详情 */
    public Archive detail(Long id) {
        return id == null ? null : archiveMapper.selectById(id);
    }
}
