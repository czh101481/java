package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Trade;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 鬼市挂单 Mapper
 *
 * 列表查询直接 join 物品与修者，一次取齐前端要展示的字段，避免 N+1。
 */
public interface TradeMapper extends BaseMapper<Trade> {

    /** 在售挂单（id 倒序，最多 limit 条） */
    @Select("SELECT t.id AS id, t.seller_id AS sellerId, t.item_id AS itemId, t.qty AS qty, "
            + "t.price AS price, t.currency AS currency, t.status AS status, t.buyer_id AS buyerId, "
            + "t.created_at AS createdAt, t.sold_at AS soldAt, "
            + "i.name AS itemName, i.icon AS itemIcon, i.category AS itemCategory, "
            + "i.effect_type AS effectType, i.effect_value AS effectValue, i.desc_text AS itemDesc, "
            + "u.name AS sellerName, u.avatar AS sellerAvatar, u.cultivation AS sellerRealm, "
            + "b.name AS buyerName "
            + "FROM demon_trade t "
            + "JOIN demon_item i ON i.id = t.item_id "
            + "JOIN demon_user u ON u.id = t.seller_id "
            + "LEFT JOIN demon_user b ON b.id = t.buyer_id "
            + "WHERE t.status = 0 ORDER BY t.id DESC LIMIT #{limit}")
    List<Map<String, Object>> listOnSale(@Param("limit") int limit);

    /** 某修者的挂单（含已售 / 已撤，便于自查） */
    @Select("SELECT t.id AS id, t.seller_id AS sellerId, t.item_id AS itemId, t.qty AS qty, "
            + "t.price AS price, t.currency AS currency, t.status AS status, t.buyer_id AS buyerId, "
            + "t.created_at AS createdAt, t.sold_at AS soldAt, "
            + "i.name AS itemName, i.icon AS itemIcon, i.category AS itemCategory, "
            + "i.effect_type AS effectType, i.effect_value AS effectValue, i.desc_text AS itemDesc, "
            + "u.name AS sellerName, u.avatar AS sellerAvatar, u.cultivation AS sellerRealm, "
            + "b.name AS buyerName "
            + "FROM demon_trade t "
            + "JOIN demon_item i ON i.id = t.item_id "
            + "JOIN demon_user u ON u.id = t.seller_id "
            + "LEFT JOIN demon_user b ON b.id = t.buyer_id "
            + "WHERE t.seller_id = #{sellerId} ORDER BY t.id DESC LIMIT #{limit}")
    List<Map<String, Object>> listBySeller(@Param("sellerId") Long sellerId, @Param("limit") int limit);
}
