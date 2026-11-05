package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.ArtRank;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.BagItem;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.Item;
import com.fenglin.springboottest.entity.UserArt;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.BagItemMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.ItemMapper;
import com.fenglin.springboottest.mapper.UserArtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 功法阁（完整 / 残片 · 六重进度 · 心魔）
 *
 * 购买：上架 → 境界 → 是否已持 → 功法数上限 → 扣款（按阶位定价，后台不可改价）
 * 修炼：每部功法每日 3 课，每课耗 10 煞气；完整 +10 熟练，残片 +5 且止步大成
 * 顿悟：圆满 + 完整 +「悟道机缘」→ 入道
 * 破境：入道 + 完整 +「死战悟道」→ 无上
 * 补全：残片可付差价（完整价 - 残片价）转为完整，续修圆满/入道/无上
 */
@Service
public class ArtService {

    /** 引煞境可驾驭功法数（之后每境 +2） */
    public static final int BASE_ART_CAP = 5;

    /** 每提升一境增加的功法上限 */
    public static final int ART_CAP_PER_REALM = 2;

    /** 顿悟秘宝代号 */
    public static final String ITEM_INSIGHT = "insight_fate";

    /** 破境秘宝代号 */
    public static final String ITEM_TRANSCEND = "death_battle";

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

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private BagItemMapper bagItemMapper;

    /** 业务拒绝（文案直接给玩家看） */
    public static class Deny extends RuntimeException {
        public Deny(String message) {
            super(message);
        }
    }

    // ==================== 文案 ====================

    /** 阶位文案，如「黄阶」 */
    public static String gradeText(Art a) {
        return nvl(a.getGrade()) + "阶";
    }

    /** 完整功法售价文案 */
    public static String priceText(Art a) {
        return ArtRank.priceText(a.getGrade(), 0);
    }

    /** 残片售价文案 */
    public static String fragPriceText(Art a) {
        return ArtRank.priceText(a.getGrade(), 1);
    }

    /** 功法战力（未计形态/重数） */
    public static int powerOf(Art a) {
        return CultivateService.artPower(a);
    }

    /** 该修者可驾驭的功法数上限 */
    public int capOf(DemonUser user) {
        return BASE_ART_CAP + Realm.indexOf(user.getCultivation()) * ART_CAP_PER_REALM;
    }

    // ==================== 购得 ====================

    /**
     * 购得功法
     *
     * @param form 0=完整 1=残片
     */
    public Map<String, Object> buy(Long userId, Long artId, int form) {
        DemonUser user = require(userId);
        Art art = requireArt(artId);
        int f = form == 1 ? 1 : 0;

        if (art.getStatus() != null && art.getStatus() == 0) {
            throw new Deny("此功法已从功法阁撤下，暂且求不得。");
        }
        int myIndex = Realm.indexOf(user.getCultivation());
        int needIndex = Realm.indexOf(art.getRealmReq());
        if (myIndex < needIndex) {
            throw new Deny(String.format("修为不足，需修至「%s」境方可习得此功。", Realm.name(needIndex)));
        }

        UserArt exists = rowOf(userId, artId);
        if (exists != null) {
            if (nz(exists.getForm()) == 1 && f == 0) {
                throw new Deny(String.format(
                        "你已持有《%s》残篇——可付差价 %d %s「补全」为完整功法。",
                        art.getName(), fullPriceOf(art) - ArtRank.fragPrice(art.getGrade()),
                        ArtRank.currencyName(art.getGrade())));
            }
            throw new Deny(String.format("此功法早已%s。", nz(exists.getForm()) == 1 ? "持残篇在手" : "烂熟于心"));
        }

        List<UserArt> mine = userArtMapper.selectList(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId));
        int cap = capOf(user);
        if (mine.size() >= cap) {
            throw new Deny(String.format("以「%s」境之魔识，最多同时驾驭 %d 部功法——先冲境或弃功去罢。",
                    Realm.name(myIndex), cap));
        }

        String currency = ArtRank.currencyOf(art.getGrade());
        int amount = ArtRank.priceOf(art.getGrade(), f);
        Wallet wallet = coinService.walletOf(userId);
        if (amount > 0) {
            Wallet after = coinService.spend(userId, currency, amount, "art", "art", art.getId());
            if (after == null) {
                int balance = balanceOf(wallet, currency);
                throw new Deny(String.format("%s不足，%s此功需 %d，尚缺 %d。",
                        ArtRank.currencyName(art.getGrade()), ArtRank.formName(f),
                        amount, amount - balance));
            }
        }

        UserArt ua = new UserArt();
        ua.setUserId(userId);
        ua.setArtId(artId);
        ua.setForm(f);
        ua.setStage(1);
        ua.setProf(0);
        ua.setTrainCount(0);
        userArtMapper.insert(ua);

        String msg = amount > 0
                ? String.format("以 %d %s 购得《%s》%s，功法入体，尚在入门。",
                        amount, ArtRank.currencyName(art.getGrade()), art.getName(), ArtRank.formName(f))
                : String.format("《%s》%s已录入道基，不取分文。", art.getName(), ArtRank.formName(f));
        if (f == 1) {
            msg += "（残篇残缺，最高止步大成，威能折损四成有余。）";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", msg);
        data.put("art", mineBrief(art, ua));
        data.put("payCurrency", currency);
        data.put("payAmount", amount);
        data.put("wallet", coinService.overview(userId));
        data.put("learned", mine.size() + 1);
        data.put("cap", cap);
        data.put("stats", cultivateService.panel(userMapper.selectById(userId)));
        return data;
    }

    /** 残片补全为完整功法（付差价） */
    public Map<String, Object> complete(Long userId, Long artId) {
        DemonUser user = require(userId);
        Art art = requireArt(artId);
        UserArt ua = rowOf(userId, artId);
        if (ua == null) {
            throw new Deny("尚未习得此功，无需补全。");
        }
        if (nz(ua.getForm()) != 1) {
            throw new Deny("此功已是完整卷，无须补全。");
        }
        String currency = ArtRank.currencyOf(art.getGrade());
        int diff = fullPriceOf(art) - ArtRank.fragPrice(art.getGrade());
        if (diff > 0) {
            Wallet after = coinService.spend(userId, currency, diff, "art", "art", art.getId());
            if (after == null) {
                throw new Deny(String.format("补全需 %d %s，%s不足（尚缺 %d）。",
                        diff, ArtRank.currencyName(art.getGrade()),
                        ArtRank.currencyName(art.getGrade()),
                        diff - balanceOf(coinService.walletOf(userId), currency)));
            }
        }
        UpdateWrapper<UserArt> uw = new UpdateWrapper<>();
        uw.eq("id", ua.getId()).set("form", 0);
        userArtMapper.update(null, uw);
        ua.setForm(0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", String.format("补齐残篇，得《%s》完整卷——可续修圆满、入道、无上。", art.getName()));
        data.put("payCurrency", currency);
        data.put("payAmount", diff);
        data.put("wallet", coinService.overview(userId));
        data.put("art", mineBrief(art, ua));
        data.put("stats", cultivateService.panel(userMapper.selectById(userId)));
        return data;
    }

    // ==================== 修炼 ====================

    /** 修炼一课：耗煞气、涨熟练、残片积心魔 */
    public Map<String, Object> cultivate(Long userId, Long artId) {
        DemonUser user = require(userId);
        Art art = requireArt(artId);
        UserArt ua = rowOf(userId, artId);
        if (ua == null) {
            throw new Deny("尚未习得此功，无从修炼。");
        }
        int form = nz(ua.getForm());
        int stage = clampStage(ua.getStage());

        if (form == 1 && stage >= ArtRank.FRAG_MAX_STAGE) {
            throw new Deny("残篇残缺，最高只能修至大成——若要圆满、入道、无上，须先补全完整功法。");
        }
        if (stage == 4) {
            throw new Deny("已至圆满，日课再难寸进——须持「悟道机缘」顿悟入道。");
        }

        LocalDate today = LocalDate.now();
        int used = today.equals(ua.getTrainDate()) ? nz(ua.getTrainCount()) : 0;
        if (used >= ArtRank.TRAIN_DAILY) {
            throw new Deny(String.format("《%s》今日已修 %d 课，气机须得调息——明日再来。",
                    art.getName(), used));
        }

        int exp = user.getExp() == null ? 0 : user.getExp();
        if (exp < ArtRank.TRAIN_COST_EXP) {
            throw new Deny(String.format("煞气不足：修炼一课需 %d 点煞气，当前仅 %d 点。",
                    ArtRank.TRAIN_COST_EXP, exp));
        }

        int gain = form == 1 ? ArtRank.TRAIN_PROF_FRAG : ArtRank.TRAIN_PROF_FULL;
        int prof = nz(ua.getProf()) + gain;
        boolean advanced = false;
        while (stage < 4) {
            int need = ArtRank.needOf(stage);
            if (need <= 0 || prof < need) {
                break;
            }
            prof -= need;
            stage++;
            advanced = true;
            if (form == 1 && stage > ArtRank.FRAG_MAX_STAGE) {
                stage = ArtRank.FRAG_MAX_STAGE;
                prof = 0;
                break;
            }
        }
        if (stage >= 4) {
            prof = 0;
        }

        // 心魔：残篇反噬（每课 +2）；完整魔功入道后以煞养心（每课 -1）
        int heart = nz(user.getHeartDemon());
        if (form == 1) {
            heart = Math.min(ArtRank.HEART_MAX, heart + ArtRank.HEART_FRAG_TRAIN);
        } else if (stage >= 5) {
            heart = Math.max(0, heart - ArtRank.HEART_CALM);
        }

        UserArt upd = new UserArt();
        upd.setId(ua.getId());
        upd.setStage(stage);
        upd.setProf(prof);
        upd.setTrainDate(today);
        upd.setTrainCount(used + 1);
        userArtMapper.updateById(upd);

        UpdateWrapper<DemonUser> duw = new UpdateWrapper<>();
        duw.eq("id", userId).set("exp", exp - ArtRank.TRAIN_COST_EXP).set("heart_demon", heart);
        userMapper.update(null, duw);

        ua.setStage(stage);
        ua.setProf(prof);
        ua.setTrainDate(today);
        ua.setTrainCount(used + 1);
        DemonUser fresh = userMapper.selectById(userId);

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("静室运功，%s熟练 +%d。", ArtRank.stageName(stage), gain));
        if (advanced) {
            msg.append(String.format("功候精进，已入「%s」。", ArtRank.stageName(stage)));
        }
        if (form == 1) {
            msg.append(String.format("残篇气机逆冲，心魔 +%d。", ArtRank.HEART_FRAG_TRAIN));
        } else if (stage >= 5) {
            msg.append(String.format("以煞养心，心魔 -%d。", ArtRank.HEART_CALM));
        }
        if (form == 1 && stage >= ArtRank.FRAG_MAX_STAGE) {
            msg.append("残篇至此已是尽头。");
        }
        if (stage == 4) {
            msg.append("圆满无破绽——欲入道，须借「悟道机缘」一悟。");
        } else if (stage == 5) {
            msg.append("已触本阶道韵，欲至无上，须经「死战悟道」。");
        } else if (stage == 6) {
            msg.append("功法极致，本命神通已生。");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", msg.toString());
        data.put("art", mineBrief(art, ua));
        data.put("advanced", advanced);
        data.put("exp", exp - ArtRank.TRAIN_COST_EXP);
        data.put("heartDemon", heart);
        data.put("heartState", ArtRank.heartState(heart));
        data.put("heartPenalty", ArtRank.heartPenalty(heart));
        data.put("trainLeft", Math.max(0, ArtRank.TRAIN_DAILY - used - 1));
        data.put("wallet", coinService.overview(userId));
        if (fresh != null) {
            data.put("stats", cultivateService.panel(fresh));
        }
        return data;
    }

    /** 圆满 → 入道：耗一枚「悟道机缘」 */
    public Map<String, Object> insight(Long userId, Long artId) {
        DemonUser user = require(userId);
        Art art = requireArt(artId);
        UserArt ua = rowOf(userId, artId);
        if (ua == null) {
            throw new Deny("尚未习得此功，谈何入道。");
        }
        if (nz(ua.getForm()) == 1) {
            throw new Deny("残篇永远无法领悟本阶道韵——须先补全完整功法。");
        }
        if (clampStage(ua.getStage()) != 4) {
            throw new Deny(String.format("须先修至「圆满」方可悟道——当前为「%s」。",
                    ArtRank.stageName(clampStage(ua.getStage()))));
        }
        consumeItem(userId, ITEM_INSIGHT, "悟道机缘",
                "入道须一悟——请先在藏宝阁备下「悟道机缘」（秘境机缘亦可得）。");

        UpdateWrapper<UserArt> uw = new UpdateWrapper<>();
        uw.eq("id", ua.getId()).set("stage", 5).set("prof", 0);
        userArtMapper.update(null, uw);
        ua.setStage(5);
        ua.setProf(0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", String.format("一念贯通，《%s》道韵自生——已入「入道」，同阶残篇再难撄锋。", art.getName()));
        data.put("art", mineBrief(art, ua));
        data.put("stats", cultivateService.panel(userMapper.selectById(userId)));
        return data;
    }

    /** 入道 → 无上：耗一枚「死战悟道」 */
    public Map<String, Object> transcend(Long userId, Long artId) {
        DemonUser user = require(userId);
        Art art = requireArt(artId);
        UserArt ua = rowOf(userId, artId);
        if (ua == null) {
            throw new Deny("尚未习得此功，谈何无上。");
        }
        if (nz(ua.getForm()) == 1) {
            throw new Deny("残篇不可能衍生本命神通——须先补全完整功法。");
        }
        if (clampStage(ua.getStage()) != 5) {
            throw new Deny("须先「入道」，方可求无上。");
        }
        consumeItem(userId, ITEM_TRANSCEND, "死战悟道",
                "无上须经死战——请先在藏宝阁备下「死战悟道」。");

        UpdateWrapper<UserArt> uw = new UpdateWrapper<>();
        uw.eq("id", ua.getId()).set("stage", 6).set("prof", 0);
        userArtMapper.update(null, uw);
        ua.setStage(6);
        ua.setProf(0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", String.format("死战中一线灵光不灭，《%s》臻至「无上」——阶位不变，战力跨阶。", art.getName()));
        data.put("art", mineBrief(art, ua));
        data.put("stats", cultivateService.panel(userMapper.selectById(userId)));
        return data;
    }

    // ==================== 查询 ====================

    /** 我习得的功法（含形态、进度、实得数值与战力） */
    public List<Map<String, Object>> mine(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Art a : cultivateService.learnedArts(userId)) {
            UserArt ua = rowOf(userId, a.getId());
            list.add(mineBrief(a, ua));
        }
        return list;
    }

    /** 单部功法视图（详情页用：含我的持有形态与进度） */
    public Map<String, Object> artView(Long userId, Long artId) {
        Art a = requireArt(artId);
        return mineBrief(a, rowOf(userId, artId));
    }

    /** 商店卡片（未习得时的功法信息） */
    public Map<String, Object> artBrief(Art a) {
        Map<String, Object> m = baseBrief(a);
        m.put("priceFull", ArtRank.fullPrice(a.getGrade()));
        m.put("priceFrag", ArtRank.fragPrice(a.getGrade()));
        m.put("currency", ArtRank.currencyOf(a.getGrade()));
        m.put("currencyName", ArtRank.currencyName(a.getGrade()));
        m.put("priceFullText", ArtRank.priceText(a.getGrade(), 0));
        m.put("priceFragText", ArtRank.priceText(a.getGrade(), 1));
        return m;
    }

    /** 已习功法：在基础信息上叠加形态与进度 */
    public Map<String, Object> mineBrief(Art a, UserArt ua) {
        Map<String, Object> m = artBrief(a);
        int form = ua == null ? 0 : nz(ua.getForm());
        int stage = ua == null ? 1 : clampStage(ua.getStage());
        int prof = ua == null ? 0 : nz(ua.getProf());
        int need = ArtRank.needOf(stage);
        int used = 0;
        if (ua != null && LocalDate.now().equals(ua.getTrainDate())) {
            used = nz(ua.getTrainCount());
        }
        double factor = ArtRank.factorOf(form, stage);

        m.put("owned", ua != null);
        m.put("form", form);
        m.put("formName", ArtRank.formName(form));
        m.put("stage", stage);
        m.put("stageName", ArtRank.stageName(stage));
        m.put("stageMax", ArtRank.STAGES.length);
        m.put("prof", prof);
        m.put("profNeed", need);
        m.put("profPercent", need <= 0 ? (stage >= 4 ? 100 : 0)
                : Math.max(0, Math.min(100, Math.round(prof * 100f / need))));
        m.put("factor", factor);
        m.put("trainLeft", ua == null ? 0 : Math.max(0, ArtRank.TRAIN_DAILY - used));
        m.put("canTrain", ua != null && ArtRank.trainable(form, stage)
                && Math.max(0, ArtRank.TRAIN_DAILY - used) > 0);
        m.put("canInsight", ua != null && form == 0 && stage == 4);
        m.put("canTranscend", ua != null && form == 0 && stage == 5);
        m.put("canComplete", ua != null && form == 1);
        m.put("nextHint", nextHint(form, stage));

        // 实得五维与战力（按形态与重数折算）
        m.put("atk", scaled(nz(a.getBonusAtk()), factor));
        m.put("def", scaled(nz(a.getBonusDef()), factor));
        m.put("hp", scaled(nz(a.getBonusHp()), factor));
        m.put("insight", scaled(nz(a.getBonusInsight()), factor));
        m.put("agility", scaled(nz(a.getBonusAgility()), factor));
        m.put("power", CultivateService.artPower(a, factor));
        m.put("basePower", CultivateService.artPower(a));
        return m;
    }

    /** 商店列表用（含我的持有状态） */
    public Map<String, Object> shopBrief(Art a, UserArt ua) {
        Map<String, Object> m = ua == null ? artBrief(a) : mineBrief(a, ua);
        return m;
    }

    private Map<String, Object> baseBrief(Art a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("category", a.getCategory());
        m.put("grade", a.getGrade());
        m.put("gradeText", gradeText(a));
        int layer = a.getLayer() == null ? 1 : a.getLayer();
        m.put("layer", layer);
        m.put("layerName", ArtRank.layerName(layer));
        m.put("maxLayer", ArtRank.maxLayerOf(a.getGrade()));
        m.put("series", a.getSeries());
        m.put("realmReq", a.getRealmReq());
        m.put("summary", a.getSummary());
        m.put("bonusAtk", nz(a.getBonusAtk()));
        m.put("bonusDef", nz(a.getBonusDef()));
        m.put("bonusHp", nz(a.getBonusHp()));
        m.put("bonusInsight", nz(a.getBonusInsight()));
        m.put("bonusAgility", nz(a.getBonusAgility()));
        m.put("basePower", CultivateService.artPower(a));
        m.put("fragMaxStage", ArtRank.FRAG_MAX_STAGE);
        m.put("fragMaxStageName", ArtRank.stageName(ArtRank.FRAG_MAX_STAGE));
        m.put("stageNames", List.of(ArtRank.STAGES));
        return m;
    }

    /** 下一重提示 */
    private String nextHint(int form, int stage) {
        if (form == 1 && stage >= ArtRank.FRAG_MAX_STAGE) {
            return "残篇止步大成（补全后可续修）";
        }
        if (stage == 4) {
            return "须「悟道机缘」顿悟入道";
        }
        if (stage == 5) {
            return "可日课「以煞养心」；欲至无上，须「死战悟道」";
        }
        if (stage >= 6) {
            return "已至无上，本命神通已成；日课以煞养心";
        }
        return "日课修炼可进";
    }

    // ==================== 工具 ====================

    /** 持有一条习得记录（未习返回 null） */
    public UserArt rowOf(Long userId, Long artId) {
        if (userId == null || artId == null) {
            return null;
        }
        List<UserArt> rows = userArtMapper.selectList(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId)
                .eq(UserArt::getArtId, artId)
                .last("LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean owned(Long userId, Long artId) {
        return rowOf(userId, artId) != null;
    }

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

    /** 持有形态（未持有返回 null） */
    public Integer ownedForm(Long userId, Long artId) {
        UserArt ua = rowOf(userId, artId);
        return ua == null ? null : nz(ua.getForm());
    }

    private void consumeItem(Long userId, String code, String name, String denyMsg) {
        Item it = itemMapper.selectOne(new LambdaQueryWrapper<Item>()
                .eq(Item::getCode, code).last("LIMIT 1"));
        if (it == null) {
            throw new Deny("「" + name + "」尚未录入藏宝阁，暂不可用。");
        }
        UpdateWrapper<BagItem> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId).eq("item_id", it.getId()).ge("qty", 1).setSql("qty = qty - 1");
        if (bagItemMapper.update(null, uw) == 0) {
            throw new Deny(denyMsg);
        }
    }

    private DemonUser require(Long userId) {
        if (userId == null) {
            throw new Deny("未持魔契，请先入魔道。");
        }
        DemonUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new Deny("未持魔契，请先入魔道。");
        }
        return user;
    }

    private Art requireArt(Long artId) {
        if (artId == null) {
            throw new Deny("未指明欲取之功法。");
        }
        Art art = artMapper.selectById(artId);
        if (art == null) {
            throw new Deny("魔渊之中并无此功法。");
        }
        return art;
    }

    private int fullPriceOf(Art a) {
        return ArtRank.fullPrice(a.getGrade());
    }

    private static int balanceOf(Wallet w, String currency) {
        if (w == null) {
            return 0;
        }
        return Coin.JADE.equals(currency)
                ? (w.getJade() == null ? 0 : w.getJade())
                : (w.getCoin() == null ? 0 : w.getCoin());
    }

    private static int scaled(int base, double factor) {
        return (int) Math.round(base * factor);
    }

    public static int clampStage(Integer stage) {
        int s = stage == null ? 1 : stage;
        return Math.max(1, Math.min(ArtRank.STAGES.length, s));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
