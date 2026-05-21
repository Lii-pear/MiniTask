# Baseline Profile 生成说明

## 已接入内容
- 新增 `:baselineprofile` 模块（Macrobenchmark + BaselineProfileRule）
- 已接入 `targetProjectPath = ":app"`，会对 `com.example.minitask` 进行采集
- 采集脚本会覆盖冷启动、周/月切换相关滑动路径

## 生成命令（真机 USB 连接后执行）
```powershell
.\gradlew.bat :baselineprofile:connectedReleaseAndroidTest
```

## 重新打包并应用新 profile
```powershell
.\gradlew.bat :app:assembleRelease
```

## 说明
- 当前工程已经有 `app/src/main/baseline-prof.txt` 与 `startup-prof.txt`，可作为兜底配置。
- 如果你后续想完全自动覆盖 `app/src/main/baseline-prof.txt`，我可以下一步再加“采集后自动回填”任务。
