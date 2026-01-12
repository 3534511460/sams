package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_schedule")
public class SysSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private String courseName;

    private String className;

    // ★★★ 关键修复：补上这个字段，否则后端“看不见”地点 ★★★
    private String location;

    private Integer weekDay;

    private Integer section;
}