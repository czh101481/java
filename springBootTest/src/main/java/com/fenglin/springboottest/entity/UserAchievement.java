package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修者成就解锁记录，对应表 demon_user_achievement
 * 唯一键 (user_id, achievement_id)：达成后由读接口自动铭刻，无需手动领取。
 */
@Data
@TableName("demon_user_achievement")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long achievementId;

    private LocalDateTime unlockedAt;
}
