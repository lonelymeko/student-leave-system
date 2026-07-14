package com.school.leave.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 请假类型字典 leave_type */
@Data
@TableName("leave_type")
public class LeaveTypeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeCode;
    private String typeName;
    private Integer maxDays;
    private Integer needProof;
    private Integer sort;
    private Integer enabled;
}
