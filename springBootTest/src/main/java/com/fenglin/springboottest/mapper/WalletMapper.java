package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Wallet;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface WalletMapper extends BaseMapper<Wallet> {

    /**
     * 富甲榜：钱囊 join 修者，按指定口径倒序。
     * orderBy 由 CoinService 以白名单常量传入（score / w.coin / w.jade），不接受外部原文。
     */
    @Select("SELECT w.user_id AS userId, u.name AS name, u.avatar AS avatar, "
            + "u.cultivation AS cultivation, w.coin AS coin, w.jade AS jade, "
            + "(w.coin + w.jade * 100) AS score "
            + "FROM demon_wallet w JOIN demon_user u ON u.id = w.user_id "
            + "WHERE (w.coin + w.jade * 100) > 0 "
            + "ORDER BY ${orderBy} DESC, w.user_id ASC LIMIT #{limit}")
    List<Map<String, Object>> rankList(@Param("orderBy") String orderBy, @Param("limit") int limit);

    /** 统计某项数值高于我的修者数（用于算我的名次） */
    @Select("SELECT COUNT(*) FROM demon_wallet w WHERE ${col} > #{val}")
    long countGreater(@Param("col") String col, @Param("val") int val);
}
