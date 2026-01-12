package com.example.sams.common;

import com.alibaba.fastjson.JSON;
import com.example.sams.entity.SysLog; // 需创建实体类
import com.example.sams.mapper.SysLogMapper; // 需创建Mapper
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    @Autowired private SysLogMapper logMapper;
    @Autowired private JwtUtils jwtUtils;

    @Pointcut("@annotation(com.example.sams.common.Log)")
    public void logPointCut() {}

    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void saveLog(JoinPoint joinPoint, Object jsonResult) {
        SysLog sysLog = new SysLog();

        // 1. 解析注解上的描述
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);
        if (logAnnotation != null) {
            sysLog.setOperation(logAnnotation.value());
        }

        // 2. 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("Authorization");

            // 解析用户 (从Token中获取)
            if (token != null && token.startsWith("Bearer ")) {
                String username = jwtUtils.getUsernameFromToken(token.substring(7));
                sysLog.setUsername(username);
                // 实际项目中这里应该查库获取角色，这里偷懒不存角色或存默认
            }
            sysLog.setIp(request.getRemoteAddr());
            sysLog.setMethod(request.getMethod() + " " + request.getRequestURI());
        }

        // 3. 请求参数 (简易处理，转JSON)
        Object[] args = joinPoint.getArgs();
        try {
            String params = JSON.toJSONString(args[0]); // 假设第一个参数是DTO
            sysLog.setParams(params.length() > 500 ? params.substring(0, 500) + "..." : params);
        } catch (Exception e) {
            sysLog.setParams("无法解析参数");
        }

        sysLog.setCreateTime(LocalDateTime.now());
        logMapper.insert(sysLog);
    }
}