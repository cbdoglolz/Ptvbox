# 0.2.0 代码变更清单

相对本轮开发前保存的源码快照，应用内容发生变化的文件共14个；没有凭Git未跟踪状态把所有旧文件算作新增。原始哈希清单：`artifacts/qa-0.2.0/source-changes.json`。

| 文件 | 实际变化 |
|---|---|
| app/build.gradle.kts | versionCode 2、versionName 0.2.0；保留包名/签名配置 |
| Domain.kt | 三态、ID类型、十进制时长单位及用户快照 |
| Library.kt | v2表、迁移/私有备份/保留区、身份、目录、索引、状态及同步快照 |
| UserRepository.kt | 后台串行写入、状态Flow、初始化等待和错误反馈 |
| EpisodeCatalogRepository.kt | 有界独立目录加载、来源分组、缓存、刷新/取消；独立详情ViewModel |
| Models.kt | 类型化作品键、旧键、人物/背景、来源身份、明确单集匹配 |
| Network.kt | 版本User-Agent、真实演职人员/背景字段及TMDB关联资料；原搜源规则保留 |
| PtvModel.kt | UI消费后台快照，首页行、独立详情、状态动作、配置后台读取、冷启动续播等待 |
| MainActivity.kt | 路由/覆盖导航、页面状态、UI数据路径；播放器外部留白移除 |
| TvScreens.kt | 72dp导航、260dp Drawer、首页横行、继续观看及新详情目录UI |
| HomeChannels.kt | 旧深链相关身份与移除偏好兼容，Watch Next旧行复用 |
| DomainTest.kt | 新增5项数据/匹配回归 |
| QaInstrumentation.kt | Debug专用v2测试入口、截图与真实网络播放检查 |
| V2Qa.kt | Debug专用Android SQLite迁移与ATV provider兼容测试 |

原32站默认配置、公钥、HomeArtwork与只读海报Provider、应用Manifest、Keystore实现和构建签名脚本的内容未改。源码快照中的app/root两个build.gradle.kts在ZIP内同名，核对时按各自内容还原路径，避免错报。

TMDB附加资料使用其官方Details接口的append_to_response能力；参考[电影Details](https://developer.themoviedb.org/reference/movie-details)和[剧集Details](https://developer.themoviedb.org/reference/tv-series-details)。没有个人Key就不请求受保护TMDB资料，不内置他人凭据。
