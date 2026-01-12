package com.example.sams.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserImportDto {
    @ExcelProperty("学号/工号")
    private String username;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("角色(STUDENT/TEACHER)")
    private String role;

    @ExcelProperty("班级")
    private String className;
}