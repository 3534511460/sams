package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("attendance_session")
public class AttendanceSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private String courseName;

    /**
     * 新增字段：目标班级
     * 必须加这个，Controller 里的 setTargetClass 才能用
     */
    private String targetClass;

    private String checkCode; // 动态口令

    private LocalDateTime expireTime; // 过期时间

    private LocalDateTime createTime;

    // 新增：老师发布签到时的经纬度（存储打卡基准坐标）
    private Double latitude; // 纬度
    private Double longitude; // 经度
}