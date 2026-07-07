package com.school.leave.leave;

import com.school.leave.common.enums.ApprovalAction;
import lombok.Data;

import java.time.LocalDateTime;

/** 审批时间线条目 */
@Data
public class RecordVO {
    private String operatorName;
    private String action;
    private String comment;
    private LocalDateTime createTime;

    public String getActionText() { return ApprovalAction.textOf(action); }
}
