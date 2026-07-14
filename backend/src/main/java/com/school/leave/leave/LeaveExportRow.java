package com.school.leave.leave;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/** 请假数据导出 Excel 行模型 */
@Data
@ColumnWidth(18)
public class LeaveExportRow {
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("姓名")
    private String studentName;
    @ExcelProperty("班级")
    private String className;
    @ExcelProperty("类型")
    private String typeText;
    @ExcelProperty("开始时间")
    private String startTime;
    @ExcelProperty("结束时间")
    private String endTime;
    @ExcelProperty("天数")
    private String days;
    @ExcelProperty("事由")
    @ColumnWidth(40)
    private String reason;
    @ExcelProperty("状态")
    private String statusText;
    @ExcelProperty("审批人")
    private String approverName;
    @ExcelProperty("审批意见")
    @ColumnWidth(40)
    private String approveComment;
    @ExcelProperty("创建时间")
    private String createTime;
}
