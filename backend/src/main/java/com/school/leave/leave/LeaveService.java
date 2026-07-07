package com.school.leave.leave;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.leave.auth.UserContext;
import com.school.leave.common.BizException;
import com.school.leave.common.PageVO;
import com.school.leave.common.enums.ApprovalAction;
import com.school.leave.common.enums.LeaveStatus;
import com.school.leave.common.enums.LeaveType;
import com.school.leave.user.SysUser;
import com.school.leave.user.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveMapper leaveMapper;
    private final ApprovalRecordMapper recordMapper;
    private final SysUserMapper userMapper;

    @Data
    public static class SubmitDTO {
        private String type;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String reason;
        private String destination;
        private String contactPhone;
    }

    /** 学生提交请假 */
    @Transactional
    public LeaveVO submit(SubmitDTO dto) {
        if (dto.getType() == null || !LeaveType.isValid(dto.getType())) {
            throw BizException.badParam("请假类型不合法");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw BizException.badParam("起止时间不能为空");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw BizException.badParam("结束时间必须晚于开始时间");
        }
        if (dto.getStartTime().isBefore(LocalDate.now().atStartOfDay())) {
            throw BizException.badParam("开始时间不得早于今天0点");
        }
        String reason = dto.getReason() == null ? "" : dto.getReason().trim();
        if (reason.length() < 5 || reason.length() > 200) {
            throw BizException.badParam("请假事由需为5~200字");
        }

        LeaveRequest lr = new LeaveRequest();
        lr.setStudentId(UserContext.id());
        lr.setType(dto.getType());
        lr.setStartTime(dto.getStartTime());
        lr.setEndTime(dto.getEndTime());
        lr.setDays(calcDays(dto.getStartTime(), dto.getEndTime()));
        lr.setReason(reason);
        lr.setDestination(dto.getDestination());
        lr.setContactPhone(dto.getContactPhone());
        lr.setStatus(LeaveStatus.PENDING.name());
        lr.setCreateTime(LocalDateTime.now());
        leaveMapper.insert(lr);

        recordMapper.insert(ApprovalRecord.of(lr.getId(), UserContext.id(), ApprovalAction.SUBMIT.name(), reason));
        return leaveMapper.findVoById(lr.getId());
    }

    /** 自然天数：日期差 + 1 */
    public static BigDecimal calcDays(LocalDateTime start, LocalDateTime end) {
        long d = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
        return BigDecimal.valueOf(d).setScale(1);
    }

    public PageVO<LeaveVO> pageMy(String status, long page, long size) {
        return PageVO.of(leaveMapper.pageMy(new Page<>(page, size), UserContext.id(), status));
    }

    /** 详情 + 时间线（含越权校验） */
    public Map<String, Object> detail(Long id) {
        LeaveVO vo = leaveMapper.findVoById(id);
        if (vo == null) throw BizException.notFound("请假单不存在");
        checkReadScope(vo);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        // 平铺 LeaveVO 字段 + records
        data.put("id", vo.getId());
        data.put("type", vo.getType());
        data.put("typeText", vo.getTypeText());
        data.put("startTime", vo.getStartTime());
        data.put("endTime", vo.getEndTime());
        data.put("days", vo.getDays());
        data.put("reason", vo.getReason());
        data.put("destination", vo.getDestination());
        data.put("contactPhone", vo.getContactPhone());
        data.put("status", vo.getStatus());
        data.put("statusText", vo.getStatusText());
        data.put("approverName", vo.getApproverName());
        data.put("approveComment", vo.getApproveComment());
        data.put("approveTime", vo.getApproveTime());
        data.put("cancelApplyTime", vo.getCancelApplyTime());
        data.put("cancelNote", vo.getCancelNote());
        data.put("completeTime", vo.getCompleteTime());
        data.put("createTime", vo.getCreateTime());
        data.put("studentName", vo.getStudentName());
        data.put("studentNo", vo.getStudentNo());
        data.put("className", vo.getClassName());
        data.put("records", leaveMapper.findRecords(id));
        return data;
    }

    private void checkReadScope(LeaveVO vo) {
        SysUser me = UserContext.get();
        switch (me.getRole()) {
            case "STUDENT" -> {
                if (!vo.getStudentId().equals(me.getId())) throw BizException.forbidden("只能查看自己的请假单");
            }
            case "TEACHER" -> {
                SysUser student = userMapper.selectById(vo.getStudentId());
                if (student == null || !me.getId().equals(student.getTeacherId())) {
                    throw BizException.forbidden("只能查看自己名下学生的请假单");
                }
            }
            default -> { /* ADMIN 可看全部 */ }
        }
    }

    /** 学生撤回（仅 PENDING） */
    @Transactional
    public void revoke(Long id) {
        LeaveRequest lr = mustGetOwn(id);
        if (!LeaveStatus.PENDING.name().equals(lr.getStatus())) {
            throw BizException.badState("仅待审批状态可撤回");
        }
        lr.setStatus(LeaveStatus.REVOKED.name());
        leaveMapper.updateById(lr);
        recordMapper.insert(ApprovalRecord.of(id, UserContext.id(), ApprovalAction.REVOKE.name(), "学生撤回申请"));
    }

    /** 学生申请销假（仅 APPROVED） */
    @Transactional
    public void cancelApply(Long id, String note) {
        LeaveRequest lr = mustGetOwn(id);
        if (!LeaveStatus.APPROVED.name().equals(lr.getStatus())) {
            throw BizException.badState("仅请假中状态可申请销假");
        }
        lr.setStatus(LeaveStatus.CANCEL_PENDING.name());
        lr.setCancelApplyTime(LocalDateTime.now());
        lr.setCancelNote(note);
        leaveMapper.updateById(lr);
        recordMapper.insert(ApprovalRecord.of(id, UserContext.id(), ApprovalAction.CANCEL_APPLY.name(),
                StringUtils.hasText(note) ? note : "申请销假"));
    }

    private LeaveRequest mustGetOwn(Long id) {
        LeaveRequest lr = leaveMapper.selectById(id);
        if (lr == null) throw BizException.notFound("请假单不存在");
        if (!lr.getStudentId().equals(UserContext.id())) {
            throw BizException.forbidden("只能操作自己的请假单");
        }
        return lr;
    }

    // ==================== 辅导员 ====================

    public PageVO<LeaveVO> pagePending(long page, long size) {
        return PageVO.of(leaveMapper.pagePending(new Page<>(page, size), UserContext.id()));
    }

    public PageVO<LeaveVO> pageHistory(long page, long size) {
        return PageVO.of(leaveMapper.pageHistory(new Page<>(page, size), UserContext.id()));
    }

    /** 审批（仅 PENDING），REJECT 必填意见 */
    @Transactional
    public LeaveVO approve(Long leaveId, String action, String comment) {
        boolean isApprove = "APPROVE".equals(action);
        boolean isReject = "REJECT".equals(action);
        if (!isApprove && !isReject) throw BizException.badParam("action 必须为 APPROVE 或 REJECT");
        if (isReject && !StringUtils.hasText(comment)) throw BizException.badParam("驳回时必须填写审批意见");

        LeaveRequest lr = mustGetInMyCharge(leaveId);
        if (!LeaveStatus.PENDING.name().equals(lr.getStatus())) {
            throw BizException.badState("仅待审批状态可审批");
        }
        lr.setStatus(isApprove ? LeaveStatus.APPROVED.name() : LeaveStatus.REJECTED.name());
        lr.setApproverId(UserContext.id());
        lr.setApproveComment(comment);
        lr.setApproveTime(LocalDateTime.now());
        leaveMapper.updateById(lr);
        recordMapper.insert(ApprovalRecord.of(leaveId, UserContext.id(),
                isApprove ? ApprovalAction.APPROVE.name() : ApprovalAction.REJECT.name(), comment));
        return leaveMapper.findVoById(leaveId);
    }

    /** 确认销假（仅 CANCEL_PENDING）→ COMPLETED */
    @Transactional
    public LeaveVO cancelConfirm(Long leaveId) {
        LeaveRequest lr = mustGetInMyCharge(leaveId);
        if (!LeaveStatus.CANCEL_PENDING.name().equals(lr.getStatus())) {
            throw BizException.badState("仅销假待确认状态可确认销假");
        }
        lr.setStatus(LeaveStatus.COMPLETED.name());
        lr.setCompleteTime(LocalDateTime.now());
        leaveMapper.updateById(lr);
        recordMapper.insert(ApprovalRecord.of(leaveId, UserContext.id(), ApprovalAction.CANCEL_CONFIRM.name(), "销假确认"));
        return leaveMapper.findVoById(leaveId);
    }

    /** 辅导员只能操作自己名下学生的单 */
    public LeaveRequest mustGetInMyCharge(Long leaveId) {
        LeaveRequest lr = leaveMapper.selectById(leaveId);
        if (lr == null) throw BizException.notFound("请假单不存在");
        SysUser student = userMapper.selectById(lr.getStudentId());
        if (student == null || !UserContext.id().equals(student.getTeacherId())) {
            throw BizException.forbidden("只能处理自己名下学生的请假单");
        }
        return lr;
    }

    // ==================== 管理端 ====================

    public PageVO<LeaveVO> pageAdmin(String status, String keyword, long page, long size) {
        return PageVO.of(leaveMapper.pageAdmin(new Page<>(page, size), status, keyword));
    }
}
