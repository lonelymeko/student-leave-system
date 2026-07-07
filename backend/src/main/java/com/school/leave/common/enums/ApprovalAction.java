package com.school.leave.common.enums;

/** 审批时间线动作 */
public enum ApprovalAction {
    SUBMIT("提交申请"), APPROVE("审批通过"), REJECT("审批驳回"),
    REVOKE("撤回申请"), CANCEL_APPLY("申请销假"), CANCEL_CONFIRM("销假确认");

    private final String text;

    ApprovalAction(String text) { this.text = text; }

    public String getText() { return text; }

    public static String textOf(String name) {
        for (ApprovalAction a : values()) if (a.name().equals(name)) return a.text;
        return name;
    }
}
