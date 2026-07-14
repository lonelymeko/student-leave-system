package com.school.leave.oplog;

import com.school.leave.auth.UserContext;
import com.school.leave.user.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 写操作（POST/PUT/DELETE）操作日志拦截器。
 * 在 afterCompletion 里 best-effort 记录，绝不影响请求。
 * 注意：UserContext 会在 AuthInterceptor.afterCompletion 里被清空，
 * 因此本拦截器必须注册在 AuthInterceptor 之后（afterCompletion 逆序执行 → 本拦截器先跑，此时 UserContext 仍在）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE = Set.of("POST", "PUT", "DELETE");

    private final OperationLogMapper mapper;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            String method = request.getMethod();
            if (!WRITE.contains(method.toUpperCase())) return;

            OperationLog log = new OperationLog();
            SysUser me = UserContext.get();
            log.setUserId(me == null ? null : me.getId());
            log.setOperation(deriveOperation(request));
            String uri = request.getRequestURI();
            log.setMethod(truncate(method + " " + uri, 120));
            log.setIp(truncate(clientIp(request), 40));
            log.setCreateTime(LocalDateTime.now());
            mapper.insert(log);
        } catch (Exception e) {
            OperationLogInterceptor.log.warn("操作日志落库失败: {}", e.toString());
        }
    }

    /** 简单描述：取路径首段作为模块名 */
    private String deriveOperation(HttpServletRequest request) {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) path = path.substring(ctx.length());
        String[] seg = path.replaceFirst("^/", "").split("/");
        String module = seg.length > 0 ? seg[0] : "";
        return truncate(request.getMethod() + " " + module, 80);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
