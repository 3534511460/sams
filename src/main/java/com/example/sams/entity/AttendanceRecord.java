package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("attendance_record")
public class AttendanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long studentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime checkInTime;

    private Integer status; // 1-正常, 2-迟到, 3-缺勤, 4-请假

    // ★ 新增字段
    private String appealReason;
    private Integer appealStatus; // 0-无, 1-申诉中, 2-通过, 3-驳回
}