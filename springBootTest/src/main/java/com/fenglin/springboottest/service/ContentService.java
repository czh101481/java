package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.Announcement;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.Codex;
import com.fenglin.springboottest.entity.Debate;
import com.fenglin.springboottest.entity.Trial;
import com.fenglin.springboottest.mapper.AnnouncementMapper;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.CodexMapper;
import com.fenglin.springboottest.mapper.DebateMapper;
import com.fenglin.springboottest.mapper.TrialMapper;
import com.fenglin.springboottest.vo.AnnouncementVO;
import com.fenglin.springboottest.vo.DetailVO;
import com.fenglin.springboottest.vo.OverviewVO;
import com.fenglin.springboottest.vo.SearchGroupVO;
import com.fenglin.springboottest.vo.SearchItemVO;
import com.fenglin.springboottest.vo.SearchResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 魔族修行 · 首页内容业务（功法阁 / 历练录 / 宗门论道 / 魔典秘录）
 */
@Service
public class ContentService {

    @Autowired
    private ArtMapper artMapper;
    @Autowired
    private TrialMapper trialMapper;
    @Autowired
    private DebateMapper debateMapper;
    @Autowired
    private CodexMapper codexMapper;
    @Autowired
    private AnnouncementMapper announcementMapper;

    private static final String ART = "art";
    private static final String TRIAL = "trial";
    private static final String DEBATE = "debate";
    private static final String CODEX = "codex";
    private static final String ART_NAME = "功法阁";
    private static final String TRIAL_NAME = "历练录";
    private static final String DEBATE_NAME = "宗门论道";
    private static final String CODEX_NAME = "魔典秘录";

    /** 首页概览：各模块数量 + 精选（取最新 3 条） */
    public OverviewVO overview() {
        OverviewVO vo = new OverviewVO();
        vo.setArtCount(artMapper.selectCount(null));
        vo.setTrialCount(trialMapper.selectCount(null));
        vo.setDebateCount(debateMapper.selectCount(null));
        vo.setCodexCount(codexMapper.selectCount(null));

        vo.setArtFeatured(top3(artMapper.selectList(
                new LambdaQueryWrapper<Art>().orderByDesc(Art::getId)), ART, ART_NAME));
        vo.setTrialFeatured(top3(trialMapper.selectList(
                new LambdaQueryWrapper<Trial>().orderByDesc(Trial::getId)), TRIAL, TRIAL_NAME));
        vo.setDebateFeatured(top3(debateMapper.selectList(
                new LambdaQueryWrapper<Debate>().orderByDesc(Debate::getId)), DEBATE, DEBATE_NAME));
        vo.setCodexFeatured(top3(codexMapper.selectList(
                new LambdaQueryWrapper<Codex>().orderByDesc(Codex::getId)), CODEX, CODEX_NAME));
        return vo;
    }

    /** 跨表检索：按关键字匹配各表相关字段，返回分组结果 */
    public SearchResultVO search(String kw) {
        SearchResultVO result = new SearchResultVO();
        result.setKeyword(kw);
        List<SearchGroupVO> groups = new ArrayList<>();

        if (StringUtils.hasText(kw)) {
            String like = "%" + kw + "%";

            List<Art> arts = artMapper.selectList(new LambdaQueryWrapper<Art>()
                    .and(w -> w.like(Art::getName, like)
                            .or().like(Art::getCategory, like)
                            .or().like(Art::getGrade, like)
                            .or().like(Art::getRealmReq, like)
                            .or().like(Art::getSummary, like)));
            addGroup(groups, ART, ART_NAME, toItems(arts, ART, ART_NAME));

            List<Trial> trials = trialMapper.selectList(new LambdaQueryWrapper<Trial>()
                    .and(w -> w.like(Trial::getTitle, like)
                            .or().like(Trial::getRegion, like)
                            .or().like(Trial::getRealmReq, like)
                            .or().like(Trial::getDifficulty, like)
                            .or().like(Trial::getSummary, like)));
            addGroup(groups, TRIAL, TRIAL_NAME, toItems(trials, TRIAL, TRIAL_NAME));

            List<Debate> debates = debateMapper.selectList(new LambdaQueryWrapper<Debate>()
                    .and(w -> w.like(Debate::getTitle, like)
                            .or().like(Debate::getSpeaker, like)
                            .or().like(Debate::getSummary, like)));
            addGroup(groups, DEBATE, DEBATE_NAME, toItems(debates, DEBATE, DEBATE_NAME));

            List<Codex> codexes = codexMapper.selectList(new LambdaQueryWrapper<Codex>()
                    .and(w -> w.like(Codex::getTitle, like)
                            .or().like(Codex::getVolume, like)
                            .or().like(Codex::getSummary, like)));
            addGroup(groups, CODEX, CODEX_NAME, toItems(codexes, CODEX, CODEX_NAME));
        }

        long total = groups.stream().mapToLong(SearchGroupVO::getTotal).sum();
        result.setTotal(total);
        result.setGroups(groups);
        return result;
    }

    private <T> List<SearchItemVO> top3(List<T> list, String type, String typeName) {
        return list.stream().limit(3).map(o -> toItem(o, type, typeName)).collect(Collectors.toList());
    }

    private void addGroup(List<SearchGroupVO> groups, String type, String typeName, List<SearchItemVO> items) {
        if (items.isEmpty()) return;
        SearchGroupVO g = new SearchGroupVO();
        g.setType(type);
        g.setTypeName(typeName);
        g.setTotal(items.size());
        g.setItems(items);
        groups.add(g);
    }

    private SearchItemVO toItem(Object o, String type, String typeName) {
        SearchItemVO item = new SearchItemVO();
        item.setType(type);
        item.setTypeName(typeName);
        if (o instanceof Art a) {
            item.setId(a.getId());
            item.setTitle(a.getName());
            item.setSummary(a.getSummary());
            item.setExtra(a.getGrade() + "·" + a.getRealmReq());
        } else if (o instanceof Trial t) {
            item.setId(t.getId());
            item.setTitle(t.getTitle());
            item.setSummary(t.getSummary());
            item.setExtra(t.getRegion() + "·" + t.getDifficulty());
        } else if (o instanceof Debate d) {
            item.setId(d.getId());
            item.setTitle(d.getTitle());
            item.setSummary(d.getSummary());
            item.setExtra("论者：" + d.getSpeaker());
        } else if (o instanceof Codex c) {
            item.setId(c.getId());
            item.setTitle(c.getTitle());
            item.setSummary(c.getSummary());
            item.setExtra(c.getVolume());
        }
        return item;
    }

    private List<SearchItemVO> toItems(List<?> list, String type, String typeName) {
        List<SearchItemVO> items = new ArrayList<>();
        for (Object o : list) {
            items.add(toItem(o, type, typeName));
        }
        return items;
    }

    /** 魔门公告：按发布时间倒序返回全部公告 */
    public List<AnnouncementVO> announcements() {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreatedAt));
        List<AnnouncementVO> vos = new ArrayList<>();
        for (Announcement a : list) {
            AnnouncementVO v = new AnnouncementVO();
            v.setId(a.getId());
            v.setTitle(a.getTitle());
            v.setContent(a.getContent());
            v.setLevel(a.getLevel());
            v.setCreatedAt(a.getCreatedAt() == null ? "" : a.getCreatedAt().toString().replace("T", " "));
            vos.add(v);
        }
        return vos;
    }

    /** 热门修习：每个模块各取最新 2 条，混合为横向推荐列表 */
    public List<SearchItemVO> highlights() {
        List<SearchItemVO> items = new ArrayList<>();
        items.addAll(toItems(artMapper.selectList(
                new LambdaQueryWrapper<Art>().orderByDesc(Art::getId).last("LIMIT 2")), ART, ART_NAME));
        items.addAll(toItems(trialMapper.selectList(
                new LambdaQueryWrapper<Trial>().orderByDesc(Trial::getId).last("LIMIT 2")), TRIAL, TRIAL_NAME));
        items.addAll(toItems(debateMapper.selectList(
                new LambdaQueryWrapper<Debate>().orderByDesc(Debate::getId).last("LIMIT 2")), DEBATE, DEBATE_NAME));
        items.addAll(toItems(codexMapper.selectList(
                new LambdaQueryWrapper<Codex>().orderByDesc(Codex::getId).last("LIMIT 2")), CODEX, CODEX_NAME));
        return items;
    }

    /** 模块列表：按类型返回条目（kw 非空时按关键字过滤），倒序（新收录在前） */
    public List<SearchItemVO> list(String type, String kw) {
        String t = type == null ? "" : type;
        String like = StringUtils.hasText(kw) ? "%" + kw.trim() + "%" : null;

        if (ART.equals(t)) {
            LambdaQueryWrapper<Art> w = new LambdaQueryWrapper<Art>().orderByDesc(Art::getId);
            if (like != null) {
                w.and(x -> x.like(Art::getName, like).or().like(Art::getCategory, like)
                        .or().like(Art::getGrade, like).or().like(Art::getSummary, like));
            }
            return toItems(artMapper.selectList(w), ART, ART_NAME);
        }
        if (TRIAL.equals(t)) {
            LambdaQueryWrapper<Trial> w = new LambdaQueryWrapper<Trial>().orderByDesc(Trial::getId);
            if (like != null) {
                w.and(x -> x.like(Trial::getTitle, like).or().like(Trial::getRegion, like)
                        .or().like(Trial::getDifficulty, like).or().like(Trial::getSummary, like));
            }
            return toItems(trialMapper.selectList(w), TRIAL, TRIAL_NAME);
        }
        if (DEBATE.equals(t)) {
            LambdaQueryWrapper<Debate> w = new LambdaQueryWrapper<Debate>().orderByDesc(Debate::getId);
            if (like != null) {
                w.and(x -> x.like(Debate::getTitle, like).or().like(Debate::getSpeaker, like)
                        .or().like(Debate::getSummary, like));
            }
            return toItems(debateMapper.selectList(w), DEBATE, DEBATE_NAME);
        }
        if (CODEX.equals(t)) {
            LambdaQueryWrapper<Codex> w = new LambdaQueryWrapper<Codex>().orderByDesc(Codex::getId);
            if (like != null) {
                w.and(x -> x.like(Codex::getTitle, like).or().like(Codex::getVolume, like)
                        .or().like(Codex::getSummary, like));
            }
            return toItems(codexMapper.selectList(w), CODEX, CODEX_NAME);
        }
        return new ArrayList<>();
    }

    /** 内容详情：按类型 + id 取单条完整信息（含正文与属性表），不存在返回 null */
    public DetailVO detail(String type, Long id) {
        String t = type == null ? "" : type;
        if (id == null) return null;
        DetailVO vo = new DetailVO();

        if (ART.equals(t)) {
            Art a = artMapper.selectById(id);
            if (a == null) return null;
            vo.setType(ART);
            vo.setTypeName(ART_NAME);
            vo.setId(a.getId());
            vo.setTitle(a.getName());
            vo.setSummary(a.getSummary());
            vo.setContent(a.getContent());
            vo.getMeta().add(new DetailVO.Meta("门 类", nvl(a.getCategory())));
            vo.getMeta().add(new DetailVO.Meta("品 阶", nvl(a.getGrade())));
            vo.getMeta().add(new DetailVO.Meta("所需境界", nvl(a.getRealmReq())));
            return vo;
        }
        if (TRIAL.equals(t)) {
            Trial x = trialMapper.selectById(id);
            if (x == null) return null;
            vo.setType(TRIAL);
            vo.setTypeName(TRIAL_NAME);
            vo.setId(x.getId());
            vo.setTitle(x.getTitle());
            vo.setSummary(x.getSummary());
            vo.setContent(x.getSummary());
            vo.getMeta().add(new DetailVO.Meta("地 域", nvl(x.getRegion())));
            vo.getMeta().add(new DetailVO.Meta("所需境界", nvl(x.getRealmReq())));
            vo.getMeta().add(new DetailVO.Meta("凶 险", nvl(x.getDifficulty())));
            return vo;
        }
        if (DEBATE.equals(t)) {
            Debate d = debateMapper.selectById(id);
            if (d == null) return null;
            vo.setType(DEBATE);
            vo.setTypeName(DEBATE_NAME);
            vo.setId(d.getId());
            vo.setTitle(d.getTitle());
            vo.setSummary(d.getSummary());
            vo.setContent(d.getSummary());
            vo.getMeta().add(new DetailVO.Meta("论 者", nvl(d.getSpeaker())));
            return vo;
        }
        if (CODEX.equals(t)) {
            Codex c = codexMapper.selectById(id);
            if (c == null) return null;
            vo.setType(CODEX);
            vo.setTypeName(CODEX_NAME);
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setSummary(c.getSummary());
            vo.setContent(c.getSummary());
            vo.getMeta().add(new DetailVO.Meta("卷 册", nvl(c.getVolume())));
            return vo;
        }
        return null;
    }

    private String nvl(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }
}
