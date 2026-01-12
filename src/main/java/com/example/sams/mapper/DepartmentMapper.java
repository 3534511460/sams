package com.example.sams.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sams.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}