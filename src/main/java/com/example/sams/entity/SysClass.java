package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_class")
public class SysClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String className;
    
    // 非数据库字段：学生人数
    @TableField(exist = false)
    private Integer studentCount;
}