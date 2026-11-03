package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.MessageMapper;
import com.fenglin.springboottest.vo.ApplyVO;
import com.fenglin.springboottest.vo.FriendVO;
import com.fenglin.springboottest.vo.MessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 社交（好友 + 传音）业务逻辑
 * 好友关系与申请共用 demon_friend 单表（status 区分），双向对称存储；
 * 私信存 demon_message，含「修炼心得卡片」类型（msg_type=1）。
 */
@Service
public class SocialService {

    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private DemonUserMapper userMapper;

    // ==================== 好友申请 ====================

    /**
     * 向某修者递出/重递道友契约。
     * 规则：已为道友→拒绝；申请途中→提示；曾被拒/删→重新递交；对方已先递来→引导前去应允。
     */
    public Result<?> apply(Long meId, String toName, String message) {
        if (toName == null || toName.trim().isEmpty()) {
            return Result.error(400, "未指明欲结交的修者。");
        }
        DemonUser target = userMapper.selectOne(
                new LambdaQueryWrapper<DemonUser>().eq(DemonUser::getName, toName.trim()));
        if (target == null) {
            return Result.error(404, "魔域之中并无此修者。");
        }
        if (target.getId().equals(meId)) {
            return Result.error(400, "不可与自己结为道友。");
        }

        // 正向：我 → 对方
        Friend forward = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, meId).eq(Friend::getFriendId, target.getId()));
        if (forward != null) {
            if (forward.getStatus() == 1) {
                return Result.error(400, "你与「" + target.getName() + "」已是道友。");
            }
            if (forward.getStatus() == 0) {
                return Result.error(400, "道友契约已在递交途中，静候回应。");
            }
            // status==2：曾被拒/删，重新递交
            forward.setStatus(0);
            forward.setMessage(message);
            forward.setHandledAt(null);
            forward.setUpdatedAt(LocalDateTime.now());
            friendMapper.updateById(forward);
            return Result.success("已向「" + target.getName() + "」重递道友契约。");
        }

        // 反向：对方 → 我（对方已先递来）
        Friend reverse = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, target.getId()).eq(Friend::getFriendId, meId));
        if (reverse != null) {
            if (reverse.getStatus() == 1) {
                return Result.error(400, "你与「" + target.getName() + "」已是道友。");
            }
            if (reverse.getStatus() == 0) {
                return Result.error(400, "对方已向你递出道友契约，前去「请求」处应允即可。");
            }
        }

        Friend f = new Friend();
        f.setUserId(meId);
        f.setFriendId(target.getId());
        f.setStatus(0);
        f.setMessage(message);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        friendMapper.insert(f);
        return Result.success("已向「" + target.getName() + "」递出道友契约，静候回应。");
    }

    // ==================== 待处理请求（呈予我） ====================

    public Result<?> listRequests(Long meId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendId, meId).eq(Friend::getStatus, 0)
                .orderByDesc(Friend::getCreatedAt));
        List<ApplyVO> list = new ArrayList<>();
        for (Friend r : rows) {
            DemonUser u = userMapper.selectById(r.getUserId());
            ApplyVO vo = new ApplyVO();
            vo.setApplyId(r.getId());
            vo.setFromId(r.getUserId());
            vo.setName(u == null ? "无名" : u.getName());
            vo.setRealm(u == null ? "" : u.getRealm());
            vo.setCultivation(u == null ? "" : u.getCultivation());
            vo.setMessage(r.getMessage());
            vo.setCreatedAt(r.getCreatedAt());
            list.add(vo);
        }
        return Result.success(list);
    }

    // ==================== 处理请求 ====================

    public Result<?> handle(Long meId, Long applyId, String action) {
        Friend apply = friendMapper.selectById(applyId);
        if (apply == null) {
            return Result.error(404, "并无此契约。");
        }
        if (!apply.getFriendId().equals(meId)) {
            return Result.error(403, "此契约非呈予你的。");
        }
        if (apply.getStatus() != 0) {
            return Result.error(400, "该契约已处理。");
        }

        if ("accept".equals(action)) {
            apply.setStatus(1);
            apply.setHandledAt(LocalDateTime.now());
            apply.setUpdatedAt(LocalDateTime.now());
            friendMapper.updateById(apply);
            // 反向关系（对方视角）：保证双向对称
            Friend reverse = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                    .eq(Friend::getUserId, meId).eq(Friend::getFriendId, apply.getUserId()));
            if (reverse == null) {
                reverse = new Friend();
                reverse.setUserId(meId);
                reverse.setFriendId(apply.getUserId());
                reverse.setStatus(1);
                reverse.setCreatedAt(LocalDateTime.now());
                reverse.setUpdatedAt(LocalDateTime.now());
                friendMapper.insert(reverse);
            } else if (reverse.getStatus() != 1) {
                reverse.setStatus(1);
                reverse.setHandledAt(LocalDateTime.now());
                reverse.setUpdatedAt(LocalDateTime.now());
                friendMapper.updateById(reverse);
            }
            return Result.success("已与「" + nameOf(apply.getUserId()) + "」结为道友。");
        } else if ("reject".equals(action)) {
            apply.setStatus(2);
            apply.setHandledAt(LocalDateTime.now());
            apply.setUpdatedAt(LocalDateTime.now());
            friendMapper.updateById(apply);
            return Result.success("已婉拒该道友契约。");
        }
        return Result.error(400, "未知的处理方式。");
    }

    // ==================== 好友列表 ====================

    public Result<?> listFriends(Long meId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, meId).eq(Friend::getStatus, 1)
                .orderByDesc(Friend::getUpdatedAt));
        List<FriendVO> list = new ArrayList<>();
        for (Friend r : rows) {
            DemonUser u = userMapper.selectById(r.getFriendId());
            FriendVO vo = new FriendVO();
            vo.setUserId(r.getFriendId());
            vo.setName(u == null ? "无名" : u.getName());
            vo.setRealm(u == null ? "" : u.getRealm());
            vo.setCultivation(u == null ? "" : u.getCultivation());

            // 最近一条消息预览
            Message last = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                    .and(w -> w.eq(Message::getFromId, meId).eq(Message::getToId, r.getFriendId()))
                    .or(w -> w.eq(Message::getFromId, r.getFriendId()).eq(Message::getToId, meId))
                    .orderByDesc(Message::getId).last("limit 1"));
            if (last != null) {
                vo.setLastMessage(last.getMsgType() == 1
                        ? ("〔修炼心得〕" + (last.getRefTitle() != null ? last.getRefTitle() : ""))
                        : last.getContent());
                vo.setLastMsgType(last.getMsgType());
                vo.setLastMsgId(last.getId());
                vo.setLastTime(last.getCreatedAt());
            }

            // 来自该好友的未读
            long unread = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getToId, meId)
                    .eq(Message::getFromId, r.getFriendId())
                    .eq(Message::getRead, 0));
            vo.setUnread((int) unread);
            list.add(vo);
        }
        return Result.success(list);
    }

    // ==================== 删除好友 ====================

    public Result<?> removeFriend(Long meId, Long friendId) {
        if (friendId == null) {
            return Result.error(400, "未指明道友。");
        }
        friendMapper.update(null, new UpdateWrapper<Friend>()
                .eq("user_id", meId).eq("friend_id", friendId)
                .set("status", 2).set("updated_at", LocalDateTime.now()));
        friendMapper.update(null, new UpdateWrapper<Friend>()
                .eq("user_id", friendId).eq("friend_id", meId)
                .set("status", 2).set("updated_at", LocalDateTime.now()));
        return Result.success("已与此道友斩断契约。");
    }

    // ==================== 搜索修者 ====================

    public Result<?> searchUsers(Long meId, String kw) {
        LambdaQueryWrapper<DemonUser> w = new LambdaQueryWrapper<DemonUser>().ne(DemonUser::getId, meId);
        if (kw != null && !kw.trim().isEmpty()) {
            w.like(DemonUser::getName, kw.trim());
        }
        w.last("limit 30");
        List<DemonUser> users = userMapper.selectList(w);
        List<Map<String, Object>> list = new ArrayList<>();
        for (DemonUser u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("realm", u.getRealm());
            m.put("cultivation", u.getCultivation());
            list.add(m);
        }
        return Result.success(list);
    }

    // ==================== 会话历史（增量 + 标记已读） ====================

    public Result<?> history(Long meId, Long friendId, Long since) {
        if (friendId == null) {
            return Result.error(400, "未指明传音对象。");
        }
        // 注意：整组 (A 或 B) 必须先用 and() 包成一组，否则 gt 只作用于最后一个 or 分支
        // （SQL 中 AND 优先级高于 OR，会导致 since 过滤失效）
        LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<Message>()
                .and(x -> x.eq(Message::getFromId, meId).eq(Message::getToId, friendId)
                        .or().eq(Message::getFromId, friendId).eq(Message::getToId, meId));
        if (since != null) {
            w.gt(Message::getId, since);
        }
        w.orderByAsc(Message::getId);
        List<Message> msgs = messageMapper.selectList(w);

        // 把来自对方的未读标记为已读（`read` 为保留字，需反引号转义）
        messageMapper.update(null, new UpdateWrapper<Message>()
                .eq("to_id", meId).eq("from_id", friendId).eq("`read`", 0).set("`read`", 1));

        List<MessageVO> list = new ArrayList<>();
        for (Message m : msgs) {
            list.add(toVO(m, meId));
        }
        return Result.success(list);
    }

    // ==================== 发送消息（含修炼心得卡片） ====================

    public Result<?> send(Long meId, Long toId, String content, Integer msgType,
                          String refType, Long refId, String refTitle, String refSummary) {
        if (toId == null) {
            return Result.error(400, "未指明传音对象。");
        }
        if (toId.equals(meId)) {
            return Result.error(400, "不可向自己传音。");
        }
        // 必须为好友（双向任一方向存在 status=1 即可）
        long friendCount = friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getStatus, 1)
                .and(x -> x.eq(Friend::getUserId, meId).eq(Friend::getFriendId, toId)
                        .or().eq(Friend::getUserId, toId).eq(Friend::getFriendId, meId)));
        if (friendCount == 0) {
            return Result.error(403, "非道友，不可传音。先结为道友吧。");
        }

        int type = (msgType == null ? 0 : msgType);
        Message m = new Message();
        m.setFromId(meId);
        m.setToId(toId);
        if (type == 1) {
            // 修炼心得卡片：以被分享内容的标题作为正文，附带 ref 信息
            m.setContent(refTitle != null ? refTitle : "");
            m.setMsgType(1);
            m.setRefType(refType);
            m.setRefId(refId);
            m.setRefTitle(refTitle);
            m.setRefSummary(refSummary);
        } else {
            if (content == null || content.trim().isEmpty()) {
                return Result.error(400, "传音内容不可为空。");
            }
            String c = content.length() > 2000 ? content.substring(0, 2000) : content;
            m.setContent(c);
            m.setMsgType(0);
        }
        m.setRead(0);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);
        return Result.success(toVO(m, meId));
    }

    // ==================== 未读统计 ====================

    public Result<?> unread(Long meId) {
        List<Message> unread = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getToId, meId).eq(Message::getRead, 0));
        Map<Long, Long> counts = new HashMap<>();
        for (Message m : unread) {
            counts.merge(m.getFromId(), 1L, Long::sum);
        }
        List<Map<String, Object>> per = new ArrayList<>();
        for (Map.Entry<Long, Long> e : counts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("friendId", e.getKey());
            item.put("count", e.getValue());
            per.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", unread.size());
        data.put("list", per);
        return Result.success(data);
    }

    // ---- 内部工具 ----

    private MessageVO toVO(Message m, Long meId) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setFromId(m.getFromId());
        vo.setToId(m.getToId());
        vo.setContent(m.getContent());
        vo.setMsgType(m.getMsgType());
        vo.setRefType(m.getRefType());
        vo.setRefId(m.getRefId());
        vo.setRefTitle(m.getRefTitle());
        vo.setRefSummary(m.getRefSummary());
        vo.setRead(m.getRead());
        vo.setCreatedAt(m.getCreatedAt());
        vo.setMine(m.getFromId().equals(meId));
        return vo;
    }

    private String nameOf(Long id) {
        DemonUser u = userMapper.selectById(id);
        return u == null ? "无名" : u.getName();
    }
}
