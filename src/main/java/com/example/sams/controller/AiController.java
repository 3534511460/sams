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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI智能助手控制器
 * 提供智能问答和数据分析功能
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AttendanceRecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendanceSessionMapper sessionMapper;

    /**
     * AI聊天接口
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");
        Integer userId = (Integer) params.get("userId");
        String role = (String) params.get("role");
        String className = (String) params.get("className");

        String reply = generateReply(message, userId, role, className);

        Map<String, String> result = new HashMap<>();
        result.put("reply", reply);
        return Result.success(result);
    }

    /**
     * 获取AI分析报告
     */
    @GetMapping("/analysis")
    public Result getAnalysis(@RequestParam(required = false) Integer userId,
                              @RequestParam(required = false) String role,
                              @RequestParam(required = false) String className) {
        Map<String, Object> analysis = new HashMap<>();

        // 1. 获取考勤统计
        Map<String, Object> stats = getAttendanceStats(userId, role, className);
        analysis.put("stats", stats);

        // 2. 生成AI建议
        List<String> suggestions = generateSuggestions(stats, role);
        analysis.put("suggestions", suggestions);

        // 3. 趋势分析
        String trend = analyzeTrend(stats);
        analysis.put("trend", trend);

        // 4. 风险提示
        List<String> risks = analyzeRisks(stats, role);
        analysis.put("risks", risks);

        // 5. 生成综合评语
        String summary = generateSummary(stats, role);
        analysis.put("summary", summary);

        return Result.success(analysis);
    }

    /**
     * 生成智能回复
     */
    private String generateReply(String message, Integer userId, String role, String className) {
        String msg = message.toLowerCase();

        // 签到相关
        if (msg.contains("签到") || msg.contains("打卡")) {
            if ("STUDENT".equals(role)) {
                return "**签到方法：**\n1. 点击左侧菜单「我要签到」\n2. 输入老师提供的签到口令\n3. 点击「确认签到」按钮\n\n⚠️ 注意：迟到超过15分钟将无法签到！";
            } else {
                return "**发起签到：**\n1. 点击左侧菜单「发起签到」\n2. 选择课程和目标班级\n3. 设置签到时长\n4. 点击「发布签到」\n\n系统会自动生成签到口令，学生凭口令进行签到。";
            }
        }

        // 请假相关
        if (msg.contains("请假") || msg.contains("假")) {
            if ("STUDENT".equals(role)) {
                return "**请假流程：**\n1. 点击左侧菜单「我的请假」\n2. 点击「新增请假」按钮\n3. 填写请假原因和时间\n4. 提交等待审批\n\n审批结果会在页面显示，请关注状态变化。";
            } else {
                return "**审批请假：**\n1. 点击左侧菜单「请假审批」\n2. 查看待审批的请假申请\n3. 点击「通过」或「驳回」进行处理";
            }
        }

        // 出勤率查询
        if (msg.contains("出勤") || msg.contains("考勤") || msg.contains("统计")) {
            Map<String, Object> stats = getAttendanceStats(userId, role, className);
            Long total = (Long) stats.getOrDefault("total", 0L);
            Long normal = (Long) stats.getOrDefault("normal", 0L);
            Long late = (Long) stats.getOrDefault("late", 0L);
            Long absent = (Long) stats.getOrDefault("absent", 0L);

            double rate = total > 0 ? (normal + late) * 100.0 / total : 100;

            return String.format("**你的考勤统计：**\n" +
                    "- 总记录：%d 次\n" +
                    "- 正常签到：%d 次\n" +
                    "- 迟到：%d 次\n" +
                    "- 缺勤：%d 次\n" +
                    "- 出勤率：%.1f%%\n\n" +
                    "点击左侧「考勤记录」可查看详细信息。", total, normal, late, absent, rate);
        }

        // 课表相关
        if (msg.contains("课") || msg.contains("课表")) {
            return "**查看课表：**\n点击左侧菜单「我的课表」，可以看到本周所有课程安排，包括：\n- 课程名称\n- 上课时间\n- 上课地点\n- 任课老师";
        }

        // 密码修改
        if (msg.contains("密码") || msg.contains("修改")) {
            return "**修改密码：**\n1. 点击右上角头像\n2. 选择「个人中心」\n3. 切换到「安全中心」标签\n4. 输入旧密码和新密码\n5. 点击「确认修改」";
        }

        // 默认回复
        return String.format("你好！我是SAMS智能助手。\n\n关于「%s」，建议你：\n" +
                "1. 查看左侧菜单的相关功能\n" +
                "2. 或者换个方式描述你的问题\n\n" +
                "我可以帮助你了解签到、请假、课表、考勤等相关操作。", message);
    }

    /**
     * 获取考勤统计数据 - 从数据库实时查询
     */
    private Map<String, Object> getAttendanceStats(Integer userId, String role, String className) {
        Map<String, Object> stats = new HashMap<>();

        // 获取会话ID列表（针对老师角色）
        List<Long> sessionIds = null;
        if ("TEACHER".equals(role) && userId != null) {
            // 老师：先查询自己创建的所有签到会话
            QueryWrapper<AttendanceSession> sessionWrapper = new QueryWrapper<>();
            sessionWrapper.eq("teacher_id", userId);
            List<AttendanceSession> sessions = sessionMapper.selectList(sessionWrapper);
            sessionIds = sessions.stream().map(AttendanceSession::getId).collect(Collectors.toList());

            // 如果该老师没有创建过任何签到会话，返回空统计
            if (sessionIds.isEmpty()) {
                stats.put("total", 0L);
                stats.put("normal", 0L);
                stats.put("late", 0L);
                stats.put("absent", 0L);
                stats.put("leave", 0L);
                stats.put("attendanceRate", 0.0);
                return stats;
            }
        }

        // 根据角色设置查询条件
        QueryWrapper<AttendanceRecord> totalWrapper = new QueryWrapper<>();
        if ("STUDENT".equals(role) && userId != null) {
            // 学生：只查询自己的记录
            totalWrapper.eq("student_id", userId);
        } else if ("TEACHER".equals(role) && sessionIds != null) {
            // 老师：查询自己发起的签到会话下的所有记录
            totalWrapper.in("session_id", sessionIds);
        } else if ("ADMIN".equals(role)) {
            // 管理员：查询全部记录
            // 不添加额外条件，查询所有记录
        } else {
            // 其他情况，查询所有记录
            // 不添加额外条件
        }

        Long total = recordMapper.selectCount(totalWrapper);
        stats.put("total", total);

        // 正常 (status=1)
        QueryWrapper<AttendanceRecord> normalWrapper = new QueryWrapper<>();
        if ("STUDENT".equals(role) && userId != null) {
            normalWrapper.eq("student_id", userId);
        } else if ("TEACHER".equals(role) && sessionIds != null) {
            normalWrapper.in("session_id", sessionIds);
        } else if ("ADMIN".equals(role)) {
            // 管理员查询全部
        }
        normalWrapper.eq("status", 1);
        Long normal = recordMapper.selectCount(normalWrapper);
        stats.put("normal", normal);

        // 迟到 (status=2)
        QueryWrapper<AttendanceRecord> lateWrapper = new QueryWrapper<>();
        if ("STUDENT".equals(role) && userId != null) {
            lateWrapper.eq("student_id", userId);
        } else if ("TEACHER".equals(role) && sessionIds != null) {
            lateWrapper.in("session_id", sessionIds);
        } else if ("ADMIN".equals(role)) {
            // 管理员查询全部
        }
        lateWrapper.eq("status", 2);
        Long late = recordMapper.selectCount(lateWrapper);
        stats.put("late", late);

        // 缺勤 (status=3)
        QueryWrapper<AttendanceRecord> absentWrapper = new QueryWrapper<>();
        if ("STUDENT".equals(role) && userId != null) {
            absentWrapper.eq("student_id", userId);
        } else if ("TEACHER".equals(role) && sessionIds != null) {
            absentWrapper.in("session_id", sessionIds);
        } else if ("ADMIN".equals(role)) {
            // 管理员查询全部
        }
        absentWrapper.eq("status", 3);
        Long absent = recordMapper.selectCount(absentWrapper);
        stats.put("absent", absent);

        // 请假 (status=4)
        QueryWrapper<AttendanceRecord> leaveWrapper = new QueryWrapper<>();
        if ("STUDENT".equals(role) && userId != null) {
            leaveWrapper.eq("student_id", userId);
        } else if ("TEACHER".equals(role) && sessionIds != null) {
            leaveWrapper.in("session_id", sessionIds);
        } else if ("ADMIN".equals(role)) {
            // 管理员查询全部
        }
        leaveWrapper.eq("status", 4);
        Long leave = recordMapper.selectCount(leaveWrapper);
        stats.put("leave", leave);

        // 计算出勤率 (正常+迟到 / 总数) - 避免除零错误
        double rate = total > 0 ? (normal + late) * 100.0 / total : 0;
        stats.put("attendanceRate", Math.round(rate * 10) / 10.0);

        return stats;
    }

    /**
     * 生成AI建议
     */
    private List<String> generateSuggestions(Map<String, Object> stats, String role) {
        List<String> suggestions = new ArrayList<>();
        double rate = (Double) stats.getOrDefault("attendanceRate", 100.0);
        Long late = (Long) stats.getOrDefault("late", 0L);
        Long absent = (Long) stats.getOrDefault("absent", 0L);

        if ("STUDENT".equals(role)) {
            if (rate >= 95) {
                suggestions.add("出勤表现优秀，继续保持！");
            } else if (rate >= 80) {
                suggestions.add("出勤率良好，但仍有提升空间");
            } else {
                suggestions.add("出勤率偏低，建议加强时间管理");
            }

            if (late > 3) {
                suggestions.add("迟到次数较多，建议提前10分钟出发");
            }

            if (absent > 2) {
                suggestions.add("缺勤次数较多，请注意不要影响学业");
            }
        } else {
            suggestions.add("定期查看班级考勤数据，关注异常学生");
            suggestions.add("对频繁缺勤的学生进行关怀和沟通");
            if (rate < 90) {
                suggestions.add("整体出勤率偏低，建议了解原因并改进");
            }
        }

        return suggestions;
    }

    /**
     * 趋势分析
     */
    private String analyzeTrend(Map<String, Object> stats) {
        double rate = (Double) stats.getOrDefault("attendanceRate", 100.0);

        if (rate >= 95) {
            return "📈 出勤趋势良好，保持稳定";
        } else if (rate >= 85) {
            return "📊 出勤率处于正常水平";
        } else if (rate >= 70) {
            return "⚠️ 出勤率需要关注，有下降风险";
        } else {
            return "🚨 出勤率过低，需要重点关注";
        }
    }

    /**
     * 风险分析
     */
    private List<String> analyzeRisks(Map<String, Object> stats, String role) {
        List<String> risks = new ArrayList<>();
        double rate = (Double) stats.getOrDefault("attendanceRate", 100.0);
        Long absent = (Long) stats.getOrDefault("absent", 0L);

        if (rate < 80) {
            risks.add("出勤率低于80%，可能影响学业成绩");
        }

        if (absent > 5) {
            risks.add("缺勤次数超过5次，建议尽快补救");
        }

        if (risks.isEmpty()) {
            risks.add("暂无风险提示，继续保持");
        }

        return risks;
    }

    /**
     * 生成综合评语
     */
    private String generateSummary(Map<String, Object> stats, String role) {
        double rate = (Double) stats.getOrDefault("attendanceRate", 100.0);
        Long total = (Long) stats.getOrDefault("total", 0L);

        if (total == 0) {
            return "暂无考勤记录，开始你的学习之旅吧！";
        }

        if (rate >= 95) {
            return "🌟 出勤表现优异！你的自律和坚持值得称赞，继续保持这份热情！";
        } else if (rate >= 85) {
            return "👍 出勤情况良好，稍加努力就能达到优秀标准，加油！";
        } else if (rate >= 70) {
            return "💪 出勤率还有提升空间，建议合理安排时间，减少迟到和缺勤。";
        } else {
            return "⚠️ 出勤率需要重点改善，频繁缺勤会影响学习效果，请务必重视！";
        }
    }
}
