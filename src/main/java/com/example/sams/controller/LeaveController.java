package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sams.common.Result;
import com.example.sams.common.Log;
import com.example.sams.entity.LeaveApplication;
import com.example.sams.entity.User;
import com.example.sams.mapper.LeaveApplicationMapper;
import com.example.sams.mapper.UserMapper;
import com.example.sams.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave")
public class LeaveController {

    @Autowired private LeaveApplicationMapper leaveMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private MessageService messageService; // 必须注入消息服务

    /**
     * 辅助方法：给所有老师和管理员发送通知（★ 修改签名：新增 type 和 relationId 参数 ★）
     */
    private void notifyTeachers(String content, String type, Long relationId) {
        // 查询所有老师和管理员
        QueryWrapper<User> query = new QueryWrapper<>();
        query.in("role", "TEACHER", "ADMIN");
        List<User> teachers = userMapper.selectList(query);

        for (User teacher : teachers) {
            // ★ 调用 messageService.send 四参数版本，传入类型和关联ID ★
            messageService.send(teacher.getId(), content, type, relationId);
        }
    }

    /**
     * 1. 学生提交请假申请 -> 通知老师（★ 传入 LEAVE_AUDIT 类型和请假单ID ★）
     */
    @Log("提交请假申请")
    @PostMapping("/apply")
    public Result apply(@RequestBody LeaveApplication leave) {
        // 获取学生信息，用于拼装消息
        User student = userMapper.selectById(leave.getStudentId());

        leave.setStatus(0);
        leave.setCreateTime(LocalDateTime.now());
        leaveMapper.insert(leave); // 插入后生成自增ID

        // ★ 发送通知给老师：传入 LEAVE_AUDIT 类型 + leave.getId()（关联ID）
        String msg = "收到新的请假申请：学生【" + student.getName() + "】申请 " + leave.getType();
        notifyTeachers(msg, "LEAVE_AUDIT", leave.getId()); // 修改调用传参

        return Result.success("申请成功");
    }

    /**
     * 2. 查询请假列表 (保持不变)
     */
    @GetMapping("/list")
    public Result list(@RequestParam Long userId,
                       @RequestParam String role,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<LeaveApplication> query = new QueryWrapper<>();
        if ("STUDENT".equals(role)) {
            query.eq("student_id", userId);
        }
        query.orderByAsc("status").orderByDesc("create_time");
        Page<LeaveApplication> page = leaveMapper.selectPage(new Page<>(pageNum, pageSize), query);

        List<Map<String, Object>> result = new ArrayList<>();
        for (LeaveApplication item : page.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("reason", item.getReason());
            map.put("type", item.getType());
            map.put("startTime", item.getStartTime());
            map.put("endTime", item.getEndTime());
            map.put("status", item.getStatus());
            User student = userMapper.selectById(item.getStudentId());
            map.put("studentName", student != null ? student.getName() : "未知");
            result.add(map);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("records", result);
        res.put("total", page.getTotal());
        return Result.success(res);
    }

    /**
     * 3. 学生申请销假 -> 通知老师（★ 传入 LEAVE_AUDIT 类型和请假单ID ★）
     */
    @Log("申请销假")
    @PostMapping("/cancelApply")
    public Result cancelApply(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        LeaveApplication leave = leaveMapper.selectById(id);

        if (leave == null) return Result.error("记录不存在");
        if (leave.getStatus() != 1) return Result.error("只有【已通过】的请假单才能申请销假");

        leave.setStatus(3); // 状态 3 = 销假审核中
        leaveMapper.updateById(leave);

        // ★ 发送通知给老师：传入 LEAVE_AUDIT 类型 + leave.getId()（关联ID）
        User student = userMapper.selectById(leave.getStudentId());
        String msg = "收到销假申请：学生【" + student.getName() + "】申请销假，请及时处理。";
        notifyTeachers(msg, "LEAVE_AUDIT", leave.getId()); // 修改调用传参

        return Result.success("销假申请已提交，请等待审批");
    }

    /**
     * 4. 审批接口 (新增清除未读红点逻辑)
     */
    @Log("审批请假")
    @PostMapping("/audit")
    public Result audit(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());

        LeaveApplication leave = leaveMapper.selectById(id);

        // 定义通知给学生的消息内容
        String studentMsg = "";

        // 判断当前是在处理“销假”还是“普通请假”
        if (leave.getStatus() == 3) {
            // --- 处理销假逻辑 ---
            if (status == 1) {
                leave.setStatus(4); // 4 = 已销假
                studentMsg = "您的【销假申请】已通过，该假条已失效。";
            } else {
                leave.setStatus(1); // 1 = 保持请假状态
                studentMsg = "您的【销假申请】被驳回，请假状态继续生效。";
            }
        } else {
            // --- 处理普通请假逻辑 ---
            leave.setStatus(status);
            if (status == 1) {
                studentMsg = "恭喜，您的【请假申请】已获批准。";
            } else {
                studentMsg = "很遗憾，您的【请假申请】被驳回。";
            }
        }

        leaveMapper.updateById(leave);

        // 1. 通知学生结果 (原有逻辑)
        messageService.send(leave.getStudentId(), studentMsg);

        // ★★★ 2. 新增：清除所有老师/管理员关于这条申请的未读红点 ★★★
        // 既然已经处理了，其他人就不需要再看到了
        messageService.markRelatedAsRead("LEAVE_AUDIT", id);

        return Result.success("操作完成");
    }

    // 5. 获取今日休假 (保持不变)
    @GetMapping("/today")
    public Result getTodayList() {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<LeaveApplication> query = new QueryWrapper<>();
        query.eq("status", 1).le("start_time", now).ge("end_time", now);
        List<LeaveApplication> list = leaveMapper.selectList(query);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LeaveApplication item : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("type", item.getType());
            map.put("reason", item.getReason());
            User student = userMapper.selectById(item.getStudentId());
            if (student != null) {
                map.put("studentName", student.getName());
                map.put("className", student.getClassName());
                map.put("username", student.getUsername());
            }
            result.add(map);
        }
        return Result.success(result);
    }
}