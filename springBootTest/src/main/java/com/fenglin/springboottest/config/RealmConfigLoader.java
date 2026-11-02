package com.fenglin.springboottest.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.RealmConfig;
import com.fenglin.springboottest.mapper.RealmConfigMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 修行境界配置加载器
 *
 * 启动时读取 demon_realm 表并覆盖 Realm 内置十境，实现「境界可配置」。
 * 任何异常（表不存在 / 表空 / 位阶不连续 / 重名）都不会中断启动，
 * 仅打印告警并回退到 Realm 内置默认十境。
 */
@Component
public class RealmConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(RealmConfigLoader.class);

    private final RealmConfigMapper realmConfigMapper;

    public RealmConfigLoader(RealmConfigMapper realmConfigMapper) {
        this.realmConfigMapper = realmConfigMapper;
    }

    @PostConstruct
    public void load() {
        try {
            // 先回到内置默认值，确保本次加载失败时不会残留上一次的表配置
            Realm.resetToDefaults();

            List<RealmConfig> rows = realmConfigMapper.selectList(
                    new LambdaQueryWrapper<RealmConfig>().orderByAsc(RealmConfig::getIdx));

            if (rows == null || rows.isEmpty()) {
                log.warn("修行境界表 demon_realm 无数据，沿用代码内置默认十境");
                return;
            }

            // 位阶必须从 0 开始连续，否则视为配置错误（防止中间断档导致进度错乱）
            for (int i = 0; i < rows.size(); i++) {
                RealmConfig r = rows.get(i);
                if (r.getIdx() == null || r.getIdx() != i) {
                    throw new IllegalStateException("位阶不连续：第 " + i + " 行期望 idx=" + i
                            + "，实际 idx=" + r.getIdx());
                }
            }

            List<String> names = new ArrayList<>(rows.size());
            List<String> titles = new ArrayList<>(rows.size());
            List<String> descs = new ArrayList<>(rows.size());
            List<Integer> thresholds = new ArrayList<>(rows.size());
            for (RealmConfig r : rows) {
                names.add(r.getName());
                titles.add(r.getTitle());
                descs.add(r.getDescription());
                thresholds.add(r.getThreshold());
            }

            Realm.applyTable(names, titles, descs, thresholds);

            log.info("修行境界配置已加载：共 {} 境，{} → {}，阈值 {}",
                    names.size(), names.get(0), names.get(names.size() - 1), thresholds);
        } catch (Exception e) {
            Realm.resetToDefaults();
            log.warn("修行境界表加载失败，回退为代码内置默认十境：{}", e.getMessage());
        }
    }
}
