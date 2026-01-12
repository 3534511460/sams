package com.example.sams.dto;

import lombok.Data;

@Data
public class CheckInRequest {
    private Long studentId; // 谁在打卡
    private String code;    // 输入的 6 位口令
    // 扩展项：如果是二维码扫码，可以加 sessionId
    // private Long sessionId;

    // 新增：学生打卡时的经纬度
    private Double lat;  // 纬度
    private Double lon;  // 经度
}