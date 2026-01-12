package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sams.common.Result;
import com.example.sams.entity.SysLog;
import com.example.sams.mapper.SysLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private SysLogMapper logMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime) {

        QueryWrapper<SysLog> query = new QueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            query.like("username", username);
        }
        if (startTime != null && !startTime.isEmpty()) {
            query.ge("create_time", startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            query.le("create_time", endTime);
        }
        query.orderByDesc("create_time");

        return Result.success(logMapper.selectPage(new Page<>(pageNum, pageSize), query));
    }
}