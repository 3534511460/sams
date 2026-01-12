package com.example.sams.service;

import com.example.sams.entity.Department;
import java.util.List;

public interface IDepartmentService {
    List<Department> getAllDepartments();
    Department getDepartmentById(Long id);
    void addDepartment(Department department);
    void updateDepartment(Department department);
    void deleteDepartment(Long id);
}