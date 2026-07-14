-- ============================================================
-- 学生请销假系统 leave_sys — 建库建表 + 种子数据
-- 密码：admin=admin123，其余=123456（BCrypt）
-- ============================================================
CREATE DATABASE IF NOT EXISTS leave_sys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leave_sys;

DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS leave_request;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,          -- BCrypt
  real_name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,               -- STUDENT/TEACHER/ADMIN
  student_no VARCHAR(30), class_name VARCHAR(50), phone VARCHAR(20),
  teacher_id BIGINT,                       -- 学生的辅导员(自关联 sys_user.id)
  wx_openid VARCHAR(64) NULL UNIQUE,       -- 微信 openid(小程序一键登录绑定)
  status TINYINT DEFAULT 1,                -- 1正常 0禁用
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE leave_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL, start_time DATETIME NOT NULL, end_time DATETIME NOT NULL,
  days DECIMAL(4,1) NOT NULL, reason VARCHAR(200) NOT NULL,
  destination VARCHAR(100), contact_phone VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  approver_id BIGINT, approve_comment VARCHAR(200), approve_time DATETIME,
  cancel_apply_time DATETIME, cancel_note VARCHAR(200), complete_time DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_student(student_id), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  leave_id BIGINT NOT NULL, operator_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL, comment VARCHAR(200),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, KEY idx_leave(leave_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 种子用户 ----------------
-- admin/admin123，其余/123456
INSERT INTO sys_user (id, username, password, real_name, role, student_no, class_name, phone, teacher_id, status, create_time) VALUES
(1,'admin','$2b$10$IR./kCDlE/VsMUw6c.Jv2em3oMJIElU.Dt1M1zm59rk60O/QWcasy','系统管理员','ADMIN',NULL,NULL,NULL,NULL,1,'2026-01-10 09:00:00'),
(2,'teacher1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm','李辅导','TEACHER',NULL,NULL,'13900000001',NULL,1,'2026-01-10 09:05:00'),
(3,'teacher2','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm','王辅导','TEACHER',NULL,NULL,'13900000002',NULL,1,'2026-01-10 09:06:00'),
(4,'student1','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm','张三','STUDENT','20230001','软件2101','13800000001',2,1,'2026-01-10 09:10:00'),
(5,'student2','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm','李四','STUDENT','20230002','软件2101','13800000002',2,1,'2026-01-10 09:11:00'),
(6,'student3','$2b$10$7NTN.bFSBQ1uyp73TTfWleMTctwiFBYpekK4SRjEt.Vj.nv1oJcvm','王五','STUDENT','20230003','软件2102','13800000003',3,1,'2026-01-10 09:12:00');

-- ---------------- 种子请假单（近6个月，覆盖全部状态） ----------------
INSERT INTO leave_request (id, student_id, type, start_time, end_time, days, reason, destination, contact_phone, status, approver_id, approve_comment, approve_time, cancel_apply_time, cancel_note, complete_time, create_time) VALUES
(1,4,'SICK','2026-02-10 08:00:00','2026-02-12 18:00:00',3.0,'感冒发烧，需回家休养并就医','家中-本市','13800000001','COMPLETED',2,'同意，注意休息按时就医','2026-02-09 15:30:00','2026-02-12 19:00:00','已返校，身体恢复','2026-02-13 09:00:00','2026-02-09 14:00:00'),
(2,5,'PERSONAL','2026-03-02 08:00:00','2026-03-03 18:00:00',2.0,'家中有事需回家处理户口迁移材料','南京','13800000002','COMPLETED',2,'同意，尽快返校','2026-03-01 16:00:00','2026-03-03 20:00:00','已按时返校','2026-03-04 08:30:00','2026-03-01 10:00:00'),
(3,4,'PERSONAL','2026-03-20 08:00:00','2026-03-20 18:00:00',1.0,'想请假外出参加朋友生日聚会','市区','13800000001','REJECTED',2,'理由不充分，临近期中考试不予批准','2026-03-19 17:00:00',NULL,NULL,NULL,'2026-03-19 09:30:00'),
(4,6,'EMERGENCY','2026-04-08 08:00:00','2026-04-09 18:00:00',2.0,'家中突发急事，需立即回家处理','杭州','13800000003','COMPLETED',3,'情况紧急，同意，保持联系','2026-04-07 20:00:00','2026-04-09 21:00:00','事情已处理完毕，已返校','2026-04-10 08:00:00','2026-04-07 19:00:00'),
(5,5,'SICK','2026-04-21 08:00:00','2026-04-25 18:00:00',5.0,'急性肠胃炎需住院治疗，已有医院证明','市第一医院','13800000002','COMPLETED',2,'同意，安心治疗，返校后补交病历复印件','2026-04-20 21:00:00','2026-04-25 19:30:00','已出院并返校','2026-04-26 09:00:00','2026-04-20 20:00:00'),
(6,4,'OTHER','2026-05-11 08:00:00','2026-05-11 18:00:00',1.0,'参加校外编程大赛现场决赛','上海','13800000001','REVOKED',NULL,NULL,NULL,NULL,NULL,NULL,'2026-05-10 11:00:00'),
(7,6,'PERSONAL','2026-05-18 08:00:00','2026-05-19 18:00:00',2.0,'回家参加亲戚婚礼仪式','苏州','13800000003','REJECTED',3,'本周有必修课程考核，不予批准','2026-05-17 18:00:00',NULL,NULL,NULL,'2026-05-17 15:00:00'),
(8,5,'EMERGENCY','2026-06-15 08:00:00','2026-06-16 18:00:00',2.0,'外婆突发疾病住院，需回家探望','无锡','13800000002','CANCEL_PENDING',2,'同意，路上注意安全','2026-06-14 22:00:00','2026-06-17 08:00:00','已于16日晚返校',NULL,'2026-06-14 21:00:00'),
(9,4,'SICK','2026-06-29 08:00:00','2026-07-08 18:00:00',10.0,'腿部骨折手术后需居家休养，附诊断证明','家中-武汉','13800000001','APPROVED',2,'同意，好好养伤，课程资料已安排同学共享','2026-06-28 17:00:00',NULL,NULL,NULL,'2026-06-28 16:00:00'),
(10,6,'PERSONAL','2026-07-09 08:00:00','2026-07-10 18:00:00',2.0,'需回户籍地办理身份证补办手续','市政务服务中心','13800000003','PENDING',NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-06 10:00:00');

-- ---------------- 种子审批记录（时间线） ----------------
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

-- ================================================================
-- 增量子系统表（迭代二：类型字典/配置/AI记录/通知/操作日志/附件）
-- ================================================================
-- 请假类型字典表
CREATE TABLE IF NOT EXISTS leave_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code  VARCHAR(20) NOT NULL UNIQUE COMMENT '类型编码',
  type_name  VARCHAR(30) NOT NULL COMMENT '类型名称',
  max_days   INT COMMENT '单次最大天数',
  need_proof TINYINT DEFAULT 0 COMMENT '是否需证明',
  sort INT DEFAULT 0, enabled TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假类型字典表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key   VARCHAR(60) NOT NULL UNIQUE COMMENT '配置键',
  config_value VARCHAR(500) COMMENT '配置值',
  description  VARCHAR(120) COMMENT '说明',
  update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- AI 对话记录表
CREATE TABLE IF NOT EXISTS ai_chat_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id  BIGINT NOT NULL COMMENT '提问用户id',
  question VARCHAR(500) NOT NULL COMMENT '问题',
  answer   TEXT COMMENT 'AI回复',
  provider VARCHAR(30) COMMENT '模型供应商',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

-- 消息通知表
CREATE TABLE IF NOT EXISTS sys_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id  BIGINT NOT NULL COMMENT '接收用户id',
  title    VARCHAR(80) NOT NULL COMMENT '标题',
  content  VARCHAR(255) COMMENT '内容',
  biz_type VARCHAR(30) COMMENT '业务类型',
  biz_id   BIGINT COMMENT '关联业务id',
  is_read  TINYINT DEFAULT 0 COMMENT '是否已读',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  CONSTRAINT fk_notify_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id   BIGINT COMMENT '操作用户id',
  operation VARCHAR(80) COMMENT '操作描述',
  method    VARCHAR(120) COMMENT '请求方法/路径',
  ip        VARCHAR(40) COMMENT '来源IP',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 请假附件表
CREATE TABLE IF NOT EXISTS leave_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  leave_id  BIGINT NOT NULL COMMENT '请假单id',
  file_name VARCHAR(120) NOT NULL COMMENT '文件名',
  file_url  VARCHAR(255) NOT NULL COMMENT '存储地址',
  file_size BIGINT COMMENT '大小(字节)',
  file_type VARCHAR(30) COMMENT '类型',
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_leave (leave_id),
  CONSTRAINT fk_att_leave FOREIGN KEY (leave_id) REFERENCES leave_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假附件表';

-- 字典/配置种子
INSERT INTO leave_type (type_code, type_name, max_days, need_proof, sort) VALUES
('SICK','病假',15,1,1),('PERSONAL','事假',7,0,2),('EMERGENCY','急事假',3,0,3),('OTHER','其他',5,0,4);
INSERT INTO sys_config (config_key, config_value, description) VALUES
('ai.provider','auto','AI供应商 auto/openai/anthropic'),
('ai.openai.model','gpt-4o','OpenAI默认模型'),
('leave.policy','病假超过3天需二级及以上医院诊断证明；事假每学期累计不超过7天；请假期满当天须销假。','请假制度');
