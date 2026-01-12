package com.example.sams;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.sams.mapper") // <--- 关键！扫描 Mapper 包
public class SamsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SamsApplication.class, args);
    }
}