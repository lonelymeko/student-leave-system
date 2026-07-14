package com.school.leave.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 角色鉴权注解，标注在 Controller 类或方法上 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 允许的角色：STUDENT / TEACHER / LEADER / ADMIN */
    String[] value();
}
