package com.example.sams.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sams.common.Result;
import com.example.sams.common.Log;
import com.example.sams.dto.UserImportDto;
import com.example.sams.entity.User;
import com.example.sams.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 新增或更新用户
     * POST /user/save
     */
    @Log("新增或更新用户")
    @PostMapping("/save")
    public Result save(@RequestBody User user) {
        if (user.getId() == null) {
            // 新增
            // 默认密码 123456 (如果前端没传)
            if (!StringUtils.hasLength(user.getPassword())) {
                user.setPassword("123456");
            }
            userMapper.insert(user);
        } else {
            // 更新
            userMapper.updateById(user);
        }
        return Result.success("保存成功");
    }

    /**
     * 删除用户
     * DELETE /user/1
     */
    @Log("删除用户")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 查询用户列表 (支持分页和按角色/姓名搜索)
     * GET /user/list?pageNum=1&pageSize=10&role=STUDENT
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String role) {

        // 构造查询条件
        QueryWrapper<User> query = new QueryWrapper<>();
        if (StringUtils.hasLength(username)) {
            query.like("username", username).or().like("name", username);
        }
        if (StringUtils.hasLength(role)) {
            query.eq("role", role);
        }
        query.orderByDesc("id");

        // 执行分页查询
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), query);

        return Result.success(page);
    }

    /**
     * 修改密码
     * POST /user/password
     */
    @Log("修改用户密码")
    @PostMapping("/password")
    public Result updatePassword(@RequestBody Map<String, String> params) {
        Long userId = Long.valueOf(params.get("userId"));
        String oldPass = params.get("oldPass");
        String newPass = params.get("newPass");

        User user = userMapper.selectById(userId);
        if (!user.getPassword().equals(oldPass)) {
            return Result.error("原密码错误");
        }

        user.setPassword(newPass);
        userMapper.updateById(user);
        return Result.success("修改成功，请重新登录");
    }
//     * 5. Excel 批量导入用户
//     */
    @Log("批量导入用户")
    @PostMapping("/import")
    public Result importUsers(MultipartFile file) throws IOException {
        List<User> saveList = new ArrayList<>();

        EasyExcel.read(file.getInputStream(), UserImportDto.class, new ReadListener<UserImportDto>() {
            @Override
            public void invoke(UserImportDto data, AnalysisContext context) {
                // 简单的转换与去重检查
                if (data.getUsername() == null) return;

                // 查库看是否存在 (实际大批量导入应使用批量查询优化，这里简单处理)
                QueryWrapper<User> query = new QueryWrapper<>();
                query.eq("username", data.getUsername());
                if (userMapper.selectCount(query) == 0) {
                    User user = new User();
                    user.setUsername(data.getUsername());
                    user.setName(data.getName());
                    user.setRole(data.getRole());
                    user.setClassName(data.getClassName());
                    user.setPassword("123456"); // 默认密码
                    user.setCreateTime(LocalDateTime.now());
                    saveList.add(user);
                }
            }
            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 批量插入
                if (!saveList.isEmpty()) {
                    // MyBatisPlus 提供的批量保存 service.saveBatch() 更好，Mapper 需要循环
                    // 这里为了简单，直接循环插入 (生产环境建议优化)
                    for (User u : saveList) userMapper.insert(u);
                }
            }
        }).sheet().doRead();

        return Result.success("导入成功，共新增 " + saveList.size() + " 人");
    }
}