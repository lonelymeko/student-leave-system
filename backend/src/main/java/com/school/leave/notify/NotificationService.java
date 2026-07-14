package com.school.leave.notify;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.leave.auth.UserContext;
import com.school.leave.common.BizException;
import com.school.leave.common.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 消息通知服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper mapper;

    public PageVO<Notification> pageMine(long page, long size) {
        IPage<Notification> p = mapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Notification>().eq("user_id", UserContext.id())
                        .orderByDesc("create_time", "id"));
        return PageVO.of(p);
    }

    public long unreadCount() {
        Long c = mapper.selectCount(new QueryWrapper<Notification>()
                .eq("user_id", UserContext.id()).eq("is_read", 0));
        return c == null ? 0 : c;
    }

    /** 标记已读，仅本人的通知可标记 */
    public void markRead(Long id) {
        Notification n = mapper.selectById(id);
        if (n == null) throw BizException.notFound("通知不存在");
        if (!UserContext.id().equals(n.getUserId())) throw BizException.forbidden("只能操作自己的通知");
        if (n.getIsRead() != null && n.getIsRead() == 1) return;
        n.setIsRead(1);
        mapper.updateById(n);
    }

    /** best-effort 发送通知，失败绝不影响主流程 */
    public void send(Long userId, String title, String content, String bizType, Long bizId) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setTitle(title);
            n.setContent(content);
            n.setBizType(bizType);
            n.setBizId(bizId);
            n.setIsRead(0);
            n.setCreateTime(LocalDateTime.now());
            mapper.insert(n);
        } catch (Exception e) {
            log.warn("通知落库失败 userId={} bizId={}: {}", userId, bizId, e.toString());
        }
    }
}
