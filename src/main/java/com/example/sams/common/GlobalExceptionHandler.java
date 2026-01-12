package com.example.sams.common;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常捕获器
 * 作用：拦截后端报错，将其转换为友好的 JSON 提示返回给前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ★★★ 核心修复：专门捕获数据库重复录入异常 ★★★
     * 当插入重复的学号/工号时，Spring 会抛出 DuplicateKeyException
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKeyException(DuplicateKeyException e) {
        return Result.error("保存失败：该学号/工号已存在，请检查后重试！");
    }

    /**
     * 捕获 SQL 完整性约束异常 (备用)
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result handleSqlException(SQLIntegrityConstraintViolationException e) {
        if (e.getMessage().contains("Duplicate entry")) {
            return Result.error("保存失败：该学号/工号已存在！");
        }
        return Result.error("数据库约束异常");
    }

    /**
     * 捕获其他未知异常
     * 注意：不要把 e.getMessage() 直接返给前端，防止泄露代码细节，
     * 除非是在开发环境下调试。
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        e.printStackTrace(); // 在后端控制台打印错误，方便你看日志
        return Result.error("系统繁忙，操作失败");
    }
}