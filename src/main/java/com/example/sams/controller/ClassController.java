package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.sams.common.Result;
import com.example.sams.common.Log;
import com.example.sams.entity.SysClass;
import com.example.sams.entity.User;
import com.example.sams.mapper.SysClassMapper;
import com.example.sams.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/class")
public class ClassController {

    @Autowired
    private SysClassMapper classMapper;

    @Autowired
    private UserMapper userMapper;

    // 获取所有班级（包含学生人数）
    @GetMapping("/list")
    public Result list() {
        List<SysClass> classList = classMapper.selectList(null);
        
        // 为每个班级查询学生人数
        for (SysClass sysClass : classList) {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("class_name", sysClass.getClassName())
                   .eq("role", "STUDENT");
            Long count = userMapper.selectCount(wrapper);
            sysClass.setStudentCount(count.intValue());
        }
        
        return Result.success(classList);
    }

    // 新增班级
    @Log("新增班级")
    @PostMapping("/add")
    public Result add(@RequestBody SysClass sysClass) {
        try {
            classMapper.insert(sysClass);
            return Result.success("添加成功");
        } catch (Exception e) {
            return Result.error("班级已存在或添加失败");
        }
    }

    // 修改班级
    @Log("修改班级")
    @PostMapping("/update")
    public Result update(@RequestBody SysClass sysClass) {
        try {
            classMapper.updateById(sysClass);
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error("修改失败");
        }
    }

    // 删除班级
    @Log("删除班级")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        classMapper.deleteById(id);
        return Result.success("删除成功");
    }
}