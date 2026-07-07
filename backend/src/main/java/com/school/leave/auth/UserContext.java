package com.school.leave.auth;

import com.school.leave.user.SysUser;

/** 当前登录用户 ThreadLocal 上下文 */
public class UserContext {
    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    public static void set(SysUser user) { HOLDER.set(user); }

    public static SysUser get() { return HOLDER.get(); }

    public static Long id() { return HOLDER.get().getId(); }

    public static void clear() { HOLDER.remove(); }
}
