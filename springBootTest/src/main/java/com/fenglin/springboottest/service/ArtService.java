package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.UserArt;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.UserArtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 功法阁业务（分重 · 货币购得）
 *
 * 分重规则：黄 1-4 重 · 玄 1-6 重 · 地 1-7 重 · 天 1-10 重 · 仙 1-10 重。
 * 同一系列（series）须按重递进：习得第 N 重前，须已习得该系列第 N-1 重。
 *
 * 购买校验顺序：上架 → 重数合法 → 境界 → 是否已习 → 前置重 → 功法数上限 → 扣款。
 */
@Service
public class ArtService {

    /** 各品阶重数上限 */
    public static final Map<String, Integer> GRADE_MAX_LAYER = Map.of(
            "黄", 4, "玄", 6, "地", 7, "天", 10, "仙", 10);

    /** 引煞境可驾驭功法数（之后每境 +2） */
    public static final int BASE_ART_CAP = 5;

    /** 每提升一境增加的功法上限 */
    public static final int ART_CAP_PER_REALM = 2;

    @Autowired
    private ArtMapper artMapper;

    @Autowired
    private UserArtMapper userArtMapper;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private CoinService coinService;

    @Autowired
    private CultivateService cultivateService;

    /** 业务拒绝（文案直接给玩家看） */
    public static class Deny extends RuntimeException {
        public Deny(String message) {
            super(message);
        }
    }

    /** 品阶重数上限（未知品阶按黄级 4 重） */
    public static int maxLayerOf(String grade) {
        return GRADE_MAX_LAYER.getOrDefault(grade == null ? "" : grade, 4);
    }

    /** 重数中文（1~10） */
    public static String layerText(Integer layer) {
        int n = layer == null ? 1 : Math.max(1, layer);
        String[] names = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        return names[Math.min(n, 10) - 1] + "重";
    }

    /** 品阶·重，如「黄·三重」 */
    public static String gradeText(Art a) {
        return nvl(a.getGrade()) + "·" + layerText(a.getLayer());
    }

    /** 售价文案，如「魔晶 260」/「血玉 8」/「免费」 */
    public static String priceText(Art a) {
        if (a.getPriceJade() != null && a.getPriceJade() > 0) {
            return Coin.JADE_NAME + " " + a.getPriceJade();
        }
        if (a.getPriceCoin() != null && a.getPriceCoin() > 0) {
            return Coin.COIN_NAME + " " + a.getPriceCoin();
        }
        return "免费";
    }

    /** 功法战力（与数值面板同口径） */
    public static int powerOf(Art a) {
        return CultivateService.artPower(a);
    }

    /** 该修者可驾驭的功法数上限 */
    public int capOf(DemonUser user) {
        return BASE_ART_CAP + Realm.indexOf(user.getCultivation()) * ART_CAP_PER_REALM;
    }

    /** 购得功法：校验 + 扣款 + 入库 */
    public Map<String, Object> buy(Long userId, Long artId) {
        if (userId == null) {
            throw new Deny("未持魔契，请先入魔道。");
        }
        if (artId == null) {
            throw new Deny("未指明欲购之功法。");
        }
        DemonUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new Deny("未持魔契，请先入魔道。");
        }
        Art art = artMapper.selectById(artId);
        if (art == null) {
            throw new Deny("魔渊之中并无此功法。");
        }
        if (art.getStatus() != null && art.getStatus() == 0) {
            throw new Deny("此功法已从功法阁撤下，暂且求不得。");
        }

        int layer = art.getLayer() == null ? 1 : art.getLayer();
        int maxLayer = maxLayerOf(art.getGrade());
        if (layer < 1 || layer > maxLayer) {
            throw new Deny(String.format("「%s」品阶功法至多 %d 重，此卷配置为第 %d 重，暂不可习。",
                    nvl(art.getGrade()), maxLayer, layer));
        }

        int myIndex = Realm.indexOf(user.getCultivation());
        int needIndex = Realm.indexOf(art.getRealmReq());
        if (myIndex < needIndex) {
            throw new Deny(String.format("修为不足，需修至「%s」境方可习得此功。",
                    Realm.name(needIndex)));
        }

        if (owned(userId, artId)) {
            throw new Deny("此功法早已烂熟于心。");
        }

        // 同系列前置重：须先习得上一重
        Art prev = previousLayer(art);
        if (prev != null && !owned(userId, prev.getId())) {
            throw new Deny(String.format("「%s」须先习得《%s》——同系列功法须按重递进。",
                    art.getName(), prev.getName()));
        }

        List<UserArt> mine = userArtMapper.selectList(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId));
        int cap = capOf(user);
        if (mine.size() >= cap) {
            throw new Deny(String.format("以「%s」境之魔识，最多同时驾驭 %d 部功法——先冲境或弃功去罢。",
                    Realm.name(myIndex), cap));
        }

        boolean useJade = art.getPriceJade() != null && art.getPriceJade() > 0;
        int amount = useJade ? art.getPriceJade() : (art.getPriceCoin() == null ? 0 : art.getPriceCoin());
        Wallet wallet = coinService.walletOf(userId);
        Wallet after = wallet;
        if (amount > 0) {
            after = coinService.spend(userId, useJade ? Coin.JADE : Coin.COIN,
                    amount, "art", "art", art.getId());
            if (after == null) {
                int balance = useJade
                        ? (wallet == null ? 0 : wallet.getJade())
                        : (wallet == null ? 0 : wallet.getCoin());
                throw new Deny(String.format("%s不足，购此功法需 %d，尚缺 %d。",
                        useJade ? Coin.JADE_NAME : Coin.COIN_NAME, amount, amount - balance));
            }
        }

        UserArt ua = new UserArt();
        ua.setUserId(userId);
        ua.setArtId(artId);
        userArtMapper.insert(ua);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", amount > 0
                ? String.format("以 %d %s 购得《%s》（%s），功法入体，数值大涨。",
                        amount, useJade ? Coin.JADE_NAME : Coin.COIN_NAME,
                        art.getName(), gradeText(art))
                : String.format("《%s》（%s）已录入道基，不取分文。", art.getName(), gradeText(art)));
        data.put("art", artBrief(art));
        data.put("payCurrency", useJade ? Coin.JADE : Coin.COIN);
        data.put("payAmount", amount);
        data.put("wallet", coinService.overview(userId));
        data.put("learned", mine.size() + 1);
        data.put("cap", cap);
        data.put("stats", cultivateService.panel(userMapper.selectById(userId)));
        return data;
    }

    /** 我习得的功法（含数值与贡献战力） */
    public List<Map<String, Object>> mine(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Art a : cultivateService.learnedArts(userId)) {
            list.add(artBrief(a));
        }
        return list;
    }

    /** 功法摘要（前端卡片/列表用） */
    public Map<String, Object> artBrief(Art a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("category", a.getCategory());
        m.put("grade", a.getGrade());
        m.put("layer", a.getLayer());
        m.put("layerText", layerText(a.getLayer()));
        m.put("gradeText", gradeText(a));
        m.put("series", a.getSeries());
        m.put("realmReq", a.getRealmReq());
        m.put("priceCoin", a.getPriceCoin());
        m.put("priceJade", a.getPriceJade());
        m.put("priceText", priceText(a));
        m.put("bonusAtk", nz(a.getBonusAtk()));
        m.put("bonusDef", nz(a.getBonusDef()));
        m.put("bonusHp", nz(a.getBonusHp()));
        m.put("bonusInsight", nz(a.getBonusInsight()));
        m.put("bonusAgility", nz(a.getBonusAgility()));
        m.put("power", powerOf(a));
        m.put("summary", a.getSummary());
        return m;
    }

    /** 同系列上一重（无前置返回 null） */
    public Art previousLayer(Art art) {
        int layer = art.getLayer() == null ? 1 : art.getLayer();
        if (layer <= 1 || !StringUtils.hasText(art.getSeries())) {
            return null;
        }
        List<Art> prevs = artMapper.selectList(new LambdaQueryWrapper<Art>()
                .eq(Art::getSeries, art.getSeries())
                .lt(Art::getLayer, layer)
                .orderByDesc(Art::getLayer)
                .last("LIMIT 1"));
        return prevs.isEmpty() ? null : prevs.get(0);
    }

    /** 是否已习得 */
    public boolean owned(Long userId, Long artId) {
        if (userId == null || artId == null) {
            return false;
        }
        return userArtMapper.selectCount(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId)
                .eq(UserArt::getArtId, artId)) > 0;
    }

    /** 已习功法 id 集合（详情页判定按钮状态，避免逐条查询） */
    public java.util.Set<Long> ownedIds(Long userId) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (userId == null) {
            return ids;
        }
        for (UserArt ua : userArtMapper.selectList(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId))) {
            ids.add(ua.getArtId());
        }
        return ids;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
