package com.fenglin.springboottest.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Origins;
import com.fenglin.springboottest.entity.OriginConfig;
import com.fenglin.springboottest.mapper.OriginConfigMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 魔种天赋配置加载器
 *
 * 启动时读取 demon_origin 表并覆盖 Origins 内置四系天赋，实现「天赋可配置」。
 * 任何异常（表不存在 / 表空 / 字段非法）都不会中断启动，
 * 仅打印告警并回退到 Origins 内置默认值。
 */
@Component
public class OriginConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(OriginConfigLoader.class);

    private final OriginConfigMapper originConfigMapper;

    public OriginConfigLoader(OriginConfigMapper originConfigMapper) {
        this.originConfigMapper = originConfigMapper;
    }

    @PostConstruct
    public void load() {
        try {
            Origins.resetToDefaults();

            List<OriginConfig> rows = originConfigMapper.selectList(
                    new LambdaQueryWrapper<OriginConfig>().orderByAsc(OriginConfig::getSort));

            if (rows == null || rows.isEmpty()) {
                log.warn("魔种天赋表 demon_origin 无数据，沿用代码内置默认四系");
                return;
            }

            Origins.applyTable(rows);

            log.info("魔种天赋配置已加载：共 {} 系 → {}",
                    Origins.all().size(),
                    Origins.all().stream().map(Origins.Bonus::getOrigin).toList());
        } catch (Exception e) {
            Origins.resetToDefaults();
            log.warn("魔种天赋表加载失败，回退为代码内置默认四系：{}", e.getMessage());
        }
    }
}
