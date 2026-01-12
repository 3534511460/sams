package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    private String content;
    private Integer isRead; // 0-未读, 1-已读
    private LocalDateTime createTime;
    private String type;       // 消息类型
    private Long relationId;   // 关联ID
}