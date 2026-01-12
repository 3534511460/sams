package com.example.sams.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sams.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    /**
     * 【自定义SQL】关联查询：统计每门课的实际签到人数
     * 用于数据大屏 (ECharts 柱状图)
     * 返回格式示例: [{"course_name": "Java", "count": 45}, ...]
     */
    @Select("SELECT s.course_name, COUNT(r.id) as count " +
            "FROM attendance_session s " +
            "LEFT JOIN attendance_record r ON s.id = r.session_id " +
            "GROUP BY s.course_name")
    List<Map<String, Object>> getAttendanceCountByCourse();
}