package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ItemUse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;

import java.time.LocalDate;

/**
 * 道具每日使用量 Mapper。
 *
 * 额度占用采用「先确保当日行存在 → 带条件原子自增」两步，
 * 条件写在 SQL 的 WHERE 里（qty + n <= limit），并发下由数据库裁决，
 * 不依赖 Connector/J 的 affected-rows 语义，避免 ON DUPLICATE KEY 的歧义。
 */
@Mapper
public interface ItemUseMapper extends BaseMapper<ItemUse> {

    @Insert("INSERT IGNORE INTO demon_item_use (user_id, item_id, use_date, qty) "
            + "VALUES (#{uid}, #{itemId}, #{date}, 0)")
    int ensureRow(@Param("uid") Long uid, @Param("itemId") Long itemId,
                  @Param("date") LocalDate date);

    @Update("UPDATE demon_item_use SET qty = qty + #{n} "
            + "WHERE user_id = #{uid} AND item_id = #{itemId} AND use_date = #{date} "
            + "AND qty + #{n} <= #{limit}")
    int incrementIfAllowed(@Param("uid") Long uid, @Param("itemId") Long itemId,
                           @Param("date") LocalDate date, @Param("n") int n,
                           @Param("limit") int limit);

    @Update("UPDATE demon_item_use SET qty = qty + #{n} "
            + "WHERE user_id = #{uid} AND item_id = #{itemId} AND use_date = #{date}")
    int increment(@Param("uid") Long uid, @Param("itemId") Long itemId,
                  @Param("date") LocalDate date, @Param("n") int n);

    /** 结算失败时退回本次占用的额度（GREATEST 防并发退成负数） */
    @Update("UPDATE demon_item_use SET qty = GREATEST(0, qty - #{n}) "
            + "WHERE user_id = #{uid} AND item_id = #{itemId} AND use_date = #{date}")
    int refund(@Param("uid") Long uid, @Param("itemId") Long itemId,
               @Param("date") LocalDate date, @Param("n") int n);

    @Select("SELECT COALESCE(qty, 0) FROM demon_item_use "
            + "WHERE user_id = #{uid} AND item_id = #{itemId} AND use_date = #{date}")
    Integer usedToday(@Param("uid") Long uid, @Param("itemId") Long itemId,
                      @Param("date") LocalDate date);
}
