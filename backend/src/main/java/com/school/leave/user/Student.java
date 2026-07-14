package com.school.leave.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 学生身份档案（1:1 sys_user） */
@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private String gender;
    private Long classId;
    private Long counselorId;   // -> teacher.id
    private Integer enrollYear;
    private String phone;
}
