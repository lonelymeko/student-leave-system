package com.school.leave.leave;

import com.school.leave.common.PageVO;
import com.school.leave.common.Result;
import com.school.leave.config.RequireRole;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 学生端请假接口 */
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @Data
    public static class CancelApplyDTO {
        private String note;
    }

    @PostMapping
    @RequireRole("STUDENT")
    public Result<LeaveVO> submit(@RequestBody LeaveService.SubmitDTO dto) {
        return Result.ok(leaveService.submit(dto));
    }

    @GetMapping("/my")
    @RequireRole("STUDENT")
    public Result<PageVO<LeaveVO>> my(@RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pageMy(status, page, size));
    }

    /** 详情：学生看自己的，辅导员看名下学生的，管理员看全部 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(leaveService.detail(id));
    }

    @PutMapping("/{id}/revoke")
    @RequireRole("STUDENT")
    public Result<Void> revoke(@PathVariable Long id) {
        leaveService.revoke(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel-apply")
    @RequireRole("STUDENT")
    public Result<Void> cancelApply(@PathVariable Long id, @RequestBody(required = false) CancelApplyDTO dto) {
        leaveService.cancelApply(id, dto == null ? null : dto.getNote());
        return Result.ok();
    }
}
