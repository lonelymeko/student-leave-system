package com.school.leave.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.leave.auth.JwtUtil;
import com.school.leave.auth.UserContext;
import com.school.leave.common.Result;
import com.school.leave.user.SysUser;
import com.school.leave.user.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/** JWT 认证 + @RequireRole 角色鉴权拦截器 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或Token失效");
        }
        Long userId = jwtUtil.parseUserId(auth.substring(7).trim());
        if (userId == null) {
            return reject(response, 401, "未登录或Token失效");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return reject(response, 401, "账号不存在或已禁用");
        }
        UserContext.set(user);

        RequireRole rr = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (rr == null) {
            rr = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (rr != null && Arrays.stream(rr.value()).noneMatch(r -> r.equals(user.getRole()))) {
            return reject(response, 403, "无权限");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, msg)));
        return false;
    }
}
