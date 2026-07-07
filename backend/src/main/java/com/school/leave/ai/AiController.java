package com.school.leave.ai;

import com.school.leave.common.BizException;
import com.school.leave.common.Result;
import com.school.leave.config.RequireRole;
import com.school.leave.leave.LeaveRequest;
import com.school.leave.leave.LeaveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final LeaveService leaveService;

    @Data
    public static class DraftDTO {
        private String text;
    }

    @Data
    public static class AdviceDTO {
        private Long leaveId;
    }

    @Data
    public static class ChatDTO {
        private String message;
    }

    /** AI 智能填单（学生） */
    @PostMapping("/draft")
    @RequireRole("STUDENT")
    public Result<Map<String, Object>> draft(@RequestBody DraftDTO dto) {
        return Result.ok(aiService.draft(dto.getText()));
    }

    /** AI 审批建议（辅导员，只能对自己名下学生的单） */
    @PostMapping("/approval-advice")
    @RequireRole("TEACHER")
    public Result<Map<String, Object>> approvalAdvice(@RequestBody AdviceDTO dto) {
        if (dto.getLeaveId() == null) throw BizException.badParam("leaveId 不能为空");
        LeaveRequest lr = leaveService.mustGetInMyCharge(dto.getLeaveId());
        return Result.ok(aiService.approvalAdvice(lr));
    }

    /** AI 制度问答（登录即可） */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody ChatDTO dto) {
        return Result.ok(aiService.chat(dto.getMessage()));
    }
}
