package com.example.sams.dto;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role; // ✅ 新增：角色字段
}