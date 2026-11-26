# Ptvbox 项目进度与交接规范

Revision: `PTVBOX-2026-11-26.2`  
Last updated: `2026-11-26 18:26 Asia/Shanghai`  
Last updated by: `Codex`  
Current implementation checkpoint: `1.0.0 / versionCode 24 / GitHub draft Release verified`

本文件是Ptvbox当前状态的入口。它帮助任何开发者在不依赖聊天历史的情况下接手到一半的工作。全局流程见`C:\Users\cbdog\.codex\PROJECT-CONTINUITY.md`。

## 1. 项目目标与边界

Ptvbox是PCBOX功能集的Android客户端。目前交付物是支持遥控器、鼠标和触摸的横屏ATV APK，不是已完成的原生竖屏手机App。浏览器手机配置台也不等于原生手机App。

当前范围：

- Android TV主界面、搜索、详情、播放、直播、收藏、历史和设置
- TVBox默认站、私人订阅、国际站和18个动画规则
- 字幕、弹幕、网盘授权、手机局域网配置
- 豆瓣、TMDB、IMDb、Bangumi、AniList之间的资料补充和身份匹配
- GitHub标签自动构建和草稿Release

明确不在当前完成声明内：

- PCBOX与Ptvbox完整双向迁移
- 原生手机UI
- 全站、全直播、全动画首帧覆盖
- 所有第三方插件非标准自绘窗口治理
- 完整PC功能对等

## 2. 当前发布状态

| 项目 | 当前值 |
|---|---|
| 可见版本 | `1.0.0` |
| Android versionCode | `24` |
| 包名 | `com.cbdog.ptvbox` |
| minSdk / targetSdk / compileSdk | 26 / 35 / 37 |
| 分支 | `main` |
| 远程 | `https://github.com/cbdoglolz/Ptvbox.git` |
| 标签 | `v1.0.0` |
| GitHub Actions | `Ptvbox release`成功 |
| GitHub Release | 草稿，名称/正文均为`Ptvbox 1.0.0` |
| Release附件 | 通用APK、ARM APK，共2个文件 |
| 物理电视验收 | 未完成 |

GitHub下载附件的实际SHA-256：

```text
4a5bc7a3c290877dfd48706d2fe1fb19e28df51ed26482379b922d2fdd232bbb  Ptvbox-1.0.0-ARM.apk
7efb1e4553323ba4581d264400fd69c0988392429ddd05db3f48113a1687c60c  Ptvbox-1.0.0.apk
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

## 6. 当前已完成和验证

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
6. `Ptvbox release`工作流构建通用/ARM包并创建草稿Release
7. 核验GitHub Release标题和正文只有`Ptvbox X.Y.Z`
8. 核验附件只有两个APK；不得上传日志、测试报告、截图、源码归档或私人数据
9. 下载实际附件，验证包内版本、签名和哈希
10. 保持Release草稿，由所有者发布

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

1. 先读本文件、`AGENTS.md`、`PROGRESS-CHECKLIST.md`、最新报告和`git status`
2. 检查所有者是否提供了真实电视上的私人播放截图/日志
3. 若提示仍出现，先区分Toast、应用内`notice`和第三方自绘悬浮窗；不要继续盲目扩大Toast方法名
4. 用户反馈优先于继续扩展站点；当前没有所有者确认不要提前开发原生手机或大规模云同步
5. 若进入下一版本，选择明确的用户请求范围，更新版本、测试、GitHub Release和本文件

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

