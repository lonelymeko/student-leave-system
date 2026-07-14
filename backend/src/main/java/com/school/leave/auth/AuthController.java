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
    private final WxService wxService;

    @Data
    public static class LoginDTO {
        private String username;
        private String password;
    }

    @Data
    public static class WxLoginDTO {
        private String code;
    }

    @Data
    public static class WxBindDTO {
        private String ticket;
        private String username;
        private String password;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw BizException.badParam("用户名和密码不能为空");
        }
        SysUser user = userMapper.enrichByUsername(dto.getUsername());
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

    /** 小程序探测微信登录是否可用（未配置 AppID/Secret 时前端隐藏按钮） */
    @GetMapping("/wx-enabled")
    public Result<Map<String, Object>> wxEnabled() {
        return Result.ok(Map.of("enabled", wxService.enabled()));
    }

    /** 微信一键登录：已绑定→JWT；未绑定→4010+短时绑定票据 */
    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody WxLoginDTO dto) {
        if (!StringUtils.hasText(dto.getCode())) throw BizException.badParam("code 不能为空");
        String openid = wxService.codeToOpenid(dto.getCode());
        SysUser user = userMapper.enrichByOpenid(openid);
        if (user == null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("bindTicket", jwtUtil.createBindTicket(openid));
            return Result.of(4010, "微信未绑定账号", data);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", jwtUtil.createToken(user.getId(), user.getRole()));
        data.put("user", buildVO(user));
        return Result.ok(data);
    }

    /** 首次绑定：校验账号密码后把 openid 绑到该账号并直接登录 */
    @PostMapping("/wx-bind")
    public Result<Map<String, Object>> wxBind(@RequestBody WxBindDTO dto) {
        if (!StringUtils.hasText(dto.getTicket())) throw BizException.badParam("ticket 不能为空");
        String openid = jwtUtil.parseBindTicket(dto.getTicket());
        if (openid == null) throw BizException.badParam("绑定票据无效或已过期，请重新微信登录");
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw BizException.badParam("用户名和密码不能为空");
        }
        SysUser user = userMapper.enrichByUsername(dto.getUsername());
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw BizException.badParam("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用");
        }
        if (StringUtils.hasText(user.getWxOpenid()) && !openid.equals(user.getWxOpenid())) {
            throw BizException.badState("该账号已绑定其他微信，请先联系管理员解绑");
        }
        SysUser occupied = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                        .eq("wx_openid", openid));
        if (occupied != null && !occupied.getId().equals(user.getId())) {
            throw BizException.badState("该微信已绑定其他账号");
        }
        user.setWxOpenid(openid);
        userMapper.updateById(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", jwtUtil.createToken(user.getId(), user.getRole()));
        data.put("user", buildVO(user));
        return Result.ok(data);
    }

    private UserVO buildVO(SysUser user) {
        String teacherName = null;
        // 学生的 teacherId = 辅导员的 sys_user.id；enrich 后 realName 取自 teacher 表
        if (user.getTeacherId() != null) {
            SysUser teacher = userMapper.enrichById(user.getTeacherId());
            if (teacher != null) teacherName = teacher.getRealName();
        }
        return UserVO.of(user, teacherName);
    }
}
