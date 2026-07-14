package com.school.leave.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 载入“组装后的”用户：账号 + 角色码 + 身份档案(student/teacher/class_info)。
     * role      = sys_user_role -> sys_role.role_code（单角色）
     * realName  = student.real_name / teacher.real_name / (ADMIN)'系统管理员'
     * studentNo = student.student_no（学生）
     * className = class_info.class_name（学生）
     * phone     = student.phone / teacher.phone
     * teacherId = 学生辅导员对应的 sys_user.id（cu.id，即 teacher(counselor).user_id）
     * 说明：ADMIN 无档案表，realName 用常量兜底；LEFT JOIN 保证账号无档案也返回一行。
     */
    String ENRICH = "SELECT su.id, su.username, su.password, su.wx_openid, su.status, " +
            "su.last_login_time, su.create_time, " +
            "(SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON ur.role_id=r.id " +
            "  WHERE ur.user_id=su.id ORDER BY FIELD(r.role_code,'ADMIN','LEADER','TEACHER','STUDENT') LIMIT 1) AS role, " +
            "COALESCE(st.real_name, te.real_name, CASE WHEN EXISTS(" +
            "  SELECT 1 FROM sys_user_role ur2 JOIN sys_role r2 ON ur2.role_id=r2.id " +
            "  WHERE ur2.user_id=su.id AND r2.role_code='ADMIN') THEN '系统管理员' ELSE NULL END) AS real_name, " +
            "st.student_no AS student_no, " +
            "ci.class_name AS class_name, " +
            "COALESCE(st.phone, te.phone) AS phone, " +
            "cu.id AS teacher_id " +
            "FROM sys_user su " +
            "LEFT JOIN student st ON st.user_id = su.id " +
            "LEFT JOIN class_info ci ON ci.id = st.class_id " +
            "LEFT JOIN teacher ct ON ct.id = st.counselor_id " +
            "LEFT JOIN sys_user cu ON cu.id = ct.user_id " +
            "LEFT JOIN teacher te ON te.user_id = su.id ";

    @Select(ENRICH + " WHERE su.id = #{id}")
    SysUser enrichById(@Param("id") Long id);

    @Select(ENRICH + " WHERE su.username = #{username}")
    SysUser enrichByUsername(@Param("username") String username);

    @Select(ENRICH + " WHERE su.wx_openid = #{openid}")
    SysUser enrichByOpenid(@Param("openid") String openid);

    /** 后台用户列表：角色/关键字过滤 + 分页由外层 IPage 处理 */
    @Select("<script>" + ENRICH +
            "<where>" +
            "<if test='role != null and role != \"\"'> AND (SELECT r.role_code FROM sys_user_role ur " +
            "  JOIN sys_role r ON ur.role_id=r.id WHERE ur.user_id=su.id " +
            "  ORDER BY FIELD(r.role_code,'ADMIN','LEADER','TEACHER','STUDENT') LIMIT 1) = #{role}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (su.username LIKE CONCAT('%',#{keyword},'%') " +
            "  OR st.real_name LIKE CONCAT('%',#{keyword},'%') OR te.real_name LIKE CONCAT('%',#{keyword},'%') " +
            "  OR st.student_no LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</where>" +
            " ORDER BY su.id ASC</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<SysUser> pageEnriched(
            com.baomidou.mybatisplus.core.metadata.IPage<SysUser> page,
            @Param("role") String role, @Param("keyword") String keyword);

    /** 辅导员下拉：返回 teacher 对应的 sys_user.id + 姓名（TEACHER 角色、账号启用） */
    @Select("SELECT su.id AS id, te.real_name AS realName " +
            "FROM teacher te JOIN sys_user su ON su.id = te.user_id " +
            "JOIN sys_user_role ur ON ur.user_id = su.id JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'TEACHER' AND su.status = 1 ORDER BY su.id ASC")
    List<java.util.Map<String, Object>> teacherOptions();
}
