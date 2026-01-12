package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat; // ★★★ 记得导入这个包
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("leave_application")
public class LeaveApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private String reason;

    private String type;

    /**
     * ★★★ 核心修复：添加 @JsonFormat 注解 ★★★
     * timezone="GMT+8" 用于解决时区差8小时的问题
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}