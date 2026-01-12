package com.example.sams.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.sams.entity.Department;
import com.example.sams.mapper.DepartmentMapper;
import com.example.sams.service.IDepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements IDepartmentService {
    @Override
    public List<Department> getAllDepartments() {
        return this.list();
    }
    
    @Override
    public Department getDepartmentById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public void addDepartment(Department department) {
        this.save(department);
    }
    
    @Override
    public void updateDepartment(Department department) {
        this.updateById(department);
    }
    
    @Override
    public void deleteDepartment(Long id) {
        this.removeById(id);
    }
}