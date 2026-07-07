package com.school.leave.leave;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.leave.common.enums.LeaveStatus;
import com.school.leave.common.enums.LeaveType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 请假单视图对象（含学生/审批人姓名与文案） */
@Data
public class LeaveVO {
    private Long id;
    @JsonIgnore
    private Long studentId;   // 仅用于越权校验，不出现在响应里
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal days;
    private String reason;
    private String destination;
    private String contactPhone;
    private String status;
    private String approverName;
    private String approveComment;
    private LocalDateTime approveTime;
    private LocalDateTime cancelApplyTime;
    private String cancelNote;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private String studentName;
    private String studentNo;
    private String className;

    public String getTypeText() { return LeaveType.textOf(type); }

    public String getStatusText() { return LeaveStatus.textOf(status); }
}
