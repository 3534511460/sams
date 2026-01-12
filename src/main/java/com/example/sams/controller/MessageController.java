package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.sams.common.Result;
import com.example.sams.common.Log;
import com.example.sams.entity.SysMessage;
import com.example.sams.mapper.SysMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private SysMessageMapper messageMapper;

    /**
     * 获取未读数量 (用于红点展示)
     */
    @GetMapping("/unreadCount")
    public Result getUnreadCount(@RequestParam Long userId) {
        QueryWrapper<SysMessage> query = new QueryWrapper<>();
        query.eq("receiver_id", userId).eq("is_read", 0);
        return Result.success(messageMapper.selectCount(query));
    }

    /**
     * 获取我的消息列表 (按时间倒序)
     */
    @GetMapping("/list")
    public Result getList(@RequestParam Long userId) {
        QueryWrapper<SysMessage> query = new QueryWrapper<>();
        query.eq("receiver_id", userId).orderByDesc("create_time");
        return Result.success(messageMapper.selectList(query));
    }

    /**
     * 一键已读
     */
    @Log("一键已读消息")
    @PostMapping("/readAll")
    public Result readAll(@RequestBody SysMessage msg) {
        SysMessage update = new SysMessage();
        update.setIsRead(1);

        QueryWrapper<SysMessage> query = new QueryWrapper<>();
        query.eq("receiver_id", msg.getReceiverId());

        messageMapper.update(update, query);
        return Result.success("已全部设为已读");
    }
@Log("标记消息已读")
    @PostMapping("/read")
    public Result read(@RequestBody Map<String, Long> params) {
        Long id = params.get("id");
        if (id == null) return Result.error("ID不能为空");

        SysMessage message = new SysMessage();
        message.setId(id);
        message.setIsRead(1);

        // updateById 默认只更新非空字段
        messageMapper.updateById(message);

        return Result.success("已读");
    }
}