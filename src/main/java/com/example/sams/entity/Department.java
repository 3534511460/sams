package com.example.sams.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String departmentName;
    
    private String description;
    
    // 部门类型：DEPARTMENT(部门) 或 RESEARCH_GROUP(教研室)
    private String type;
}