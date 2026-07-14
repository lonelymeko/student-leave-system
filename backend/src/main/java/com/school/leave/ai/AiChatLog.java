package com.school.leave.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** AI 对话记录 ai_chat_log */
@Data
@TableName("ai_chat_log")
public class AiChatLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String question;
    private String answer;
    private String provider;
    private LocalDateTime createTime;
}
