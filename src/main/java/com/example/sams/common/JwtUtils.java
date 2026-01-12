package com.example.sams.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {
    // 密钥（保持你的原始密钥不变）
    private static final String SECRET_KEY = "sams-super-secret-key-2025";
    // 过期时间 24小时（保持你的原始过期时间不变）
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000L;

    /**
     * 保留你原有：仅传入用户名生成 Token 的方法（兼容旧代码）
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 新增：传入用户名 + 角色，生成包含角色信息的 Token（用于权限校验）
     * 兼容原有逻辑，不影响旧接口使用
     */
    public String generateToken(String username, String role) {
        // 构建额外参数，存入用户角色
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims) // 存入角色信息
                .setSubject(username) // 保留用户名作为主题
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 保留你原有：解析 Token 获取用户名的方法
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null; // 解析失败或过期
        }
    }

    /**
     * 新增：缺失的 getClaimsFromToken 方法（解决拦截器报错）
     * 用于解析 Token 获取所有声明（包含角色信息）
     */
    public Claims getClaimsFromToken(String token) {
        try {
            // 直接复用你的解析逻辑，仅返回 Claims 对象
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // 解析失败（Token过期、篡改等）返回 null，避免报错
            return null;
        }
    }
}