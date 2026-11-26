# 0.2.0 数据与兼容契约

- 数据库版本2。`media_v2`以外部provider/type/id定位作品，持久UUID用作内部身份。标题变化不改变已有UUID，不按同名跨豆瓣/TMDB自动合并。`aliases_v2`保留旧provider:id与新键的映射；歧义旧键不猜测对应类型。
- `episodes_v2`持久单集UUID；已知普通集使用作品/季/集号匹配，未知编号使用来源身份和名称形成临时条目。电影使用单个正片进度。`episode_sources_v2`保存目录来源映射。旧未知编号无法还原来源作品ID时保留legacy来源标识，不假装已可靠跨站统一。
- `favorites_v2`独立于`state_v2`。未标记为空，其他仅三态；`progress_v2`的单集done不把整剧标成已看。`hidden_v2`只控制继续观看显隐。
- 旧整数`position`、`duration`保持毫秒含义。结构列为`position_ms`/`duration_ms`；`syncSnapshot()`给出稳定作品/单集ID、独立状态和三位小数秒字符串。`updated_ms`是UTC epoch毫秒，与播放时长分开。
- `changes_v2`在用户变更的同一事务保存本机device_id、session_id、单调revision和变更类型（含清除/删除）。revision不是跨设备全局时钟。本轮没有开启联网同步或冲突合并。
- 目录缓存仅含来源key、名称和集名，不保存认证请求头或临时播放URL；选集播放时仍经现有搜源/探测链路确认链接。

## 升级、失败与回退

首次打开v1库，备份旧works/favorites/progress至应用私有 `files/migration-backups/<库名>.v1.json`，然后在SQLiteOpenHelper升级事务中创建/迁移v2表。旧表不删。先从作品和嵌在进度中的作品恢复身份，再迁移收藏。

坏JSON或歧义键记录到`migration_issues`，原始记录仍在旧表；不能恢复曾被v1相同主键覆盖的历史。备份或数据库升级失败会阻止升级，不删除数据库重来。模拟器已验证事务失败后version仍为1、旧记录仍在。

v2首次使用后的新变更不写回v1表。不能直接降级安装0.1.0访问v2数据库；需要经用户确认的备份恢复与新数据处理方案。Keystore别名、加密凭据存储和签名身份没有更换。个人数据与迁移备份不进入APK或公开仓库。

未来完整跨提供方人工关联、未知季/集结构化编辑及同步传输属于后续工作；本轮不以未使用的扩展类型宣称这些能力已完成。
