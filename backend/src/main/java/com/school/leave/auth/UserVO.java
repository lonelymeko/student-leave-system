package com.school.leave.auth;

import com.school.leave.user.SysUser;
import lombok.Data;

/** 登录/me 返回的用户对象 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String studentNo;
    private String className;
    private String phone;
    private String teacherName;

    public static UserVO of(SysUser u, String teacherName) {
        UserVO vo = new UserVO();
        vo.id = u.getId();
        vo.username = u.getUsername();
        vo.realName = u.getRealName();
        vo.role = u.getRole();
        vo.studentNo = u.getStudentNo();
        vo.className = u.getClassName();
        vo.phone = u.getPhone();
        vo.teacherName = teacherName;
        return vo;
    }
}
