# Groups 前端交接说明

## 这次做了什么

按照 Fiona 的 FE-01 至 FE-09 清单，在现有 Groups 页面上接通 Eddie 的接口。保留 Home、Find Buddies、Groups、Sessions、Profile 的导航和其他页面，没有恢复之前撤回的邀请制或导航合并方案。

- 创建小组：提交五个字段，成功清空表单并刷新列表；失败保留输入。
- 小组列表：显示名称、课程、描述、聚会信息、创建者、创建时间、成员数。
- 加入与退出：根据 `joined` 显示按钮，完成后刷新列表和已打开的成员列表。
- 创建者自动加入，显示 Creator，不能退出自己的小组。
- Open group：在同页下方展开资料、成员和留言板。
- 登录用户可以阅读；只有成员可以发帖。退出后隐藏发帖表单。
- 各区域独立显示加载、空状态和错误，可手动刷新。成功提示使用小状态栏。
- 提交期间禁止重复提交；留言草稿按小组暂存在当前页面内存，切换详情不会串到另一组。刷新页面不会保留未发送草稿。
- 无 WebSocket、额外前端框架或数据库改动。后续新增的编辑和删除前端需要两项新接口，后端尚待实现。
- 创建者专用 Edit group / Delete group：预填编辑对话框、删除确认框、提交锁定与失败保留。接口详见 `Documents/groups-edit-delete-api.md`。

## 主要文件和阅读顺序

1. `Application/studybuddy/studybuddy/src/main/resources/templates/groups.html`：原表单、新增详情区域和状态栏；加载 groups.js。
2. `Application/studybuddy/studybuddy/src/main/resources/static/js/groups.js`：先看底部事件绑定，再看 createGroup、loadGroups、changeMembership、openGroup、loadMembers、loadPosts、createGroupPost。
3. `Application/studybuddy/studybuddy/src/main/resources/static/css/groups.css`：延续现有卡片、按钮和手机布局。

脚本只使用普通函数、fetch 和 DOM。groupRequest 统一处理 JSON、登录 Cookie、CSRF 和错误。groups 保存列表，selectedGroupId 表示当前打开的小组，currentUser 用于判断创建者。请求序号仅用于防止快速切换小组时旧响应覆盖新内容。

## 已有接口

| 操作 | 方法和地址 |
| --- | --- |
| 获取当前登录者 | GET /api/auth/me |
| 列表和创建 | GET /api/groups，POST /api/groups |
| 加入 | POST /api/groups/{groupId}/join |
| 退出 | DELETE /api/groups/{groupId}/leave |
| 成员 | GET /api/groups/{groupId}/members |
| 留言 | GET /api/groups/{groupId}/posts，POST /api/groups/{groupId}/posts |

创建提交 `name`、`course`、`description`、`meetingTime`、`location`；课程仍是后端定义的字符串，不是 courseId。发帖只提交 `content`。名字、邮箱等用户身份不作为创建或发帖请求参数，由后端从登录会话确定。

## 已验证

- 使用命令行覆盖配置，49 个 Java 测试在 H2 模式通过。
- 浏览器使用真实 Spring 后端和隔离 H2 数据库，两个临时账号验证创建、加入、读取成员、发帖、刷新读取、退出，以及退出后后端拒绝发帖。
- 通过测试网络拦截检查 500/503、401、空列表和失败保留草稿；正式代码没有模拟成功逻辑。
- 五个主导航地址返回正常页面；检查了桌面和 390px 手机宽度。
- 用户内容以 textContent 渲染，HTML 字符串不会作为页面代码执行。

## 组员手工验收

1. A 登录并创建小组，检查创建者身份和人数，刷新后小组仍存在。
2. B 登录后打开小组，未加入时能看成员和留言，不能发帖。
3. B 加入，人数更新，并可发帖；A 点击 Refresh posts 能看到留言。
4. B 退出，发帖表单隐藏；A 的创建者卡片没有 Leave 按钮。
5. 检查空白输入、错误提示、刷新按钮和手机布局。

## 测试命令

在 `Application/studybuddy/studybuddy` 下运行隔离的 Java 测试：

```sh
sh mvnw test -Dspring.flyway.enabled=false -Dspring.datasource.url=jdbc:h2:mem:frontend_check -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.datasource.username=sa -Dspring.datasource.password= -Dspring.jpa.hibernate.ddl-auto=create-drop
```

本地浏览器测试需要 Node、Playwright 和 Chrome。先启动独立测试应用：

```sh
sh mvnw spring-boot:test-run -Dspring-boot.run.main-class=studdybuddy.LocalStudybuddyApplication '-Dspring-boot.run.arguments=--server.port=18081 --spring.flyway.enabled=false --spring.datasource.url=jdbc:h2:mem:frontend_browser --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=create-drop'
```

然后在仓库根目录运行 `node tests/groups-frontend.cjs`。可用 STUDYBUDDY_TEST_URL 指向其他 localhost 端口、CHROME_PATH 指定浏览器路径；Playwright 需可由 Node 加载。测试会创建临时账号，只应针对独立测试数据库运行。

这些命令只为测试覆盖配置，没有修改 application.properties。H2 数据在进程停止后清空；以上通过不代表 MySQL/Flyway 迁移已经验证。

## 待数据库联调

Forest 与 Eddie 仍需确认 MySQL 迁移覆盖 `study_groups` 的 name、course、meeting_time、location、created_at，`group_members.id`，以及 `group_posts` 表。数据库就绪后再用真实 MySQL 执行同一套双账号验收。

## 分支说明

- 本次在本地 `feature/groups-frontend`，基于远程 main 开发，未推送。
- Profile Name 显示的三个本地修改继续保留。
- `local/database-fixes-20260927` 是已有的临时数据库整合分支，仍保留，未推送。本次前端分支不包含它额外的 V4 迁移，以免前端 PR 混入待合并的数据库修复。
- 等数据库修复正式进入 main 后，更新前端分支，确认修复和 Name 改动都在，再清理临时分支；不要提前删除。

## 编辑与删除补充验证

`node tests/groups-management.cjs` 使用隔离的模拟接口验证 PUT 更新、DELETE 204 空响应、创建者按钮、取消与确认、失败保留和手机对话框。它不调用真实后端，不代表新增后端已经完成。
