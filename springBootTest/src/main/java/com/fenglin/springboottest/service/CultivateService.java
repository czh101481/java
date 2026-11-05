package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Origins;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.UserArt;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.UserArtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
 *   2) 功法加成：已习功法的五维 bonus_* 累加；
 *   3) 魔种天赋：仅作用于战力（战意滔天），沿用原切磋公式，不改动既有平衡。
 *
 * 战力 = (境阶含重折算 × 8000 + 累计煞气 + 功法战力) × (1 + 魔种战力加成)
 * 功法战力 = 攻伐×8 + 护体×6 + 气血×1 + 悟性×4 + 身法×3
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

    @Autowired
    private ArtMapper artMapper;

    @Autowired
    private UserArtMapper userArtMapper;

    /** 已习功法（按习得顺序） */
    public List<Art> learnedArts(Long userId) {
        List<UserArt> owned = userArtMapper.selectList(new LambdaQueryWrapper<UserArt>()
                .eq(UserArt::getUserId, userId).orderByAsc(UserArt::getId));
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

    /** 功法战力折算 */
    public static int artPower(Art a) {
        if (a == null) {
            return 0;
        }
        return nz(a.getBonusAtk()) * 8 + nz(a.getBonusDef()) * 6 + nz(a.getBonusHp())
                + nz(a.getBonusInsight()) * 4 + nz(a.getBonusAgility()) * 3;
    }

    /** 计算修者当前数值 */
    public Stats stats(DemonUser user) {
        Stats s = new Stats();
        int index = Realm.indexOf(user.getCultivation());
        int exp = user.getExp() == null ? 0 : user.getExp();
        int layer = Realm.layerOf(exp, index);

        s.index = index;
        s.layer = layer;
        s.totalLayer = Realm.totalLayer(index, layer);
        s.cultivation = Realm.name(index);
        s.layerName = Realm.layerName(layer);
        s.title = index >= 0 && index < Realm.TITLES.size() ? Realm.TITLES.get(index) : "";
        s.exp = exp;

        List<Art> arts = learnedArts(user.getId());
        s.artCount = arts.size();
        for (Art a : arts) {
            s.artAtk += nz(a.getBonusAtk());
            s.artDef += nz(a.getBonusDef());
            s.artHp += nz(a.getBonusHp());
            s.artInsight += nz(a.getBonusInsight());
            s.artAgility += nz(a.getBonusAgility());
            s.powerArt += artPower(a);
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

        s.powerRealm = (int) Math.round(Realm.realmLevel(index, layer) * Realm.POWER_PER_REALM);
        long raw = (long) s.powerRealm + exp + s.powerArt;
        s.power = (int) Math.round(raw * (1 + s.originPowerMod));
        s.powerExp = exp;
        return s;
    }

    /** 「修为」面板数据（数值卡 + 来源拆解 + 九重进度 + 已习功法） */
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
        m.put("canBreakthrough", !Realm.isFullLayer(s.layer) ? false
                : s.exp >= Realm.nextRealmExp(s.index) && s.index < Realm.NAMES.size() - 1);
        m.put("needFullLayer", !Realm.isFullLayer(s.layer));

        m.put("power", s.power);
        m.put("powerRealm", s.powerRealm);
        m.put("powerExp", s.powerExp);
        m.put("powerArt", s.powerArt);
        m.put("originTitle", s.originTitle);
        m.put("originPowerMod", s.originPowerMod);

        List<Map<String, Object>> attrs = new ArrayList<>();
        attrs.add(attr("atk", "攻 伐", s.atk, s.realmAtk, s.artAtk, "出手之威，影响切磋与秘境杀伤"));
        attrs.add(attr("def", "护 体", s.def, s.realmDef, s.artDef, "受击之御，护体愈高秘境负伤愈轻"));
        attrs.add(attr("hp", "气 血", s.hp, s.realmHp, s.artHp, "性命之本，气血足则连战不衰"));
        attrs.add(attr("insight", "悟 性", s.insight, s.realmInsight, s.artInsight, "参悟之资，悟性高者更易顿悟"));
        attrs.add(attr("agility", "身 法", s.agility, s.realmAgility, s.artAgility, "进退之速，身法快者先手伤人"));
        m.put("attrs", attrs);

        List<Map<String, Object>> arts = new ArrayList<>();
        for (Art a : learnedArts(user.getId())) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId());
            am.put("name", a.getName());
            am.put("grade", a.getGrade());
            am.put("layer", a.getLayer());
            am.put("series", a.getSeries());
            am.put("category", a.getCategory());
            am.put("atk", nz(a.getBonusAtk()));
            am.put("def", nz(a.getBonusDef()));
            am.put("hp", nz(a.getBonusHp()));
            am.put("insight", nz(a.getBonusInsight()));
            am.put("agility", nz(a.getBonusAgility()));
            am.put("power", artPower(a));
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
        public int power;
        public int powerRealm;
        public int powerExp;
        public int powerArt;
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
