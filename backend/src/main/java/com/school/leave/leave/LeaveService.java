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
    private final com.school.leave.dict.LeaveTypeService leaveTypeService;
    private final com.school.leave.notify.NotificationService notificationService;
    private final LeaveAttachmentMapper attachmentMapper;

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir:./uploads}")
    private String uploadDir;

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
        if (!leaveTypeService.existsEnabled(dto.getType())) {
            throw BizException.badParam("请假类型不在字典中或已停用");
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
        data.put("attachments", listAttachments(id));
        return data;
    }

    /** 附件列表（按上传时间升序） */
    public java.util.List<LeaveAttachment> listAttachments(Long leaveId) {
        return attachmentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LeaveAttachment>()
                .eq("leave_id", leaveId).orderByAsc("upload_time", "id"));
    }

    /** 上传附件：仅该单学生本人；保存到 uploadDir，插入元数据 */
    public LeaveAttachment uploadAttachment(Long leaveId, org.springframework.web.multipart.MultipartFile file) {
        LeaveRequest lr = mustGetOwn(leaveId);
        if (file == null || file.isEmpty()) throw BizException.badParam("文件不能为空");
        String original = org.springframework.util.StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);
        String stored = "leave" + lr.getId() + "_" + System.currentTimeMillis()
                + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + ext;
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path target = dir.resolve(stored);
            file.transferTo(target);
        } catch (java.io.IOException e) {
            throw new BizException(5000, "文件保存失败: " + e.getMessage());
        }
        LeaveAttachment att = new LeaveAttachment();
        att.setLeaveId(lr.getId());
        att.setFileName(original);
        att.setFileUrl("/uploads/" + stored);
        att.setFileSize(file.getSize());
        att.setFileType(file.getContentType());
        att.setUploadTime(LocalDateTime.now());
        attachmentMapper.insert(att);
        return att;
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

    /**
     * 多级审批：
     * - 辅导员(TEACHER)处理 PENDING：通过→ 若 days>max_days 转 LEADER_PENDING(待副书记)，否则 APPROVED；驳回→ REJECTED
     * - 副书记(LEADER)处理 LEADER_PENDING：通过→ APPROVED；驳回→ REJECTED
     * REJECT 必填意见；越权(角色处理不属于自己阶段的单)返回 4009
     */
    @Transactional
    public LeaveVO approve(Long leaveId, String action, String comment) {
        boolean isApprove = "APPROVE".equals(action);
        boolean isReject = "REJECT".equals(action);
        if (!isApprove && !isReject) throw BizException.badParam("action 必须为 APPROVE 或 REJECT");
        if (isReject && !StringUtils.hasText(comment)) throw BizException.badParam("驳回时必须填写审批意见");

        SysUser me = UserContext.get();
        String role = me.getRole();

        LeaveRequest lr;
        String targetStatus;

        if ("LEADER".equals(role)) {
            // 副书记只处理 LEADER_PENDING
            lr = leaveMapper.selectById(leaveId);
            if (lr == null) throw BizException.notFound("请假单不存在");
            if (!LeaveStatus.LEADER_PENDING.name().equals(lr.getStatus())) {
                throw BizException.badState("副书记仅可审批待副书记审批状态的请假单");
            }
            targetStatus = isApprove ? LeaveStatus.APPROVED.name() : LeaveStatus.REJECTED.name();
        } else {
            // 辅导员只处理名下学生的 PENDING
            lr = mustGetInMyCharge(leaveId);
            if (!LeaveStatus.PENDING.name().equals(lr.getStatus())) {
                throw BizException.badState("辅导员仅可审批待审批状态的请假单");
            }
            if (isReject) {
                targetStatus = LeaveStatus.REJECTED.name();
            } else {
                // 通过：超过该类型最大天数则转副书记
                Integer maxDays = leaveTypeService.maxDaysOf(lr.getType());
                boolean overLimit = maxDays != null
                        && lr.getDays() != null
                        && lr.getDays().compareTo(BigDecimal.valueOf(maxDays)) > 0;
                targetStatus = overLimit ? LeaveStatus.LEADER_PENDING.name() : LeaveStatus.APPROVED.name();
            }
        }

        lr.setStatus(targetStatus);
        lr.setApproverId(UserContext.id());
        lr.setApproveComment(comment);
        lr.setApproveTime(LocalDateTime.now());
        leaveMapper.updateById(lr);
        recordMapper.insert(ApprovalRecord.of(leaveId, UserContext.id(),
                isApprove ? ApprovalAction.APPROVE.name() : ApprovalAction.REJECT.name(), comment));

        // best-effort 通知学生
        boolean toLeader = LeaveStatus.LEADER_PENDING.name().equals(targetStatus);
        boolean finalApproved = LeaveStatus.APPROVED.name().equals(targetStatus);
        String title = toLeader ? "请假申请已转副书记审批"
                : (finalApproved ? "请假申请已通过" : "请假申请被驳回");
        String body = "您的" + LeaveType.textOf(lr.getType()) + "申请"
                + (toLeader ? "超过辅导员权限天数，已转副书记审批"
                    : (finalApproved ? "已通过" : "被驳回"))
                + (StringUtils.hasText(comment) ? "，意见：" + comment : "");
        notificationService.send(lr.getStudentId(), title, body, "APPROVAL_RESULT", leaveId);
        return leaveMapper.findVoById(leaveId);
    }

    // ==================== 副书记 ====================

    /** 副书记待办：所有 LEADER_PENDING */
    public PageVO<LeaveVO> pageLeaderPending(long page, long size) {
        return PageVO.of(leaveMapper.pageLeaderPending(new Page<>(page, size)));
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
        // best-effort 通知学生销假确认
        notificationService.send(lr.getStudentId(), "销假已确认",
                "您的" + LeaveType.textOf(lr.getType()) + "已完成销假，流程结束", "APPROVAL_RESULT", leaveId);
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

    // ==================== 排名 / 导出 ====================

    /**
     * 请假次数排名：TEACHER 仅统计自己名下学生；LEADER/ADMIN 统计全部。
     * 返回 [{studentId,studentName,studentNo,className,leaveCount}] 按次数降序（排除 REVOKED）。
     */
    public java.util.List<Map<String, Object>> ranking() {
        SysUser me = UserContext.get();
        Long tid = "TEACHER".equals(me.getRole()) ? me.getId() : null;
        return leaveMapper.ranking(tid);
    }

    /**
     * 导出数据源：TEACHER 仅名下；LEADER/ADMIN 全部；可按 status 过滤。
     */
    public java.util.List<LeaveVO> exportData(String status) {
        SysUser me = UserContext.get();
        Long tid = "TEACHER".equals(me.getRole()) ? me.getId() : null;
        return leaveMapper.exportList(tid, status);
    }

    // ==================== 管理端 ====================

    public PageVO<LeaveVO> pageAdmin(String status, String keyword, long page, long size) {
        return PageVO.of(leaveMapper.pageAdmin(new Page<>(page, size), status, keyword));
    }
}
