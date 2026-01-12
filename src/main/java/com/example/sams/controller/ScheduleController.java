package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.sams.common.Result;
import com.example.sams.common.Log;
import com.example.sams.entity.SysSchedule;
import com.example.sams.entity.User;
import com.example.sams.mapper.SysScheduleMapper;
import com.example.sams.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private SysScheduleMapper scheduleMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 1. 获取我的课表 (学生/老师通用)
     * GET /schedule/my?userId=1
     */
    @GetMapping("/my")
    public Result getMySchedule(@RequestParam Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        QueryWrapper<SysSchedule> query = new QueryWrapper<>();

        if ("STUDENT".equals(user.getRole())) {
            // 学生：查自己班级的课
            query.eq("class_name", user.getClassName());
        } else if ("TEACHER".equals(user.getRole())) {
            // 老师：查自己教的课
            query.eq("teacher_id", userId);
        }
        // 管理员：不传条件查不到，或者可以在这里放开逻辑

        query.orderByAsc("week_day").orderByAsc("section");
        return Result.success(scheduleMapper.selectList(query));
    }

    /**
     * 2. 管理员：查询所有/筛选排课
     * GET /schedule/list?className=计科1班
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String className,
                       @RequestParam(required = false) Long teacherId) {
        QueryWrapper<SysSchedule> query = new QueryWrapper<>();
        if (className != null && !className.isEmpty()) query.eq("class_name", className);
        if (teacherId != null) query.eq("teacher_id", teacherId);

        query.orderByAsc("week_day").orderByAsc("section");
        return Result.success(scheduleMapper.selectList(query));
    }

    /**
     * 3. 管理员：新增/修改排课 (含冲突检测)
     * POST /schedule/save
     */
    @Log("新增/修改排课")
    @PostMapping("/save")
    public Result save(@RequestBody SysSchedule schedule) {
        // ★★★ 冲突检测 ★★★
        // 同一时间段 (week_day + section)，同一个老师不能分身，同一个班级不能上两门课
        QueryWrapper<SysSchedule> check = new QueryWrapper<>();
        check.eq("week_day", schedule.getWeekDay())
                .eq("section", schedule.getSection())
                .and(wrapper -> wrapper.eq("teacher_id", schedule.getTeacherId())
                        .or()
                        .eq("class_name", schedule.getClassName()));

        // 如果是修改操作，要排除掉自己这条记录
        if (schedule.getId() != null) {
            check.ne("id", schedule.getId());
        }

        if (scheduleMapper.selectCount(check) > 0) {
            return Result.error("排课冲突：该老师或该班级在此时间段已有课程！");
        }

        if (schedule.getId() == null) {
            scheduleMapper.insert(schedule);
        } else {
            scheduleMapper.updateById(schedule);
        }

        return Result.success("排课成功");
    }

    /**
     * 4. 删除排课
     * DELETE /schedule/1
     */
    @Log("删除排课")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        scheduleMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 5. 智能获取当前应该上的课
     * 用于老师“发起签到”页面自动填充
     */
    @GetMapping("/current")
    public Result getCurrentClass(@RequestParam Long teacherId) {
        LocalDateTime now = LocalDateTime.now();
        int weekDay = now.getDayOfWeek().getValue(); // 1(周一) - 7(周日)
        int hour = now.getHour();

        // 简单的时间段映射：
        // 08:00-10:00 = 第1大节
        // 10:00-12:00 = 第2大节
        // 14:00-16:00 = 第3大节
        // 16:00-18:00 = 第4大节
        int section = 0;
        if (hour >= 8 && hour < 10) section = 1;
        else if (hour >= 10 && hour < 12) section = 2;
        else if (hour >= 14 && hour < 16) section = 3;
        else if (hour >= 16 && hour < 18) section = 4;

        if (section == 0) return Result.success(null); // 现在不是上课时间

        QueryWrapper<SysSchedule> query = new QueryWrapper<>();
        query.eq("teacher_id", teacherId)
                .eq("week_day", weekDay)
                .eq("section", section);

        SysSchedule schedule = scheduleMapper.selectOne(query);
        return Result.success(schedule); // 可能返回 null，前端做静默处理
    }

    /**
     * 6. 获取系统里已有的所有课程名称 (去重)
     * 用于前端下拉框选择课程
     */
    @GetMapping("/courses")
    public Result getCourseNames() {
        QueryWrapper<SysSchedule> query = new QueryWrapper<>();
        // 只查 course_name 字段，并去重
        query.select("DISTINCT course_name");
        List<Map<String, Object>> list = scheduleMapper.selectMaps(query);

        // 提取成 List<String>
        List<String> courses = new ArrayList<>();
        for (Map<String, Object> map : list) {
            if (map.get("course_name") != null) {
                courses.add(map.get("course_name").toString());
            }
        }
        return Result.success(courses);
    }
}