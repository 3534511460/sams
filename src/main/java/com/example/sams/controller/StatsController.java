package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.sams.common.Result;
import com.example.sams.entity.User;
import com.example.sams.mapper.AttendanceRecordMapper;
import com.example.sams.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendanceRecordMapper recordMapper;

    /**
     * 1. 获取首页顶部卡片的基础统计数据
     */
    @GetMapping("/counts")
    public Result getCounts() {
        Map<String, Long> map = new HashMap<>();

        // 统计学生总数
        map.put("studentCount", userMapper.selectCount(new QueryWrapper<User>().eq("role", "STUDENT")));

        // 统计老师总数
        map.put("teacherCount", userMapper.selectCount(new QueryWrapper<User>().eq("role", "TEACHER")));

        // 统计总签到人次
        map.put("checkInCount", recordMapper.selectCount(null));

        return Result.success(map);
    }

    /**
     * 2. 获取课程签到排行 (用于图表)
     * 注意：这里调用了我们在 Mapper 里写的自定义 SQL 方法
     */
    @GetMapping("/course-rank")
    public Result getCourseRank() {
        List<Map<String, Object>> list = recordMapper.getAttendanceCountByCourse();
        return Result.success(list);
    }
}