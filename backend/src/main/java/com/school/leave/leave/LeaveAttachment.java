package com.school.leave.leave;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 请假附件 leave_attachment */
@Data
@TableName("leave_attachment")
public class LeaveAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long leaveId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadTime;
}
