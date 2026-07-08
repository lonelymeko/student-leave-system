package com.school.leave.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String realName;
    private String role;
    private String studentNo;
    private String className;
    private String phone;
    private Long teacherId;
    /** 微信 openid（小程序一键登录绑定），序列化隐藏 */
    @JsonIgnore
    private String wxOpenid;
    private Integer status;
    private LocalDateTime createTime;
}
