package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.ArtRank;
import com.fenglin.springboottest.common.Origins;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.UserArt;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.UserArtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 魔族修行 · 修者数值服务（「修为」面板）
 *
 * 数值来源三分：
 *   1) 境界基础：位阶与重数（每境九重）线性成长；
 *   2) 功法加成：已习功法五维 × 形态系数（完整/残片）× 六重系数；
 *   3) 魔种天赋：仅作用于战力，沿用原切磋公式，不改动既有平衡。
 *
 * 战力 = (境阶含重折算 × 8000 + 累计煞气 + 功法战力) × (1 + 魔种战力加成) × 心魔折损
 * 功法战力 = 攻伐×8 + 护体×6 + 气血×1 + 悟性×4 + 身法×3（再乘形态与重数系数）
 */
@Service
public class CultivateService {

    /** 五维基础值（引煞一重、未习功法时） */
    public static final int BASE_ATK = 10;
    public static final int BASE_DEF = 8;
    public static final int BASE_HP = 100;
    public static final int BASE_INSIGHT = 5;
    public static final int BASE_AGILITY = 5;

    /** 每累计一重（跨境累加）带来的五维成长 */
    public static final int GROW_ATK = 2;
    public static final int GROW_DEF = 2;
    public static final int GROW_HP = 15;
    public static final int GROW_INSIGHT = 1;
    public static final int GROW_AGILITY = 2;

    /** 引煞境可驾驭功法数（与 ArtService 同源，避免循环依赖直接写死基数） */
    public static final int BASE_ART_CAP = 5;
    public static final int ART_CAP_PER_REALM = 2;

    @Autowired
    private ArtMapper artMapper;

    @Autowired
    private UserArtMapper userArtMapper;

    /** 已习功法（按习得顺序） */
    public List<Art> learnedArts(Long userId) {
        List<UserArt> owned = myRows(userId);
        if (owned.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = owned.stream().map(UserArt::getArtId).collect(Collectors.toList());
        Map<Long, Art> byId = artMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Art::getId, a -> a, (a, b) -> a));
        List<Art> ordered = new ArrayList<>();
        for (Long id : ids) {
            Art a = byId.get(id);
            if (a != null) {
                ordered.add(a);
            }
        }
        return ordered;
    }

    /** 我的习得记录（按习得顺序） */
    public List<UserArt> myRows(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return userArtMapper.selectList(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId).orderByAsc(UserArt::getId));
    }

    /** 功法战力折算（不计形态与重数） */
    public static int artPower(Art a) {
        return artPower(a, 1.0);
    }

    /** 功法战力折算（按形态与重数系数折算后） */
    public static int artPower(Art a, double factor) {
        if (a == null) {
            return 0;
        }
        double raw = nz(a.getBonusAtk()) * 8.0 + nz(a.getBonusDef()) * 6.0 + nz(a.getBonusHp())
                + nz(a.getBonusInsight()) * 4.0 + nz(a.getBonusAgility()) * 3.0;
        return (int) Math.round(raw * factor);
    }

    /** 计算修者当前数值 */
    public Stats stats(DemonUser user) {
        Stats s = new Stats();
        int index = Realm.indexOf(user.getCultivation());
        int exp = user.getExp() == null ? 0 : user.getExp();
        int layer = Math.max(nz(user.getLayer()), Realm.layerOf(exp, index));

        s.index = index;
        s.layer = layer;
        s.totalLayer = Realm.totalLayer(index, layer);
        s.cultivation = Realm.name(index);
        s.layerName = Realm.layerName(layer);
        s.title = index >= 0 && index < Realm.TITLES.size() ? Realm.TITLES.get(index) : "";
        s.exp = exp;

        Map<Long, Art> arts = learnedArts(user.getId()).stream()
                .collect(Collectors.toMap(Art::getId, a -> a, (a, b) -> a));
        List<UserArt> rows = myRows(user.getId());
        s.artCount = rows.size();
        s.artCap = BASE_ART_CAP + index * ART_CAP_PER_REALM;
        for (UserArt ua : rows) {
            Art a = arts.get(ua.getArtId());
            if (a == null) {
                continue;
            }
            double f = ArtRank.factorOf(nz(ua.getForm()), ArtService.clampStage(ua.getStage()));
            s.artAtk += (int) Math.round(nz(a.getBonusAtk()) * f);
            s.artDef += (int) Math.round(nz(a.getBonusDef()) * f);
            s.artHp += (int) Math.round(nz(a.getBonusHp()) * f);
            s.artInsight += (int) Math.round(nz(a.getBonusInsight()) * f);
            s.artAgility += (int) Math.round(nz(a.getBonusAgility()) * f);
            s.powerArt += artPower(a, f);
        }

        s.realmAtk = BASE_ATK + s.totalLayer * GROW_ATK;
        s.realmDef = BASE_DEF + s.totalLayer * GROW_DEF;
        s.realmHp = BASE_HP + s.totalLayer * GROW_HP;
        s.realmInsight = BASE_INSIGHT + s.totalLayer * GROW_INSIGHT;
        s.realmAgility = BASE_AGILITY + s.totalLayer * GROW_AGILITY;

        s.atk = s.realmAtk + s.artAtk;
        s.def = s.realmDef + s.artDef;
        s.hp = s.realmHp + s.artHp;
        s.insight = s.realmInsight + s.artInsight;
        s.agility = s.realmAgility + s.artAgility;

        Origins.Bonus origin = Origins.of(user.getRealm());
        s.originTitle = origin.getTitle();
        s.originPowerMod = origin.getSparPowerBonus();

        s.heartDemon = nz(user.getHeartDemon());
        s.heartPenalty = ArtRank.heartPenalty(s.heartDemon);

        s.powerRealm = (int) Math.round(Realm.realmLevel(index, layer) * Realm.POWER_PER_REALM);
        long raw = (long) s.powerRealm + exp + s.powerArt;
        s.powerBeforeHeart = (int) Math.round(raw * (1 + s.originPowerMod));
        s.power = (int) Math.round(s.powerBeforeHeart * s.heartPenalty);
        s.powerExp = exp;
        return s;
    }

    /** 「修为」面板数据（数值卡 + 来源拆解 + 九重进度 + 已习功法 + 心魔） */
    public Map<String, Object> panel(DemonUser user) {
        Stats s = stats(user);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", user.getName());
        m.put("cultivation", s.cultivation);
        m.put("realmIndex", s.index);
        m.put("realmTitle", s.title);
        m.put("layer", s.layer);
        m.put("layerName", s.layerName);
        m.put("layerMax", Realm.LAYERS);
        m.put("totalLayer", s.totalLayer);
        m.put("layerFull", Realm.isFullLayer(s.layer));

        m.put("exp", s.exp);
        m.put("layerStartExp", Realm.layerStartExp(s.index, s.layer));
        m.put("layerNextExp", Realm.layerNextExp(s.index, s.layer));
        m.put("layerPercent", layerPercent(s));
        m.put("nextRealm", s.index >= Realm.NAMES.size() - 1 ? null : Realm.name(s.index + 1));
        m.put("nextRealmExp", Realm.nextRealmExp(s.index));
        m.put("canBreakthrough", Realm.isFullLayer(s.layer)
                && s.exp >= Realm.nextRealmExp(s.index)
                && s.index < Realm.NAMES.size() - 1
                && s.heartDemon < ArtRank.HEART_GATE);
        m.put("needFullLayer", !Realm.isFullLayer(s.layer));
        m.put("heartBlocked", s.heartDemon >= ArtRank.HEART_GATE);

        m.put("power", s.power);
        m.put("powerRealm", s.powerRealm);
        m.put("powerExp", s.powerExp);
        m.put("powerArt", s.powerArt);
        m.put("powerBeforeHeart", s.powerBeforeHeart);
        m.put("originTitle", s.originTitle);
        m.put("originPowerMod", s.originPowerMod);

        m.put("learnedCount", s.artCount);
        m.put("artCap", s.artCap);

        m.put("heartDemon", s.heartDemon);
        m.put("heartMax", ArtRank.HEART_MAX);
        m.put("heartState", ArtRank.heartState(s.heartDemon));
        m.put("heartPenalty", s.heartPenalty);
        m.put("heartGate", ArtRank.HEART_GATE);

        List<Map<String, Object>> attrs = new ArrayList<>();
        attrs.add(attr("atk", "攻 伐", s.atk, s.realmAtk, s.artAtk, "出手之威，影响切磋与秘境杀伤"));
        attrs.add(attr("def", "护 体", s.def, s.realmDef, s.artDef, "受击之御，护体愈高秘境负伤愈轻"));
        attrs.add(attr("hp", "气 血", s.hp, s.realmHp, s.artHp, "性命之本，气血足则连战不衰"));
        attrs.add(attr("insight", "悟 性", s.insight, s.realmInsight, s.artInsight, "参悟之资，悟性高者更易顿悟"));
        attrs.add(attr("agility", "身 法", s.agility, s.realmAgility, s.artAgility, "进退之速，身法快者先手伤人"));
        m.put("attrs", attrs);

        Map<Long, Art> byId = learnedArts(user.getId()).stream()
                .collect(Collectors.toMap(Art::getId, a -> a, (a, b) -> a));
        List<Map<String, Object>> arts = new ArrayList<>();
        for (UserArt ua : myRows(user.getId())) {
            Art a = byId.get(ua.getArtId());
            if (a == null) {
                continue;
            }
            int form = nz(ua.getForm());
            int stage = ArtService.clampStage(ua.getStage());
            int need = ArtRank.needOf(stage);
            int prof = nz(ua.getProf());
            int used = LocalDate.now().equals(ua.getTrainDate()) ? nz(ua.getTrainCount()) : 0;
            double f = ArtRank.factorOf(form, stage);

            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId());
            am.put("name", a.getName());
            am.put("grade", a.getGrade());
            am.put("gradeText", a.getGrade() + "阶");
            am.put("category", a.getCategory());
            am.put("form", form);
            am.put("formName", ArtRank.formName(form));
            am.put("stage", stage);
            am.put("stageName", ArtRank.stageName(stage));
            am.put("stageMax", ArtRank.STAGES.length);
            am.put("prof", prof);
            am.put("profNeed", need);
            am.put("profPercent", need <= 0 ? (stage >= 4 ? 100 : 0)
                    : Math.max(0, Math.min(100, Math.round(prof * 100f / need))));
            am.put("atk", (int) Math.round(nz(a.getBonusAtk()) * f));
            am.put("def", (int) Math.round(nz(a.getBonusDef()) * f));
            am.put("hp", (int) Math.round(nz(a.getBonusHp()) * f));
            am.put("insight", (int) Math.round(nz(a.getBonusInsight()) * f));
            am.put("agility", (int) Math.round(nz(a.getBonusAgility()) * f));
            am.put("power", artPower(a, f));
            am.put("basePower", artPower(a));
            am.put("trainLeft", Math.max(0, ArtRank.TRAIN_DAILY - used));
            am.put("canTrain", ArtRank.trainable(form, stage) && used < ArtRank.TRAIN_DAILY);
            am.put("canInsight", form == 0 && stage == 4);
            am.put("canTranscend", form == 0 && stage == 5);
            am.put("canComplete", form == 1);
            arts.add(am);
        }
        m.put("arts", arts);
        return m;
    }

    /** 本重进度百分比 */
    private int layerPercent(Stats s) {
        int start = Realm.layerStartExp(s.index, s.layer);
        int next = Realm.layerNextExp(s.index, s.layer);
        if (next <= start) {
            return 100;
        }
        int p = (int) Math.round((s.exp - start) * 100.0 / (next - start));
        return Math.max(0, Math.min(100, p));
    }

    private Map<String, Object> attr(String key, String name, int value,
                                     int fromRealm, int fromArt, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("name", name);
        m.put("value", value);
        m.put("fromRealm", fromRealm);
        m.put("fromArt", fromArt);
        m.put("desc", desc);
        return m;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 修者数值快照 */
    public static class Stats {
        public String cultivation;
        public String layerName;
        public String title;
        public String originTitle;
        public int index;
        public int layer;
        public int totalLayer;
        public int exp;
        public int artCount;
        public int artCap;
        public int heartDemon;
        public double heartPenalty = 1.0;
        public int power;
        public int powerRealm;
        public int powerExp;
        public int powerArt;
        public int powerBeforeHeart;
        public double originPowerMod;
        public int atk;
        public int def;
        public int hp;
        public int insight;
        public int agility;
        public int realmAtk;
        public int realmDef;
        public int realmHp;
        public int realmInsight;
        public int realmAgility;
        public int artAtk;
        public int artDef;
        public int artHp;
        public int artInsight;
        public int artAgility;
    }
}
