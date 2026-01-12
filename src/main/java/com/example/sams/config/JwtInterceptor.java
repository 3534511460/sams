package com.example.sams.config;

import com.example.sams.common.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 处理 OPTIONS 请求 (跨域预检)，直接放行（保持原有逻辑）
        if("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();
        
        // 2. 允许公共API无需认证访问
        if (isPublicApi(requestURI)) {
            return true;
        }

        // 3. 获取 Token（从Authorization头中提取，去除Bearer前缀，保持原有逻辑）
        String token = request.getHeader("Authorization");
        if (StringUtils.hasLength(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 4. 校验Token是否有效（复用你的原有校验逻辑，无Token或解析失败返回401）
        if (!StringUtils.hasLength(token) || jwtUtils.getUsernameFromToken(token) == null) {
            response.setStatus(401); // 未授权
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或Token已过期\"}");
            return false;
        }

        // 5. 解析Token获取用户角色，进行管理员接口权限校验（现在方法存在，不会报错）
        String userRole = null;
        try {
            // 调用补充后的 getClaimsFromToken 方法，提取角色信息
            userRole = jwtUtils.getClaimsFromToken(token).get("role", String.class);
        } catch (Exception e) {
            // 提取角色失败（如Token中无角色信息），视为无权限
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token解析失败，无有效角色信息\"}");
            return false;
        }

        // 6. 拦截 /admin/ 开头的接口，仅允许 ADMIN 角色访问
        if (requestURI.startsWith("/admin/") && !"ADMIN".equals(userRole)) {
            response.setStatus(403); // 权限不足
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"仅管理员可访问该接口\"}");
            return false;
        }

        return true; // 所有校验通过，放行
    }
    
    // 判断是否为公共API，无需认证
    private boolean isPublicApi(String requestURI) {
        // 部门列表API允许公共访问，包括代理路径
        return requestURI.startsWith("/department/list") || requestURI.startsWith("/api/department/list");
    }
}