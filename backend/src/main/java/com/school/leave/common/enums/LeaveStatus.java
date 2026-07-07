package com.school.leave.common.enums;

/** 请假单状态机 */
public enum LeaveStatus {
    PENDING("待审批"), APPROVED("请假中"), REJECTED("已驳回"),
    REVOKED("已撤回"), CANCEL_PENDING("销假待确认"), COMPLETED("已完成");

    private final String text;

    LeaveStatus(String text) { this.text = text; }

    public String getText() { return text; }

    public static String textOf(String name) {
        for (LeaveStatus s : values()) if (s.name().equals(name)) return s.text;
        return name;
    }
}
