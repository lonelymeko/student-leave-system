package com.school.leave.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 专业表（组织层级，只读展示用途） */
@Data
@TableName("major")
public class Major {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collegeId;
    private String majorCode;
    private String majorName;
}
