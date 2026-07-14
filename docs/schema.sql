-- ================================================================
-- 学生请销假系统 leave_sys — 完整规范化设计（18 张表 · 4 子系统）
-- MySQL 8 / InnoDB / utf8mb4
-- 用户组织(6) · 权限(2) · 请假业务(6) · 系统支撑(4)
-- 设计要点：账号(sys_user)与身份档案(student/teacher)分离消除单表继承空值列；
--   学院→专业→班级层级消除 class_name 传递依赖；角色走多对多支持一人多角色；
--   请假/审批表仍以 sys_user.id 为业务主体键（数据零迁移，接口契约不变，VO 用 JOIN 拼名字）。
-- 密码：admin=admin123，其余=123456（BCrypt）
-- ================================================================
DROP DATABASE IF EXISTS leave_sys;
CREATE DATABASE leave_sys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leave_sys;

-- ===================== 一、用户与组织 =====================
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '账号id(业务主体键)',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
  password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  wx_openid VARCHAR(64) NULL UNIQUE COMMENT '微信openid',
  status TINYINT DEFAULT 1 COMMENT '1正常 0禁用',
  last_login_time DATETIME COMMENT '最后登录',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户账号表';

CREATE TABLE college (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  college_code VARCHAR(20) NOT NULL UNIQUE COMMENT '学院编码',
  college_name VARCHAR(50) NOT NULL COMMENT '学院名称'
) ENGINE=InnoDB COMMENT='学院表';

CREATE TABLE major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  college_id BIGINT NOT NULL COMMENT '所属学院',
  major_code VARCHAR(20) NOT NULL UNIQUE,
  major_name VARCHAR(50) NOT NULL,
  KEY idx_college(college_id),
  CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES college(id)
) ENGINE=InnoDB COMMENT='专业表';

CREATE TABLE teacher (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE COMMENT '对应账号',
  teacher_no VARCHAR(30) NOT NULL UNIQUE COMMENT '工号',
  real_name VARCHAR(50) NOT NULL COMMENT '姓名',
  gender VARCHAR(10),
  title VARCHAR(30) COMMENT '职称',
  college_id BIGINT COMMENT '所属学院(副书记按学院管辖)',
  phone VARCHAR(20),
  KEY idx_college(college_id),
  CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_teacher_college FOREIGN KEY (college_id) REFERENCES college(id)
) ENGINE=InnoDB COMMENT='教职工(辅导员/副书记)信息表';

CREATE TABLE class_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  major_id BIGINT NOT NULL COMMENT '所属专业',
  class_name VARCHAR(50) NOT NULL,
  grade VARCHAR(20) COMMENT '年级',
  head_teacher_id BIGINT COMMENT '辅导员',
  KEY idx_major(major_id),
  CONSTRAINT fk_class_major FOREIGN KEY (major_id) REFERENCES major(id),
  CONSTRAINT fk_class_teacher FOREIGN KEY (head_teacher_id) REFERENCES teacher(id)
) ENGINE=InnoDB COMMENT='班级表';

CREATE TABLE student (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE COMMENT '对应账号',
  student_no VARCHAR(30) NOT NULL UNIQUE COMMENT '学号',
  real_name VARCHAR(50) NOT NULL COMMENT '姓名',
  gender VARCHAR(10),
  class_id BIGINT COMMENT '所属班级',
  counselor_id BIGINT COMMENT '辅导员(审批人)',
  enroll_year INT COMMENT '入学年份',
  phone VARCHAR(20),
  KEY idx_class(class_id), KEY idx_counselor(counselor_id),
  CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES class_info(id),
  CONSTRAINT fk_student_counselor FOREIGN KEY (counselor_id) REFERENCES teacher(id)
) ENGINE=InnoDB COMMENT='学生信息表';

-- ===================== 二、权限(RBAC) =====================
CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(30) NOT NULL UNIQUE COMMENT 'STUDENT/TEACHER/LEADER/ADMIN',
  role_name VARCHAR(50) NOT NULL,
  description VARCHAR(100)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role(user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB COMMENT='用户角色关联表(多对多,支持一人多角色)';

-- ===================== 三、请假业务 =====================
CREATE TABLE leave_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(20) NOT NULL UNIQUE,
  type_name VARCHAR(30) NOT NULL,
  max_days INT COMMENT '单次上限(超过转副书记二级审批)',
  need_proof TINYINT DEFAULT 0,
  sort INT DEFAULT 0, enabled TINYINT DEFAULT 1
) ENGINE=InnoDB COMMENT='请假类型字典表';

CREATE TABLE semester (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_name VARCHAR(50) NOT NULL,
  start_date DATE, end_date DATE, is_current TINYINT DEFAULT 0
) ENGINE=InnoDB COMMENT='学期表';

CREATE TABLE leave_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL COMMENT '申请学生(sys_user.id)',
  type VARCHAR(20) NOT NULL COMMENT '类型编码(对应leave_type.type_code)',
  semester_id BIGINT COMMENT '所属学期',
  start_time DATETIME NOT NULL, end_time DATETIME NOT NULL,
  days DECIMAL(4,1) NOT NULL, reason VARCHAR(200) NOT NULL,
  destination VARCHAR(100), contact_phone VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING/LEADER_PENDING/APPROVED/REJECTED/REVOKED/CANCEL_PENDING/COMPLETED',
  approver_id BIGINT COMMENT '当前/最近审批人(sys_user.id)',
  approve_comment VARCHAR(200), approve_time DATETIME,
  cancel_apply_time DATETIME, cancel_note VARCHAR(200), complete_time DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_student(student_id), KEY idx_status(status), KEY idx_semester(semester_id),
  CONSTRAINT fk_lr_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  CONSTRAINT fk_lr_semester FOREIGN KEY (semester_id) REFERENCES semester(id)
) ENGINE=InnoDB COMMENT='请假单表';

CREATE TABLE approval_flow_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  leave_type_id BIGINT NOT NULL COMMENT '适用请假类型',
  node_order INT NOT NULL COMMENT '审批顺序',
  node_name VARCHAR(50) NOT NULL COMMENT '节点(辅导员审批/副书记审批)',
  approver_role_id BIGINT NOT NULL COMMENT '审批人角色',
  KEY idx_type(leave_type_id),
  CONSTRAINT fk_node_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(id),
  CONSTRAINT fk_node_role FOREIGN KEY (approver_role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB COMMENT='审批流程节点配置表';

CREATE TABLE approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  leave_id BIGINT NOT NULL, operator_id BIGINT NOT NULL COMMENT '操作人(sys_user.id)',
  action VARCHAR(30) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/REVOKE/CANCEL_APPLY/CANCEL_CONFIRM',
  comment VARCHAR(200),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_leave(leave_id),
  CONSTRAINT fk_ar_leave FOREIGN KEY (leave_id) REFERENCES leave_request(id),
  CONSTRAINT fk_ar_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='审批记录表(审计时间线)';

CREATE TABLE leave_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  leave_id BIGINT NOT NULL, file_name VARCHAR(120) NOT NULL,
  file_url VARCHAR(255) NOT NULL, file_size BIGINT, file_type VARCHAR(30),
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_leave(leave_id),
  CONSTRAINT fk_att_leave FOREIGN KEY (leave_id) REFERENCES leave_request(id)
) ENGINE=InnoDB COMMENT='请假附件表';

-- ===================== 四、系统支撑 =====================
CREATE TABLE sys_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL, title VARCHAR(80) NOT NULL, content VARCHAR(255),
  biz_type VARCHAR(30), biz_id BIGINT, is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user(user_id),
  CONSTRAINT fk_notify_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='消息通知表';

CREATE TABLE ai_chat_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL, question VARCHAR(500) NOT NULL, answer TEXT,
  provider VARCHAR(30), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user(user_id),
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='AI对话记录表';

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT, operation VARCHAR(80), method VARCHAR(120), ip VARCHAR(40),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, KEY idx_user(user_id)
) ENGINE=InnoDB COMMENT='操作日志表';

CREATE TABLE sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(60) NOT NULL UNIQUE, config_value VARCHAR(500),
  description VARCHAR(120),
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统配置表';

-- ================================================================
-- 种子数据
-- ================================================================
-- 账号：1admin 2teacher1 3teacher2 4student1 5student2 6student3 7leader1
INSERT INTO sys_user (id, username, password, status, create_time) VALUES
(1,'admin','$2b$10$IR./kCDlE/VsMUw6c.Jv2em3oMJIElU.Dt1M1zm59rk60O/QWcasy',1,'2026-01-10 09:00:00'),
(2,'teacher1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1,'2026-01-10 09:05:00'),
(3,'teacher2','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1,'2026-01-10 09:06:00'),
(4,'student1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1,'2026-01-10 09:10:00'),
(5,'student2','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1,'2026-01-10 09:11:00'),
(6,'student3','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1,'2026-01-10 09:12:00'),
(7,'leader1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1,'2026-01-10 09:13:00');

INSERT INTO college (id, college_code, college_name) VALUES (1,'SE','软件学院');
INSERT INTO major (id, college_id, major_code, major_name) VALUES (1,1,'SE01','软件工程');
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1,'STUDENT','学生','提交请假与销假'),(2,'TEACHER','辅导员','一级审批名下学生'),
(3,'LEADER','副书记','长假二级审批'),(4,'ADMIN','管理员','系统与用户管理');
INSERT INTO sys_user_role (user_id, role_id) VALUES (1,4),(2,2),(3,2),(4,1),(5,1),(6,1),(7,3);

-- 教职工：teacher.id 1李辅导(u2) 2王辅导(u3) 3张副书记(u7)
INSERT INTO teacher (id, user_id, teacher_no, real_name, gender, title, college_id, phone) VALUES
(1,2,'T2023001','李辅导','女','讲师',1,'13900000001'),
(2,3,'T2023002','王辅导','男','讲师',1,'13900000002'),
(3,7,'T2020001','张副书记','男','副教授',1,'13900000003');
INSERT INTO class_info (id, major_id, class_name, grade, head_teacher_id) VALUES
(1,1,'软件2101','2021级',1),(2,1,'软件2102','2021级',2);
-- 学生：student.id 1张三(u4,c1,辅1) 2李四(u5,c1,辅1) 3王五(u6,c2,辅2)
INSERT INTO student (id, user_id, student_no, real_name, gender, class_id, counselor_id, enroll_year, phone) VALUES
(1,4,'20230001','张三','男',1,1,2021,'13800000001'),
(2,5,'20230002','李四','女',1,1,2021,'13800000002'),
(3,6,'20230003','王五','男',2,2,2021,'13800000003');

INSERT INTO leave_type (id, type_code, type_name, max_days, need_proof, sort) VALUES
(1,'SICK','病假',15,1,1),(2,'PERSONAL','事假',7,0,2),(3,'EMERGENCY','急事假',3,0,3),(4,'OTHER','其他',5,0,4);
INSERT INTO semester (id, semester_name, start_date, end_date, is_current) VALUES
(1,'2025-2026学年第二学期','2026-02-24','2026-07-10',1);
INSERT INTO approval_flow_node (leave_type_id, node_order, node_name, approver_role_id) VALUES
(1,1,'辅导员审批',2),(1,2,'副书记审批',3),(2,1,'辅导员审批',2),(3,1,'辅导员审批',2),(4,1,'辅导员审批',2);
INSERT INTO sys_config (config_key, config_value, description) VALUES
('ai.provider','auto','AI供应商 auto/openai/anthropic'),
('ai.openai.model','gpt-4o','OpenAI默认模型'),
('leave.policy','病假超过3天需二级及以上医院诊断证明；事假每学期累计不超过7天；请假期满当天须销假。','请假制度');

-- 请假单（student_id/approver_id 仍用 sys_user.id；补 semester_id=1）
INSERT INTO leave_request (id, student_id, type, semester_id, start_time, end_time, days, reason, destination, contact_phone, status, approver_id, approve_comment, approve_time, cancel_apply_time, cancel_note, complete_time, create_time) VALUES
(1,4,'SICK',1,'2026-02-10 08:00:00','2026-02-12 18:00:00',3.0,'感冒发烧，需回家休养并就医','家中-本市','13800000001','COMPLETED',2,'同意，注意休息按时就医','2026-02-09 15:30:00','2026-02-12 19:00:00','已返校，身体恢复','2026-02-13 09:00:00','2026-02-09 14:00:00'),
(2,5,'PERSONAL',1,'2026-03-02 08:00:00','2026-03-03 18:00:00',2.0,'家中有事需回家处理户口迁移材料','南京','13800000002','COMPLETED',2,'同意，尽快返校','2026-03-01 16:00:00','2026-03-03 20:00:00','已按时返校','2026-03-04 08:30:00','2026-03-01 10:00:00'),
(3,4,'PERSONAL',1,'2026-03-20 08:00:00','2026-03-20 18:00:00',1.0,'想请假外出参加朋友生日聚会','市区','13800000001','REJECTED',2,'理由不充分，临近期中考试不予批准','2026-03-19 17:00:00',NULL,NULL,NULL,'2026-03-19 09:30:00'),
(4,6,'EMERGENCY',1,'2026-04-08 08:00:00','2026-04-09 18:00:00',2.0,'家中突发急事，需立即回家处理','杭州','13800000003','COMPLETED',3,'情况紧急，同意，保持联系','2026-04-07 20:00:00','2026-04-09 21:00:00','事情已处理完毕，已返校','2026-04-10 08:00:00','2026-04-07 19:00:00'),
(5,5,'SICK',1,'2026-04-21 08:00:00','2026-04-25 18:00:00',5.0,'急性肠胃炎需住院治疗，已有医院证明','市第一医院','13800000002','COMPLETED',2,'同意，安心治疗，返校后补交病历复印件','2026-04-20 21:00:00','2026-04-25 19:30:00','已出院并返校','2026-04-26 09:00:00','2026-04-20 20:00:00'),
(6,4,'OTHER',1,'2026-05-11 08:00:00','2026-05-11 18:00:00',1.0,'参加校外编程大赛现场决赛','上海','13800000001','REVOKED',NULL,NULL,NULL,NULL,NULL,NULL,'2026-05-10 11:00:00'),
(7,6,'PERSONAL',1,'2026-05-18 08:00:00','2026-05-19 18:00:00',2.0,'回家参加亲戚婚礼仪式','苏州','13800000003','REJECTED',3,'本周有必修课程考核，不予批准','2026-05-17 18:00:00',NULL,NULL,NULL,'2026-05-17 15:00:00'),
(8,5,'EMERGENCY',1,'2026-06-15 08:00:00','2026-06-16 18:00:00',2.0,'外婆突发疾病住院，需回家探望','无锡','13800000002','CANCEL_PENDING',2,'同意，路上注意安全','2026-06-14 22:00:00','2026-06-17 08:00:00','已于16日晚返校',NULL,'2026-06-14 21:00:00'),
(9,4,'SICK',1,'2026-06-29 08:00:00','2026-07-08 18:00:00',10.0,'腿部骨折手术后需居家休养，附诊断证明','家中-武汉','13800000001','APPROVED',2,'同意，好好养伤，课程资料已安排同学共享','2026-06-28 17:00:00',NULL,NULL,NULL,'2026-06-28 16:00:00'),
(10,6,'PERSONAL',1,'2026-07-09 08:00:00','2026-07-10 18:00:00',2.0,'需回户籍地办理身份证补办手续','市政务服务中心','13800000003','PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-06 10:00:00');

INSERT INTO approval_record (leave_id, operator_id, action, comment, create_time) VALUES
(1,4,'SUBMIT','感冒发烧，需回家休养并就医','2026-02-09 14:00:00'),
(1,2,'APPROVE','同意，注意休息按时就医','2026-02-09 15:30:00'),
(1,4,'CANCEL_APPLY','已返校，身体恢复','2026-02-12 19:00:00'),
(1,2,'CANCEL_CONFIRM','销假确认','2026-02-13 09:00:00'),
(2,5,'SUBMIT','家中有事需回家处理户口迁移材料','2026-03-01 10:00:00'),
(2,2,'APPROVE','同意，尽快返校','2026-03-01 16:00:00'),
(2,5,'CANCEL_APPLY','已按时返校','2026-03-03 20:00:00'),
(2,2,'CANCEL_CONFIRM','销假确认','2026-03-04 08:30:00'),
(3,4,'SUBMIT','想请假外出参加朋友生日聚会','2026-03-19 09:30:00'),
(3,2,'REJECT','理由不充分，临近期中考试不予批准','2026-03-19 17:00:00'),
(4,6,'SUBMIT','家中突发急事，需立即回家处理','2026-04-07 19:00:00'),
(4,3,'APPROVE','情况紧急，同意，保持联系','2026-04-07 20:00:00'),
(4,6,'CANCEL_APPLY','事情已处理完毕，已返校','2026-04-09 21:00:00'),
(4,3,'CANCEL_CONFIRM','销假确认','2026-04-10 08:00:00'),
(5,5,'SUBMIT','急性肠胃炎需住院治疗，已有医院证明','2026-04-20 20:00:00'),
(5,2,'APPROVE','同意，安心治疗，返校后补交病历复印件','2026-04-20 21:00:00'),
(5,5,'CANCEL_APPLY','已出院并返校','2026-04-25 19:30:00'),
(5,2,'CANCEL_CONFIRM','销假确认','2026-04-26 09:00:00'),
(6,4,'SUBMIT','参加校外编程大赛现场决赛','2026-05-10 11:00:00'),
(6,4,'REVOKE','比赛延期，先撤回申请','2026-05-10 14:00:00'),
(7,6,'SUBMIT','回家参加亲戚婚礼仪式','2026-05-17 15:00:00'),
(7,3,'REJECT','本周有必修课程考核，不予批准','2026-05-17 18:00:00'),
(8,5,'SUBMIT','外婆突发疾病住院，需回家探望','2026-06-14 21:00:00'),
(8,2,'APPROVE','同意，路上注意安全','2026-06-14 22:00:00'),
(8,5,'CANCEL_APPLY','已于16日晚返校','2026-06-17 08:00:00'),
(9,4,'SUBMIT','腿部骨折手术后需居家休养，附诊断证明','2026-06-28 16:00:00'),
(9,2,'APPROVE','同意，好好养伤，课程资料已安排同学共享','2026-06-28 17:00:00'),
(10,6,'SUBMIT','需回户籍地办理身份证补办手续','2026-07-06 10:00:00');

INSERT INTO leave_attachment (leave_id, file_name, file_url, file_size, file_type) VALUES
(9,'诊断证明.jpg','/uploads/proof_demo.jpg',204800,'image/jpeg');
INSERT INTO sys_notification (user_id, title, content, biz_type, biz_id) VALUES
(4,'请假审批通过','您的病假申请已通过审批','APPROVAL_RESULT',9);
