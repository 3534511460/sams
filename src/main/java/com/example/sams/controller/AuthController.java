package com.example.sams.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.sams.common.JwtUtils;
import com.example.sams.common.Log;
import com.example.sams.common.Result;
import com.example.sams.dto.LoginRequest;
import com.example.sams.entity.User;
import com.example.sams.mapper.UserMapper;
import com.example.sams.service.IDepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // 新增：导入 LocalDateTime 类
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 登录接口 (已升级：校验角色)
     */
    @Log("用户登录")
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        // 1. 查询用户
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", request.getUsername());
        User user = userMapper.selectOne(query);

        // 2. 基础校验
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 3. ★★★ 角色校验 (新增逻辑) ★★★
        // 如果数据库里的角色，和前端选的角色不一致，拦截！
        if (request.getRole() != null && !user.getRole().equals(request.getRole())) {
            return Result.error("登录失败：您不是" + getRoleName(request.getRole()) + "账号");
        }

        // 4. ★★★ 关键修改：调用带角色的 Token 生成方法，传入用户名 + 数据库中的用户角色 ★★★
        // 原代码：String token = jwtUtils.generateToken(user.getUsername());
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole()); // 新增传入角色

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("user", user);

        return Result.success(map);
    }

    /**
     * 注册接口 (已升级：支持选择角色 + 新增注册时间)
     */
    @Log("用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        // 1. 检查账号是否存在
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        if (userMapper.selectCount(query) > 0) {
            return Result.error("该账号已存在！");
        }

        // 2. 默认密码
        if (user.getPassword() == null) user.setPassword("123456");

        // 3. ★★★ 角色处理 ★★★
        // 如果前端没传角色，默认给 STUDENT
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("STUDENT");
        }

        // 4. ★★★ 部门/教研室处理 ★★★
        // 如果是老师角色且未指定部门，则设置为null（可选）
        if ("TEACHER".equals(user.getRole()) && (user.getClassName() == null || user.getClassName().isEmpty())) {
            user.setClassName(null);
        }

        // ★★★ 核心新增：设置注册时间为当前系统时间 ★★★
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);
        return Result.success("注册成功，请登录");
    }

    // 辅助方法：把英文角色转中文，用于报错提示
    private String getRoleName(String role) {
        if ("ADMIN".equals(role)) return "管理员";
        if ("TEACHER".equals(role)) return "老师";
        return "学生";
    }
}