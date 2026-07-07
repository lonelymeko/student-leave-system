package com.school.leave.leave;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leave_request")
public class LeaveRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal days;
    private String reason;
    private String destination;
    private String contactPhone;
    private String status;
    private Long approverId;
    private String approveComment;
    private LocalDateTime approveTime;
    private LocalDateTime cancelApplyTime;
    private String cancelNote;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
