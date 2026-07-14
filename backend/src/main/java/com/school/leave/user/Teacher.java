package com.school.leave.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 教职工身份档案（1:1 sys_user）：辅导员 / 副书记 */
@Data
@TableName("teacher")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String teacherNo;
    private String realName;
    private String gender;
    private String title;
    private Long collegeId;
    private String phone;
}
