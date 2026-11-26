# Ptvbox 1.0.1 交互体验、动画页重建与国漫年番自动匹配

版本：`1.0.1` / `versionCode 25`  
日期：2026年11月26日  
构建类型：横屏 ATV APK（兼容遥控器、鼠标和触摸，非原生竖屏手机 App）

## 本版已完成功能

- [x] **详情页单集防误触**：触摸未选中的单集时先选中高亮并展示下方简介，再次点击已选中的该集才开始播放；遥控器方向键聚焦后按确认仍直接播放。
- [x] **相似推荐一行五部**：详情页下方相似推荐由一行 6 部调整为一行 5 部，避免海报下方评分与标题被挤压。
- [x] **删除 Netflix 重复 TOP250**：Netflix 标签页移除与豆瓣完全重复的 TOP250；从豆瓣 TOP250 切换至 Netflix 时自动回落至“热门”。
- [x] **侧边栏动态播放三角**：将“返回播放”移至 HOME 按钮上方，新增 `NowPlayingTriangle` 循环变色呼吸动画图标（暂停时切换静态柔和色）。
- [x] **详情页分类标签点击搜索优化**：点击详情页类型/地区/年份标签时，本地按类型+国家+年份三维度即时命中缓存，线上走豆瓣分类推荐流 `筛选:$tag`（最多两页 40 部，无结果回退另一类型），移除 `SearchResults040` 中对非类型标签的二次强制过滤误杀。
- [x] **动画页面重建（参考首页 + Kazumi）**：顶部改为多分类横排 Tab——第 1 个为默认展开的`近期热门番剧`海报墙，第 2 个为`时间表`（点进后才显示年份、季度与周一至周日北京时间排期），并新增`国漫 / 动画剧集`、`动画电影`、`高分动画`瀑布流分类及快捷搜索/动画源管理入口。
- [x] **外部海报中文化与详情页单行自适应标题**：`presented050` 对非中文标题优先回填已关联的豆瓣/TMDB/别名中文标题（不改写作品身份键）；详情页无艺术字 Logo 时采用 `AutoFitSingleLineTitle` 单行自适应缩字号显示，消除长标题上下两行堆叠。
- [x] **国漫年番/异名作品四层自动匹配**：
  - `Identity` 支持识别 `年番X` / `年番` 与 `重制版/特别篇/番外篇`、`seriesCore` 剥离与 `searchQueries` 两级搜站降级；
  - `Identity.matchScore` 支持年番作品按优先级匹配精确年番/对应季 > 通用年番 > 无季号合并总集，同时拒绝明确冲突季号/年番号，保留普通季番（如《无职转生 第三季》）严格不混第一季的保护规则；
  - `RatingMatch055` 与 `TmdbSeasonResolver101` 支持年番自动关联 TMDB 总剧集词条并按首播年份/年番号自动对齐对应 Season 拉取海报、剧照与单集简介；
  - `IdentityBridge060` 为动画作品补充 Bangumi 中文名、官方别名与外部 ID 反哺搜源。
- [x] **TV 端防抖快进与防二次重播**：
  - 遥控器左右方向键（全屏与进度条焦点）、手机双击左右统一接入预览累加 + **650ms 防抖合并提交**（按确认键立即跳，按返回键取消），不再每按一次就触发一次网络缓冲；
  - 进度条触摸拖动改为本地 `scrubPreview` 预览，手指松开时才一次性 `seekTo`；
  - `seek051` 启用 `SeekParameters.CLOSEST_SYNC` 关键帧快对齐并即时更新 UI 位置；`ensurePlayer` 先释放旧实例防双声、`select` 前先 `stop()/clearMediaItems()` 清理旧音频管线，Seek 后 5 秒内短暂非 401/403/404 源错误优先原位恢复，避免误换源重头播放有声无画。
- [x] **动画页小海报豆瓣双评分与中文名匹配**：
  - `Work.enriched` 与 `MetadataQueries040` 将中文标题/中文别名置顶，使 TMDB 匹配回来的中文名第一时间反哺豆瓣查询；
  - `Identity` 新增剧集末尾季号识别（如 `赛博朋克：边缘行者 2` / `Cyberpunk: Edgerunners 2`），`RatingMatch055` 支持续集跨库对齐 TMDB 总剧集与豆瓣对应季；
  - `PresentationQueue050` 升级至 `match101` 缓存签名、4 路并发、缺失豆瓣时缩短重查窗口，并过滤 `TMDB 0.0` 为 `TMDB -`；`SearchResults040` 筛选标签改为按频次降序。
- [x] **20 部一页分页加载（动画页与分类搜索页）**：动画页“近期热门番剧”与“时间表”由一次性渲染 200+ 部改为每次 20 部瀑布流追加；详情页分类标签跳转搜索同步改为首屏 20 部、滑到底部继续加载下一页 20 部，大幅提升首屏加载速度。
- [x] **罗马数字季号识别（修复《无职转生III》等）**：`Identity.romanSeason` 与 `stripRomanAndSubtitle` 支持全角/半角罗马数字（`Ⅱ/Ⅲ/Ⅳ/II/III/IV` 紧跟汉字且位于 `～` 副标题前），自动剥离季号与副标题完成 TMDB/豆瓣匹配。
- [x] **无 TMDB Key 时的豆瓣原生高清背景**：`Metadata.doubanHdImage` 将豆瓣封面由 `s_ratio_poster`/`m_ratio_poster` 升级为 `/view/photo/l/` 原图大图，`dbWork` 优先取 `large`，豆瓣详情额外抓取横版高清剧照作为 `backdrop`，彻底改善无 TMDB Key 时详情背景模糊问题。
- [x] **侧边栏“返回播放”动态水流渐变三角**：`NowPlayingTriangle` 升级为 `TileMode.Repeated` 对角线连续平移动态水流渐变 + 流动发光外圈，播放中明显流动，暂停时切回静态柔和色。
- [x] **动画季号全模式补齐（图 1 问题修复）**：`Identity.season()` 与 `searchTitle()` 补齐英文序数季（`5th Season`）、日文片假名季（`シーズン2`）与分期（`2クール` / `Part 2`）；`Presentation050` 升级至 `match102` 并优化含假名标题的中文名覆盖。
- [x] **播放器控制栏与上下信息栏 UI 重设计（图 2 & 图 3）**：
  - 底部 `播放源`、`倍速`、`画面比例` 统一补齐圆角矩形背景并随文字长度自适应宽度；
  - `弹幕` 按钮重设计为统一圆角矩形背景，内部整合 `[弹]` 徽标 + `ON/OFF` 彩色小胶囊 + 弹幕数量；
  - 删除右上角收藏星星与右下角冗余音频/字幕/弹幕解析文字（右下角仅保留时间）；
  - 顶部信息栏精简多余免责声明并在末尾追加字幕与弹幕状态。
- [x] **直播与体育实测修复**：`LiveDetail050` 改用 `m.sites.liveDetail054(entry)` 解决进详情误报“线路读取失败（0条）”的问题（BlueStacks 实测拉出 15 条蓝光/超清/流畅线路并成功 1080P 出帧）；`Sites.probe` 对不支持 `Range` 头的直播流自动回退无 Range 截断探测。
- [x] **《赛博朋克：边缘行者 2》季与单集修复**：`Metadata.episodeSeries()` 优先复用 `work.facts.tmdbId` 与已保存的 `rating_link_tmdb`，搜 TMDB 放宽续集首季年份限制，且当目标季尚未建季（404）时自动回退至 TMDB 存在的正片季，保证关联的全部季与单集正常展示。
- [x] **体育/直播加载提示、台标持久缓存与八八看球加固**：`LiveHub050` 与 `LiveDetail050` 增加醒目的“正在加载频道列表/线路，请稍候…”提示条；`LiveLogoImage101` 为台标与封面绑定稳定内存/磁盘缓存键；`playLive050` 超时放宽至 35 秒，`Sites.probe` 对直播/体育网页自动走 `WebMedia052.resolve` 嗅探与无 Range 回退。
- [x] **纯空间上限 LRU 缓存机制（取消时间过期限制）**：新增 `CacheBudget101`（详情/单集/动画目录磁盘上限 LRU 淘汰、命中刷新访问时间；`presentation_v5` 按 2000 条上限 LRU 淘汰）；`PtvApplication` 响应拦截器移除图片 `Pragma`/`Expires` 并改写长效 `Cache-Control`，使海报与直播台标严格遵循用户设置的磁盘空间上限与 LRU 淘汰，不再因短时间头重复下载。
- [x] **本地构建脚本兼容**：`scripts/Build.ps1` 兼容 Windows PowerShell 5.1 与 PowerShell 7。

## 自动化测试、BlueStacks 隔离模拟器实测与构建证据

- **JVM 单元测试**：`.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache` 通过，共 **147 项测试（24 suites）**，0 失败、0 错误、0 跳过。
- **18 个动画规则实网排查（《凡人修仙传 年番4》）**：确认原搜不到的主因是直传 `年番4` 导致绝大多数站返回 0 条且 `Identity.matches` 未识别年番/合并总集；去掉后缀搜 `凡人修仙传` 后 `baimao`、`MXdm`、`sorani`、`7sefun`、`LMM` 5 站均返回真实条目（`AGE`/`dalvdm`/`mutefun` 403，`xfdmneo` 500，其余站以日番为主未收录）。
- **BlueStacks 隔离模拟器实测（`-s 127.0.0.1:5555`，未触碰用户电视盒子）**：
  - 覆盖安装 `Ptvbox-1.0.1-ARM.apk` 成功，`versionCode=25`、`versionName=1.0.1`；
  - `artifacts/qa-1.0.1/05-netflix.png`：确认 Netflix 页签 TOP250 已删除；
  - `artifacts/qa-1.0.1/09-anime-schedule.png`：动画页点进“时间表”展开年份/季度/周一至周日排期；
  - `artifacts/qa-1.0.1/10-detail-top.png`、`11-detail-ep2-selected.png`、`12-detail-related-5col.png`：详情页单集与简介、相似推荐一行 5 部且下方双评分完整显示；
  - `artifacts/qa-1.0.1/14-anime-20page.png`、`15-tag-search-20page.png`：动画热门与分类搜索均为 20 部一页加载，《赛博朋克：边缘行者 2》《药屋少女的呢喃 第三季》《黑色五叶草 第二季》显示中文标题、中文海报与双评分；
  - `artifacts/qa-1.0.1/22-live-huya-playing.png`、`24-live-iptv-play.png`、`25-live-detail-fixed.png`：直播详情页正常列出 15 条蓝光/超清线路，虎牙 1080P 20M 成功出帧，左侧边栏 HOME 上方显示动态水流渐变三角；
  - `artifacts/qa-1.0.1/26-player-chrome-new.png`：新版播放器底部按钮矩形自适应背景、矩形弹幕按钮（`[弹] ON 0`）、精简上方信息栏、去重右下角及删除右上角收藏均验收通过；
  - `artifacts/qa-1.0.1/29-edgerunners2-detail.png`、`30-edgerunners2-episodes.png`：《赛博朋克：边缘行者 2》详情页自动关联 TMDB 剧集并完整展示第 1 季（2022·10集）与第 2 季（2026·10集）及全部分集剧照、评分与中文简介。
- **本地双签名 APK 与原签名核验**：
  - 通用包：[Ptvbox-1.0.1.apk](../../artifacts/release-v1.0.1/Ptvbox-1.0.1.apk)  
    `versionName='1.0.1' versionCode='25'`  
    SHA-256：`180b86121329eb50eae5615e321a3a6bbf07dbacc9b765b7080259841f31d3e3`
  - ARM 包：[Ptvbox-1.0.1-ARM.apk](../../artifacts/release-v1.0.1/Ptvbox-1.0.1-ARM.apk)  
    `versionName='1.0.1' versionCode='25'`  
    SHA-256：`b1d8c50e32b81da2895213ec29b0d6324f0d310f41a14df5f55647cb38fe3f58`
  - 两包均使用原 V2 签名证书 `33d71fe4cf85185651c1e254a2e62000cd7b348d5e50a45ff1b8dfa6656e1208`，可直接覆盖安装保留原有数据。

## 物理设备与 PC 对齐未完成边界

- [ ] **物理设备验收**：本轮已在 BlueStacks 隔离模拟器完成 UI 与匹配截图验证，但尚未在真实电视盒子/手机上做物理遥控器快进手感与私人订阅长时播放验收；不以模拟器通过代替真机阶段验收。
- [ ] **线上发布**：本版目前仅在本地生成双签名测试 APK，**未提交推送 GitHub、未打 `v1.0.1` 标签、未创建 GitHub 草稿 Release、未发送企业微信**。
- [ ] **资源全量覆盖**：`AGE`/`dalvdm`/`mutefun` 403、`xfdmneo` 500、全部默认/私人/国际/直播源首帧与长期播放未全量验收。
- [ ] **PC 端对齐差距**：PCBOX ↔ Ptvbox 双向加密迁移、分类内逐站拖拽、完整 HDR/Dolby 映射、100+ 站 60fps 硬体验收与原生安卓竖屏手机 App 仍未完成。

## 本版未开发内容与下版建议

- **本版未做**：未修改 Windows PCBOX、未实现双向跨端迁移、未接入原生竖屏手机界面、未改动数据库 Schema。
- **建议下一步**：
  1. 你先在手机或电视上覆盖安装本地 [Ptvbox-1.0.1-ARM.apk](../../artifacts/release-v1.0.1/Ptvbox-1.0.1-ARM.apk)（或通用包）实测：单集触摸选看简介、相似推荐一行 5 部、动画页顶部 Tab 与时间表、长标题单行自适应、详情分类点击以及《凡人修仙传 年番4》的资料与片源自动匹配。
  2. 确认无误后，告知我提交代码并推送 `v1.0.1` 标签触发 GitHub 草稿 Release。
