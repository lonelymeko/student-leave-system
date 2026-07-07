package com.school.leave.leave;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long leaveId;
    private Long operatorId;
    private String action;
    private String comment;
    private LocalDateTime createTime;

    public static ApprovalRecord of(Long leaveId, Long operatorId, String action, String comment) {
        ApprovalRecord r = new ApprovalRecord();
        r.leaveId = leaveId;
        r.operatorId = operatorId;
        r.action = action;
        r.comment = comment;
        r.createTime = LocalDateTime.now();
        return r;
    }
}
