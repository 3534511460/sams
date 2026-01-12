package com.example.sams.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.sams.entity.SysMessage;
import com.example.sams.mapper.SysMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class MessageService {
    @Autowired private SysMessageMapper messageMapper;

    /**
     * 发送消息 (重载方法，支持带业务参数)
     */
    public void send(Long receiverId, String content, String type, Long relationId) {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setType(type);           // ★
        msg.setRelationId(relationId); // ★
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    // 保留旧的简单发送方法 (兼容旧代码，默认为 NOTICE)
    public void send(Long receiverId, String content)
    {
        this.send(receiverId, content, "NOTICE", null);
    }
    public void markRelatedAsRead(String type, Long relationId) {
        if (relationId == null || type == null) return;

        SysMessage updateMsg = new SysMessage();
        updateMsg.setIsRead(1); // 设为已读

        UpdateWrapper<SysMessage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("type", type)
                .eq("relation_id", relationId)
                .eq("is_read", 0); // 只更新那些未读的

        messageMapper.update(updateMsg, updateWrapper);
    }

}