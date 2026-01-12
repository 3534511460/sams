package com.example.sams.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sams.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 基础 CRUD 已自动包含
}