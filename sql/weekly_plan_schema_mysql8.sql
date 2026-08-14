-- 周计划系统 MySQL 8 基线表结构
-- 用途：新库初始化、表结构查阅。
-- 应用运行时以 src/main/resources/db/migration/ 下的 Flyway 版本迁移为准。
-- 后续调整：请新增 V2__*.sql、V3__*.sql 等迁移，禁止修改已执行的 V1 迁移。

CREATE DATABASE IF NOT EXISTS weekly_plan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE weekly_plan;

-- 角色字典：总台管理员与普通用户。
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(32) NOT NULL COMMENT '角色编码：ADMIN / USER',
  name VARCHAR(64) NOT NULL COMMENT '角色名称',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB COMMENT='角色表';

-- 用户主体：计划、项目分配等后续业务均通过 users.id 关联人员。
CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_code VARCHAR(64) NOT NULL COMMENT '业务用户编号，由系统生成且不可变',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名，唯一',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希，禁止存储明文',
  display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
  email VARCHAR(128) NULL COMMENT '邮箱',
  phone VARCHAR(32) NULL COMMENT '手机号码',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / DISABLED',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '所属角色',
  last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_user_code (user_code),
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_role_status (role_id, status),
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB COMMENT='用户信息表';

-- 刷新令牌：当前 Access Token 已实现；本表为刷新令牌轮换和主动失效预留。
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
  token_hash VARCHAR(128) NOT NULL COMMENT '刷新令牌哈希，禁止存储原始令牌',
  expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
  revoked_at TIMESTAMP NULL DEFAULT NULL COMMENT '撤销时间，非空表示失效',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_tokens_hash (token_hash),
  KEY idx_refresh_tokens_user_expiry (user_id, expires_at),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='刷新令牌表';

-- 初始角色。重复执行时更新名称，保证脚本幂等。
INSERT INTO roles (code, name)
VALUES ('ADMIN', '总台管理员'), ('USER', '普通用户')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 项目：项目编码创建后不可修改；停用项目不可再创建周计划。
CREATE TABLE IF NOT EXISTS projects (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(64) NOT NULL COMMENT '项目编码',
  name VARCHAR(128) NOT NULL COMMENT '项目名称',
  description VARCHAR(2000) NULL COMMENT '项目描述',
  assist_org VARCHAR(128) NULL COMMENT '辅助核算',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / INACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_projects_code (code),
  KEY idx_projects_status (status)
) ENGINE=InnoDB COMMENT='项目表';

-- 周计划：每条记录独立展示，可属于同一项目与同一 ISO 周，并以星期或待定标记计划时间。
CREATE TABLE IF NOT EXISTS week_plans (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  project_id BIGINT UNSIGNED NOT NULL COMMENT '所属项目',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '计划所属用户',
  assigned_by_user_id BIGINT UNSIGNED NULL COMMENT '管理员代填人，空值表示个人创建',
  year_number INT NOT NULL COMMENT 'ISO 周所属年份',
  week_number INT NOT NULL COMMENT 'ISO 周序号，1-53',
  weekday VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '计划星期：MONDAY 至 SUNDAY 或 PENDING',
  content VARCHAR(4000) NOT NULL COMMENT '计划内容',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / ARCHIVED',
  archived_at TIMESTAMP NULL DEFAULT NULL COMMENT '归档时间',
  board_position INT NULL COMMENT '团队大板同日气泡排列位置，空值按创建时间排序',
  PRIMARY KEY (id),
  KEY idx_week_plans_user_project_week (user_id, project_id, year_number, week_number),
  KEY idx_week_plans_year_week (year_number, week_number),
  KEY idx_week_plans_user_year_week (user_id, year_number, week_number),
  KEY idx_week_plans_user_status (user_id, status),
  KEY idx_week_plans_status_year_week (status, year_number, week_number),
  KEY idx_week_plans_board_order (project_id, year_number, week_number, weekday, board_position),
  CONSTRAINT fk_week_plans_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_week_plans_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_week_plans_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='周计划表';
