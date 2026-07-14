package com.school.leave.notify;

import com.school.leave.common.PageVO;
import com.school.leave.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 消息通知接口（当前登录用户） */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<PageVO<Notification>> list(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(notificationService.pageMine(page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        return Result.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }
}
