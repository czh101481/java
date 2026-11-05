package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.entity.CoinLog;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.CoinLogMapper;
import com.fenglin.springboottest.mapper.WalletMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 钱囊业务
 *
 * 双币记账要点：
 *   1) 加币 / 扣币都用「UPDATE ... SET coin = coin ± N」原子语句，避免读改写竞态；
 *   2) 扣币带 `WHERE coin >= N` 条件，受影响行数为 0 即代表余额不足；
 *   3) 每次变动都写一条 demon_coin_log，余额取变动后的快照。
 */
@Service
public class CoinService {

    @Autowired
    private WalletMapper walletMapper;

    @Autowired
    private CoinLogMapper coinLogMapper;

    /** 取钱囊；不存在则开户（并发下靠唯一键兜底） */
    public Wallet walletOf(Long userId) {
        if (userId == null) {
            return null;
        }
        Wallet w = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        if (w != null) {
            return w;
        }
        Wallet fresh = new Wallet();
        fresh.setUserId(userId);
        fresh.setCoin(0);
        fresh.setJade(0);
        fresh.setEarnedCoin(0L);
        fresh.setSpentCoin(0L);
        fresh.setEarnedJade(0L);
        fresh.setSpentJade(0L);
        try {
            walletMapper.insert(fresh);
            return fresh;
        } catch (DuplicateKeyException e) {
            // 并发开户：另一线程已插入，直接读回
            return walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        }
    }

    /**
     * 入账。amount <= 0 时直接忽略（返回当前钱囊）。
     */
    public Wallet earn(Long userId, String currency, int amount, String reason, String refType, Long refId) {
        Wallet w = walletOf(userId);
        if (w == null || amount <= 0) {
            return w;
        }
        boolean coin = !Coin.JADE.equals(currency);
        UpdateWrapper<Wallet> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId);
        if (coin) {
            uw.setSql("coin = coin + " + amount)
              .setSql("earned_coin = earned_coin + " + amount);
        } else {
            uw.setSql("jade = jade + " + amount)
              .setSql("earned_jade = earned_jade + " + amount);
        }
        walletMapper.update(null, uw);

        Wallet now = walletOf(userId);
        writeLog(userId, currency, amount, coin ? now.getCoin() : now.getJade(), reason, refType, refId);
        return now;
    }

    /**
     * 支出。余额不足返回 null（调用方据此提示玩家）。
     */
    public Wallet spend(Long userId, String currency, int amount, String reason, String refType, Long refId) {
        Wallet w = walletOf(userId);
        if (w == null) {
            return null;
        }
        if (amount <= 0) {
            return w;
        }
        boolean coin = !Coin.JADE.equals(currency);
        String col = coin ? "coin" : "jade";
        String spentCol = coin ? "spent_coin" : "spent_jade";

        UpdateWrapper<Wallet> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId)
          .ge(col, amount)
          .setSql(col + " = " + col + " - " + amount)
          .setSql(spentCol + " = " + spentCol + " + " + amount);
        int rows = walletMapper.update(null, uw);
        if (rows == 0) {
            return null;
        }

        Wallet now = walletOf(userId);
        writeLog(userId, currency, -amount, coin ? now.getCoin() : now.getJade(), reason, refType, refId);
        return now;
    }

    private void writeLog(Long userId, String currency, int amount, int balance,
                          String reason, String refType, Long refId) {
        CoinLog log = new CoinLog();
        log.setUserId(userId);
        log.setCurrency(Coin.JADE.equals(currency) ? Coin.JADE : Coin.COIN);
        log.setAmount(amount);
        log.setBalance(balance);
        log.setReason(reason);
        log.setReasonText(Coin.reasonText(reason));
        log.setRefType(refType == null ? "" : refType);
        log.setRefId(refId);
        coinLogMapper.insert(log);
    }

    /** 流水（倒序） */
    public List<Map<String, Object>> logs(Long userId, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        List<CoinLog> rows = coinLogMapper.selectList(new LambdaQueryWrapper<CoinLog>()
                .eq(CoinLog::getUserId, userId)
                .orderByDesc(CoinLog::getId)
                .last("LIMIT " + size));
        List<Map<String, Object>> list = new ArrayList<>();
        for (CoinLog r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("currency", r.getCurrency());
            m.put("currencyName", Coin.JADE.equals(r.getCurrency()) ? Coin.JADE_NAME : Coin.COIN_NAME);
            m.put("amount", r.getAmount());
            m.put("balance", r.getBalance());
            m.put("reason", r.getReason());
            m.put("reasonText", r.getReasonText());
            m.put("refType", r.getRefType());
            m.put("refId", r.getRefId());
            m.put("createdAt", r.getCreatedAt());
            list.add(m);
        }
        return list;
    }

    /** 钱囊总览：余额 + 今日收支 + 累计 */
    public Map<String, Object> overview(Long userId) {
        Wallet w = walletOf(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("coin", w.getCoin() == null ? 0 : w.getCoin());
        data.put("jade", w.getJade() == null ? 0 : w.getJade());
        data.put("earnedCoin", w.getEarnedCoin() == null ? 0 : w.getEarnedCoin());
        data.put("spentCoin", w.getSpentCoin() == null ? 0 : w.getSpentCoin());
        data.put("earnedJade", w.getEarnedJade() == null ? 0 : w.getEarnedJade());
        data.put("spentJade", w.getSpentJade() == null ? 0 : w.getSpentJade());

        Map<String, Integer> today = todayFlow(userId);
        data.put("todayCoinIn", today.get("coinIn"));
        data.put("todayCoinOut", today.get("coinOut"));
        data.put("todayJadeIn", today.get("jadeIn"));
        data.put("todayJadeOut", today.get("jadeOut"));
        data.put("coinName", Coin.COIN_NAME);
        data.put("jadeName", Coin.JADE_NAME);
        return data;
    }

    /** 今日收支统计 */
    public Map<String, Integer> todayFlow(Long userId) {
        List<CoinLog> rows = coinLogMapper.selectList(new LambdaQueryWrapper<CoinLog>()
                .eq(CoinLog::getUserId, userId)
                .ge(CoinLog::getCreatedAt, LocalDate.now().atStartOfDay()));
        int coinIn = 0, coinOut = 0, jadeIn = 0, jadeOut = 0;
        for (CoinLog r : rows) {
            int amt = r.getAmount() == null ? 0 : r.getAmount();
            boolean isJade = Coin.JADE.equals(r.getCurrency());
            if (isJade) {
                if (amt >= 0) jadeIn += amt; else jadeOut += -amt;
            } else {
                if (amt >= 0) coinIn += amt; else coinOut += -amt;
            }
        }
        Map<String, Integer> m = new HashMap<>();
        m.put("coinIn", coinIn);
        m.put("coinOut", coinOut);
        m.put("jadeIn", jadeIn);
        m.put("jadeOut", jadeOut);
        return m;
    }
}
