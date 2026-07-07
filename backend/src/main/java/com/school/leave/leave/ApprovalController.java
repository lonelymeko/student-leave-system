package com.school.leave.leave;

import com.school.leave.common.PageVO;
import com.school.leave.common.Result;
import com.school.leave.config.RequireRole;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 辅导员审批接口 */
@RestController
@RequestMapping("/approval")
@RequireRole("TEACHER")
@RequiredArgsConstructor
public class ApprovalController {

    private final LeaveService leaveService;

    @Data
    public static class ApprovalDTO {
        private String action;   // APPROVE / REJECT
        private String comment;
    }

    /** 待办：PENDING + CANCEL_PENDING，PENDING 在前 */
    @GetMapping("/pending")
    public Result<PageVO<LeaveVO>> pending(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pagePending(page, size));
    }

    @GetMapping("/history")
    public Result<PageVO<LeaveVO>> history(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pageHistory(page, size));
    }

    @PostMapping("/{leaveId}")
    public Result<LeaveVO> approve(@PathVariable Long leaveId, @RequestBody ApprovalDTO dto) {
        return Result.ok(leaveService.approve(leaveId, dto.getAction(), dto.getComment()));
    }

    @PostMapping("/{leaveId}/cancel-confirm")
    public Result<LeaveVO> cancelConfirm(@PathVariable Long leaveId) {
        return Result.ok(leaveService.cancelConfirm(leaveId));
    }
}
