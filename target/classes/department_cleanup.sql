-- 清理重复的部门数据，只保留ID最小的记录
DELETE t1 FROM sys_department t1
INNER JOIN sys_department t2 
WHERE t1.id > t2.id AND t1.department_name = t2.department_name;

-- 验证清理结果
SELECT * FROM sys_department ORDER BY id;