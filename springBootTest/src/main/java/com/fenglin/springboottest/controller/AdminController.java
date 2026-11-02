package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.config.RealmConfigLoader;
import com.fenglin.springboottest.dto.AdminLoginDTO;
import com.fenglin.springboottest.common.Origins;
import com.fenglin.springboottest.config.OriginConfigLoader;
import com.fenglin.springboottest.entity.Announcement;
import com.fenglin.springboottest.entity.Archive;
import com.fenglin.springboottest.entity.Art;
import com.fenglin.springboottest.entity.Codex;
import com.fenglin.springboottest.entity.Debate;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.OriginConfig;
import com.fenglin.springboottest.entity.Trial;
import com.fenglin.springboottest.mapper.AnnouncementMapper;
import com.fenglin.springboottest.mapper.ArchiveMapper;
import com.fenglin.springboottest.mapper.ArtMapper;
import com.fenglin.springboottest.mapper.CodexMapper;
import com.fenglin.springboottest.mapper.DebateMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.OriginConfigMapper;
import com.fenglin.springboottest.mapper.RealmLogMapper;
import com.fenglin.springboottest.mapper.TrialMapper;
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

    /** 允许扫描的配图目录（白名单） */
    private static final Set<String> IMAGE_DIRS = Set.of("art", "trial", "codex");

    /** 管理员令牌表（内存，演示用） */
    private final Set<String> adminTokens = ConcurrentHashMap.newKeySet();

    /**
     * 管理员登录 POST /api/admin/login → 仅登录，无注册
     * 凭 demon_user 表校验：道号 + 明文密令匹配，且 permission = 0（管理员权限）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AdminLoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername())
                || !StringUtils.hasText(dto.getPassword())) {
            return Result.error(400, "管理员账号与密令皆不可为空。");
        }
        DemonUser user = userMapper.selectOne(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getName, dto.getUsername().trim()));
        if (user == null || !dto.getPassword().equals(user.getPassword())) {
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

    /** 功法阁列表 GET /api/admin/art/list?kw= → 全量（id 倒序，kw 过滤功法名/简介） */
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
        qw.orderByDesc(Art::getId);
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
        artMapper.deleteById(id);
        return Result.success("已将该功法从功法阁除名。");
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
