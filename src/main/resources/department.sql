-- 创建部门表
CREATE TABLE IF NOT EXISTS `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `department_name` varchar(100) NOT NULL COMMENT '部门名称',
  `description` varchar(255) DEFAULT NULL COMMENT '部门描述',
  `type` varchar(50) DEFAULT 'DEPARTMENT' COMMENT '部门类型：DEPARTMENT(部门)或RESEARCH_GROUP(教研室)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 插入一些默认部门数据
INSERT INTO `sys_department` (`department_name`, `description`, `type`) VALUES
('计算机科学与技术学院', '计算机相关专业教学与研究', 'DEPARTMENT'),
('电子信息工程学院', '电子信息相关专业教学与研究', 'DEPARTMENT'),
('机械工程学院', '机械工程相关专业教学与研究', 'DEPARTMENT'),
('数学与统计学院', '数学与统计学教学与研究', 'DEPARTMENT'),
('外语学院', '外语教学与研究', 'DEPARTMENT'),
('计算机教研室', '计算机基础课程教学', 'RESEARCH_GROUP'),
('软件工程教研室', '软件工程专业教学', 'RESEARCH_GROUP'),
('数据科学教研室', '数据科学与大数据技术教学', 'RESEARCH_GROUP');