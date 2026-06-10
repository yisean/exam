# 前后端解耦部署（Nginx 托管前端 + 反代后端）

适用于把 **exam-vue（前端静态）** 与 **exam-api（后端 jar）** 部署在各自进程/容器，由 Nginx 统一对外：Nginx 托管前端 `dist`，并把后端相关前缀反向代理到后端服务。前端用相对路径调用（`VUE_APP_BASE_API=''`），全程同源，无需 CORS。

```
浏览器 ──→ Nginx :80
            ├─ /            → 前端 dist（静态文件，history 回退 index.html）
            ├─ /exam        → 后端 :8101（业务接口）
            ├─ /common      → 后端 :8101（文件上传）
            └─ /upload      → 后端 :8101（文件访问）
```

## 1. 构建前端

```bash
cd exam-vue
npm install
npm run build:prod      # 产物输出到 exam-vue/dist
```

将 `dist/` 拷贝到服务器，例如 `/var/www/exam`。

## 2. 启动后端（独立进程）

```bash
cd exam-api
mvn clean package -DskipTests          # 产出 target/*.jar
java -jar target/exam-api.jar          # 默认 8101；外置配置见 docs/ops/run-package/
```

> 后端的 `conf.upload.url` 建议用相对值 `/upload/file/`，使返回的文件地址同源，由 Nginx 反代到后端。

## 3. Nginx 配置示例

```nginx
server {
    listen       80;
    server_name  exam.example.com;

    # 前端静态资源（Vue history 模式回退）
    root   /var/www/exam;
    index  index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端接口与文件，反代到后端服务
    location ^~ /exam/ {
        proxy_pass http://127.0.0.1:8101;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ^~ /common/ {
        proxy_pass http://127.0.0.1:8101;
        proxy_set_header Host $host;
        # 上传大文件时放开限制
        client_max_body_size 50m;
    }

    location ^~ /upload/ {
        proxy_pass http://127.0.0.1:8101;
        proxy_set_header Host $host;
    }
}
```

后端在其它主机/容器时，把 `127.0.0.1:8101` 换成对应地址（如 `http://exam-api:8101`）。

## 4. 对照：开发态

开发态等价配置见 `exam-vue/vue.config.js` 的 `devServer.proxy`（同样代理 `/exam`、`/common`、`/upload` 到 `http://localhost:8101`）。即：**开发用 webpack devServer 代理，生产用 Nginx 反代，前端代码与 `VUE_APP_BASE_API=''` 两端一致**。

## 5. 校验清单

- [ ] 打开站点首页能加载（静态资源 200）。
- [ ] 登录接口 `/exam/api/sys/user/login` 走通（Network 面板看到 200、同源）。
- [ ] 刷新二级路由（如 `/booking/index`）不 404（history 回退生效）。
- [ ] 上传图片成功且能回显（`/common` 上传、`/upload/file/...` 访问均经 Nginx）。
