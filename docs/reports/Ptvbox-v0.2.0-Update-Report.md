# Ptvbox 0.2.0 更新报告

2026年11月20日本地交付。实际实施了数据v2、电视首页/导航、播放前独立目录和新详情页，不是重发0.1.0。

- [完整变化与安装说明](../releases/0.2.0/UPDATE.md)
- [界面](../releases/0.2.0/UI.md) / [14个代码文件变化](../releases/0.2.0/CODE.md)
- [自动化、模拟器与真实网络结果](../releases/0.2.0/TEST.md)
- [性能原始结果与未达标项](../releases/0.2.0/PERFORMANCE.md)
- [数据迁移与保留契约](../releases/0.2.0/DATA-V2.md)
- [已知问题与PC迁移缺口](../releases/0.2.0/KNOWN-ISSUES.md)
- [下一轮边界](../releases/0.2.0/NEXT.md)

APK：`D:\Ptvbox\artifacts\release-v0.2.0\Ptvbox-0.2.0.apk`，11,581,118字节；SHA-256 `7a437eccddce581e45675fbf494a97b6a75743a4d850991ffcf8d5a0aa474227`。

16项JVM及12项Android数据检查通过；模拟器实际读目录、首帧、浏览保留播放和6类ATV频道验证通过。真实机顶盒、原生手机、性能达标与PC完整功能对等均未验收；当前ATV系统后台仍为保存并暂停，完整停止/释放留在0.3.0。

未上传、未连接真实盒子、未更改Windows PCBOX。旧0.1.0包及历史报告保留。
