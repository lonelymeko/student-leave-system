package com.school.leave.common;

import lombok.Getter;

/** 业务异常，code 与接口文档错误码对齐 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public static BizException badParam(String msg) { return new BizException(4001, msg); }
    public static BizException notFound(String msg) { return new BizException(4004, msg); }
    public static BizException forbidden(String msg) { return new BizException(403, msg); }
    public static BizException badState(String msg) { return new BizException(4009, msg); }
    public static BizException aiUnavailable() { return new BizException(5001, "AI服务暂不可用"); }
    public static BizException wxUnavailable(String msg) { return new BizException(5002, msg); }
}
