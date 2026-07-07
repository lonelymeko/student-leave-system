package com.school.leave.common.enums;

/** 请假类型 */
public enum LeaveType {
    SICK("病假"), PERSONAL("事假"), EMERGENCY("急事"), OTHER("其他");

    private final String text;

    LeaveType(String text) { this.text = text; }

    public String getText() { return text; }

    public static boolean isValid(String name) {
        for (LeaveType t : values()) if (t.name().equals(name)) return true;
        return false;
    }

    public static String textOf(String name) {
        for (LeaveType t : values()) if (t.name().equals(name)) return t.text;
        return name;
    }
}
