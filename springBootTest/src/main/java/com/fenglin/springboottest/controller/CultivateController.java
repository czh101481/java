package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.ArtService;
import com.fenglin.springboottest.service.CultivateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 魔族修行 · 修为数值面板
 * 基底址：/api/cultivate
 *
 *   面板  GET /api/cultivate/panel → 战力 / 攻伐 / 护体 / 气血 / 悟性 / 身法 + 来源拆解 + 九重进度
 *   规则  GET /api/cultivate/rules → 分重与数值公式说明（公开）
 */
@RestController
@RequestMapping("/api/cultivate")
public class CultivateController {

    @Autowired
    private CultivateService cultivateService;

    @Autowired
    private ArtService artService;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private TokenManager tokenManager;

    @GetMapping("/panel")
    public Result<?> panel(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        DemonUser user = userMapper.selectById(uid);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> data = cultivateService.panel(user);
        data.put("artCap", artService.capOf(user));
        data.put("learnedCount", cultivateService.learnedArts(uid).size());
        return Result.success(data);
    }

    /** 数值规则（公开，前端「修为」页脚注用） */
    @GetMapping("/rules")
    public Result<?> rules() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("layersPerRealm", Realm.LAYERS);
        data.put("powerPerRealm", Realm.POWER_PER_REALM);
        data.put("grades", com.fenglin.springboottest.common.ArtRank.GRADES);
        data.put("stages", java.util.List.of(com.fenglin.springboottest.common.ArtRank.STAGES));
        data.put("fragRatio", com.fenglin.springboottest.common.ArtRank.FRAG_RATIO);
        data.put("fragMaxStage", com.fenglin.springboottest.common.ArtRank.FRAG_MAX_STAGE);
        data.put("heartGate", com.fenglin.springboottest.common.ArtRank.HEART_GATE);
        Map<String, Object> prices = new LinkedHashMap<>();
        for (String g : com.fenglin.springboottest.common.ArtRank.GRADES) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("currency", com.fenglin.springboottest.common.ArtRank.currencyName(g));
            p.put("full", com.fenglin.springboottest.common.ArtRank.fullPrice(g));
            p.put("frag", com.fenglin.springboottest.common.ArtRank.fragPrice(g));
            prices.put(g, p);
        }
        data.put("prices", prices);
        data.put("baseArtCap", ArtService.BASE_ART_CAP);
        data.put("artCapPerRealm", ArtService.ART_CAP_PER_REALM);
        data.put("baseStats", Map.of(
                "atk", CultivateService.BASE_ATK,
                "def", CultivateService.BASE_DEF,
                "hp", CultivateService.BASE_HP,
                "insight", CultivateService.BASE_INSIGHT,
                "agility", CultivateService.BASE_AGILITY));
        data.put("growPerLayer", Map.of(
                "atk", CultivateService.GROW_ATK,
                "def", CultivateService.GROW_DEF,
                "hp", CultivateService.GROW_HP,
                "insight", CultivateService.GROW_INSIGHT,
                "agility", CultivateService.GROW_AGILITY));
        data.put("desc", "每境九重，九重圆满方可冲境；战力 = 境阶含重×8000 + 煞气 + 功法战力；"
                + "功法分完整/残片：残片价 = 完整×0.35，最高止步大成；"
                + "进度 入门→小成→大成→圆满→入道→无上，圆满后须悟道机缘，入道后须死战悟道。");
        return Result.success(data);
    }

    private Long uid(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenManager.getUserId(authorization.substring(7).trim());
    }
}
