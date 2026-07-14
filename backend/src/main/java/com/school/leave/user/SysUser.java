package com.school.leave.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号（规范化后仅存账号字段）。
 * 身份档案已拆分到 student/teacher，角色走 sys_user_role；
 * 但为保持业务代码与接口契约不变，下列字段用 @TableField(exist=false) 作为“组装字段”，
 * 由 SysUserMapper 的 JOIN 查询 / UserAssembler 填充，不落库：
 *   role      —— 该用户在 sys_user_role 的角色码（种子每人单角色）
 *   realName  —— student/teacher 的姓名（ADMIN 取“系统管理员”）
 *   studentNo/className/phone —— 学生取 student(+class_info)，教师取 teacher.phone
 *   teacherId —— 学生的辅导员对应的 sys_user.id（业务主体键；保持 me.getId() 比对语义不变）
 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    /** 微信 openid（小程序一键登录绑定），序列化隐藏 */
    @JsonIgnore
    private String wxOpenid;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    // ---------- 以下为组装字段，不落库（保持接口契约/业务逻辑不变） ----------
    @TableField(exist = false)
    private String realName;
    @TableField(exist = false)
    private String role;
    @TableField(exist = false)
    private String studentNo;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String phone;
    /** 学生辅导员的 sys_user.id（业务主体键） */
    @TableField(exist = false)
    private Long teacherId;
}
