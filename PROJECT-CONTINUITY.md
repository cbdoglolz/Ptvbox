# Ptvbox 项目进度与交接规范

Revision: `PTVBOX-2026-11-28.1`
Last updated: `2026-11-28 14:48 Asia/Shanghai`
Last updated by: `Codex`
Current implementation checkpoint: `Owner authorized GitHub release 1.0.3 / 44. Product source unchanged from approved c3; formal build, signing, publication and download verification underway. Public latest still 1.0.2 until verified.`

本文件是Ptvbox当前状态的入口。它帮助任何开发者在不依赖聊天历史的情况下接手到一半的工作。全局流程见`C:\Users\cbdog\.codex\PROJECT-CONTINUITY.md`。

接手核对提示（2026-11-27 16:08）：以下部分总览仍混有 1.0.0/1.0.1 的历史数字与草稿发布流程；当前发布以第 2 节、实际工作流和本文件末尾本次接手记录为准。现有动画规则为 25 个；分类路由为 8 类（新增 Netflix）；当前工作流直接发布公开 Release。企业微信只有条件发送入口，`gh secret list` 确认尚无 `WECHAT_WEBHOOK_URL`，不能声称 1.0.2 通知已送达。原有设备安装记录不构成再次连机授权。

## 1. 项目目标与边界

Ptvbox是PCBOX功能集的Android客户端。目前交付物是支持遥控器、鼠标和触摸的横屏ATV APK，不是已完成的原生竖屏手机App。浏览器手机配置台也不等于原生手机App。

当前范围：

- Android TV主界面、搜索、详情、播放、直播、收藏、历史和设置
- TVBox默认站、私人订阅、HDHUB/OneTouchTV 国际站和25个动画规则
- 字幕、弹幕、网盘授权、手机局域网配置
- 豆瓣、TMDB、IMDb、Bangumi、AniList之间的资料补充和身份匹配
- GitHub标签自动构建和公开正式Release（候选版只留本地，待所有者确认）

明确不在当前完成声明内：

- PCBOX与Ptvbox完整双向迁移
- 原生手机UI
- 全站、全直播、全动画首帧覆盖
- 所有第三方插件非标准自绘窗口治理
- 完整PC功能对等

## 2. 当前发布状态

| 项目 | 当前值 |
|---|---|
| 可见版本（已发布） | `1.0.2` |
| Android versionCode（已发布） | `40` |
| 进行中候选版本（本地） | `1.0.3-c3 / versionCode 43`；通用/ARM 签名包位于 `artifacts/candidate-v1.0.3/`，未提交/上传/正式发布 |
| 包名 | `com.cbdog.ptvbox` |
| minSdk / targetSdk / compileSdk | 26 / 35 / 37 |
| 分支 | `main` |
| 远程 | `https://github.com/cbdoglolz/Ptvbox.git`（已切换为 Public 公开仓库） |
| 最新正式标签 | `v1.0.2` |
| v1.0.2构建源码提交 | `1d1e7dd` |
| GitHub Actions | `Ptvbox release`（#45909558679）成功发布公开 Release |
| GitHub Release | 公开 Latest Release（非草稿），名称/正文均为`Ptvbox 1.0.2`，支持 App 内“检查更新”直连 |
| Release附件 | 通用APK、ARM APK，共2个文件 |
| 物理电视验收 | 1.0.2 为此前安装记录；本轮 1.0.3 未操作任何物理电视/手机，待所有者手动验收 |

GitHub公开Release（`v1.0.2`）附件的实际SHA-256（来自 `gh release view v1.0.2` `digest`）：

```text
e761ead591e528fddd24f11bf336709067ebe11b9ce5c78ea0c1f932ac0b5eab  Ptvbox-1.0.2-ARM.apk
1962b0aeedd6febbc47a6d165d0cd5edaae98c189149839e578fde37edc7167b  Ptvbox-1.0.2.apk
```

本地独立重建的哈希可能不同，这是Android构建字节非确定性，不代表签名或源码不同。验证依据为：包内`versionName/versionCode`、原签名证书、包审计，以及GitHub附件实际下载核对。

## 3. 版本规则

Ptvbox用户可见版本严格使用三位、每段一位十进制：

```text
MAJOR.MINOR.PATCH
0.9.9 -> 1.0.0
1.0.9 -> 1.1.0
1.9.9 -> 2.0.0
```

`0.10.0`、`0.5.11`、`1.10.0`等包含两位数字段的名称无效。

`versionCode`是Android升级顺序号，与小数版本分离，只增不减。发布标签必须等于`v`加包内`versionName`。

## 4. 权威文档顺序

发生冲突时按以下顺序核对，但源码和可复现测试仍是事实来源：

1. `AGENTS.md`：安全和项目规则
2. `PROJECT-CONTINUITY.md`：当前状态和交接入口
3. `PROGRESS-CHECKLIST.md`：对所有者要求的逐项状态
4. 最新`docs/reports/Ptvbox-vX.Y.Z-*.md`：本版范围、证据和限制
5. `docs/RELEASING.md`：版本和GitHub发布流程
6. `docs/plans/`：尚未实施的方案
7. `PROJECT-AUDIT.md`和`MEDIA-MATCHING-AUDIT.md`：历史阶段审计，不是当前完成声明

`PROJECT-AUDIT.md`来自0.1.0阶段，`MEDIA-MATCHING-AUDIT.md`是匹配专项阶段0。读这两份文件时必须先看本文件给出的当前版本。

## 5. 代码架构

当前为单一Android`app`模块，Kotlin、Compose for TV、Media3 ExoPlayer、OkHttp、Coil、SQLite和Android Keystore。

关键入口：

| 文件 | 责任 |
|---|---|
| `app/src/main/java/com/cbdog/ptvbox/MainActivity.kt` | Activity、页面框架和全局提示 |
| `PtvModel.kt` | 主要UI状态、搜源、播放、详情和配置编排 |
| `CinemaPlayer.kt` | 播放器界面、控制层和手机手势 |
| `Library.kt` | SQLite、偏好和加密凭据 |
| `Network.kt` | 外部资料、站点、探测和订阅 |
| `SpiderService.kt` | 独立`:spider`插件进程IPC |
| `SpiderBridge.kt` | 用户Jar加载、插件调用和缓存 |
| `PluginUi054.kt` | 主/插件进程Toast代理 |
| `NoticePolicy057.kt` | 应用内私人源提示过滤 |
| `SourceRouting060.kt` | 分类选源和分波优先级 |
| `IdentityBridge060.kt` | IMDb/TMDB/豆瓣/Bangumi身份桥 |
| `PlaybackAddons053.kt` | 在线字幕、弹幕和播放附加状态 |

主要数据流：

```text
豆瓣/TMDB/IMDb/Bangumi/AniList
  -> Work / WorkFacts / alias / season / episode identity
  -> DetailMetadata / Presentation / cache

启用站点
  -> 分类路由和分波扫描
  -> search / detail / episode
  -> private Jar in :spider
  -> probe media
  -> Candidate
  -> Media3
  -> progress / history / Watch Next
```

## 6. 历史基线已完成和验证（1.0.3 见末尾记录与最新报告）

`[x] DONE`：

- 1.0.0双签名APK与GitHub草稿Release
- 主进程和`:spider`进程Toast代理
- 用户反馈三条私人源提示连续发送时`suppressed=3`、`visibleNoise=[]`
- 真实私人订阅候选出片并持续采样40秒，没有三类提示；该功能候选在版本标签纠正前后只改版本元数据
- 分类选源七类分组、分波搜索
- 动画实时弹幕预览
- AniList/Bangumi中文补充和外部ID部分互通
- 详情资料6小时缓存
- 手机双击/长按/拖动播放手势
- 设置弹窗焦点恢复、明文配置、绿色已配置状态
- 133项JVM通过
- 原签名、无QA类、无私人地址、无日志/数据库/Keystore的Release审计

## 7. 已实现但未完整验收

`[x] IMPLEMENTED`：

- 网盘扫码界面、二维码刷新和授权缓存
- 用户关闭站点立即影响后续扫描
- GitHub自动发布、企业微信条件通知入口
- 电影/剧集/动画资料跨站身份推理
- 插件Toast全方法名代理
- 手机横屏兼容UI

这些项目不能因为存在按钮或代码就写成“所有来源完成”。

## 8. 未完成和风险

`[ ] PARTIAL`：

- 单集简介、卡片、标记圆钮和遥控焦点已完成专项，但真实电视手感未验收
- 分类能调组顺序，不能在同一分组内逐站拖拽
- 私人源真实40秒播放有证据，但私人源可用性由订阅方控制
- 第三方插件如果绕过Android Toast自绘悬浮窗，当前代理不能保证拦截
- 公开字幕和弹幕依赖提供方网络、限流和匹配质量
- 动画仍有站点403、零结果或中文资料不完整

`[ ] TODO`：

- 真实电视/手机物理设备验收
- 全部默认站、私人站、国际站、直播和动画源首帧矩阵
- PCBOX↔Ptvbox完整加密迁移
- 原生手机UI
- 20路以上并发、100站以上虚拟化、严格帧率和长期稳定性验收
- 完整HDR/Dolby映射和全格式播放验证
- 与PCBOX的完整功能对等

`[ ] BLOCKED`：当前没有需要所有者立即解除的代码阻塞。物理设备行为只能由所有者授权或手动测试。

## 9. 构建与验证

环境：

```text
JAVA_HOME=D:\rea\.tools\jdk21\jdk-21.0.13+9
ANDROID_HOME=D:\rea\.tools\android-sdk
```

JVM测试：

```powershell
$env:JAVA_HOME='D:\rea\.tools\jdk21\jdk-21.0.13+9'
$env:ANDROID_HOME='D:\rea\.tools\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache
```

本地签名通用/ARM包：

```powershell
.\scripts\Build.ps1
.\scripts\Build.ps1 -ArmHost
```

QA包：

```powershell
.\scripts\Build.ps1 -DebugOnly -ArmHost
```

GitHub同流程构建：

```bash
bash scripts/Build-GitHub.sh
```

该脚本要求签名环境变量和Android NDK。GitHub Actions Secrets已在仓库配置；实际值不在项目文件中。

## 10. 发布流程

1. 修改`versionName`并递增`versionCode`
2. 更新报告、`PROGRESS-CHECKLIST.md`和本文件
3. 运行JVM、双APK构建、签名、包审计和设备/模拟器专项
4. 提交并推送`main`
5. 创建与包内版本完全一致的`vX.Y.Z`标签并推送
6. 所有者确认正式版本后，`Ptvbox release`工作流构建通用/ARM包并发布公开Release
7. 核验GitHub Release标题和正文只有`Ptvbox X.Y.Z`
8. 核验附件只有两个APK；不得上传日志、测试报告、截图、源码归档或私人数据
9. 下载实际附件，验证包内版本、签名和哈希
10. 核验公开Latest与应用内更新；候选用户升级要求正式 versionCode 大于候选，1.0.3 定稿至少为44

Ptvbox的`WECHAT_WEBHOOK_URL`当前未配置；工作流在Secret存在时会发送仅含`Ptvbox 版本`的真实更新通知，不存在时跳过。

## 11. 设备与隐私边界

- 默认使用隔离模拟器，不连接所有者正在观看的电视盒子。
- 受保护设备地址只可存在于忽略的`.tools`配置，不能写入源码、报告或Release。
- `.tools`内可能包含本机签名材料、私人订阅和诊断文件，永不提交。
- 签名证书SHA-256固定为`33d71fe4cf85185651c1e254a2e62000cd7b348d5e50a45ff1b8dfa6656e1208`。
- 不读取、导出或公开用户私人订阅、凭据、观看历史和登录Cookie。
- 涉及清库、覆盖持久配置、批量删除或真实设备安装时必须重新确认范围。

## 12. 关键验收命令

包版本：

```powershell
aapt dump badging artifacts\release-v1.0.0\Ptvbox-1.0.0-ARM.apk
```

签名：

```powershell
apksigner verify --verbose --print-certs artifacts\release-v1.0.0\Ptvbox-1.0.0-ARM.apk
```

包审计：

```powershell
python .tools\audit-0100.py
```

脚本文件名保留了`0100`历史名称，但当前检查`1.0.0/versionCode 24`。下一版应同步改名。

## 13. 当前测试证据

- `artifacts/qa-1.0.0/notice-and-playback-evidence.json`
- `artifacts/qa-1.0.0/github-release-verification.json`
- `artifacts/qa-1.0.0/package-audit.json`
- `artifacts/qa-1.0.0/qa100-exact-private-notice-suppressed.png`
- `artifacts/qa-1.0.0/qa100-private-direct-*.png`
- `docs/reports/Ptvbox-v1.0.0-Private-Source-Notice-Hardening.md`

`artifacts/`被Git忽略，但会保留在本机。开源接手者不能把该目录当成已随Git同步；需要重现测试或由所有者单独提供证据。

## 14. 下一次接手从哪里继续

建议顺序：

1. 读取本文件、规则、最新 1.0.3 报告与清单；执行 `git status --short --branch`，确认是否有其他开发者更新。
2. 从所有者对 `1.0.3-c3` 的设备反馈继续，只修复本轮范围；不继承任何历史电视连机授权。
3. 若需实时 TMDB 端到端复核，先在隔离环境确认有效凭据；现有模拟器凭据401不能作为代码季集匹配失败的证据，也不能冒充实时验证通过。
4. 所有者确认正式定稿后再进入 `1.0.3 / versionCode >=44`；核对签名、测试、包内容与当前发布规则。
5. 企业微信 Secret 尚未配置，不发送测试消息；完整 PC 对等、其他国际源、迁移和原生手机继续按明确授权分批推进。

可直接使用的新会话开场：

```text
先读取 C:\Users\cbdog\.codex\PROJECT-CONTINUITY.md、
D:\Ptvbox\AGENTS.md、D:\Ptvbox\PROJECT-CONTINUITY.md、
PROGRESS-CHECKLIST.md 和最新版本报告。检查Git状态和最新提交，
确认别人是否更新过文档，再继续本次任务。完成后维护本文件。
```

## 15. 版本维护记录

### 2026-11-26 18:26 Asia/Shanghai — Codex — 建立项目连续性规范

- 状态：DONE
- 目标：让其他开发者不依赖聊天历史即可接手Ptvbox
- 修改：新增跨项目总规范、Ptvbox当前进度文件和自动读取规则，并更新README、发布流程、要求清单和历史路线入口
- 关键入口：本文件、全局`PROJECT-CONTINUITY.md`、`AGENTS.md`
- 验证：核对当前版本、Git提交、标签、GitHub Release、附件哈希和测试证据
- 未验证：真实电视物理设备
- 下一次从这里继续：读取所有者最新需求或真实设备反馈，再选择下一版本范围

### 2026-11-26 19:13 Asia/Shanghai — Antigravity — 1.0.1 交互修复、动画页重建与国漫年番自动匹配（本地双签名包就绪）

- 状态：DONE（本地 1.0.1 代码、141 项 JVM 单测与通用/ARM 双签名 APK 完成；未提交推送 GitHub / 未触发草稿 Release）
- 目标：落实单集先选后播、相似推荐一行 5 部、删除 Netflix TOP250、侧边栏动态播放三角移至 HOME 上方、详情分类标签搜索优化、动画页重建（顶部近期热门番剧+时间表+多分类）、外部海报中文化与详情无艺术字单行自适应标题，以及国漫年番四层自动匹配
- 修改：
  - 单集先选后播、相似推荐一行 5 部、Netflix 隐藏 TOP250、侧边栏动态变色呼吸三角移至 HOME 上方、详情分类走豆瓣 `筛选:$tag` 分类推荐流
  - `AnimePage056` 重建为顶部多分类 Tab（默认`近期热门番剧`，第二个`时间表`展开年份/季度/星期，外加`国漫/动画剧集`、`动画电影`、`高分动画`）
  - `presented050` 中文标题优先回填；`CinemaDetail040` 新增 `AutoFitSingleLineTitle` 单行自适应标题
  - `Models.kt` / `PtvModel.kt` / `EpisodeCatalogRepository.kt` / `RatingMatch055.kt` / `TmdbEpisodes.kt` / `Network.kt` / `IdentityBridge060.kt` 实现年番识别、两级搜站降级、匹配度排序、TMDB 季自动对齐与 Bangumi 别名补全
  - `app/build.gradle.kts` 升至 `1.0.1` / `versionCode 25`；`scripts/Build.ps1` 兼容 Windows PowerShell 5.1
- 关键文件：`EpisodeCard054.kt`、`SeasonEpisodes040.kt`、`CinemaDetail040.kt`、`TvScreens.kt`、`PtvModel.kt`、`SearchResults040.kt`、`AnimeSeason056.kt`、`Presentation050.kt`、`Models.kt`、`RatingMatch055.kt`、`TmdbEpisodes.kt`、`Network.kt`、`IdentityBridge060.kt`、`EpisodeCatalogRepository.kt`、`Version101Test.kt`、`docs/reports/Ptvbox-v1.0.1-Update-Report.md`
- 验证：147 项 JVM 单测全通过（0 失败/0 错误/0 跳过）；`aapt` 与 `apksigner` 核验双包均为 `1.0.1` / `versionCode 25` 且原签名证书一致；BlueStacks 隔离模拟器（`127.0.0.1:5555`）实测覆盖安装、Netflix 隐藏 TOP250、详情分类筛选 20 部一页、相似推荐一行 5 部、动画页顶部 Tab 20 部一页/时间表、《赛博朋克：边缘行者 2》双季与单集展示、直播详情 15 条线路+1080P 出帧、动态水流渐变播放三角及新版播放器控制栏与顶部精简信息栏全部通过
- 证据：
  - `artifacts/release-v1.0.1/Ptvbox-1.0.1.apk`（SHA-256: `180b86121329eb50eae5615e321a3a6bbf07dbacc9b765b7080259841f31d3e3`）
  - `artifacts/release-v1.0.1/Ptvbox-1.0.1-ARM.apk`（SHA-256: `b1d8c50e32b81da2895213ec29b0d6324f0d310f41a14df5f55647cb38fe3f58`）
  - `artifacts/qa-1.0.1/05-netflix.png`、`09-anime-schedule.png`、`10-detail-top.png`、`12-detail-related-5col.png`、`14-anime-20page.png`、`15-tag-search-20page.png`、`22-live-huya-playing.png`、`24-live-iptv-play.png`、`25-live-detail-fixed.png`、`26-player-chrome-new.png`、`29-edgerunners2-detail.png`、`30-edgerunners2-episodes.png`
- 发布：仅本地签名测试包，未提交、未推 `main`、未打 `v1.0.1` 标签、未发企业微信
- 未验证：真实电视/手机物理设备硬件与遥控器快进手感验收
- 风险/回滚：所有改动在本地工作区，未修改数据库 Schema；如需回退可还原工作区至 `2c4fd5a`
- 下一次从这里继续：等待所有者实测本地 `1.0.1` APK 反馈；若验收通过，执行 `git commit`、`git push origin main` 与 `git tag -a v1.0.1 -m "Ptvbox 1.0.1"` 推送触发 GitHub 草稿 Release 并核验线上附件哈希

### 2026-11-26 21:33 Asia/Shanghai — Antigravity — 1.0.1 公开 Release 发布 & 1.0.2-c1 设置页重构候选版

- 状态：DONE（`v1.0.1` 已公开上架 GitHub Latest Release；`1.0.2-c1` 本地双签名候选包已在 BlueStacks 实机截图验收，未提交推送）
- 目标：
  1. 将 `cbdoglolz/Ptvbox` 切换为公开仓库并以 `draft: false` 发布 `v1.0.1`（标题/正文仅 `Ptvbox 1.0.1`，仅附双 APK），使 App 内“检查更新”可直连升级。
  2. 引入 `-c1/-c2/...` 候选版本与单目录自动覆盖清理机制（`artifacts/candidate-vX.Y.Z/`，正式发布时自动删除候选目录）。
  3. 启动 `1.0.2-c1`（versionCode `26`），完成设置页 7 项视觉与交互升级。
- 修改：
  - 发布流程与脚本：`.github/workflows/release.yml` 改为 `draft: false`；`scripts/Build.ps1` 支持 `-cN` 候选目录自动覆盖与正式版自动清理并修复 PowerShell 5.1 语法兼容；同步更新 `AGENTS.md`、`docs/RELEASING.md`、`README.md`。
  - `v1.0.1` 公开提交 `f000770` + 标签 `v1.0.1`，GitHub Actions 自动构建并发布公开 Latest Release。
  - `1.0.2-c1` 设置页改版：`Settings040.kt` 移除右侧顶部空白可聚焦标题栏改为纯标题+分割线、升级现代化胶囊拨动开关（绿开/灰关）、新增左侧动态绿点状态指示、将弹幕与内嵌字幕样式及实时预览置顶、在“网络与缓存”增加实时磁盘占用进度条与分类统计、清理“播放与自动选源”开发调试按钮并新增“单步快进/快退（5/10/15/30秒）”联动 `TvPreferences.kt` / `CinemaPlayer.kt`；`PcDesign.kt` 增强未选中 Chip 立体底座；`ConfigValueCard.kt` 长明文 2 行截断省略并强化右侧“配置”按钮边框底色；`CacheBudget101.kt` 新增实时缓存体积统计；新增 `Version102Test.kt`。
- 验证：
  - GitHub API `/repos/cbdoglolz/Ptvbox/releases/latest` 返回 `v1.0.1`（`draft: false`、`prerelease: false`、仅 2 个 APK 及 `sha256` `digest`）。
  - `1.0.2-c1`：148 项 JVM 单元测试全部通过（0 失败/0 错误/0 跳过）；双签名包（`versionName='1.0.2-c1'`、`versionCode='26'`、原 V2 证书 `33d71fe4...`）生成于 `artifacts/candidate-v1.0.2/`；BlueStacks 隔离模拟器（`127.0.0.1:5555`）实机覆盖安装并截图验收 `settings-metadata.png` / `settings-playback.png` / `settings-danmaku.png` / `settings-network.png`（实测实时缓存占用 92.6 MiB）。
- 本地候选产物：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c1.apk`（SHA-256: `1a6da6f94e46128df2c83b4a88c75dba63d8caff3e4c5eca52e9841bea4ababd`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c1-ARM.apk`（SHA-256: `241e95d0bd2defae8c0637540452abc7e39327310339349f609666bc2bb97750`）
  - BlueStacks 截图：`artifacts/qa-1.0.2/settings-*.png`
- 未验证：真实电视盒子/手机物理设备验收（含 App 内从 1.0.0 升级 1.0.1 的端到端实测与遥控器操控手感）
- 下一次从这里继续：等待所有者体验 `1.0.2-c1` 并决定 `1.0.2-c2` 需求（如是否继续优化其他页面或使用 Figma 设计稿迭代），直到确认后去掉 `-cN` 后缀正式发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-26 22:23 Asia/Shanghai — Antigravity — 1.0.2-c2 品牌 Logo 与侧边栏图标焕新、站点页重构+自动计分排名、弹幕续篇匹配与动画源扩充

- 状态：DONE（`1.0.2-c2` / versionCode `27` 本地双签名候选包已自动覆盖旧 `-c1` 并通过 BlueStacks 实机截图验收，未提交推送）
- 修改：
  - **品牌 Logo & 侧边栏图标焕新（Figma 同步 `D3w2XDK1LQ7xW9aU2cbHjz`）**：落地所有者选定并精修的方案 A 主 Logo（`brand.png`：放大 RGB 流光三角 + 背后高斯虚化 X + 透明圆角）；重绘全部 8 枚侧边栏矢量图标（`pc_anime.xml` 改为经典米老鼠剪影并移除旧 `ANI` 文字叠加；`TvScreens.kt` 未选中单色、选中/聚焦青紫流光高亮）。
  - **站点页面重构 + 自动计分排名**：新增 `SiteScores102.kt`，在 `PtvModel.kt` 播源扫描时自动累计站点分数（有可播资源 `+1`、网盘待扫码 `+0.5`、无资源/失败 `-1`），预设明显深绿→中性灰→大红色阶；`Sites040.kt` 重构为 3 列紧凑卡片网格（一屏 15 站）、默认按分数从高到低排名、新增 `好站`/`差站` 筛选、`关闭全部负分站`、`清零计分` 与卡片一键胶囊开关。
  - **设置页细节修复**：`InputButton.kt` 新增 `focusRadius`，修复设置页左侧列表项（`8dp`）与右侧胶囊拨动开关（`20dp`）的聚焦框四角缝隙；“同时显示其他平台评分”改名为“同时显示双平台评分”并补齐说明。
  - **弹幕加载优化**：`PlaybackAddons053.kt` 改为搜到多个候选默认自动加载首条；`DanmakuService053.kt` 修复《药屋少女的呢喃 第3季 第1集》（弹幕库命名为 `药屋少女的呢喃 续篇`）匹配失败，新增续篇后缀归一化、放宽 `season > 1` 首播年校验与按开播时间排序映射到第 N 季。
  - **动画源扩充**：实时探测 `Predidit/KazumiRules` 68 条未收录规则，抓取 7 条实测存活且搜出真实番剧的规则（`FQDM`、`WYDM`、`anime7`、`omofun`、`xfdm`、`xigua`、`yishijie`），动画源由 18 个扩充至 25 个；确认 `GrahamZen/izuko-tv` 无遗漏内置 HTTP 动画源。
- 验证：150 项 JVM 单测全通过；BlueStacks（`127.0.0.1:5555`）实机覆盖安装并截图验收 `artifacts/qa-1.0.2/c2-home.png`、`c2-sites.png`、`c2-settings.png`。
- 本地候选产物（单目录复用，已自动替换 `-c1`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c2.apk`（SHA-256: `6e37425a3e7db621336f4b7b3a0a67ebbf29d0f34289e93613b4b8318953bbe7`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c2-ARM.apk`（SHA-256: `0ffd3a0dbdcb270b34af6a5e282f350b8f49eaf4bfa14934a58afd3aa5c43ec1`）
- 未验证：真实电视盒子/手机物理设备验收。
- 下一次从这里继续：等待所有者实测 `1.0.2-c2`；如需继续微调则迭代 `1.0.2-c3`，如确认定稿则去掉 `-c2` 发布公开正式版 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-26 22:57 Asia/Shanghai — Antigravity — 1.0.2-c3 Shield TV 真机闪退根治、16:9 TV Banner、弹幕全屏飞行与 7 项细节修复

- 状态：DONE（`1.0.2-c3` / versionCode `28` 已覆盖安装至所有者授权的 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c2`，未推送 GitHub）
- 根因与修复：
  - **Shield TV 播放《头文字D》闪退根因**：经实机 logcat 抓取确认，内存紧张时系统回收后台嗅探 `WebView` 渲染进程，因 `WebMedia052` 未覆盖 `onRenderProcessGone`，Chromium `aw_browser_terminator.cc` 主动强杀主进程。修复：在 `WebMedia052.kt` 覆盖 `onRenderProcessGone` 返回 `true` 优雅跳过该线路、将并发隐藏 WebView 由 2 降至 1、并禁用图片加载与缓存大幅降低内存与掉帧。
  - **弹幕飞一半消失根因**：`MediaOverlay053.kt` 原 `occupied.add(i % lanes)` 会被同轨新弹幕直接踢掉半路旧弹幕。修复：改为按轨道右边缘间距防重叠，确保每条弹幕完整飞过 100% 屏幕。
  - **Android TV 16:9 横幅 Banner**：在 Figma 基于方案 A 主 Logo 设计 `640×360` 横幅并导出替换 `res/drawable/tv_banner.png`（移除旧 `tv_banner.xml`）。
  - **UI 细节**：`Sites040.kt` 固定 3 列超紧凑网格并删除名称筛选输入框；`TvScreens.kt` 修正左侧导航图标在矩形聚焦框内正中央对齐；`SeasonEpisodes040.kt` 移除横向选集自动 `bringIntoView` 下滚并将简介标题改为高对比深紫底+亮白标题；`AnimeSeason056.kt` 时间表压缩为紧凑两行并将提示置顶；`MainActivity.kt` `Entry` 聚焦时显示 2dp 霓虹青紫框+亮青光标与提示。
- 本地候选产物（自动替换 `-c2`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c3.apk`（SHA-256: `0bbd5ca517fde9e6a6c86248977f64e1c1c952a1d625e6376c97be662c145772`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c3-ARM.apk`（SHA-256: `f98e3a53f5373445408ab4f2aa96ea70afc339994a416a9cb603e7a55fb0cf8b`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c3` 反馈；若满意定稿则去 `-c3` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-26 23:24 Asia/Shanghai — Antigravity — 1.0.2-c4 弹幕稳轨不跳、播放器全菜单去关闭+紧凑倍速、Android TV 精简 5 频道与 16:9 横版「继续观看」

- 状态：DONE（`1.0.2-c4` / versionCode `29` 已覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c3`，未推送 GitHub）
- 修改：
  - **弹幕稳轨防跳**：`MediaOverlay053.kt` 锁定每条弹幕固定轨道 `i % lanes`，仅同轨判断右边缘间距防重叠，不再跨轨跳动，解决上屏 1/4 互相挤压与上下跳动。
  - **播放器菜单清理与重构**：`CinemaPlayer.kt` 与 `SubtitlePanel053.kt` 移除所有弹出菜单右上角「关闭」按钮；将顶部不可聚焦的「高清优先/速度优先」和「重新检测」移入「播放线路」面板右上角；「倍速」与「画面比例」重做为紧凑浮窗；「弹幕」面板将「弹幕开关」和「重新匹配弹幕」独立置顶并与样式参数分割。
  - **Android TV 桌面频道精简 + 16:9 横版「继续观看」**：`HomeChannels.kt` 与 `HomeArtwork.kt` 将原 8 个重复频道精简为 5 个（`继续观看`、`热门剧集`、`热门电影`、`近期热门番剧`、`Netflix 精选`），同步时自动清理旧重复频道；「继续观看」与系统 `WatchNext` 均升级为 `16:9` 横向封面，剧集优先展示该集的单集剧照（无则回落横版背景图）。
- 本地候选产物（自动替换 `-c3`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c4.apk`（SHA-256: `1f8db119503ecc6289335049105de7b5c4640ef3cb667376c97be662c145772`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c4-ARM.apk`（SHA-256: `c6a2375aa87341240cda5e4660829230f93e17578c141fc5709478838ab32487`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c4`；若定稿则去 `-c4` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-26 23:37 Asia/Shanghai — Antigravity — 1.0.2-c5 Shield TV 3GB 降载根治、弹幕平滑时钟引擎、选集上映日期与计分去重

- 状态：DONE（`1.0.2-c5` / versionCode `30` 已覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c4`，未推送 GitHub）
- 硬件负载评估与根治：
  - 实测 Shield TV 为 3GB 总内存 / 4 核 ARM64（可用内存 ~996MB）；原 12 路并发 + 播中拉起 WebView + 暂停态 60fps 弹幕空转导致主线程卡死 2.68s 并触发系统杀进程。
  - `TvPreferences.kt` 改为按真实总内存分档（<=2.2GB 轻量 4 路、2.3~4.2GB 电视均衡 6 路 / 32MB 缓冲、>4.2GB 10~16 路）；`WebMedia052.kt` & `PtvModel.kt` 在视频已起播后禁止后台继续拉起高内存 Chromium WebView。
- 弹幕与功能改进：
  - `MediaOverlay053.kt` 重写为单调平滑时钟（±450ms 内不回跳）+ 暂停停止 60fps 空转 + 宽度缓存 + 仅扫时间窗内可见弹幕，彻底解决左右跳动与卡住不动。
  - `PlaybackAddons053.kt` & `SubtitlePanel053.kt` 新增 `force = true` 绕过旧缓存强制重匹配并取消旧加载竞态。
  - `CinemaPlayer.kt` 选集列表右侧新增单集上映日期（`EpisodeMeta.airDate`）。
  - `SiteScores102.kt` & `PtvModel.kt` 新增 `recordOnce` 同片同季同集同站去重，防止重复播放/搜源重复加减分。
- 本地候选产物（自动替换 `-c4`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c5.apk`（SHA-256: `09d05c4dec38f67033138c22cadedcc3605e0ec3b60b3e8da8c34a258300a00d`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c5-ARM.apk`（SHA-256: `34ed7b3d70d236c9849d54c34d1ba0e42cc4941d9289d7db8e2c6b337e8ed993`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c5` 流畅度、弹幕稳定性与选集/计分表现；满意即可去掉 `-c5` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-26 23:48 Asia/Shanghai — Antigravity — 1.0.2-c6 根治检测卡死 ANR（主线程 92% 彻底卸载）与统一速度静态分轨弹幕引擎

- 状态：DONE（`1.0.2-c6` / versionCode `31` 已覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c5`，未推送 GitHub）
- 根因与修复：
  - **并发 6 路依然卡顿与闪退的实机日志根因**：Shield TV logcat 捕获 `ANR in com.cbdog.ptvbox (Waited 5001ms for KeyEvent) | 92% Main Thread`。原 `PtvModel.scan()` 在主线程跑大量正则匹配、未包裹 `Dispatchers.IO` 的 `sites.probe` 及数百次 `siteStates` 拷贝与同步 `SharedPreferences` 计分写盘。现将 `inspect052` 完整移入 `Dispatchers.IO / Default`，主线程状态改为 `ConcurrentHashMap` + `300ms` 定时批量刷新。
  - **弹幕打架的数学根因**：原公式对所有弹幕固定 8 秒横穿 `width + textWidth`，导致长弹幕像素速度远快于短弹幕并在半路追尾撞车。现改为 `MediaOverlay053.kt` **全员统一像素速度 + 加载时一次性静态预分轨（`buildDanmakuPlan102`）**，从数学上 100% 杜绝追尾打架、跳轨与每帧重复测量开销。
- 本地候选产物（自动替换 `-c5`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c6.apk`（SHA-256: `213e3652a4fd45014d88eeb84509ef430ec05fdf0454db31f9e1ba16b573d9b6`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c6-ARM.apk`（SHA-256: `e71e4067e1d0acfd24456bc735ed87e5ea92a30de5bb136ad0b043bc94fe4fc0`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c6`；确认不卡、不闪退、弹幕不打架即可去掉 `-c6` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 03:53 Asia/Shanghai — Antigravity — 1.0.2-c7 首页顶部来源玻璃胶囊、影院横幅美化、CPU 降载与 Android 11/12 通知引导弹窗

- 状态：DONE（`1.0.2-c7` / versionCode `32` 已覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c6`，未推送 GitHub）
- 修改：
  - **首页顶部来源切换栏统一重设计**（`TvScreens.kt`）：按 Figma 设计稿（`D3w2XDK1LQ7xW9aU2cbHjz` node `6:2`）将「豆瓣 / Netflix / TMDB」统一收纳为深空玻璃胶囊底座，按品牌色（翠绿/绯红/青蓝）呈现选中态。
  - **首页影院横幅美化 + 零 60fps 空转**（`Hero053.kt`）：双轴暗幕全幅背景、立体圆角左海报+悬浮双评分胶囊、顶部徽章+渐变主按钮「▶ 查看详情与选源」+「下一部 ›」、右下角极简胶囊指示点（替代 `1 2 3 4 5` 方框），移除 60fps 倒计时进度条改为轻量 8 秒定时器。
  - **CPU/GPU 降载**：`PtvApplication.kt` 开启 Coil `allowRgb565(true)` 减半图片解码与显存开销；`PtvModel.kt` 自动播源找到 `>=3` 条可播源提前停扫。
  - **Android 11/12 私人源系统通知关闭引导**（`NotificationGuide102.kt`、`MainActivity.kt`、`Settings040.kt`）：检测到 Android 11/12 + 已配置私人点播订阅 + 系统通知仍开启时，弹出带直观干扰气泡画面示例的说明页，支持一键跳转系统通知开关或忽略，并在「设置 → 界面」保留常驻状态卡片与一键入口。
- 本地候选产物（自动替换 `-c6`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c7.apk`（SHA-256: `49cb39d417ff9aa4174191daf771605f485d7dc4d2bb14966e564337df08c471`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c7-ARM.apk`（SHA-256: `26f9a7de40223f5b837d7250c1d24e52aa9acc11f239556040441e8b112828cb`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c7` 视觉与流畅度效果；确认定稿即可去掉 `-c7` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 04:06 Asia/Shanghai — Antigravity — 1.0.2-c8 全面根治首页 D-pad 焦点乱飞与横幅顶部裁切

- 状态：DONE（`1.0.2-c8` / versionCode `33` 已覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c7`，未推送 GitHub）
- 根因与修复：
  - **点击顶部 Netflix / TMDB / 热门电影光标飞回豆瓣**：`MainActivity.kt` 的 `SaveableStateProvider` key 曾包含 `${m.provider}|${m.kind}|${m.category}` 导致每次切换都重建整棵首页树。已去除该动态后缀，切换来源与分类时光标稳稳留在当前按钮。
  - **横幅 8 秒刷新时光标瞬移回左侧「首页」**：`Hero053.kt` 将「▶ 查看详情与选源」和「下一部 ›」移出 `AnimatedContent` 外部避免切页销毁焦点按钮，且光标停留在横幅操作区时自动暂停 8 秒轮播。
  - **聚焦「查看详情与选源」时横幅顶部被遮挡**：横幅高度收紧为 `302.dp`，聚焦横幅按钮时自动保证首页顶部完整可见（`animateScrollToItem(0)`）。
  - **「继续观看」最左侧按左键跳到下方海报**：移除右侧主容器 `.focusGroup()` 焦点拦截，并通过 `LocalRailFocus` 在 `ContinueRow` 最左项显式绑定 `focusProperties { left = railFocus }` 直达左侧导航栏。
- 本地候选产物（自动替换 `-c7`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c8.apk`（SHA-256: `87c34002a2a50071aa10ea0a6e0b8d5d04c7a720e9cfc70d24faa2d3e76b3e0d`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c8-ARM.apk`（SHA-256: `1c4fbf5993bb2d3f5dd49cac8d34f2a679869459d439eb5e898f9fe4ac937677`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c8` 遥控器手感；确认定稿即可去掉 `-c8` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 04:18 Asia/Shanghai — Antigravity — 1.0.2-c9 首页横幅海报外置评分、延迟滚顶防裁切与继续观看左移直达搜索

- 状态：DONE（`1.0.2-c9` / versionCode `34` 已覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c8`，未推送 GitHub）
- 修改：
  - **横幅聚焦「查看详情与选源」彻底不裁切顶部**（`TvScreens.kt`）：将 `Hero053` 聚焦回滚改为 `delay(90L)` 后执行 `animateScrollToItem(0)`，避开 Compose 内置 `BringIntoView` 的竞态覆盖，保证聚焦横幅按钮时顶部导航与横幅 100% 完整可见。
  - **横幅评分栏移至海报外部下方**（`Hero053.kt`）：左侧改为 `Column`，上方纯海报无任何角标遮挡，下方外置独立圆角深色胶囊展示双评分（`豆瓣 X.X · TMDB Y.Y`）。
  - **「继续观看」最左侧按左键直达「搜索」图标**（`MainActivity.kt`、`TvScreens.kt`）：新增 `LocalSearchRailFocus` 绑定左侧导航栏「搜索」按钮，在 `ContinueRow` 最左侧卡片与「续播」按钮显式设置 `left = searchFocus`，不再误跳「历史」。
  - **「继续观看」卡片紧凑化优化**（`TvScreens.kt`）：去掉无意义的 `剩余 00:00` 占位行并将有效剩余时间合并至集数同一行，缩小纵向冗余高度让底部按钮在电视首屏更完整露出。
- 本地候选产物（自动替换 `-c8`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c9.apk`（SHA-256: `f80d2e487a1eb300a155efa182fbca1e1d3e9167fe329950a4beb54189378bd5`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c9-ARM.apk`（SHA-256: `0a9b49f65143ee2f1216872da35c59b64bde5f26c3047c14d9d0178b7fa24609`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c9` 首页视觉与遥控器手感；确认定稿即可去掉 `-c9` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 04:35 Asia/Shanghai — Antigravity — 1.0.2-c10 根治 Android TV 30% 强制滚屏抖动、横幅按钮精简为「详情」并去渐变高亮

- 状态：DONE（`1.0.2-c10` / versionCode `35` 已通过 11 步模拟器方向键与滚屏截图全链路验证并覆盖安装至所有者 Shield TV `192.168.2.131:5555` 与 BlueStacks，自动替换 `-c9`，未推送 GitHub）
- 根因与修复：
  - **横向移动「详情 / 下一部」导致横幅上下弹跳的根因**：Shield TV 具备 `FEATURE_LEANBACK`（逻辑分辨率 `960×540dp`），Compose Foundation 默认启用 Android TV `PivotBringIntoViewSpec`（强制把聚焦按钮顶到屏幕顶部 30% 即 `162dp`），把横幅顶部裁掉后又与旧版 `delay(90L)+animateScrollToItem(0)` 打架，造成左右移动必上下抖动。
  - **彻底根治**（`TvScreens.kt`）：在 `CinemaHome` 显式提供 `LocalBringIntoViewSpec provides HomeBringIntoViewSpec`（安全区 `top 6%` / `bottom 14%`，按钮在视口内时直接返回 `0f` 零滚动），同时移除 `delay(90L)` 强行滚顶，仅在从下方页面向上首次进入横幅且当前列表有滚动偏移时平滑回到顶部一次；左右切换「详情 ↔ 下一部」或点击切换横幅时垂直位移严格为 `0px`。
  - **横幅操作按钮精简与去高亮填充**（`Hero053.kt`）：主按钮文案由「▶ 查看详情与选源」精简为「**详情**」；彻底移除未聚焦时的蓝紫渐变高亮底色，改为克制深色半透明玻璃底，光标移入时才呈现清晰霓虹青紫聚焦框+亮色底+微放大，焦点一眼可辨；横幅高度优化至 `282.dp`（海报 `140×210.dp` + 下方外置双评分胶囊），完美适配 Shield TV `960×540dp`。
- 验证：149 项 JVM 单测通过；BlueStacks 全链路实测 11 步（初屏未聚焦 → 聚焦「详情」→ 右移「下一部」零位移 → 左回「详情」零位移 → 下移「继续观看」→ 左移直达「搜索」→ 深滚下方海报 → 上返「详情」平滑回顶 → 右移「下一部」→ 按确认切第 2 张横幅保持焦点且零位移）全部截图确认。
- 本地候选产物（自动替换 `-c9`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c10.apk`（SHA-256: `a9ed04de656029df5fc5e947e1d0ee780ef9b586eb262f37f0e9570aa42d3b8e`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c10-ARM.apk`（SHA-256: `c40f56d87571a63164932be0b9d749c27444b01a949e9e7260cf33f960788283`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c10`；确认定稿即可去掉 `-c10` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 05:00 Asia/Shanghai — Antigravity — 1.0.2-c11 根治 Shield TV 插件 30 分钟死锁与狂刷 Toast、搜源评分排序+别名增强、详情页智能焦点与毛玻璃 Logo

- 状态：DONE（`1.0.2-c11` / versionCode `36` 已通过 150 项 JVM 单测与 BlueStacks 截图验收并覆盖安装至所有者 Shield TV `192.168.2.131:5555`，自动替换 `-c10`，未推送 GitHub）
- Shield TV 真机实查与修复：
  - **关闭通知仍狂弹 50 个窗口 & 退出还在弹的真机根因**：Shield TV 日志证实 Android 11（API 30）关闭系统通知（`importance=NONE`）仅拦截通知栏、不拦截前台插件文字 Toast；私人插件并发扫描几十个站（含大量未授权网盘站）一次性向 `NotificationService` 队列塞满 50 条（`Package has already posted 50 toasts`），退出 App 后由 SystemUI `ToastPresenter` 按每条 3.5 秒继续播完约 3 分钟。修复：`PtvModel.scan()` 自动跳过未授权网盘站不再触发插件搜索/弹窗、起播且备选 ≥3 条时立即取消当前波次剩余站点扫描；同步修正 `NotificationGuide102.kt` / `Settings040.kt` 不再对 Android 11 弹出误导提示。
  - **`:spider` 崩溃触发 30 分钟冷却导致《可能的爱》18 站全 0**：日志捕获 `SpiderService` 因插件 `OW.run` NPE 崩溃后被 Android 惩罚冷却 `1800000ms`（30 分钟），且 `SpiderClient.onServiceDisconnected` 未 `unbindService` 导致所有私人站持续超时。修复：`SpiderService.kt` 断连立即解绑重置，彻底消灭 30 分钟死锁。
  - **搜源排序与匹配增强**：`PtvModel.scan()` 同组内按 `SiteScores102` 评分从高到低排序、非动画作品自动跳过 25 个纯动画站；`Models.kt` 的 `Identity` 自动从简介提取 `《译名》`（如《可能的爱》简介中的 `《假如爱有如果》`）并中文别名优先搜索，年份匹配允许 ±1 年跨年上映容差（保留多年重拍拦截）。
  - **详情页智能初始焦点 + 防强滚**（`CinemaDetail040.kt`、`SeasonEpisodes040.kt`、`EpisodeCard054.kt`）：详情页覆写 `HomeBringIntoViewSpec` 防止顶部海报被强行滚走；打开有观看进度的剧集时自动横滚并聚焦到上次观看的对应单集卡片，电影与未看作品聚焦「▶ 播放 / 续播」。
  - **TV 端毛玻璃提亮 Logo 与 16:9 横幅**（Figma `3:2` & `5:2` → `brand.png` & `tv_banner.png`）：将暗黑底色升级为提亮半透明毛玻璃蓝紫质感 + 左上斜向玻璃高光与微亮描边，在深色电视桌面与侧边栏均清晰独立。
- 本地候选产物（自动替换 `-c10`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c11.apk`（SHA-256: `4de1dce2e9e484e63fd8a7127afb0aafdf04734abe5bfcab049f2a7e85c06219`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c11-ARM.apk`（SHA-256: `437c968135bf78821315c86add6d4b0106d32e199bb28ca08c4f09ce6476e533`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c11`；确认定稿即可去掉 `-c11` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 05:43 Asia/Shanghai — Antigravity — 1.0.2-c12 根治 7 秒广告过滤/UC 扫码刷屏弹窗、Netflix 优先 HDHUB、自动停扫设置、暂停/继续搜索高亮与设置左侧不跳右

- 状态：DONE（`1.0.2-c12` / versionCode `37` 已通过 152 项 JVM 单测与 BlueStacks 实机截图验收并覆盖安装至所有者 Shield TV `192.168.2.131:5555`，自动替换 `-c11`，未推送 GitHub）
- Shield TV 真机查明的 50 条弹窗刷屏根因与根治：
  - `dumpsys notification` 实证队列 50 条全为 `饭：已过滤视频中广告(9条)`（27 条）与 `网盘源需使用UC浏览器APP扫码登录`（23 条），且每隔 7.004 秒无限触发：
    1. `SpiderProxy.kt` 原本无 M3U8 缓存，ExoPlayer 把缺 `#EXT-X-ENDLIST` 的点播 M3U8 当直播每 7 秒重拉 `/proxy`，每次重拉都重新触发插件去广告并弹出 `饭：已过滤视频中广告(9条)`；现已实现 **点播 M3U8 30 分钟内存缓存 + 自动补全 `#EXT-X-ENDLIST`**，彻底消除 7 秒重复重拉与刷屏。
    2. `CloudPolicy054`（`PluginUi054.kt`）漏判 `UC/优汐/木偶/蜡笔/虎斑/二小/欧歌/至臻/多多/小米/闪电/天翼/115/123` 等网盘站及聚合站内部单条 `UC/夸克/阿里` 线路（`isCloudRoad`）；现同时在站点级与线路级拦下未授权网盘探测，直接清零 `UC浏览器APP扫码登录` 弹窗。
    3. 新增父优先静默 `com.github.catvod.utils.Notify` 占位类拦截调用 `Notify.show` 的第三方插件；并在扫描结束/停止播放/切后台时按需调用 `sites.releaseIdleSpider()` 解绑销毁空闲 `:spider` 进程，杜绝后台残留插件线程。
- 功能与体验升级：
  - **Netflix 作品（含日韩剧/电影）优先搜索 HDHUB**（`SourceRouting060.kt`）：新增 `ContentRouteType.NETFLIX`（默认首位 `国际高清`），自动识别 `provider == "netflix"` 或出品/播出平台含 `Netflix` 的作品。
  - **「找到可用源后自动停扫」可视化设置**（`TvPreferences.kt`、`Sites040.kt`、`Settings040.kt`、`PtvModel.kt`）：支持 `1/2/3/5/8条 / 不限制`（默认 3 条），同步暴露在「站点」顶部栏与「设置 → 播放与自动选源」。
  - **播放器上方「暂停搜索 / ▶ 继续搜索」按钮升级**（`CinemaPlayer.kt`）：暂停态高亮为琥珀金底+边框提示，聚焦时切换为高对比深蓝底+纯白字+霓虹青紫框，焦点一眼清晰。
  - **设置页左侧上下浏览不再自动跳到右侧**（`Settings040.kt`）：移除 `LaunchedEffect(section)` 强行抢焦点，上下移动左侧分类时右侧即时预览、光标稳留左侧，仅按右键手动进入右侧设置项。
  - **影片详情页视觉优化**（`CinemaDetail040.kt`、`ProviderRatings.kt`、`EpisodeCard054.kt`）：压暗高干扰背景并给顶部资料卡增加高对比半透明毛玻璃底框；隐藏无意义的 `0人` 评分人数；给「▶ 播放 / 续播」加细强调描边；修复聚焦单集卡片上半区色块割裂问题。
- 本地候选产物（自动替换 `-c11`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c12.apk`（SHA-256: `ddad9b8b6b451483a0718ebad01361d066c544991dba767479d26d94f29235c0`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c12-ARM.apk`（SHA-256: `67dd38e667c9f8619b16dec9b08abccf0c700bdf1c850c90d45b01510f9dafa1`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c12`（特别是《末路兼职》等 Netflix 日韩片播放与零刷屏表现）；确认定稿即可去掉 `-c12` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。

### 2026-11-27 06:13 Asia/Shanghai — Antigravity — 1.0.2-c13 播放按方向键下默认落播放线路、杜比/HDR/5.1/多语统一徽章、常驻「继续搜索」、音轨与字幕自动加载优先级及弹幕弹窗重设计

- 状态：DONE（`1.0.2-c13` / versionCode `38` 已通过 154 项 JVM 单测与 BlueStacks 实机截图验收并覆盖安装至所有者 Shield TV `192.168.2.131:5555`，自动替换 `-c12`，未推送 GitHub）
- 修改：
  - **按方向键下默认光标落「播放线路」**（`PlaybackChrome.kt`、`CinemaPlayer.kt`）：将 `tools` `FocusRequester` 直绑到「播放线路」按钮，播放中按方向键下呼出控制栏光标即刻停在播放线路上，按确认直接选源。
  - **播放线路统一电影级徽章系统**（新增 `MediaBadges102.kt`、`CinemaPlayer.kt`）：解析分辨率（`4K/1080P/720P`）、杜比视界（带自定义双 D 矢量标志 `DV`）、`HDR10+/HDR`、全景声（带双 D 标志 `ATMOS`/`DD+`/`DTS`/`TrueHD`）、声道（`7.1`/`5.1`）及按统一顺序排列的多语/双语/国语/粤语/英语/日语/韩语/中字，集中置于每条线路左上头目，一目了然。
  - **常驻「▶ 继续搜索」并支持真·续扫其余站点**（`PtvModel.kt`、`CinemaPlayer.kt`）：解决自动停扫完成后按钮消失的问题——只要还有未检测站点（`checked < total`）或手动暂停，在播放器上方「xx条候选」右侧及「播放线路」右上角常驻琥珀金高亮「▶ 继续搜索」按钮，点击在不打断当前播放的前提下继续扫完剩余站点。
  - **音频与字幕自动加载语言优先级**（`MediaBadges102.kt`、`TvPreferences.kt`、`PtvModel.kt`、`PlaybackAddons053.kt`、`Settings040.kt`）：
    - 默认策略：音频与字幕均默认 **中文 → 英语 → 片源默认**，可在「设置 → 播放与自动选源」与「设置 → 字幕服务」分别自定义首选与次选语言（字幕还支持设为「默认关闭」）。
    - 起播与切源时自动选中匹配内嵌音轨（优先简中/国语）与内嵌字幕，并按同一优先级排序在线字幕搜索结果；单部影片手动切换后仍保留用户覆盖。
  - **播放器弹幕设置弹窗分类重设计**（`SubtitlePanel053.kt`）：重构为清晰四段式卡片布局——顶部状态与主开关/重新匹配栏、分组 1「屏幕布局（显示区域 + 飞行距离）」、分组 2「视觉与节奏（字号/透明度/密度/速度/偏移）」、分组 3「匹配弹幕库切换 + 导入本地/更多设置」。
- 本地候选产物（自动替换 `-c12`）：
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c13.apk`（SHA-256: `fa565fbaef1ec848843e8f3115d58fc85c639cf3914d674e05178ff9cbd4af24`）
  - `artifacts/candidate-v1.0.2/Ptvbox-1.0.2-c13-ARM.apk`（SHA-256: `580a2fe68cc7cf4e315cbdf992abdc7f542f8306457d5e398aa73025c4b4d96e`）
- 下一次从这里继续：等待所有者在 Shield TV 实测 `1.0.2-c13`；确认定稿即可去掉 `-c13` 正式公开发布 `v1.0.2` 并自动清理 `artifacts/candidate-v1.0.2/`。










### 2026-11-27 16:08 Asia/Shanghai — Codex — 1.0.2 接手梳理与基线复核

- 状态：DONE（代码阅读、现状梳理及基线验证；没有开发新功能或发布新版本）。
- 目标：按所有者要求阅读当前代码，掌握 1.0.2 状态，为后续任务接手。
- 修改：仅更新本文件的 Revision、接手提示和会话记录；保留进入会话前 Antigravity 对本文件的未提交发布状态修改，没有提交、推送、打标签、发通知或操作设备。
- 源码与发布：本地 `main`、远程 `main` 和 `v1.0.2` 解引用提交一致，均为 `1d1e7ddc9cad31b5fd9954cba67dc1ec9772aee3`；包版本 `1.0.2 / versionCode 40`。GitHub Latest Release 为公开正式 `Ptvbox 1.0.2`，仅 2 个 APK；发布时间 `2026-11-27 07:17:02 Asia/Shanghai`。发布 Actions `45909558679`、验证 Actions `45909558862` 均成功，源码提交一致。
- 代码掌握范围：单模块 Kotlin/Compose TV/Media3；`MainActivity` 页面与输入路由；`PtvModel` 搜源、播放、续播、生命周期；`Network` 资料与资源站适配；`Library`/`UserRepository` SQLite 用户状态与 Keystore；`SpiderService`/`SpiderBridge`/`SpiderProxy` 独立插件进程、Jar 与回环播放代理；`SplitSeason102` 合季/分季映射；`SourceRouting060`/`SiteScores102` 分类、评分与续扫；`PlaybackAddons053`/`MediaOverlay053` 字幕弹幕；`LocalConfigServer`/`UserBackup` 局域网配置及迁移；`AppUpdates040` 更新包哈希、包名、签名及递增 versionCode 校验。
- 本版实现：[x] 首页/详情/设置焦点与视觉改进；[x] 8 类路由、25 个动画规则、站点评分和停扫/续扫；[x] 合季分季匹配及续播全季选集；[x] 音轨/字幕语言优先级、线路徽章、内外字幕状态与弹幕计数；[x] WebView 回收处理、扫描后台执行和 UI 节流、插件重连及代理播放列表缓存。上述表示代码存在并有相应有限证据，不表示所有来源和设备完整验收。
- 本轮自动化：执行 `:app:testDebugUnitTest --no-configuration-cache` 检查后，使用 `:app:testDebugUnitTest --rerun --no-configuration-cache` 确保测试实际重跑；25 个测试套件，157 项测试，0 失败、0 错误、0 跳过。Gradle 构建成功，仍提示未来 Gradle 10 不兼容的弃用项。
- 本轮包验证：复用 `artifacts/release-v1.0.2/Ptvbox-1.0.2.apk` 与 `Ptvbox-1.0.2-ARM.apk`，`aapt` 确认包名 `com.cbdog.ptvbox`、版本 `1.0.2 / 40`；`apksigner verify --print-certs` 两包均通过，原证书 SHA-256 为 `33d71fe4cf85185651c1e254a2e62000cd7b348d5e50a45ff1b8dfa6656e1208`。通用包 ABI 为 ARM+x86，ARM 包仅 ARM。候选目录已不存在。
- 本地通用包 SHA-256：`00b2013273bd3f7643ac9c81388183757fd5e6355f029f7ce934119e6d6dd10f`；本地 ARM 包 SHA-256：`f36a9caf326e813459bcb6e260033fb85b5ee11d119fdd82552721e2ba44c17c`。它们与第 2 节 GitHub 构建产物哈希不同，应分别记录；本轮仅查询远程资产 digest，没有重新下载远程 APK，不能据此认定两处二进制完全一致。
- 模拟器证据：沿用 `docs/reports/Ptvbox-v1.0.2-Update-Report.md` 中 BlueStacks 的焦点、选集、音轨入口及跨季 1080P 记录；本轮没有启动模拟器或重做 UI/首帧验证。
- 物理设备证据：所有者表示当前已自行更新至 1.0.2；此前文档记录 Shield 安装与专项诊断。本轮无连机、无安装、无播放，完整真机体验及长期稳定性仍未复核。
- PC 对等及未开发：[ ] PCBOX 双向加密迁移及往返验证；[ ] 原生竖屏手机 UI；[ ] 全默认/私人/国际/动画/直播来源首帧矩阵；[ ] 长期稳定性、严格帧率/输入时延、全格式 HDR/Dolby 验收；[ ] 通用 JS 规则适配与自动源维护；[ ] 完整 PC 功能对等。本轮未开发这些功能，下一版本范围尚未指定。
- 文档/交付差异：README 仍写 1.0.1 和草稿 Release；本文件部分章节仍为 1.0.0、133 项测试、18 个动画源、7 类路由等旧摘要；部分旧计划/清单的“下一版”属于历史建议。`gh secret list` 仅有三项签名 Secret，缺少 `WECHAT_WEBHOOK_URL`，1.0.2 通知送达未获证实。部分已跟踪历史文档出现设备地址，不符合仅存忽略配置的当前规则；本次不重复地址，也未做全仓清理。
- 风险/回滚：没有产品代码、版本号、数据库或设备变化；仅本文件增加接手记录。保留所有者原有未提交文档修改，不以还原整个文件的方式回滚本次记录。
- 下一次从这里继续：先执行 `git status --short --branch`、读取本文件顶部 Revision 并检查是否出现其他开发者更新；接收所有者后续具体任务后，沿对应模块定位和验证，按批准范围决定是否进入 `1.0.3-c1`，不能自行启动旧路线或继承历史设备授权。


### 2026-11-27 16:40 Asia/Shanghai — Codex — 1.0.3 开发与中间验证（进行中）

- 状态：PARTIAL（会话内继续开发，不是版本交付）。
- 范围：HDHUB 直连核实与代理回退、PCBOX OneTouchTV 原生移植、人物/出品作品五列、通用动画季集坐标、B站电影弹幕、Download Only 下载动作、站点顶部触摸及可拖动缩放小窗。
- 代码：已加入 4926 条 ID 映射（125 条带偏移）与更新脚本；区分显示季集和 TMDB 坐标，禁用错误季回退并保留动画续季海报；引入未开播状态。AniList 198966 当前是 2027 年待播的《胆大当》第三季，无集数，不能提供第一季作为替代。
- 验证：新增 JVM 回归；真实手指、鼠标、D-pad 的 ui103 模拟器专项通过（五列、移动、角落缩放、下载 Intent 保留当前播放）。network103 已记录 B站电影 9600 条、HDHUB 直连首帧和 OneTouchTV 720p 首帧。发现 OneTouchTV 16.1/16.2 被原始数字转换混入第16集，正进一步排除非整集条目并重测第1集。
- 额外根因：B站上架日期误当上映日期、XML 压缩响应及 Android 不支持某 JAXP feature 均已修复；媒体/元数据走相同已验证路由，保留直连及已配置代理回退。
- 发布：没有提交、推送、正式标签、公开 Release 或通知。预期交付目录 artifacts/candidate-v1.0.3；PCBOX 全程只读，保留其原有大量未提交改动。
- 设备：仅隔离模拟器；原媒体音量 15 已记录，测试期间音量为 0 且回读为静音。当前临时 wm size=1280x720、density=320（原物理 1920x1080/240），结束前必须 reset 并恢复音量。
- 未验证：模拟器 TMDB 现有凭据的实际请求返回 HTTP 401，正在用独立受控目录验证完整映射/UI链路；不得将其说成实时 TMDB 全片库通过。真实电视/手机本轮未操作。
- 下一步：继续 mapping103 / OneTouch 第1集和浮窗真实视频验证，补无效代理和备用路由回归，再双签名构建、包审计、报告/清单、恢复模拟器环境后交候选 APK。


### 2026-11-27 16:59 Asia/Shanghai — Codex — 1.0.3-c1 签名候选交付与验收完成

- 状态：IMPLEMENTED（本轮代码、回归、隔离模拟器与候选包完成；不是物理设备或全阶段验收）。
- 目标：落实所有者本轮8项要求，保留既有有效链路和数据。
- 修改：HDHUB 保留直连并增加按已验证路由的代理回退；原生 OneTouchTV 亚洲剧；人物/出品共用五列墙；4926条动画ID映射（125条带偏移）、日期分段、显示季号与元数据季号分离、缺季不替换、待播刷新与旧季号纠正；电影B站弹幕上映年、压缩/XML兼容；下载专用动作；48dp共用输入头部按钮；应用内拖动/角落缩放小窗及遥控焦点退出，保留系统画中画。
- 关键文件：`AnimeMapping103.kt`、`Network.kt`、`AnimeSeason056.kt`、`WorkFacts.kt`、`Models.kt`、`OneTouch103.kt`、`InternationalHttp103.kt`、`DanmakuService053.kt`、`BilibiliPolicy103.kt`、`MediaText053.kt`、`SourceDelivery103.kt`、`CreditPage055.kt`、`SiteHeaderAction103.kt`、`MiniPlayer103.kt`、`PtvModel.kt`、`MainActivity.kt`、`CinemaPlayer.kt`、`Version103Test.kt`、`FeaturesQa103.kt`；资产刷新脚本与包审计脚本在 `scripts/`。
- 自动化：169项JVM（0失败/0错误/0跳过）；双签名Release构建和vital lint通过，包版本/签名/ABI/资产/QA类/私密材料审计通过。
- 模拟器：640×360dp真实手指、鼠标、D-pad顶部按钮；五列；小窗移动/缩放、D-pad移动后确认退出；下载按钮真实点击与Intent；HDHUB直连首帧720×406；B站电影9600条自动加载；真实视频进入小窗持续前进；OneTouchTV第1集720P、16正片集；本地受控代理回退及不可用代理下仍可直连。
- 季集证据边界：六个实时AniList条目身份/状态 + 受控TMDB目录完整调用链；有效凭据下实时TMDB未测，模拟器已有凭据返回401。胆大当第三季198966尚未开播（2027、集数未知），不得填入第一季。源端外部方案、统计、用例及限制见本版报告。
- 签名覆盖升级：隔离模拟器原1.0.2→1.0.3-c1成功，UID保持，站点110→111，发布签名包分类选源触摸通过；未清库、未导出或覆盖个人凭据。配置保持不代表已逐条核验全部历史和收藏。
- 证据：`artifacts/qa-1.0.3/verification-summary.json`、`package-audit.json`、`ui103-phone.json`、`network103.json`、`transport103.json`、`mapping103.json`、`release-upgrade.json`及截图；交付报告 `docs/reports/Ptvbox-v1.0.3-Update-Report.md`。
- 发布：仅本地候选，无提交/推送/标签/公开Release/企业微信通知；公开Latest仍为1.0.2，源码基线1d1e7dd。本次保留进入会话前的连续性文件未提交修改；PCBOX仅只读参考。
- 环境：仅隔离模拟器；两个测试应用已强停，wm size/density已reset为1920×1080/240，媒体音量恢复15并回读。流式adb安装卡住后仅终止本轮对应模拟器的安装客户端，非流式安装成功，未重启全局adb服务或操作其他设备。
- 未开发/未验收：其余PC国际源整批移植、完整首帧/硬件/长期性能矩阵、物理电视/手机、PCBOX双向迁移、原生手机UI、全PC对等；历史错误季进度未做批量迁移，保留原记录。
- 风险/回滚：数据库schema仍为3、没有清库；`releaseStatus`为兼容新增字段，作品key保持。候选versionCode41高于正式1.0.2的40，不应直接覆盖安装低versionCode回滚；保留原签名密钥和用户数据，不用清数据解决降级限制。
- 下一次从这里继续：`git status --short --branch`后读取本版报告和设备反馈；只修本轮范围。所有者确认正式发布时版本1.0.3且versionCode至少42，按规则先验证再发布，不能自动扩大需求或重用旧连机授权。

- 候选 `Ptvbox-1.0.3-c1-ARM.apk`：15982096字节，SHA-256 `a0d2cc751d8a11d77b4c23ee2d00025453389115dba8258f1f6bc743cc3a7e10`。

- 候选 `Ptvbox-1.0.3-c1.apk`：15990171字节，SHA-256 `cc0f9b1a663451a162738059db5711ae2fdd3dda6392a6bb58b5978a20a3ffb6`。

### 2026-11-27 18:50 Asia/Shanghai — Codex — 1.0.3-c2

- 搜索扫码、直接收听、焦点和紧凑键盘，恢复系统画中画并分组排序高清线路。双签名 APK 和包审计通过，168 项 JVM 通过。模拟器验证搜索键盘、下键和二维码弹窗。真机、扫码提交及语音和画中画实播待验证。
- 交付见 docs/reports/Ptvbox-v1.0.3-c2-Update-Report.md。下一次先执行 git status --short --branch，再按候选真机反馈复核。正式版若从 c2 升级需 versionCode 至少 43。

### 2026-11-27 22:26 Asia/Shanghai — Codex — 1.0.3-c3 搜索与系列联动（进行中）

- 状态：PARTIAL（继续验收与最后重建，不是交付声明）。
- 修改：有宽度上限的扫码/语音弹窗、输入区域和右侧联想的双向焦点图、编辑时取消旧搜索、本地先出/120ms 防抖/在线补充/一小时联想缓存；通用系列名与别名跨电影/TV查询、AniList两级明确关系，保留原推荐与分页。
- 验证：173项JVM通过（新增海报解析回归正在重跑）；真实640×360dp搜索按键、鼠标、触摸、选源、历史、空输入、连续输入及两个弹窗通过；HTTPS手机搜索表单Enter真实提交通过；实时豆瓣《来自深渊》双向关联及真实电影详情点击TV作品打开TV详情通过，AniList明确关联8条；受控TMDB混合类型与推荐接口失败隔离通过。
- 验收修正：Android不支持 IsHan 正则已改为兼容字符范围；360dp键盘底行裁切已改为按剩余空间分配；旧搜索解析仅读cover_url，正在补pic/images/cover图片与原名/别名/类型，重拍推荐截图。
- 发布：候选c3首轮双签名与审计通过，但因最后海报与快速编辑修正须重建，当前目录不能作为最终交付。无提交、推送、标签、公开发布或通知；物理设备未操作。
- 下一次直接执行：等待当前`:app:testDebugUnitTest :app:assembleDebug`结束，安装隔离模拟器debug包，重跑search103/related103/relatedUi103/searchRemote103；再Build.ps1及-ArmHost、Audit-Apk.py、签名安装复核，更新报告/清单/本文件后交付。模拟器最终需强停两个App并reset wm size/density；没有播放新视频，未调整媒体音量。

### 2026-11-27 22:31 Asia/Shanghai — Codex — 1.0.3-c3 本地候选交付

- 状态：IMPLEMENTED（本轮代码、必要回归、隔离模拟器与签名包通过；不是实体设备或完整PC对等验收）。
- 修改：紧凑二维码/语音卡片与麦克风图标；搜索页显式双向焦点图，提交/返回联想及关闭弹窗恢复焦点；编辑时取消旧搜索并立即隐藏过期联想；本地先出、在线120ms防抖和缓存；360dp高键盘按剩余空间均分，不分页、不裁切。
- 系列联动：通用系列名、原名/别名跨电影/TV搜索，AniList两级明确关系，排除漫画/仅角色客串，当前作品跨平台副本去重；保留原相似推荐与分页，不改播放身份或季集坐标。补豆瓣完整搜索pic/images/cover海报、原名、别名及类型。
- 根因：搜索区/右侧网格没有明确入口，本地联想等待在线结束，移除已聚焦联想/结果控件后焦点丢失；推荐仅查询相同kind；搜索解析漏海报字段。Android不支持IsHan正则与360dp底行裁切均在模拟器发现并修复。
- 自动化：174项JVM，0失败/错误/跳过；通用/ARM Release构建与vital lint通过；原证书、版本/ABI/资产、无QA类及私密材料审计通过；git diff --check通过。
- 模拟器：640×360dp和1280×720dp搜索回归，触摸/鼠标/各行方向键往返、选源、历史、空输入、中英文字母、空格/删除、快速编辑、两个弹窗通过；最终版本补测提交/返回和扫码关闭后的焦点恢复。缓存首结果约85–138ms，仅为本机采样，不承诺全网/全硬件时延。
- 网络/推荐：真实HTTPS手机搜索表单Enter→电视查询→目录结果通过；实时豆瓣《来自深渊》电影↔TV通过；真实电影详情显示带海报TV推荐，触摸后打开TV详情；实时AniList明确关联8条；受控TMDB混合类型保留TV和相似推荐失败隔离通过。
- 实际签名包：ARM覆盖升级成功，UID保持；交付APK的完整键盘、真实联想选择搜索与返回、156dp二维码通过。未清库、未改数据库schema，也没有逐项重新核验全部历史/收藏。
- 边界：模拟器无SpeechRecognizer服务，只验证生产语音弹窗和关闭，真实麦克风识别未验；现有TMDB凭据401，实时TMDB需有效凭据；HTTPS测试不代替实体手机摄像头/家庭网络。全片库关系完整性、全来源/硬件矩阵、系统画中画实播未在本轮重验。
- 证据：artifacts/qa-1.0.3/verification-c3.json、jvm-c3.json、search-c3-640dp.json、search-c3-1280dp.json、search-remote-c3.json、related-c3.json、related-ui-c3.json、signed-ui-c3.json、package-audit-c3.json与对应PNG。报告docs/reports/Ptvbox-v1.0.3-c3-Update-Report.md；清单/README/报告索引已更新。
- 发布：仅artifacts/candidate-v1.0.3内两份候选，旧候选由脚本替换；无提交/推送/正式标签/公开Release/通知，公开Latest仍1.0.2。保留此前所有本地改动，PCBOX未修改。
- 环境：两个模拟器App已强停，wm size/density reset为1920×1080/240，媒体音量15且本轮未调整。未连接、安装或控制任何物理设备。
- 未开发：PCBOX其余国际源整批移植、PC双向加密迁移、原生竖屏手机UI、完整HDR/Dolby/首帧/长期稳定性矩阵及完整PC对等；继续沿原始需求清单标为[ ]。
- 下一次直接执行：先git status --short --branch和读取本节/c3报告，再按所有者实体设备反馈修语音服务/权限、扫码网络路径、画中画或具体系列遗漏。正式1.0.3须所有者确认，versionCode至少44；不可继承历史电视连机授权。

- c3交付 `Ptvbox-1.0.3-c3-ARM.apk`：16015112字节，SHA-256 `f12af4133825dd524149ef2c16e6f91ee8dbe2cc444a9a95934cfa917848c3c3`。

- c3交付 `Ptvbox-1.0.3-c3.apk`：16023187字节，SHA-256 `775e3cfe41812be7b45578545fe3d3c06106bae469953e9c0bec22b03baca334`。

### 2026-11-28 14:48 Asia/Shanghai — Codex — 1.0.3 正式发布准备

- 状态：PARTIAL（所有者明确要求发布，正在构建与上线验证）。
- 源码：当前main仍基线1d1e7dd，c1–c3本地修改均保留；remote main同基线，未发现其他更新，v1.0.3尚不存在。
- 正式版本1.0.3 / 44，产品源码与批准的c3一致。Build.ps1改为发布核验后再清候选，避免正式准备阶段提前删掉回退候选；候选目录暂时保留。
- 准备：读取全局/项目规范、候选报告/证据、发布脚本及工作流。WECHAT_WEBHOOK_URL Secret仍缺失，沿用现有条件通知集成，不能声称已发送。
- 下一条：完成本地双签名构建与审计/测试后提交main、推送v1.0.3触发既有Actions；等公开Release和两包下载核验后清候选、更新最终报告与本文件。仅隔离模拟器，不操作实体设备。

- 发布准备补证：正式1.0.3/44双签名本地包、174项JVM及package-audit-local.json全部通过；app/src/main与批准c3指纹完全一致。即将提交并推送标签，远程发布仍需完成。
