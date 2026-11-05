package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.entity.Item;
import com.fenglin.springboottest.entity.ItemUse;
import com.fenglin.springboottest.mapper.ItemUseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 道具「每日使用上限」（v1.1）
 *
 * 旧版只限「买」不限「用」：可提前囤 99 件，一天内集中用完，日产预算被瞬间击穿。
 * 本服务按 (修者, 物品, 日期) 记账，所有取用入口都必须先占用额度：
 *   1) ShopService.use        —— 丹药 / 符箓 / 奇物，POST /api/coin/use
 *   2) ArtService.consumeItem —— 破关钥匙，POST /api/art/insight|transcend
 *
 * 设计要点：
 *   - 不抛自定义异常，返回「拒绝文案」由调用方转成本模块的 Deny，避免跨模块异常耦合；
 *   - 占用额度在扣袋之前，额度用尽时连背包都不会动；
 *   - 提供 refund，供扣袋 / 效果结算失败时退回额度，保证不误扣。
 */
@Service
public class ItemUseLimitService {

    @Autowired
    private ItemUseMapper itemUseMapper;

    /** 上限取值：-1（未初始化）与 0 都表示不限；>0 才是件数 */
    public static int limitOf(Item it) {
        if (it == null || it.getDailyUseLimit() == null) {
            return 0;
        }
        return Math.max(0, it.getDailyUseLimit());
    }

    /** 今日该物已用件数 */
    public int usedToday(Long uid, Long itemId) {
        if (uid == null || itemId == null) {
            return 0;
        }
        Integer v = itemUseMapper.usedToday(uid, itemId, LocalDate.now());
        return v == null ? 0 : v;
    }

    /** 今日各物品已用量映射，供藏宝阁 / 乾坤袋列表展示 */
    public Map<Long, Integer> usedTodayMap(Long uid) {
        Map<Long, Integer> out = new HashMap<>();
        if (uid == null) {
            return out;
        }
        QueryWrapper<ItemUse> qw = new QueryWrapper<>();
        qw.select("item_id", "qty")
          .eq("user_id", uid)
          .eq("use_date", LocalDate.now());
        List<Map<String, Object>> rows = itemUseMapper.selectMaps(qw);
        for (Map<String, Object> r : rows) {
            Object id = r.get("item_id");
            Object qty = r.get("qty");
            if (id instanceof Number && qty instanceof Number) {
                out.put(((Number) id).longValue(), ((Number) qty).intValue());
            }
        }
        return out;
    }

    /**
     * 占用当日额度。
     *
     * @return null 表示成功；否则是给玩家看的中文拒绝文案（调用方转成自己的 Deny）
     */
    public String reserve(Long uid, Item it, int qty) {
        if (it == null || uid == null) {
            return "未指明所用何物。";
        }
        int limit = limitOf(it);
        LocalDate today = LocalDate.now();
        itemUseMapper.ensureRow(uid, it.getId(), today);

        if (limit <= 0) {
            // 不限用也照常记账，前端可显示「今日已用」
            itemUseMapper.increment(uid, it.getId(), today, qty);
            return null;
        }

        int used = usedToday(uid, it.getId());
        if (used + qty > limit) {
            return String.format("「%s」每日限用 %d 件，今日已用 %d 件，尚余 %d 件；明日再来。",
                    it.getName(), limit, used, Math.max(0, limit - used));
        }
        // 二次原子校验：并发下由 WHERE 条件裁决，失败即视为额度已尽
        if (itemUseMapper.incrementIfAllowed(uid, it.getId(), today, qty, limit) == 0) {
            return String.format("「%s」今日限用额度已尽（%d 件），明日再来。",
                    it.getName(), limit);
        }
        return null;
    }

    /** 结算失败时退回本次占用的额度 */
    public void refund(Long uid, Long itemId, int qty) {
        if (uid != null && itemId != null && qty > 0) {
            itemUseMapper.refund(uid, itemId, LocalDate.now(), qty);
        }
    }
}
