package com.school.leave.leave;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 审批流程节点配置（只读展示用途；当前审批链路由 leave_type.max_days 驱动） */
@Data
@TableName("approval_flow_node")
public class ApprovalFlowNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long leaveTypeId;
    private Integer nodeOrder;
    private String nodeName;
    private Long approverRoleId;
}
