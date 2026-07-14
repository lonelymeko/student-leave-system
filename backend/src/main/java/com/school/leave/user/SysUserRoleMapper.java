package com.school.leave.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /** 取用户角色码（种子每人单角色，取其一：ADMIN/LEADER/TEACHER/STUDENT 优先级排序保证稳定） */
    @Select("SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} " +
            "ORDER BY FIELD(r.role_code,'ADMIN','LEADER','TEACHER','STUDENT') LIMIT 1")
    String roleCodeOf(@Param("userId") Long userId);
}
