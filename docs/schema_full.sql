-- ================================================================
-- 学生请销假系统 · 完整数据库设计 (leave_sys_full)
-- 22 张表，按 4 个子系统组织；MySQL 8 / InnoDB / utf8mb4
-- 组织架构域(6) · 账号权限域(4) · 请假业务域(6) · 系统支撑域(6)
-- 说明：本脚本为系统的“完整物理模型”。当前迭代(MVP)已在 leave_sys
--      库落地核心闭环(sys_user/leave_request/approval_record)，本完整
--      设计将宽表按第三范式拆分并补齐周边子系统，为后续迭代做准备。
-- ================================================================
DROP DATABASE IF EXISTS leave_sys_full;
CREATE DATABASE leave_sys_full DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leave_sys_full;

-- ========================================================
-- 一、组织架构域（学院 → 专业 → 班级 → 学生 / 教师）
-- ========================================================

-- 1. 学院表
CREATE TABLE college (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学院id',
  college_code VARCHAR(20)  NOT NULL UNIQUE       COMMENT '学院编码',
  college_name VARCHAR(50)  NOT NULL              COMMENT '学院名称',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='学院表';

-- 2. 专业表
CREATE TABLE major (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '专业id',
  college_id  BIGINT      NOT NULL              COMMENT '所属学院id',
  major_code  VARCHAR(20) NOT NULL UNIQUE       COMMENT '专业编码',
  major_name  VARCHAR(50) NOT NULL              COMMENT '专业名称',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_college (college_id),
  CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES college(id)
) ENGINE=InnoDB COMMENT='专业表';

-- 6. 用户账号表（提前建，供 teacher/student 外键引用）
CREATE TABLE sys_user (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户id',
  username        VARCHAR(50)  NOT NULL UNIQUE       COMMENT '登录用户名',
  password        VARCHAR(100) NOT NULL              COMMENT '密码(BCrypt)',
  wx_openid       VARCHAR(64)  NULL UNIQUE           COMMENT '微信openid(小程序绑定)',
  status          TINYINT DEFAULT 1                  COMMENT '1正常 0禁用',
  last_login_time DATETIME                           COMMENT '最后登录时间',
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='用户账号表';

-- 3. 教师(辅导员)表
CREATE TABLE teacher (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教师id',
  user_id     BIGINT      NOT NULL UNIQUE       COMMENT '对应账号id',
  teacher_no  VARCHAR(30) NOT NULL UNIQUE       COMMENT '工号',
  name        VARCHAR(50) NOT NULL              COMMENT '姓名',
  gender      VARCHAR(10)                       COMMENT '性别',
  title       VARCHAR(30)                       COMMENT '职称',
  college_id  BIGINT                            COMMENT '所属学院id',
  phone       VARCHAR(20)                       COMMENT '联系电话',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_college (college_id),
  CONSTRAINT fk_teacher_user    FOREIGN KEY (user_id)    REFERENCES sys_user(id),
  CONSTRAINT fk_teacher_college FOREIGN KEY (college_id) REFERENCES college(id)
) ENGINE=InnoDB COMMENT='教师(辅导员)信息表';

-- 4. 班级表
CREATE TABLE class_info (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级id',
  major_id       BIGINT      NOT NULL             COMMENT '所属专业id',
  class_name     VARCHAR(50) NOT NULL             COMMENT '班级名称',
  grade          VARCHAR(20)                      COMMENT '年级',
  head_teacher_id BIGINT                          COMMENT '班主任/辅导员id',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_major (major_id),
  CONSTRAINT fk_class_major   FOREIGN KEY (major_id)        REFERENCES major(id),
  CONSTRAINT fk_class_teacher FOREIGN KEY (head_teacher_id) REFERENCES teacher(id)
) ENGINE=InnoDB COMMENT='班级表';

-- 5. 学生表
CREATE TABLE student (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生id',
  user_id      BIGINT      NOT NULL UNIQUE       COMMENT '对应账号id',
  student_no   VARCHAR(30) NOT NULL UNIQUE       COMMENT '学号',
  name         VARCHAR(50) NOT NULL              COMMENT '姓名',
  gender       VARCHAR(10)                       COMMENT '性别',
  class_id     BIGINT                            COMMENT '所属班级id',
  counselor_id BIGINT                            COMMENT '辅导员id(审批人)',
  enroll_year  INT                               COMMENT '入学年份',
  phone        VARCHAR(20)                       COMMENT '联系电话',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_class (class_id), KEY idx_counselor (counselor_id),
  CONSTRAINT fk_student_user      FOREIGN KEY (user_id)      REFERENCES sys_user(id),
  CONSTRAINT fk_student_class     FOREIGN KEY (class_id)     REFERENCES class_info(id),
  CONSTRAINT fk_student_counselor FOREIGN KEY (counselor_id) REFERENCES teacher(id)
) ENGINE=InnoDB COMMENT='学生信息表';

-- ========================================================
-- 二、账号与权限域（RBAC：用户-角色-权限 多对多）
-- ========================================================

-- 7. 角色表
CREATE TABLE sys_role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色id',
  role_code   VARCHAR(30) NOT NULL UNIQUE       COMMENT '角色编码 STUDENT/TEACHER/ADMIN',
  role_name   VARCHAR(50) NOT NULL              COMMENT '角色名称',
  description VARCHAR(100)                      COMMENT '描述',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

-- 8. 权限(菜单)表
CREATE TABLE sys_permission (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限id',
  parent_id BIGINT DEFAULT 0                   COMMENT '父权限id(0为顶级)',
  perm_code VARCHAR(60) NOT NULL UNIQUE        COMMENT '权限编码',
  perm_name VARCHAR(50) NOT NULL               COMMENT '权限/菜单名称',
  perm_type TINYINT     NOT NULL               COMMENT '1菜单 2按钮',
  path      VARCHAR(120)                       COMMENT '前端路由/接口路径',
  sort      INT DEFAULT 0                      COMMENT '排序',
  KEY idx_parent (parent_id)
) ENGINE=InnoDB COMMENT='权限(菜单)表';

-- 9. 用户-角色关联表（多对多）
CREATE TABLE sys_user_role (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户id',
  role_id BIGINT NOT NULL COMMENT '角色id',
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- 10. 角色-权限关联表（多对多）
CREATE TABLE sys_role_permission (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id       BIGINT NOT NULL COMMENT '角色id',
  permission_id BIGINT NOT NULL COMMENT '权限id',
  UNIQUE KEY uk_role_perm (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id)       REFERENCES sys_role(id),
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- ========================================================
-- 三、请假业务域（类型/学期字典 → 请假单 → 附件/审批/流程）
-- ========================================================

-- 11. 请假类型字典表
CREATE TABLE leave_type (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '类型id',
  type_code  VARCHAR(20) NOT NULL UNIQUE       COMMENT '类型编码 SICK/PERSONAL/EMERGENCY/OTHER',
  type_name  VARCHAR(30) NOT NULL              COMMENT '类型名称',
  max_days   INT                               COMMENT '单次最大天数(超过需上级审批)',
  need_proof TINYINT DEFAULT 0                 COMMENT '是否需要证明材料 1是0否',
  sort       INT DEFAULT 0,
  enabled    TINYINT DEFAULT 1                 COMMENT '是否启用'
) ENGINE=InnoDB COMMENT='请假类型字典表';

-- 12. 学期表
CREATE TABLE semester (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学期id',
  semester_name VARCHAR(50) NOT NULL             COMMENT '学期名称',
  start_date    DATE                             COMMENT '开始日期',
  end_date      DATE                             COMMENT '结束日期',
  is_current    TINYINT DEFAULT 0                COMMENT '是否当前学期'
) ENGINE=InnoDB COMMENT='学期表';

-- 13. 请假单表（核心业务表）
CREATE TABLE leave_request (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '请假单id',
  student_id          BIGINT       NOT NULL             COMMENT '申请学生id',
  leave_type_id       BIGINT       NOT NULL             COMMENT '请假类型id',
  semester_id         BIGINT                            COMMENT '所属学期id',
  start_time          DATETIME     NOT NULL             COMMENT '开始时间',
  end_time            DATETIME     NOT NULL             COMMENT '结束时间',
  days                DECIMAL(4,1) NOT NULL             COMMENT '请假天数',
  reason              VARCHAR(200) NOT NULL             COMMENT '请假事由',
  destination         VARCHAR(100)                      COMMENT '去向',
  contact_phone       VARCHAR(20)                       COMMENT '请假期间联系电话',
  status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                    COMMENT 'PENDING/APPROVED/REJECTED/REVOKED/CANCEL_PENDING/COMPLETED',
  current_approver_id BIGINT                            COMMENT '当前审批人(教师id)',
  cancel_apply_time   DATETIME                          COMMENT '销假申请时间',
  cancel_note         VARCHAR(200)                      COMMENT '销假说明',
  complete_time       DATETIME                          COMMENT '流程完结时间',
  create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_student (student_id), KEY idx_status (status), KEY idx_type (leave_type_id),
  CONSTRAINT fk_lr_student  FOREIGN KEY (student_id)          REFERENCES student(id),
  CONSTRAINT fk_lr_type     FOREIGN KEY (leave_type_id)       REFERENCES leave_type(id),
  CONSTRAINT fk_lr_semester FOREIGN KEY (semester_id)         REFERENCES semester(id),
  CONSTRAINT fk_lr_approver FOREIGN KEY (current_approver_id) REFERENCES teacher(id)
) ENGINE=InnoDB COMMENT='请假单表';

-- 14. 请假附件表（病假诊断证明等，一对多）
CREATE TABLE leave_attachment (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件id',
  leave_id    BIGINT       NOT NULL             COMMENT '请假单id',
  file_name   VARCHAR(120) NOT NULL             COMMENT '文件名',
  file_url    VARCHAR(255) NOT NULL             COMMENT '文件存储地址',
  file_size   BIGINT                            COMMENT '文件大小(字节)',
  file_type   VARCHAR(30)                       COMMENT '文件类型',
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_leave (leave_id),
  CONSTRAINT fk_att_leave FOREIGN KEY (leave_id) REFERENCES leave_request(id)
) ENGINE=InnoDB COMMENT='请假附件表';

-- 15. 审批记录表（审计流水，一对多）
CREATE TABLE approval_record (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录id',
  leave_id    BIGINT      NOT NULL              COMMENT '请假单id',
  operator_id BIGINT      NOT NULL              COMMENT '操作人(账号id)',
  action      VARCHAR(30) NOT NULL              COMMENT 'SUBMIT/APPROVE/REJECT/REVOKE/CANCEL_APPLY/CANCEL_CONFIRM',
  comment     VARCHAR(200)                      COMMENT '操作意见',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_leave (leave_id),
  CONSTRAINT fk_ar_leave    FOREIGN KEY (leave_id)    REFERENCES leave_request(id),
  CONSTRAINT fk_ar_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='审批记录表';

-- 16. 审批流程节点表（按请假类型配置多级审批）
CREATE TABLE approval_flow_node (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点id',
  leave_type_id   BIGINT      NOT NULL             COMMENT '适用的请假类型id',
  node_order      INT         NOT NULL             COMMENT '审批顺序',
  node_name       VARCHAR(50) NOT NULL             COMMENT '节点名称(如:辅导员审批/学院审批)',
  approver_role_id BIGINT     NOT NULL             COMMENT '审批人角色id',
  KEY idx_type (leave_type_id),
  CONSTRAINT fk_node_type FOREIGN KEY (leave_type_id)    REFERENCES leave_type(id),
  CONSTRAINT fk_node_role FOREIGN KEY (approver_role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB COMMENT='审批流程节点配置表';

-- ========================================================
-- 四、系统支撑域（通知/字典/配置/AI/日志）
-- ========================================================

-- 17. 消息通知表
CREATE TABLE sys_notification (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知id',
  user_id     BIGINT       NOT NULL             COMMENT '接收用户id',
  title       VARCHAR(80)  NOT NULL             COMMENT '标题',
  content     VARCHAR(255)                      COMMENT '内容',
  biz_type    VARCHAR(30)                       COMMENT '业务类型(如:审批结果)',
  biz_id      BIGINT                            COMMENT '关联业务id',
  is_read     TINYINT DEFAULT 0                 COMMENT '是否已读',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  CONSTRAINT fk_notify_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='消息通知表';

-- 18. 数据字典表
CREATE TABLE sys_dict (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典id',
  dict_code VARCHAR(40) NOT NULL UNIQUE        COMMENT '字典编码(如:gender/leave_status)',
  dict_name VARCHAR(50) NOT NULL               COMMENT '字典名称'
) ENGINE=InnoDB COMMENT='数据字典表';

-- 19. 字典项表
CREATE TABLE sys_dict_item (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典项id',
  dict_id    BIGINT      NOT NULL              COMMENT '所属字典id',
  item_label VARCHAR(50) NOT NULL              COMMENT '显示标签',
  item_value VARCHAR(50) NOT NULL              COMMENT '存储值',
  sort       INT DEFAULT 0,
  KEY idx_dict (dict_id),
  CONSTRAINT fk_item_dict FOREIGN KEY (dict_id) REFERENCES sys_dict(id)
) ENGINE=InnoDB COMMENT='字典项表';

-- 20. 系统配置表
CREATE TABLE sys_config (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置id',
  config_key   VARCHAR(60)  NOT NULL UNIQUE      COMMENT '配置键(如:ai.provider)',
  config_value VARCHAR(500)                      COMMENT '配置值',
  description  VARCHAR(120)                      COMMENT '说明',
  update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统配置表';

-- 21. AI 对话记录表
CREATE TABLE ai_chat_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录id',
  user_id     BIGINT       NOT NULL             COMMENT '提问用户id',
  question    VARCHAR(500) NOT NULL             COMMENT '用户问题',
  answer      TEXT                              COMMENT 'AI回复',
  provider    VARCHAR(30)                       COMMENT '模型供应商(openai/anthropic)',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='AI对话记录表';

-- 22. 操作日志表
CREATE TABLE sys_operation_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志id',
  user_id     BIGINT                            COMMENT '操作用户id',
  operation   VARCHAR(80)                       COMMENT '操作描述',
  method      VARCHAR(120)                      COMMENT '请求方法/接口',
  ip          VARCHAR(40)                       COMMENT '来源IP',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ================================================================
-- 基础字典 / 种子数据
-- ================================================================
INSERT INTO college (college_code, college_name) VALUES ('SE','软件学院'),('CS','计算机学院');
INSERT INTO major (college_id, major_code, major_name) VALUES (1,'SE01','软件工程'),(1,'SE02','数字媒体技术'),(2,'CS01','计算机科学与技术');

INSERT INTO sys_user (username, password, status) VALUES
('admin','$2b$10$IR./kCDlE/VsMUw6c.Jv2em3oMJIElU.Dt1M1zm59rk60O/QWcasy',1),
('teacher1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1),
('student1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm',1);

INSERT INTO teacher (user_id, teacher_no, name, gender, title, college_id, phone) VALUES
(2,'T2023001','李辅导','女','讲师',1,'13900000001');
INSERT INTO class_info (major_id, class_name, grade, head_teacher_id) VALUES (1,'软件2101','2021级',1);
INSERT INTO student (user_id, student_no, name, gender, class_id, counselor_id, enroll_year, phone) VALUES
(3,'20230001','张三','男',1,1,2021,'13800000001');

INSERT INTO sys_role (role_code, role_name, description) VALUES
('STUDENT','学生','提交请假与销假申请'),
('TEACHER','辅导员','审批名下学生请假'),
('ADMIN','管理员','系统与用户管理');
INSERT INTO sys_user_role (user_id, role_id) VALUES (1,3),(2,2),(3,1);
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, sort) VALUES
(0,'leave','请假管理',1,'/leave',1),
(1,'leave:apply','提交请假',2,'/leave/apply',1),
(0,'approval','审批管理',1,'/approval',2),
(2,'approval:audit','审批操作',2,'/approval/audit',1),
(0,'admin','系统管理',1,'/admin',3);
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (1,1),(1,2),(2,3),(2,4),(3,5);

INSERT INTO leave_type (type_code, type_name, max_days, need_proof, sort) VALUES
('SICK','病假',15,1,1),('PERSONAL','事假',7,0,2),('EMERGENCY','急事假',3,0,3),('OTHER','其他',5,0,4);
INSERT INTO semester (semester_name, start_date, end_date, is_current) VALUES
('2025-2026学年第二学期','2026-02-24','2026-07-10',1);
INSERT INTO approval_flow_node (leave_type_id, node_order, node_name, approver_role_id) VALUES
(1,1,'辅导员审批',2),(1,2,'学院审批',2),
(2,1,'辅导员审批',2),(3,1,'辅导员审批',2),(4,1,'辅导员审批',2);

INSERT INTO sys_dict (dict_code, dict_name) VALUES ('gender','性别'),('leave_status','请假状态');
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort) VALUES
(1,'男','M',1),(1,'女','F',2),
(2,'待审批','PENDING',1),(2,'请假中','APPROVED',2),(2,'已驳回','REJECTED',3),
(2,'已撤回','REVOKED',4),(2,'销假待确认','CANCEL_PENDING',5),(2,'已完成','COMPLETED',6);
INSERT INTO sys_config (config_key, config_value, description) VALUES
('ai.provider','auto','AI供应商:auto/openai/anthropic'),
('ai.openai.model','gpt-4o','OpenAI默认模型'),
('leave.policy','病假超3天需诊断证明；事假每学期累计不超过7天','请假制度文本');

INSERT INTO leave_request (student_id, leave_type_id, semester_id, start_time, end_time, days, reason, destination, contact_phone, status, current_approver_id) VALUES
(1,1,1,'2026-06-29 08:00:00','2026-07-08 18:00:00',10.0,'腿部骨折手术后需居家休养，附诊断证明','家中-武汉','13800000001','APPROVED',1);
INSERT INTO approval_record (leave_id, operator_id, action, comment) VALUES
(1,3,'SUBMIT','腿部骨折手术后需居家休养，附诊断证明'),
(1,2,'APPROVE','同意，好好养伤');
INSERT INTO leave_attachment (leave_id, file_name, file_url, file_size, file_type) VALUES
(1,'诊断证明.jpg','/upload/2026/proof_001.jpg',204800,'image/jpeg');
INSERT INTO sys_notification (user_id, title, content, biz_type, biz_id) VALUES
(3,'请假审批通过','您的病假申请已通过审批','APPROVAL_RESULT',1);
INSERT INTO ai_chat_log (user_id, question, answer, provider) VALUES
(3,'病假超过3天需要什么材料？','病假超过3天需提供二级及以上医院的诊断证明。','openai');
