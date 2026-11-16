# 周计划后端：认证与用户域

## 已实现能力

- 用户名密码注册、登录与 BCrypt 密码存储
- JWT Access Token 鉴权
- 两级角色：`ADMIN`（总台）与 `USER`（普通用户）
- 用户信息表、角色表、刷新令牌表的 Flyway 迁移
- 总台管理员专属的用户列表与创建接口
- H2 文件数据库默认开发配置；可通过环境变量切到 MySQL 8

## 数据归属

```text
roles ──< users ──< refresh_tokens
              ↑
        后续 projects / week_plans 仅外键引用 users.id
```

## 本地启动

```bash
cd /Users/chenhaoran/Desktop/product_design/weekly-plan-server
mvn spring-boot:run
```

首次启动会创建总台账号：`admin / Admin12345`。
部署前必须通过环境变量替换账号密码和 JWT 密钥：

```bash
export JWT_SECRET='至少32字节的随机生产密钥'
export BOOTSTRAP_ADMIN_USERNAME='your-admin'
export BOOTSTRAP_ADMIN_PASSWORD='your-strong-password'
```

## MySQL 8 配置

完整基线表结构文件：`sql/weekly_plan_schema_mysql8.sql`。

- 新建数据库时可手工执行该文件。
- 已由应用管理的数据库，应由 Flyway 执行迁移。
- 表结构变更一律新增 `src/main/resources/db/migration/V2__*.sql` 等版本文件，同时同步更新基线 SQL 供查阅；不要修改已执行的迁移文件。

```bash
export DB_URL='jdbc:mysql://localhost:3306/weekly_plan?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='weekly_plan'
export DB_PASSWORD='replace-me'
export DB_DRIVER='com.mysql.cj.jdbc.Driver'
```

## 当前接口

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/api/auth/register` | 公开 |
| POST | `/api/auth/login` | 公开 |
| GET | `/api/users` | ADMIN |
| POST | `/api/users` | ADMIN |

注册和登录请求：

```json
{ "username": "zhangsan", "password": "Password123" }
```

## 生产部署：`tianxiadiyi.xyz`

小程序请求地址已固定为 `https://tianxiadiyi.xyz/api`。请先在域名控制台配置 A 记录：`tianxiadiyi.xyz → 156.238.240.145`；DNS 生效前无法签发证书或将域名添加到微信小程序的合法请求域名。

在服务器安装并启动本服务（监听 `127.0.0.1:8080` 或通过防火墙禁止公网访问 `8080`）后，安装 Nginx 和 Certbot，并执行：

```bash
sudo cp deploy/nginx/tianxiadiyi.xyz.conf /etc/nginx/sites-available/weekly-plan
sudo ln -s /etc/nginx/sites-available/weekly-plan /etc/nginx/sites-enabled/weekly-plan
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d tianxiadiyi.xyz --redirect
```

证书签发完成后验证：

```bash
curl -I https://tianxiadiyi.xyz/api/auth/login
```

最后在微信公众平台的「开发管理 → 开发设置 → 服务器域名」添加 `https://tianxiadiyi.xyz` 到 request 合法域名。不要填 IP、端口或 `/api` 路径。
