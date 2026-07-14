package com.school.leave.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 学院表（组织层级，只读展示用途） */
@Data
@TableName("college")
public class College {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String collegeCode;
    private String collegeName;
}
