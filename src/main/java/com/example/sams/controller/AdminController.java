package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.sams.common.Result;
import com.example.sams.entity.AttendanceRecord;
import com.example.sams.entity.AttendanceSession;
import com.example.sams.entity.User;
import com.example.sams.mapper.AttendanceRecordMapper;
import com.example.sams.mapper.AttendanceSessionMapper;
import com.example.sams.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    // 确保所有Mapper与你的实体类对应，注入无误
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    /**
     * 获取系统全局统计信息（兼容你的AttendanceSession实体类）
     */
    @GetMapping("/system-stats")
    public Result getSystemStats() {
        Map<String, Object> statsMap = new HashMap<>();

        // 1. 学生总数：查询sys_user表中角色为STUDENT的记录
        QueryWrapper<User> studentQuery = new QueryWrapper<>();
        studentQuery.eq("role", "STUDENT");
        long studentCount = userMapper.selectCount(studentQuery);
        statsMap.put("studentCount", studentCount);

        // 2. 老师总数：查询sys_user表中角色为TEACHER的记录
        QueryWrapper<User> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("role", "TEACHER");
        long teacherCount = userMapper.selectCount(teacherQuery);
        statsMap.put("teacherCount", teacherCount);

        // 3. 累计签到人次：查询AttendanceRecord表总记录数
        long totalCheckInCount = attendanceRecordMapper.selectCount(null);
        statsMap.put("checkInCount", totalCheckInCount);

        // 4. 今日课程数量：查询今日创建的AttendanceSession记录（兼容你的实体类字段）
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        QueryWrapper<AttendanceSession> sessionQuery = new QueryWrapper<>();
        sessionQuery.ge("create_time", todayStart); // 你的实体类有createTime字段，数据库对应create_time
        long todayCourseCount = attendanceSessionMapper.selectCount(sessionQuery);
        statsMap.put("todayCourseCount", todayCourseCount);

        // 关键：打印查询结果日志，方便你排查是否查询到数据
        System.out.println("===== 管理员统计数据查询结果 =====");
        System.out.println("学生总数：" + studentCount);
        System.out.println("老师总数：" + teacherCount);
        System.out.println("累计签到人次：" + totalCheckInCount);
        System.out.println("今日课程数量：" + todayCourseCount);
        System.out.println("==================================");

        return Result.success(statsMap);
    }
}