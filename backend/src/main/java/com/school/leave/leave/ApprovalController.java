package com.school.leave.leave;

import com.alibaba.excel.EasyExcel;
import com.school.leave.common.PageVO;
import com.school.leave.common.Result;
import com.school.leave.common.enums.LeaveStatus;
import com.school.leave.common.enums.LeaveType;
import com.school.leave.config.RequireRole;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 审批接口：辅导员 + 副书记（LEADER）多级审批，排名与导出 */
@RestController
@RequestMapping("/approval")
@RequireRole({"TEACHER", "LEADER", "ADMIN"})
@RequiredArgsConstructor
public class ApprovalController {

    private final LeaveService leaveService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Data
    public static class ApprovalDTO {
        private String action;   // APPROVE / REJECT
        private String comment;
    }

    /** 辅导员待办：PENDING + CANCEL_PENDING（名下学生），PENDING 在前 */
    @GetMapping("/pending")
    @RequireRole("TEACHER")
    public Result<PageVO<LeaveVO>> pending(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pagePending(page, size));
    }

    /** 副书记待办：所有 LEADER_PENDING */
    @GetMapping("/leader-pending")
    @RequireRole("LEADER")
    public Result<PageVO<LeaveVO>> leaderPending(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pageLeaderPending(page, size));
    }

    @GetMapping("/history")
    @RequireRole("TEACHER")
    public Result<PageVO<LeaveVO>> history(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(leaveService.pageHistory(page, size));
    }

    /** 审批：TEACHER 处理 PENDING、LEADER 处理 LEADER_PENDING（内部按角色校验阶段） */
    @PostMapping("/{leaveId}")
    @RequireRole({"TEACHER", "LEADER"})
    public Result<LeaveVO> approve(@PathVariable Long leaveId, @RequestBody ApprovalDTO dto) {
        return Result.ok(leaveService.approve(leaveId, dto.getAction(), dto.getComment()));
    }

    @PostMapping("/{leaveId}/cancel-confirm")
    @RequireRole("TEACHER")
    public Result<LeaveVO> cancelConfirm(@PathVariable Long leaveId) {
        return Result.ok(leaveService.cancelConfirm(leaveId));
    }

    /** 请假次数排名：TEACHER 名下 / LEADER,ADMIN 全部，按次数降序 */
    @GetMapping("/ranking")
    @RequireRole({"TEACHER", "LEADER", "ADMIN"})
    public Result<List<Map<String, Object>>> ranking() {
        return Result.ok(leaveService.ranking());
    }

    /** 导出请假数据 Excel：TEACHER 名下 / LEADER,ADMIN 全部，可按 status 过滤 */
    @GetMapping("/leaves/export")
    @RequireRole({"TEACHER", "LEADER", "ADMIN"})
    public void export(@RequestParam(required = false) String status,
                       HttpServletResponse response) throws IOException {
        List<LeaveVO> list = leaveService.exportData(status);
        List<LeaveExportRow> rows = new ArrayList<>(list.size());
        for (LeaveVO vo : list) {
            LeaveExportRow r = new LeaveExportRow();
            r.setStudentNo(vo.getStudentNo());
            r.setStudentName(vo.getStudentName());
            r.setClassName(vo.getClassName());
            r.setTypeText(LeaveType.textOf(vo.getType()));
            r.setStartTime(fmt(vo.getStartTime()));
            r.setEndTime(fmt(vo.getEndTime()));
            r.setDays(vo.getDays() == null ? "" : vo.getDays().stripTrailingZeros().toPlainString());
            r.setReason(vo.getReason());
            r.setStatusText(LeaveStatus.textOf(vo.getStatus()));
            r.setApproverName(vo.getApproverName());
            r.setApproveComment(vo.getApproveComment());
            r.setCreateTime(fmt(vo.getCreateTime()));
            rows.add(r);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("请假数据_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment;filename*=UTF-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), LeaveExportRow.class)
                .sheet("请假数据").doWrite(rows);
    }

    private static String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(FMT);
    }
}
