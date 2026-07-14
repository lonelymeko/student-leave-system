package com.school.leave.leave;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 学期表（只读展示用途；leave_request.semester_id 可空） */
@Data
@TableName("semester")
public class Semester {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String semesterName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isCurrent;
}
