package com.example.sams.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sams.common.DistanceUtil;
import com.example.sams.common.Log;
import com.example.sams.common.Result;
import com.example.sams.dto.CheckInRequest;
import com.example.sams.entity.*;
import com.example.sams.mapper.*;
import com.example.sams.service.MessageService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired private AttendanceSessionMapper sessionMapper;
    @Autowired private AttendanceRecordMapper recordMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private LeaveApplicationMapper leaveMapper;
    // 新增：注入 MessageService
    @Autowired private MessageService messageService;

    // 1. 发布签到 (保持不变)
    @Log("发布签到")
    @PostMapping("/publish")
    public Result publish(@RequestBody Map<String, Object> params) {
        Long teacherId = Long.valueOf(params.get("teacherId").toString());
        String courseName = params.get("courseName").toString();
        String targetClass = params.get("targetClass") != null ? params.get("targetClass").toString() : null;

        // 坐标处理
        Double lat = params.get("lat") != null ? Double.valueOf(params.get("lat").toString()) : null;
        Double lon = params.get("lon") != null ? Double.valueOf(params.get("lon").toString()) : null;

        String code = String.valueOf(new Random().nextInt(900000) + 100000);

        AttendanceSession session = new AttendanceSession();
        session.setTeacherId(teacherId);
        session.setCourseName(courseName);
        session.setTargetClass(targetClass);
        session.setCheckCode(code);
        session.setLatitude(lat);
        session.setLongitude(lon);
        session.setCreateTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusMinutes(10));

        sessionMapper.insert(session);
        return Result.success(Map.of("code", code, "sessionId", session.getId()));
    }

    // 2. 结束签到 (核心：自动判断缺勤 or 请假，保持不变)
    @Log("结束签到")
    @PostMapping("/stop")
    public Result stop(@RequestBody Map<String, Object> params) {
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        AttendanceSession session = sessionMapper.selectById(sessionId);
        if (session == null) return Result.error("场次不存在");

        session.setExpireTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        Map<String, Object> resultInfo = new HashMap<>();
        List<String> absentNames = new ArrayList<>();
        int absentCount = 0;

        if (session.getTargetClass() != null) {
            // 查全班
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.eq("role", "STUDENT").eq("class_name", session.getTargetClass());
            List<User> allStudents = userMapper.selectList(userQuery);

            // 查已签
            QueryWrapper<AttendanceRecord> recordQuery = new QueryWrapper<>();
            recordQuery.eq("session_id", sessionId);
            Set<Long> checkedInIds = recordMapper.selectList(recordQuery).stream()
                    .map(AttendanceRecord::getStudentId).collect(Collectors.toSet());

            for (User student : allStudents) {
                if (!checkedInIds.contains(student.getId())) {
                    AttendanceRecord newRecord = new AttendanceRecord();
                    newRecord.setSessionId(sessionId);
                    newRecord.setStudentId(student.getId());
                    newRecord.setCheckInTime(null);

                    // ★★★ 检查请假 ★★★
                    QueryWrapper<LeaveApplication> leaveQuery = new QueryWrapper<>();
                    leaveQuery.eq("student_id", student.getId())
                            .eq("status", 1) // 已通过
                            .le("start_time", LocalDateTime.now())
                            .ge("end_time", LocalDateTime.now());

                    if (leaveMapper.selectCount(leaveQuery) > 0) {
                        newRecord.setStatus(4); // 请假
                    } else {
                        newRecord.setStatus(3); // 缺勤
                        absentCount++;
                        absentNames.add(student.getName());

                        // ★ 新增：发送缺勤通知
                        messageService.send(student.getId(), "警报：您在课程《" + session.getCourseName() + "》中被记为缺勤！");
                    }
                    recordMapper.insert(newRecord);
                }
            }
        }
        resultInfo.put("absentCount", absentCount);
        resultInfo.put("absentNames", absentNames);
        return Result.success(resultInfo);
    }

    // 3. 学生打卡 (保持不变)
    @PostMapping("/checkIn")
    public Result checkIn(@RequestBody CheckInRequest req) {
        QueryWrapper<AttendanceSession> sessionQuery = new QueryWrapper<>();
        sessionQuery.eq("check_code", req.getCode()).gt("expire_time", LocalDateTime.now()).orderByDesc("create_time").last("LIMIT 1");
        AttendanceSession session = sessionMapper.selectOne(sessionQuery);

        if (session == null) return Result.error("口令错误，或签到已结束！");

        // 核心修改：强制 GPS 校验 
        if (session.getLatitude() != null && session.getLongitude() != null) {
            // 1. 既然老师开了定位，学生必须传坐标
            if (req.getLat() == null || req.getLon() == null) {
                return Result.error("本次签到开启了防作弊，无法获取您的位置，签到失败！");
            }
            
            // 2. 计算距离
            double distance = DistanceUtil.getDistance(
                session.getLatitude(), session.getLongitude(),
                req.getLat(), req.getLon()
            );
            
            // 3. 严格限制 200 米 (考虑到GPS漂移，给200米宽容度)
            if (distance > 200) {
                return Result.error("您距离教室过远(" + (int)distance + "米)，请在教室内签到！");
            }
        }

        User student = userMapper.selectById(req.getStudentId());
        if (session.getTargetClass() != null && !session.getTargetClass().equals(student.getClassName())) {
            return Result.error("签到失败！该签到仅限 " + session.getTargetClass());
        }

        QueryWrapper<AttendanceRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("session_id", session.getId()).eq("student_id", req.getStudentId());
        if (recordMapper.selectCount(recordQuery) > 0) return Result.error("您已经签到过了，请勿重复操作！");

        AttendanceRecord record = new AttendanceRecord();
        record.setSessionId(session.getId());
        record.setStudentId(req.getStudentId());
        record.setCheckInTime(LocalDateTime.now());
        record.setStatus(1);
        recordMapper.insert(record);
        return Result.success("签到成功！");
    }

    // 4. 查询列表 (★ 分页版，保持不变)
    @GetMapping("/list")
    public Result getList(@RequestParam(required = false) String studentName,
                          @RequestParam(required = false) String courseName,
                          @RequestParam(required = false) String teacherName,
                          @RequestParam(required = false) String startTime,
                          @RequestParam(required = false) String endTime,
                          @RequestParam(required = false) Long userId,
                          @RequestParam(required = false) String role,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize) {

        // 构造查询条件
        QueryWrapper<AttendanceRecord> query = new QueryWrapper<>();
        query.orderByDesc("check_in_time");

        if ("STUDENT".equals(role)) query.eq("student_id", userId);
        else if ("TEACHER".equals(role)) query.inSql("session_id", "SELECT id FROM attendance_session WHERE teacher_id = " + userId);

        if (startTime != null && !startTime.isEmpty()) query.ge("check_in_time", startTime);
        if (endTime != null && !endTime.isEmpty()) query.le("check_in_time", endTime);

        // ★ 分页查询
        Page<AttendanceRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), query);

        // 填充数据
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (AttendanceRecord record : page.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("checkInTime", record.getCheckInTime());
            map.put("status", record.getStatus());
            map.put("appealStatus", record.getAppealStatus());
            map.put("appealReason", record.getAppealReason());

            User student = userMapper.selectById(record.getStudentId());
            map.put("studentName", student != null ? student.getName() : "未知");

            AttendanceSession session = sessionMapper.selectById(record.getSessionId());
            if (session != null) {
                map.put("courseName", session.getCourseName());
                User teacher = userMapper.selectById(session.getTeacherId());
                map.put("teacherName", teacher != null ? teacher.getName() : "未知");
            } else {
                map.put("courseName", "-"); map.put("teacherName", "-");
            }

            // 简单过滤 (分页后的内存过滤只能过滤当前页，实际建议联表查询)
            if (studentName != null && !map.get("studentName").toString().contains(studentName)) continue;
            if (courseName != null && !map.get("courseName").toString().contains(courseName)) continue;
            if (teacherName != null && !map.get("teacherName").toString().contains(teacherName)) continue;

            resultList.add(map);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("records", resultList); // 数据列表
        res.put("total", page.getTotal()); // 总条数
        return Result.success(res);
    }

    // 5. Excel 导出 (不分页，查全部，保持不变)
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String studentName,
                       @RequestParam(required = false) String courseName,
                       @RequestParam(required = false) String teacherName,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestParam(required = false) Long userId,
                       @RequestParam(required = false) String role) throws Exception {

        // 复用逻辑查全部 (pageSize 设大一点或重写逻辑，这里为了简单直接查 10000 条)
        // 实际上应该去掉 Page 参数查 List，但为了代码复用，这里 trick 一下
        Result listResult = getList(studentName, courseName, teacherName, startTime, endTime, userId, role, 1, 10000);
        Map<String, Object> mapResult = (Map<String, Object>) listResult.getData();
        List<Map<String, Object>> list = (List<Map<String, Object>>) mapResult.get("records");

        List<ExportData> exportList = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> map : list) {
            ExportData data = new ExportData();
            data.setCourseName(map.get("courseName").toString());
            data.setTeacherName(map.get("teacherName").toString());
            data.setStudentName(map.get("studentName").toString());
            int status = (int) map.get("status");
            data.setStatus(status == 1 ? "正常" : (status == 2 ? "迟到" : (status == 3 ? "缺勤" : "请假")));
            Object time = map.get("checkInTime");
            data.setCheckInTime(time != null ? dtf.format((LocalDateTime)time) : "-");
            exportList.add(data);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("考勤记录", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ExportData.class).sheet("数据").doWrite(exportList);
    }

    // 6. 修改考勤状态 (保持不变)
    @Log("修改考勤状态")
    @PostMapping("/update")
    public Result update(@RequestBody Map<String, Object> params) {
        Long recordId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        AttendanceRecord record = recordMapper.selectById(recordId);
        record.setStatus(status);
        if (status == 3 || status == 4) record.setCheckInTime(null);
        else if (record.getCheckInTime() == null) record.setCheckInTime(LocalDateTime.now());
        recordMapper.updateById(record);
        return Result.success("更新成功");
    }

    /**
     * 7. 学生申诉 -> 通知老师（★ 传入 APPEAL_AUDIT 类型和考勤记录ID ★）
     */
    @Log("学生申诉考勤")
    @PostMapping("/appeal")
    public Result appeal(@RequestBody Map<String, Object> params) {
        Long recordId = Long.valueOf(params.get("recordId").toString());
        String reason = params.get("reason").toString();

        AttendanceRecord record = recordMapper.selectById(recordId);
        if (record == null) return Result.error("记录不存在");

        record.setAppealReason(reason);
        record.setAppealStatus(1);
        recordMapper.updateById(record);

        // ★ 新增：通知老师 - 传入 APPEAL_AUDIT 类型 + recordId（关联ID）
        User student = userMapper.selectById(record.getStudentId());
        // 找到该课程的老师
        AttendanceSession session = sessionMapper.selectById(record.getSessionId());
        if (session != null) {
            String msg = "收到考勤申诉：学生【" + student.getName() + "】对《" + session.getCourseName() + "》发起了申诉。";
            // ★ 修改 send 方法调用，传入四参数：老师ID、消息内容、类型、关联ID
            messageService.send(session.getTeacherId(), msg, "APPEAL_AUDIT", recordId);
        }

        return Result.success("申诉已提交");
    }

    // 8. 申诉审核 (新增清除未读红点逻辑)
    @Log("审核考勤申诉")
    @PostMapping("/auditAppeal")
    public Result auditAppeal(@RequestBody Map<String, Object> params) {
        Long recordId = Long.valueOf(params.get("recordId").toString());
        Integer pass = Integer.valueOf(params.get("pass").toString());

        AttendanceRecord record = recordMapper.selectById(recordId);
        String msg = "";

        if (pass == 2) {
            record.setAppealStatus(2); // 通过
            record.setStatus(1); // 恢复正常
            if(record.getCheckInTime()==null) record.setCheckInTime(LocalDateTime.now());
            msg = "您的考勤申诉已【通过】，状态已修正为正常。";
        } else {
            record.setAppealStatus(3); // 驳回
            msg = "您的考勤申诉被【驳回】。";
        }
        recordMapper.updateById(record);

        // 1. 通知学生结果 (原有逻辑)
        messageService.send(record.getStudentId(), msg);

        // ★★★ 2. 新增：清除所有老师/管理员关于这条申诉的未读红点 ★★★
        messageService.markRelatedAsRead("APPEAL_AUDIT", recordId);

        return Result.success("处理完成");
    }

    /**
     * 9. 【学生端】获取当前活跃的签到 (★ 修复：增加是否已签到的状态)
     * 替换原有仅传 className 的 active 方法
     */
    @GetMapping("/active")
    public Result getActiveSession(@RequestParam String className, @RequestParam Long studentId) {
        // 1. 查该班级所有未过期的场次
        QueryWrapper<AttendanceSession> query = new QueryWrapper<>();
        query.gt("expire_time", LocalDateTime.now())
                .eq("target_class", className)
                .orderByDesc("create_time");
        List<AttendanceSession> sessions = sessionMapper.selectList(query);

        List<Map<String, Object>> result = new ArrayList<>();
        for (AttendanceSession session : sessions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", session.getId());
            map.put("courseName", session.getCourseName());
            map.put("checkCode", session.getCheckCode());
            map.put("expireTime", session.getExpireTime());
            map.put("latitude", session.getLatitude()); // 携带坐标信息
            map.put("longitude", session.getLongitude());

            // ★★★ 核心：检查该学生是否已签到 ★★★
            QueryWrapper<AttendanceRecord> checkQuery = new QueryWrapper<>();
            checkQuery.eq("session_id", session.getId()).eq("student_id", studentId);
            map.put("isSigned", recordMapper.selectCount(checkQuery) > 0);

            result.add(map);
        }
        return Result.success(result);
    }

    /**
     * 10. 【老师端】获取我发布的正在进行中的签到 (★ 新增：解决刷新丢失和多开问题)
     */
    @GetMapping("/publishing")
    public Result getMyPublishing(@RequestParam Long teacherId) {
        QueryWrapper<AttendanceSession> query = new QueryWrapper<>();
        query.eq("teacher_id", teacherId)
                .gt("expire_time", LocalDateTime.now()) // 仅查询未过期的签到
                .orderByDesc("create_time");

        return Result.success(sessionMapper.selectList(query));
    }

    @Data
    public static class ExportData {
        @ExcelProperty("课程") private String courseName;
        @ExcelProperty("老师") private String teacherName;
        @ExcelProperty("学生") private String studentName;
        @ExcelProperty("时间") private String checkInTime;
        @ExcelProperty("状态") private String status;
    }

    // 批量更新 (保持不变)
    @Log("批量修改考勤")
    @PostMapping("/batchUpdate")
    public Result batchUpdate(@RequestBody Map<String, Object> params) {
        // 接收 ID 列表
        List<Integer> ids = (List<Integer>) params.get("ids");
        Integer status = Integer.valueOf(params.get("status").toString());

        if (ids == null || ids.isEmpty()) {
            return Result.error("未选择任何记录");
        }

        // 循环更新 (MyBatis-Plus 其实有 update(wrapper) 方法可以一次性更新，这里为了逻辑清晰用循环)
        for (Integer idVal : ids) {
            Long id = Long.valueOf(idVal.toString());
            AttendanceRecord record = recordMapper.selectById(id);
            if (record != null) {
                record.setStatus(status);
                // 逻辑修正：如果是缺勤/请假，清空时间；如果是正常/迟到，补时间
                if (status == 3 || status == 4) {
                    record.setCheckInTime(null);
                } else if (record.getCheckInTime() == null) {
                    record.setCheckInTime(LocalDateTime.now());
                }
                recordMapper.updateById(record);
            }
        }
        return Result.success("批量修改成功，共更新 " + ids.size() + " 条");
    }
}