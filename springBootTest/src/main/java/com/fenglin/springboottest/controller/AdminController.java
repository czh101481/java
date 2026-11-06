package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.PasswordEncoder;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.config.RealmConfigLoader;
import com.fenglin.springboottest.dto.AdminLoginDTO;
import com.fenglin.springboottest.common.Origins;
import com.fenglin.springboottest.config.OriginConfigLoader;
import com.fenglin.springboottest.entity.Achievement;
import com.fenglin.springboottest.entity.Announcement;
import com.fenglin.springboottest.entity.Archive;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.BagItem;
import com.fenglin.springboottest.entity.Codex;
import com.fenglin.springboottest.entity.Debate;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.Item;
import com.fenglin.springboottest.entity.OriginConfig;
import com.fenglin.springboottest.entity.Purchase;
import com.fenglin.springboottest.entity.Trade;
import com.fenglin.springboottest.entity.Trial;
import com.fenglin.springboottest.entity.UserAchievement;
import com.fenglin.springboottest.mapper.AchievementMapper;
import com.fenglin.springboottest.mapper.AnnouncementMapper;
import com.fenglin.springboottest.mapper.ArchiveMapper;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.BagItemMapper;
import com.fenglin.springboottest.mapper.CodexMapper;
import com.fenglin.springboottest.mapper.DebateMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.ItemMapper;
import com.fenglin.springboottest.mapper.OriginConfigMapper;
import com.fenglin.springboottest.mapper.PurchaseMapper;
import com.fenglin.springboottest.mapper.RealmLogMapper;
import com.fenglin.springboottest.mapper.TradeMapper;
import com.fenglin.springboottest.mapper.TrialMapper;
import com.fenglin.springboottest.mapper.UserAchievementMapper;
import com.fenglin.springboottest.service.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台管理系统接口
 * 基底址：/api/admin
 * 功能：管理员登录（仅登录，无注册，凭 demon_user 表 permission=0 的账号）+ 五大内容模块数据管理 CRUD：
 *      功法阁（demon_art）/ 域外秘境（demon_trial）/ 宗门论道（demon_debate）/
 *      魔典秘录（demon_codex）/ 魔枢典册（demon_archive）
 * 图片同步：GET /api/admin/images?dir=art|trial|codex → 扫描 static/images 下实际存在的图片
 * 说明：管理员凭 demon_user 表认定（permission=0），密码为演示明文比对；
 *      令牌内存存储，重启即失效；生产环境请接入 JWT + 密码加密。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ArtMapper artMapper;
    @Autowired
    private com.fenglin.springboottest.mapper.UserArtMapper userArtMapper;
    @Autowired
    private TrialMapper trialMapper;
    @Autowired
    private DebateMapper debateMapper;
    @Autowired
    private CodexMapper codexMapper;
    @Autowired
    private ArchiveMapper archiveMapper;
    @Autowired
    private DemonUserMapper userMapper;
    @Autowired
    private OriginConfigMapper originConfigMapper;
    @Autowired
    private AnnouncementMapper announcementMapper;
    @Autowired
    private RealmLogMapper realmLogMapper;
    @Autowired
    private RealmConfigLoader realmConfigLoader;
    @Autowired
    private OriginConfigLoader originConfigLoader;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private BagItemMapper bagItemMapper;
    @Autowired
    private PurchaseMapper purchaseMapper;
    @Autowired
    private TradeMapper tradeMapper;
    @Autowired
    private com.fenglin.springboottest.service.DenService denService;
    @Autowired
    private AchievementMapper achievementMapper;
    @Autowired
    private UserAchievementMapper userAchievementMapper;

    /** 允许扫描的配图目录（白名单） */
    private static final Set<String> IMAGE_DIRS = Set.of("art", "trial", "codex");

    /** 管理员令牌表（内存，演示用） */
    private final Set<String> adminTokens = ConcurrentHashMap.newKeySet();

    /**
     * 管理员登录 POST /api/admin/login → 仅登录，无注册
     * 凭 demon_user 表校验：道号 + 密令（兼容历史明文与 PBKDF2 哈希），且 permission = 0（管理员权限）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername())
                || !StringUtils.hasText(dto.getPassword())) {
            return Result.error(400, "管理员账号与密令皆不可为空。");
        }
        DemonUser user = userMapper.selectOne(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getName, dto.getUsername().trim()));
        if (user == null || !PasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.error(401, "管理员账号或密令有误。");
        }
        if (user.getPermission() == null || user.getPermission() != 0) {
            return Result.error(403, "权限不足：仅管理员权限者可入后台，普通修者止步。");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        adminTokens.add(token);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getName());
        return Result.success(data);
    }

    /** 功法阁列表 GET /api/admin/art/list?kw= → 全量（阶位仙→黄→id，kw 过滤功法名/简介） */
    @GetMapping("/art/list")
    public Result<List<Art>> artList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Art> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            qw.like(Art::getName, kw.trim()).or().like(Art::getSummary, kw.trim());
        }
        qw.last("ORDER BY FIELD(grade, '仙', '天', '地', '玄', '黄'), id ASC");
        return Result.success(artMapper.selectList(qw));
    }

    /** 新增功法 POST /api/admin/art（JSON body：name/category/grade/realmReq/summary/image/content） */
    @PostMapping("/art")
    public Result<?> artAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Art art) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (art == null || !StringUtils.hasText(art.getName())) {
            return Result.error(400, "功法名不可为空。");
        }
        if (!StringUtils.hasText(art.getCategory())) {
            art.setCategory("攻伐");
        }
        if (!StringUtils.hasText(art.getGrade())) {
            art.setGrade("黄");
        }
        if (!StringUtils.hasText(art.getRealmReq())) {
            art.setRealmReq("引煞");
        }
        if (!com.fenglin.springboottest.common.ArtRank.GRADES.contains(art.getGrade())) {
            return Result.error(400, "阶位只能是 黄 / 玄 / 地 / 天 / 仙。");
        }
        // 重数已从界面移除，layer 仅作占位保留
        art.setLayer(1);
        normalizeArtNumbers(art);
        art.setId(null);
        artMapper.insert(art);
        return Result.success(art);
    }

    /** 修改功法 PUT /api/admin/art/{id} */
    @PutMapping("/art/{id}")
    public Result<?> artUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Art art) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Art old = artMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "魔渊之中并无此功法。");
        }
        if (art == null || !StringUtils.hasText(art.getName())) {
            return Result.error(400, "功法名不可为空。");
        }
        // 阶位合法性 + 价格按阶位恒定（后台不可改价）
        String grade = StringUtils.hasText(art.getGrade()) ? art.getGrade() : old.getGrade();
        if (!com.fenglin.springboottest.common.ArtRank.GRADES.contains(grade)) {
            return Result.error(400, "阶位只能是 黄 / 玄 / 地 / 天 / 仙。");
        }
        if (!StringUtils.hasText(art.getSeries())) {
            art.setSeries(StringUtils.hasText(art.getName()) ? art.getName() : old.getName());
        }
        art.setGrade(grade);
        // 重数不再参与展示与校验，沿用原值占位
        art.setLayer(old.getLayer() == null ? 1 : old.getLayer());
        if (art.getStatus() == null) {
            art.setStatus(old.getStatus());
        }
        art.setPriceCoin(com.fenglin.springboottest.common.ArtRank.isJade(grade)
                ? 0 : com.fenglin.springboottest.common.ArtRank.fullPrice(grade));
        art.setPriceJade(com.fenglin.springboottest.common.ArtRank.isJade(grade)
                ? com.fenglin.springboottest.common.ArtRank.fullPrice(grade) : 0);
        art.setId(id);
        artMapper.updateById(art);
        return Result.success(artMapper.selectById(id));
    }

    /** 删除功法 DELETE /api/admin/art/{id} */
    @DeleteMapping("/art/{id}")
    public Result<?> artDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (artMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此功法。");
        }
        long owned = userArtMapper.selectCount(
                new LambdaQueryWrapper<com.fenglin.springboottest.entity.UserArt>()
                        .eq(com.fenglin.springboottest.entity.UserArt::getArtId, id));
        if (owned > 0) {
            return Result.error(400, "已有修者习得此功法，不可除名——请改为下架。");
        }
        artMapper.deleteById(id);
        return Result.success("已将该功法从功法阁除名。");
    }

    /** 功法新建时补全数值字段（未填按 0 / 默认值处理，避免库里出现 NULL） */
    private void normalizeArtNumbers(Art art) {
        if (art.getStatus() == null) {
            art.setStatus(1);
        }
        // 价格恒定规则：由阶位决定，忽略后台传值
        String grade = art.getGrade() == null ? "黄" : art.getGrade();
        boolean jade = com.fenglin.springboottest.common.ArtRank.isJade(grade);
        int full = com.fenglin.springboottest.common.ArtRank.fullPrice(grade);
        art.setPriceCoin(jade ? 0 : full);
        art.setPriceJade(jade ? full : 0);
        if (art.getBonusAtk() == null) {
            art.setBonusAtk(0);
        }
        if (art.getBonusDef() == null) {
            art.setBonusDef(0);
        }
        if (art.getBonusHp() == null) {
            art.setBonusHp(0);
        }
        if (art.getBonusInsight() == null) {
            art.setBonusInsight(0);
        }
        if (art.getBonusAgility() == null) {
            art.setBonusAgility(0);
        }
        if (!StringUtils.hasText(art.getSeries())) {
            art.setSeries(art.getName());
        }
    }

    // ==================== 域外秘境（demon_trial） ====================
    /** 秘境列表 GET /api/admin/trial/list?kw= → 全量（id 倒序，kw 过滤秘境名/所在域/守关/简介） */
    @GetMapping("/trial/list")
    public Result<List<Trial>> trialList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Trial> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(Trial::getTitle, k).or().like(Trial::getRegion, k)
                    .or().like(Trial::getBoss, k).or().like(Trial::getSummary, k);
        }
        qw.orderByDesc(Trial::getId);
        return Result.success(trialMapper.selectList(qw));
    }

    /**
     * 新增秘境 POST /api/admin/trial
     * body：title/region/realmReq/difficulty/boss/costExp/dailyLimit/rewardMin/rewardMax/
     *      riskRate/treasureRate/summary/image
     * 玩法字段留空时按「凶险」自动套用预设数值
     */
    @PostMapping("/trial")
    public Result<?> trialAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Trial trial) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (trial == null || !StringUtils.hasText(trial.getTitle())) {
            return Result.error(400, "秘境名不可为空。");
        }
        fillRealmDefaults(trial);
        trial.setId(null);
        trialMapper.insert(trial);
        return Result.success(trial);
    }

    /** 修改秘境 PUT /api/admin/trial/{id}（玩法字段留空则按凶险补预设值） */
    @PutMapping("/trial/{id}")
    public Result<?> trialUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Trial trial) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (trialMapper.selectById(id) == null) {
            return Result.error(404, "魔域之中并无此秘境。");
        }
        if (trial == null || !StringUtils.hasText(trial.getTitle())) {
            return Result.error(400, "秘境名不可为空。");
        }
        fillRealmDefaults(trial);
        trial.setId(id);
        trialMapper.updateById(trial);
        return Result.success(trialMapper.selectById(id));
    }

    /** 删除秘境 DELETE /api/admin/trial/{id}（连同探索日志一并除名） */
    @DeleteMapping("/trial/{id}")
    public Result<?> trialDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (trialMapper.selectById(id) == null) {
            return Result.error(404, "魔域之中并无此秘境。");
        }
        realmLogMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                com.fenglin.springboottest.entity.RealmLog>()
                .eq(com.fenglin.springboottest.entity.RealmLog::getTrialId, id));
        trialMapper.deleteById(id);
        return Result.success("已将该秘境自魔域抹去，相关探索记录亦随之湮灭。");
    }

    // ==================== 秘境玩法数值预设 ====================

    /** 凶险 → {入场消耗, 每日次数, 产出下限, 产出上限, 凶险%, 机缘%} */
    private static final Map<String, int[]> REALM_PRESET = Map.of(
            "低", new int[]{0, 5, 10, 30, 10, 8},
            "中", new int[]{20, 3, 30, 70, 20, 12},
            "高", new int[]{60, 2, 80, 160, 30, 18},
            "绝", new int[]{150, 1, 200, 400, 45, 25});

    /** 玩法字段缺省时按凶险补齐，保证后台只填名字也能得到一个可玩的秘境 */
    private void fillRealmDefaults(Trial t) {
        if (!StringUtils.hasText(t.getRealmReq())) {
            t.setRealmReq("引煞");
        }
        if (!StringUtils.hasText(t.getDifficulty())) {
            t.setDifficulty("中");
        }
        int[] p = REALM_PRESET.getOrDefault(t.getDifficulty(), REALM_PRESET.get("中"));
        if (t.getCostExp() == null) {
            t.setCostExp(p[0]);
        }
        if (t.getDailyLimit() == null || t.getDailyLimit() <= 0) {
            t.setDailyLimit(p[1]);
        }
        if (t.getRewardMin() == null) {
            t.setRewardMin(p[2]);
        }
        if (t.getRewardMax() == null) {
            t.setRewardMax(Math.max(p[3], t.getRewardMin()));
        }
        if (t.getRiskRate() == null) {
            t.setRiskRate(p[4]);
        }
        if (t.getTreasureRate() == null) {
            t.setTreasureRate(p[5]);
        }
        if (t.getBoss() == null) {
            t.setBoss("");
        }
    }

    // ==================== 宗门论道（demon_debate） ====================

    /** 论道列表 GET /api/admin/debate/list?kw= → 全量（id 倒序，kw 过滤论题/论者/摘要） */
    @GetMapping("/debate/list")
    public Result<List<Debate>> debateList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Debate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(Debate::getTitle, k).or().like(Debate::getSpeaker, k).or().like(Debate::getSummary, k);
        }
        qw.orderByDesc(Debate::getId);
        return Result.success(debateMapper.selectList(qw));
    }

    /** 新增论道 POST /api/admin/debate（JSON body：title/speaker/summary/optionA/optionB/optionC/answer/analysis） */
    @PostMapping("/debate")
    public Result<?> debateAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Debate debate) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (debate == null || !StringUtils.hasText(debate.getTitle())) {
            return Result.error(400, "论题不可为空。");
        }
        String answer = StringUtils.hasText(debate.getAnswer())
                ? debate.getAnswer().trim().toUpperCase() : "A";
        if (!"A".equals(answer) && !"B".equals(answer) && !"C".equals(answer)) {
            return Result.error(400, "正确选项只能是 A / B / C。");
        }
        debate.setId(null);
        debate.setAnswer(answer);
        debateMapper.insert(debate);
        return Result.success(debate);
    }

    /** 修改论道 PUT /api/admin/debate/{id} */
    @PutMapping("/debate/{id}")
    public Result<?> debateUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Debate debate) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (debateMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此论题。");
        }
        if (debate == null || !StringUtils.hasText(debate.getTitle())) {
            return Result.error(400, "论题不可为空。");
        }
        String answer = StringUtils.hasText(debate.getAnswer())
                ? debate.getAnswer().trim().toUpperCase() : "A";
        if (!"A".equals(answer) && !"B".equals(answer) && !"C".equals(answer)) {
            return Result.error(400, "正确选项只能是 A / B / C。");
        }
        debate.setId(id);
        debate.setAnswer(answer);
        debateMapper.updateById(debate);
        return Result.success(debateMapper.selectById(id));
    }

    /** 删除论道 DELETE /api/admin/debate/{id} */
    @DeleteMapping("/debate/{id}")
    public Result<?> debateDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (debateMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此论题。");
        }
        debateMapper.deleteById(id);
        return Result.success("已将该论题从宗门论道撤下。");
    }

    // ==================== 魔典秘录（demon_codex） ====================

    /** 魔典列表 GET /api/admin/codex/list?kw= → 全量（id 倒序，kw 过滤典籍名/卷册/摘要） */
    @GetMapping("/codex/list")
    public Result<List<Codex>> codexList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Codex> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(Codex::getTitle, k).or().like(Codex::getVolume, k).or().like(Codex::getSummary, k);
        }
        qw.orderByDesc(Codex::getId);
        return Result.success(codexMapper.selectList(qw));
    }

    /** 新增魔典 POST /api/admin/codex（JSON body：title/volume/summary/image） */
    @PostMapping("/codex")
    public Result<?> codexAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Codex codex) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (codex == null || !StringUtils.hasText(codex.getTitle())) {
            return Result.error(400, "典籍名不可为空。");
        }
        codex.setId(null);
        codexMapper.insert(codex);
        return Result.success(codex);
    }

    /** 修改魔典 PUT /api/admin/codex/{id} */
    @PutMapping("/codex/{id}")
    public Result<?> codexUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Codex codex) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (codexMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此典籍。");
        }
        if (codex == null || !StringUtils.hasText(codex.getTitle())) {
            return Result.error(400, "典籍名不可为空。");
        }
        codex.setId(id);
        codexMapper.updateById(codex);
        return Result.success(codexMapper.selectById(id));
    }

    /** 删除魔典 DELETE /api/admin/codex/{id} */
    @DeleteMapping("/codex/{id}")
    public Result<?> codexDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (codexMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此典籍。");
        }
        codexMapper.deleteById(id);
        return Result.success("已将该典籍从魔典秘录焚毁。");
    }

    // ==================== 魔枢典册（demon_archive） ====================

    /** 典册列表 GET /api/admin/archive/list?kw=&cat= → 全量（cat 升序 + sortOrder + id） */
    @GetMapping("/archive/list")
    public Result<List<Archive>> archiveList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw,
            @RequestParam(value = "cat", required = false, defaultValue = "") String cat) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Archive> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(cat)) {
            qw.eq(Archive::getCategory, cat.trim());
        }
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.and(w -> w.like(Archive::getTitle, k)
                    .or().like(Archive::getSubtitle, k)
                    .or().like(Archive::getSummary, k));
        }
        qw.orderByAsc(Archive::getCategory)
                .orderByAsc(Archive::getSortOrder)
                .orderByAsc(Archive::getId);
        return Result.success(archiveMapper.selectList(qw));
    }

    /** 新增典册 POST /api/admin/archive（JSON body：category/title/subtitle/summary/content/sortOrder） */
    @PostMapping("/archive")
    public Result<?> archiveAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Archive archive) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (archive == null || !StringUtils.hasText(archive.getTitle())) {
            return Result.error(400, "条目名不可为空。");
        }
        if (!StringUtils.hasText(archive.getCategory())) {
            archive.setCategory("lineage");
        }
        if (archive.getSortOrder() == null) {
            archive.setSortOrder(0);
        }
        archive.setId(null);
        archiveMapper.insert(archive);
        return Result.success(archive);
    }

    /** 修改典册 PUT /api/admin/archive/{id} */
    @PutMapping("/archive/{id}")
    public Result<?> archiveUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Archive archive) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (archiveMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此条目。");
        }
        if (archive == null || !StringUtils.hasText(archive.getTitle())) {
            return Result.error(400, "条目名不可为空。");
        }
        if (archive.getSortOrder() == null) {
            archive.setSortOrder(0);
        }
        archive.setId(id);
        archiveMapper.updateById(archive);
        return Result.success(archiveMapper.selectById(id));
    }

    /** 删除典册 DELETE /api/admin/archive/{id} */
    @DeleteMapping("/archive/{id}")
    public Result<?> archiveDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (archiveMapper.selectById(id) == null) {
            return Result.error(404, "魔渊之中并无此条目。");
        }
        archiveMapper.deleteById(id);
        return Result.success("已将该条目从魔枢典册抽离。");
    }

    // ==================== 魔窟 · 洞府挂机（demon_den） ====================

    /**
     * 洞府总览 GET /api/admin/den/list?kw=
     * → 全部修者（含尚未开辟洞府者）的三设施等级、钱囊、心魔与待收预估；纯只读。
     */
    @GetMapping("/den/list")
    public Result<List<Map<String, Object>>> denList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        return Result.success(denService.adminOverview(kw));
    }

    /**
     * 洞府处置 PUT /api/admin/den/{userId}
     * body：action = none（校订等级 / 千分位 / 回拨小时）| collect（代收）| reset（拆毁）
     *       shaLevel / coinLevel / jadeLevel（0~10）、jadeCarry（0~999）、backHours（0~720）
     */
    @PutMapping("/den/{userId}")
    public Result<?> denUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> patch) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Map<String, Object> r = denService.adminAdjust(userId, patch);
        if (!Boolean.TRUE.equals(r.get("ok"))) {
            return Result.error(400, String.valueOf(r.getOrDefault("msg", "洞府处置失败。")));
        }
        return Result.success(r);
    }

    // ==================== 图片同步 ====================

    /**
     * 配图清单 GET /api/admin/images?dir=art|trial|codex
     * → 扫描 classpath static/images/{dir} 下实际存在的图片文件（按文件名排序），
     *   返回可直接用于 demon_art.image / demon_trial.image / demon_codex.image 的相对路径。
     * 管理后台配图下拉框据此渲染，向目录投放新图后刷新即可同步，无需改代码。
     */
    @GetMapping("/images")
    public Result<List<String>> images(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "dir", required = false, defaultValue = "art") String dir) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (!IMAGE_DIRS.contains(dir)) {
            return Result.error(400, "未知的配图目录。");
        }
        return Result.success(scanImages(dir));
    }

    /** 扫描指定配图目录：优先 classpath（IDE 运行/target/classes），兜底源码目录 */
    private List<String> scanImages(String dir) {
        List<String> result = new ArrayList<>();
        File folder = null;
        try {
            ClassPathResource res = new ClassPathResource("static/images/" + dir);
            if (res.exists()) {
                folder = res.getFile();
            }
        } catch (Exception e) {
            folder = null;
        }
        if (folder == null || !folder.isDirectory()) {
            // 兜底：以工作目录相对路径直找源码资源
            folder = new File("src/main/resources/static/images/" + dir);
        }
        if (folder.isDirectory()) {
            File[] files = folder.listFiles((d, name) ->
                    name.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)$"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File f : files) {
                    result.add("/images/" + dir + "/" + f.getName());
                }
            }
        }
        return result;
    }

    /**
     * 重新加载修行境界配置 POST /api/admin/realm/reload
     * 改完 demon_realm 表后调用即可生效，无需重启后端
     */
    @PostMapping("/realm/reload")
    public Result<Map<String, Object>> reloadRealm(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String err = checkAdmin(authorization);
        if (err != null) {
            return Result.error(401, err);
        }
        realmConfigLoader.load();
        Map<String, Object> data = new HashMap<>();
        data.put("fromTable", Realm.isLoadedFromTable());
        data.put("realms", Realm.NAMES);
        data.put("thresholds", Realm.THRESHOLDS);
        return Result.success(data);
    }

    // ==================== 修者名录（demon_user）· 权限管理 ====================

    /**
     * 修者名录 GET /api/admin/user/list?kw= → id 倒序，kw 过滤道号 / 灵识印记
     * 安全：密令（password）一律置空后返回，绝不外泄到后台界面
     */
    @GetMapping("/user/list")
    public Result<List<DemonUser>> userList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<DemonUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(DemonUser::getName, k).or().like(DemonUser::getEmail, k);
        }
        qw.orderByDesc(DemonUser::getId);
        List<DemonUser> users = userMapper.selectList(qw);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 修改修者 PUT /api/admin/user/{id}
     * 可改：permission（0 管理员 / 1 普通修者）、name、cultivation、exp
     * 不可改：password —— 永不被覆盖，避免后台误将密令清空
     * 保护：不允许把最后一位管理员降为普通修者，否则后台将再无人可入
     */
    @PutMapping("/user/{id}")
    public Result<?> userUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody DemonUser patch) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        DemonUser old = userMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "魔门之中并无此修者。");
        }
        if (patch == null) {
            return Result.error(400, "无可改之数。");
        }
        DemonUser up = new DemonUser();
        up.setId(id);
        if (patch.getPermission() != null) {
            int p = patch.getPermission();
            if (p != 0 && p != 1) {
                return Result.error(400, "权限只可为 0（管理员）或 1（普通修者）。");
            }
            boolean demoteAdmin = p == 1 && old.getPermission() != null && old.getPermission() == 0;
            if (demoteAdmin) {
                Long admins = userMapper.selectCount(
                        new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getPermission, 0));
                if (admins != null && admins <= 1) {
                    return Result.error(400, "魔门不可无执印人：此为最后一位管理员，不可降为普通修者。");
                }
            }
            up.setPermission(p);
        }
        if (StringUtils.hasText(patch.getName())) {
            up.setName(patch.getName().trim());
        }
        if (StringUtils.hasText(patch.getCultivation())) {
            up.setCultivation(patch.getCultivation().trim());
        }
        if (patch.getExp() != null) {
            up.setExp(patch.getExp());
        }
        up.setPassword(null);
        userMapper.updateById(up);
        DemonUser fresh = userMapper.selectById(id);
        if (fresh != null) {
            fresh.setPassword(null);
        }
        return Result.success(fresh);
    }

    // ==================== 魔种天赋（demon_origin） ====================

    /** 魔种天赋列表 GET /api/admin/origin/list?kw= → sort 升序，kw 过滤魔种名 / 天赋名 */
    @GetMapping("/origin/list")
    public Result<List<OriginConfig>> originList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<OriginConfig> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(OriginConfig::getOrigin, k).or().like(OriginConfig::getTitle, k);
        }
        qw.orderByAsc(OriginConfig::getSort);
        return Result.success(originConfigMapper.selectList(qw));
    }

    /** 新增魔种 POST /api/admin/origin（入库后自动重载天赋配置，立即生效） */
    @PostMapping("/origin")
    public Result<?> originAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody OriginConfig cfg) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (cfg == null || !StringUtils.hasText(cfg.getOrigin())) {
            return Result.error(400, "魔种名不可为空。");
        }
        if (cfg.getSort() == null) {
            cfg.setSort(99);
        }
        cfg.setId(null);
        originConfigMapper.insert(cfg);
        originConfigLoader.load();
        return Result.success(cfg);
    }

    /** 修改魔种 PUT /api/admin/origin/{id}（自动重载天赋配置） */
    @PutMapping("/origin/{id}")
    public Result<?> originUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody OriginConfig cfg) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (originConfigMapper.selectById(id) == null) {
            return Result.error(404, "魔门之中并无此魔种。");
        }
        if (cfg == null || !StringUtils.hasText(cfg.getOrigin())) {
            return Result.error(400, "魔种名不可为空。");
        }
        cfg.setId(id);
        originConfigMapper.updateById(cfg);
        originConfigLoader.load();
        return Result.success(originConfigMapper.selectById(id));
    }

    /** 删除魔种 DELETE /api/admin/origin/{id}（自动重载天赋配置） */
    @DeleteMapping("/origin/{id}")
    public Result<?> originDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (originConfigMapper.selectById(id) == null) {
            return Result.error(404, "魔门之中并无此魔种。");
        }
        originConfigMapper.deleteById(id);
        originConfigLoader.load();
        return Result.success("已将该魔种自魔门除名。");
    }

    /** 重新加载魔种天赋 POST /api/admin/origin/reload → 改完表无需重启后端 */
    @PostMapping("/origin/reload")
    public Result<Map<String, Object>> reloadOrigin(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String err = checkAdmin(authorization);
        if (err != null) {
            return Result.error(401, err);
        }
        originConfigLoader.load();
        Map<String, Object> data = new HashMap<>();
        data.put("origins", Origins.all().stream().map(Origins.Bonus::getOrigin).toList());
        return Result.success(data);
    }

    // ==================== 魔门公告（demon_announcement） ====================

    /** 公告列表 GET /api/admin/announcement/list?kw= → id 倒序，kw 过滤标题 / 正文 */
    @GetMapping("/announcement/list")
    public Result<List<Announcement>> announcementList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(Announcement::getTitle, k).or().like(Announcement::getContent, k);
        }
        qw.orderByDesc(Announcement::getId);
        return Result.success(announcementMapper.selectList(qw));
    }

    /** 发布公告 POST /api/admin/announcement */
    @PostMapping("/announcement")
    public Result<?> announcementAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Announcement a) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (a == null || !StringUtils.hasText(a.getTitle())) {
            return Result.error(400, "公告标题不可为空。");
        }
        if (!StringUtils.hasText(a.getLevel())) {
            a.setLevel("通告");
        }
        a.setId(null);
        announcementMapper.insert(a);
        return Result.success(a);
    }

    /** 修改公告 PUT /api/admin/announcement/{id} */
    @PutMapping("/announcement/{id}")
    public Result<?> announcementUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Announcement a) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (announcementMapper.selectById(id) == null) {
            return Result.error(404, "魔门之中并无此公告。");
        }
        if (a == null || !StringUtils.hasText(a.getTitle())) {
            return Result.error(400, "公告标题不可为空。");
        }
        a.setId(id);
        announcementMapper.updateById(a);
        return Result.success(announcementMapper.selectById(id));
    }

    /** 删除公告 DELETE /api/admin/announcement/{id} */
    @DeleteMapping("/announcement/{id}")
    public Result<?> announcementDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (announcementMapper.selectById(id) == null) {
            return Result.error(404, "魔门之中并无此公告。");
        }
        announcementMapper.deleteById(id);
        return Result.success("已将该公告自魔门撤下。");
    }

    // ==================== 藏宝阁物品（demon_item）· 上下架 / 改价 / 限时折扣 ====================

    /** 物品列表 GET /api/admin/item/list?kw= → sort 升序，kw 过滤物品名 / 代号 */
    @GetMapping("/item/list")
    public Result<List<Item>> itemList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Item> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(Item::getName, k).or().like(Item::getCode, k);
        }
        qw.orderByAsc(Item::getSort).orderByAsc(Item::getId);
        return Result.success(itemMapper.selectList(qw));
    }

    /**
     * 新增物品 POST /api/admin/item
     * 校验：代号唯一、名称非空、价格非负、效果类型白名单、折扣 0-99
     */
    @PostMapping("/item")
    public Result<?> itemAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (body == null) {
            return Result.error(400, "无可录之物。");
        }
        String code = text(body.get("code"));
        String name = text(body.get("name"));
        if (code.isEmpty() || name.isEmpty()) {
            return Result.error(400, "物品代号与物品名皆不可为空。");
        }
        long dup = itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getCode, code));
        if (dup > 0) {
            return Result.error(400, "此物品代号已被占用，请另起一名。");
        }
        String bad = validateItemBody(body);
        if (bad != null) {
            return Result.error(400, bad);
        }

        Item it = new Item();
        it.setCode(code);
        it.setName(name);
        it.setIcon(textOr(body.get("icon"), "丹"));
        it.setCategory(textOr(body.get("category"), "丹药"));
        it.setDescText(text(body.get("descText")));
        it.setPriceCoin(intOf(body.get("priceCoin"), 0));
        it.setPriceJade(intOf(body.get("priceJade"), 0));
        it.setEffectType(textOr(body.get("effectType"), "exp"));
        it.setEffectValue(intOf(body.get("effectValue"), 0));
        it.setDailyLimit(intOf(body.get("dailyLimit"), 0));
        it.setDailyUseLimit(intOf(body.get("dailyUseLimit"), 0));
        it.setDiscountPercent(intOf(body.get("discountPercent"), 0));
        it.setDiscountStart(parseTime(body.get("discountStart")));
        it.setDiscountEnd(parseTime(body.get("discountEnd")));
        it.setStatus(intOf(body.get("status"), 1));
        it.setSort(intOf(body.get("sort"), 0));
        itemMapper.insert(it);
        return Result.success(itemMapper.selectById(it.getId()));
    }

    /**
     * 修改物品 PUT /api/admin/item/{id}
     * 仅改请求体里出现的字段（未出现的保持原样）；
     * 折扣时段传空串即清空 —— 故此处用 UpdateWrapper 显式 set，避开 updateById 跳过 null 的坑。
     */
    @PutMapping("/item/{id}")
    public Result<?> itemUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Item old = itemMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "藏宝阁中并无此物。");
        }
        if (body == null) {
            return Result.error(400, "无可改之数。");
        }
        String bad = validateItemBody(body);
        if (bad != null) {
            return Result.error(400, bad);
        }
        if (body.containsKey("code")) {
            String code = text(body.get("code"));
            if (code.isEmpty()) {
                return Result.error(400, "物品代号不可为空。");
            }
            long dup = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                    .eq(Item::getCode, code).ne(Item::getId, id));
            if (dup > 0) {
                return Result.error(400, "此物品代号已被别的物品占用。");
            }
        }

        UpdateWrapper<Item> uw = new UpdateWrapper<>();
        uw.eq("id", id);
        if (body.containsKey("code"))           uw.set("code", text(body.get("code")));
        if (body.containsKey("name"))           uw.set("name", text(body.get("name")));
        if (body.containsKey("icon"))           uw.set("icon", textOr(body.get("icon"), "丹"));
        if (body.containsKey("category"))       uw.set("category", textOr(body.get("category"), "丹药"));
        if (body.containsKey("descText"))       uw.set("desc_text", text(body.get("descText")));
        if (body.containsKey("priceCoin"))      uw.set("price_coin", intOf(body.get("priceCoin"), 0));
        if (body.containsKey("priceJade"))      uw.set("price_jade", intOf(body.get("priceJade"), 0));
        if (body.containsKey("effectType"))     uw.set("effect_type", textOr(body.get("effectType"), "exp"));
        if (body.containsKey("effectValue"))    uw.set("effect_value", intOf(body.get("effectValue"), 0));
        if (body.containsKey("dailyLimit"))     uw.set("daily_limit", intOf(body.get("dailyLimit"), 0));
        if (body.containsKey("dailyUseLimit"))  uw.set("daily_use_limit", intOf(body.get("dailyUseLimit"), 0));
        if (body.containsKey("discountPercent")) uw.set("discount_percent", intOf(body.get("discountPercent"), 0));
        if (body.containsKey("discountStart"))  uw.set("discount_start", parseTime(body.get("discountStart")));
        if (body.containsKey("discountEnd"))    uw.set("discount_end", parseTime(body.get("discountEnd")));
        if (body.containsKey("status"))         uw.set("status", intOf(body.get("status"), 1));
        if (body.containsKey("sort"))          uw.set("sort", intOf(body.get("sort"), 0));
        itemMapper.update(null, uw);
        return Result.success(itemMapper.selectById(id));
    }

    /**
     * 删除物品 DELETE /api/admin/item/{id}
     * 保护：已有人持有 / 有购买记录 / 有在售挂单时不可删，避免脏数据；请改为下架。
     */
    @DeleteMapping("/item/{id}")
    public Result<?> itemDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Item old = itemMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "藏宝阁中并无此物。");
        }
        long held = bagItemMapper.selectCount(new LambdaQueryWrapper<BagItem>()
                .eq(BagItem::getItemId, id).gt(BagItem::getQty, 0));
        long bought = purchaseMapper.selectCount(new LambdaQueryWrapper<Purchase>()
                .eq(Purchase::getItemId, id));
        long onSale = tradeMapper.selectCount(new LambdaQueryWrapper<Trade>()
                .eq(Trade::getItemId, id).eq(Trade::getStatus, 0));
        if (held > 0 || bought > 0 || onSale > 0) {
            return Result.error(400, String.format(
                    "「%s」尚有 %d 位修者持有、%d 笔购买记录、%d 单在售，不可删除；请改为下架。",
                    old.getName(), held, bought, onSale));
        }
        itemMapper.deleteById(id);
        return Result.success("已将此物自藏宝阁抹去。");
    }

    // ==================== 道途成就（阶段 13） ====================

    /** 成就定义列表 GET /api/admin/achievement/list */
    @GetMapping("/achievement/list")
    public Result<List<Achievement>> achievementList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        LambdaQueryWrapper<Achievement> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(kw)) {
            String k = kw.trim();
            qw.like(Achievement::getName, k).or().like(Achievement::getCode, k)
                    .or().like(Achievement::getCategory, k);
        }
        qw.orderByAsc(Achievement::getSortOrder).orderByAsc(Achievement::getId);
        return Result.success(achievementMapper.selectList(qw));
    }

    /** 新增成就 POST /api/admin/achievement */
    @PostMapping("/achievement")
    public Result<?> achievementAdd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        if (body == null) {
            return Result.error(400, "无可录之功行。");
        }
        String code = text(body.get("code"));
        if (code.isEmpty()) {
            return Result.error(400, "成就代号不可为空。");
        }
        long dup = achievementMapper.selectCount(
                new LambdaQueryWrapper<Achievement>().eq(Achievement::getCode, code));
        if (dup > 0) {
            return Result.error(400, "此成就代号已被占用，请另起一名。");
        }
        String bad = validateAchievementBody(body);
        if (bad != null) {
            return Result.error(400, bad);
        }

        Achievement a = new Achievement();
        a.setCode(code);
        a.setName(text(body.get("name")));
        a.setIcon(textOr(body.get("icon"), "印"));
        a.setCategory(textOr(body.get("category"), "道途"));
        a.setSubtitle(text(body.get("subtitle")));
        a.setDescription(text(body.get("description")));
        a.setCondType(textOr(body.get("condType"), "exp"));
        a.setCondValue(intOf(body.get("condValue"), 1));
        a.setTitleReward(text(body.get("titleReward")));
        a.setStage(textOr(body.get("stage"), "初入"));
        a.setSortOrder(intOf(body.get("sortOrder"), 0));
        a.setStatus(intOf(body.get("status"), 1));
        achievementMapper.insert(a);
        return Result.success(achievementMapper.selectById(a.getId()));
    }

    /** 修改成就 PUT /api/admin/achievement/{id}：只改请求体里出现的字段 */
    @PutMapping("/achievement/{id}")
    public Result<?> achievementUpdate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Achievement old = achievementMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "道途之中并无此枚徽记。");
        }
        if (body == null) {
            return Result.error(400, "无可改之数。");
        }
        String bad = validateAchievementBody(body);
        if (bad != null) {
            return Result.error(400, bad);
        }
        if (body.containsKey("code")) {
            String code = text(body.get("code"));
            if (code.isEmpty()) {
                return Result.error(400, "成就代号不可为空。");
            }
            long dup = achievementMapper.selectCount(new LambdaQueryWrapper<Achievement>()
                    .eq(Achievement::getCode, code).ne(Achievement::getId, id));
            if (dup > 0) {
                return Result.error(400, "此成就代号已被别的徽记占用。");
            }
        }

        UpdateWrapper<Achievement> uw = new UpdateWrapper<>();
        uw.eq("id", id);
        if (body.containsKey("code"))         uw.set("code", text(body.get("code")));
        if (body.containsKey("name"))         uw.set("name", text(body.get("name")));
        if (body.containsKey("icon"))         uw.set("icon", textOr(body.get("icon"), "印"));
        if (body.containsKey("category"))     uw.set("category", textOr(body.get("category"), "道途"));
        if (body.containsKey("subtitle"))     uw.set("subtitle", text(body.get("subtitle")));
        if (body.containsKey("description"))  uw.set("description", text(body.get("description")));
        if (body.containsKey("condType"))     uw.set("cond_type", textOr(body.get("condType"), "exp"));
        if (body.containsKey("condValue"))    uw.set("cond_value", intOf(body.get("condValue"), 1));
        if (body.containsKey("titleReward"))  uw.set("title_reward", text(body.get("titleReward")));
        if (body.containsKey("stage"))        uw.set("stage", textOr(body.get("stage"), "初入"));
        if (body.containsKey("sortOrder"))    uw.set("sort_order", intOf(body.get("sortOrder"), 0));
        if (body.containsKey("status"))       uw.set("status", intOf(body.get("status"), 1));
        achievementMapper.update(null, uw);
        return Result.success(achievementMapper.selectById(id));
    }

    /** 删除成就：已有修者铭刻时不可删，改为停用 */
    @DeleteMapping("/achievement/{id}")
    public Result<?> achievementDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String denied = checkAdmin(authorization);
        if (denied != null) {
            return Result.error(401, denied);
        }
        Achievement old = achievementMapper.selectById(id);
        if (old == null) {
            return Result.error(404, "道途之中并无此枚徽记。");
        }
        long got = userAchievementMapper.selectCount(new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getAchievementId, id));
        if (got > 0) {
            return Result.error(400, String.format(
                    "「%s」已有 %d 位修者铭刻，不可删除；请改为停用（状态 0）。", old.getName(), got));
        }
        achievementMapper.deleteById(id);
        return Result.success("已将此枚徽记自道途抹去。");
    }

    /** 成就字段校验：条件类型走 AchievementService 白名单，避免后台写出永远无法达成的徽记 */
    private String validateAchievementBody(Map<String, Object> b) {
        if (b.containsKey("name") && text(b.get("name")).isEmpty()) {
            return "成就名不可为空。";
        }
        if (b.containsKey("condType")
                && !AchievementService.COND_TYPE_SET.contains(text(b.get("condType")))) {
            return "判定类型不在白名单内，可用：" + String.join(" / ", AchievementService.COND_TYPES);
        }
        if (intOf(b.get("condValue"), 1) < 1) {
            return "达成阈值须 ≥ 1。";
        }
        if (b.containsKey("stage")
                && !AchievementService.STAGES.contains(text(b.get("stage")))) {
            return "道途分期只可为：" + String.join(" / ", AchievementService.STAGES);
        }
        int st = intOf(b.get("status"), 1);
        if (st != 0 && st != 1) {
            return "状态只可为 1（启用）或 0（停用）。";
        }
        return null;
    }

    /** 物品字段校验（只校验请求体里出现的字段） */
    private String validateItemBody(Map<String, Object> b) {
        if (b.containsKey("name") && text(b.get("name")).isEmpty()) {
            return "物品名不可为空。";
        }
        int coin = intOf(b.get("priceCoin"), 0);
        int jade = intOf(b.get("priceJade"), 0);
        if (coin < 0 || jade < 0) {
            return "价格不可为负。";
        }
        if (coin == 0 && jade == 0 && (b.containsKey("priceCoin") || b.containsKey("priceJade"))) {
            return "魔晶价与血玉价不可同时为 0（白送之物不必上架）。";
        }
        if (b.containsKey("effectType")) {
            String et = text(b.get("effectType"));
            if (!"exp".equals(et) && !"explore".equals(et) && !"coin".equals(et)
                    && !"insight".equals(et) && !"transcend".equals(et)) {
                return "效果类型只可为 exp（煞气）/ explore（参悟上限）/ coin（魔晶）/ insight（入道钥）/ transcend（无上钥）。";
            }
        }
        int d = intOf(b.get("discountPercent"), 0);
        if (d < 0 || d > 99) {
            return "折扣只可填 1-99（如 80 表示八折），0 为不打折。";
        }
        int limit = intOf(b.get("dailyLimit"), 0);
        if (limit < 0) {
            return "每日限购不可为负。";
        }
        int useLimit = intOf(b.get("dailyUseLimit"), 0);
        if (useLimit < 0) {
            return "每日使用上限不可为负（0 表示不限）。";
        }
        int st = intOf(b.get("status"), 1);
        if (st != 0 && st != 1) {
            return "上架状态只可为 1（在售）或 0（下架）。";
        }
        return null;
    }

    private String text(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private String textOr(Object v, String def) {
        String s = text(v);
        return s.isEmpty() ? def : s;
    }

    private int intOf(Object v, int def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return def;
        }
        try {
            return (int) Math.round(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 解析前端 datetime-local 的 "2026-11-05T18:30"；空串或非法返回 null（表示不限） */
    private LocalDateTime parseTime(Object v) {
        String s = text(v);
        if (s.isEmpty()) {
            return null;
        }
        s = s.replace(' ', 'T');
        if (s.length() == 16) {
            s = s + ":00";
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** 管理员鉴权：有效返回 null，无效返回提示 */
    private String checkAdmin(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return "未持管理令牌，请先登录后台。";
        }
        if (!adminTokens.contains(authorization.substring(7).trim())) {
            return "管理令牌已失效，请重新登录。";
        }
        return null;
    }
}
