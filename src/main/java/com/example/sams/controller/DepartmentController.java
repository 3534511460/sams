package com.example.sams.controller;

import com.example.sams.common.Result;
import com.example.sams.common.Log;
import com.example.sams.entity.Department;
import com.example.sams.mapper.DepartmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
@CrossOrigin  // 允许跨域访问
public class DepartmentController {
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    // 获取所有部门
    @GetMapping("/list")
    public Result list() {
        return Result.success(departmentMapper.selectList(null));
    }
    
    // 新增部门
    @Log("新增部门")
    @PostMapping("/add")
    public Result add(@RequestBody Department department) {
        try {
            departmentMapper.insert(department);
            return Result.success("添加成功");
        } catch (Exception e) {
            return Result.error("部门已存在或添加失败");
        }
    }
    
    // 删除部门
    @Log("删除部门")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        departmentMapper.deleteById(id);
        return Result.success("删除成功");
    }
}