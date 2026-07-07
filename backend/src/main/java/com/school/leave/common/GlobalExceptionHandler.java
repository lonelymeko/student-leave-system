package com.school.leave.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public Result<Void> badRequest(Exception e) {
        return Result.error(4001, "参数校验失败: " + e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> notFound(NoResourceFoundException e) {
        return Result.error(4004, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) {
        log.error("服务器错误", e);
        return Result.error(500, "服务器错误");
    }
}
