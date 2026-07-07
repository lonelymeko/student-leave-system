package com.school.leave.auth;

import com.school.leave.common.BizException;
import com.school.leave.common.Result;
import com.school.leave.user.SysUser;
import com.school.leave.user.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Data
    public static class LoginDTO {
        private String username;
        private String password;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw BizException.badParam("用户名和密码不能为空");
        }
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                        .eq("username", dto.getUsername()));
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw BizException.badParam("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", jwtUtil.createToken(user.getId(), user.getRole()));
        data.put("user", buildVO(user));
        return Result.ok(data);
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(buildVO(UserContext.get()));
    }

    private UserVO buildVO(SysUser user) {
        String teacherName = null;
        if (user.getTeacherId() != null) {
            SysUser teacher = userMapper.selectById(user.getTeacherId());
            if (teacher != null) teacherName = teacher.getRealName();
        }
        return UserVO.of(user, teacherName);
    }
}
