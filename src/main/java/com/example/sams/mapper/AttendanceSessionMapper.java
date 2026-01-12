package com.example.sams.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sams.entity.AttendanceSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendanceSessionMapper extends BaseMapper<AttendanceSession> {
    // 基础 CRUD 已自动包含
}