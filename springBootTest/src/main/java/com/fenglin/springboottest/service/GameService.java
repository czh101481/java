package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Origins;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.UserArt;
import com.fenglin.springboottest.mapper.UserArtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通用玩法数值服务
 *
 * 集中放置跨玩法复用的倍率计算，避免 HomeController 与秘境玩法各算一套导致数值漂移。
 */
@Service
public class GameService {

    @Autowired
    private UserArtMapper userArtMapper;

    /**
     * 修者综合收益倍率：魔种天赋 × 转生加成 × 习得功法加成
     * 参悟与秘境产出共用此倍率，保证「养成 → 收益」链路一致。
     */
    public double exploreMultiplier(DemonUser user) {
        Origins.Bonus origin = Origins.of(user.getRealm());
        int reincarnate = user.getReincarnate() == null ? 0 : user.getReincarnate();
        long artCount = userArtMapper.selectCount(
                new LambdaQueryWrapper<UserArt>().eq(UserArt::getUserId, user.getId()));
        double artBonus = Math.min(artCount, Realm.ART_BONUS_CAP) * Realm.ART_BONUS;
        return (1 + origin.getExploreBonus())
                * (1 + reincarnate * Realm.REINCARNATE_BONUS)
                * (1 + artBonus);
    }
}
