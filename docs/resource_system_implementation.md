# iLunyu 动态资源系统实现方案

本文档规定 `ilunyu-android-native` 的译注与试题资源如何编译、发布、下载、安装、切换、更新和恢复。实现完成后，用户可以在应用内管理官方资源，也可以从 GitHub 下载资源包并通过系统文件选择器安装；资源变更会即时反映到阅读、学习、搜索和关联试题中。

## 1. 目标与最终效果

动态资源系统由资源仓库、官方注册表、Android 资源管理器和业务 Repository 共同组成。

```text
资源仓库源码
  -> GitHub Actions 校验与编译
  -> 本仓库 GitHub Release
       - resource.ilunyupack
       - release.json
       - resource.ilunyupack.sha256

官方 registry.json
  -> 汇总所有官方资源的名称、版本和 Release 下载地址

Android 应用
  -> 获取 registry.json
  -> 下载 Release 或导入本地 .ilunyupack
  -> 校验并原子安装
  -> 切换活动资源快照
  -> 阅读、学习、搜索和关联试题同步刷新
```

资源按内容边界独立安装：

- 每个译注版本对应一个 `edition` 包，例如 `edition.yangbojun`。
- 每个学年题库对应一个 `exercise` 包，例如 `exercise.2025-2026`。
- APK 内置资源登记为内置包，使首次启动和离线状态下具备完整的基础阅读与学习能力。
- 下载版本按相同 `packageId` 更新内置版本或已安装版本；不同学年的题库可以同时启用。

用户最终可以完成以下操作：

- 查看可用、已安装、可更新和正在使用的资源。
- 下载或更新官方资源，并观察下载与安装进度。
- 在已安装的译注之间切换。
- 启用或停用各学年题库。
- 从系统文件选择器导入 `.ilunyupack`。
- 卸载下载资源或恢复内置资源。

## 2. 资源协议

### 2.1 包格式

资源包扩展名统一为 `.ilunyupack`，文件内容为 ZIP，MIME 类型为：

```text
application/vnd.ilunyu.resource+zip
```

每个包的根目录必须包含 `manifest.json` 和 `manifest.sig`。清单定义包的身份、兼容性、版本、来源和全部文件哈希；签名文件保存规范化 `manifest.json` 的 Ed25519 签名：

```json
{
  "packageFormat": 1,
  "contentSchema": 1,
  "packageId": "exercise.2025-2026",
  "kind": "exercise",
  "name": "2025—2026 学年试题",
  "versionName": "1.1.0",
  "versionCode": 4,
  "minAppVersionCode": 1,
  "sourceRepository": "https://github.com/ilunyu/ilunyu-exercise-2025-2026",
  "license": "CC-BY-SA-4.0",
  "createdAt": "2026-09-20T10:00:00Z",
  "files": {
    "content/index.json": "sha256:...",
    "content/search.json": "sha256:...",
    "content/items/202509-example.json": "sha256:..."
  }
}
```

字段规则如下：

| 字段 | 规则 |
| --- | --- |
| `packageFormat` | 资源容器协议版本，由安装器判断能否解析 |
| `contentSchema` | 内容 JSON 协议版本，由对应 Repository 判断能否读取 |
| `packageId` | 全局稳定且与资源类型绑定，更新时保持不变 |
| `kind` | `edition` 或 `exercise` |
| `versionName` | 面向用户显示的语义化版本号 |
| `versionCode` | 同一 `packageId` 内严格递增的整数 |
| `minAppVersionCode` | 可以安装该包的最低应用版本 |
| `files` | 包内全部内容文件及其 SHA-256 |

译注包采用以下结构：

```text
manifest.json
manifest.sig
content/catalog.json
content/search.json
content/pian/<slug>.json
content/chapters/<chapterId>.json
```

题库包采用以下结构：

```text
manifest.json
manifest.sig
content/index.json
content/search.json
content/items/<exerciseId>.json
```

`catalog.json`、篇、章、搜索索引和题目文件沿用应用现有的数据字段含义。资源编译器负责把仓库源码转换为上述运行时结构，并对 ID 唯一性、必填字段、章节引用和索引一致性进行校验。

### 2.2 官方注册表

官方注册表发布为静态 JSON，固定入口为应用配置中的 `RESOURCE_REGISTRY_URL`。注册表采用统一的 `packages` 数组：

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-09-20T10:00:00Z",
  "packages": [
    {
      "packageId": "edition.yangbojun",
      "kind": "edition",
      "name": "杨伯峻《论语译注》",
      "description": "杨伯峻《论语译注》资源",
      "versionName": "1.2.0",
      "versionCode": 3,
      "minAppVersionCode": 1,
      "size": 184520,
      "sha256": "...",
      "sourceRepository": "https://github.com/ilunyu/ilunyu-edition-yangbojun",
      "releasePageUrl": "https://github.com/ilunyu/ilunyu-edition-yangbojun/releases/tag/v1.2.0",
      "downloadUrls": [
        "https://github.com/ilunyu/ilunyu-edition-yangbojun/releases/download/v1.2.0/resource.ilunyupack"
      ]
    }
  ]
}
```

应用按照 `packageId` 和 `versionCode` 比较远程与本地状态。`downloadUrls` 按顺序列出官方地址和可用镜像；下载器依次尝试。注册表缓存到应用私有目录，并记录 `ETag`、获取时间和内容哈希。

## 3. 自动发布链路

每个译注或题库资源仓库配置同一套复用工作流。发布由版本标签触发，执行顺序如下：

1. 检出资源源码并安装统一资源编译工具。
2. 校验源码格式、标识符、引用关系、版权信息和版本字段。
3. 生成包内运行时 JSON、索引和 `manifest.json`。
4. 以可复现参数生成 `resource.ilunyupack`。
5. 使用官方 Ed25519 私钥签名规范化 `manifest.json`，将 `manifest.sig` 写入包中，并计算最终包文件的 SHA-256。
6. 创建 GitHub Release，上传包、哈希和 `release.json`。
7. 向注册表仓库发送 `repository_dispatch`，携带本次 Release 元数据。
8. 注册表仓库校验 Release 后更新 `registry.json` 并发布到 GitHub Pages。

`release.json` 保存与注册表条目一致的单包发布信息，便于注册表自动汇总和人工核验。资源仓库中的 Pull Request 运行同一套内容校验与试打包任务；正式版本标签对应一个确定且可重复生成的资源包。

资源编译工具作为 `lunyu` 体系内的共享命令维护，并提供统一入口：

```text
build-resource validate <source-directory>
build-resource package <source-directory> --output resource.ilunyupack
build-resource inspect resource.ilunyupack
```

## 4. Android 端实现

### 4.1 包级存储与安装记录

资源文件位于应用私有目录：

```text
filesDir/resources/
  downloads/
  staging/
  packages/
    <packageId>/
      <versionCode>/
        manifest.json
        content/...
```

Room 保存资源的安装与活动状态：

```text
installed_resource
  package_id
  kind
  version_code
  version_name
  location_type       bundled | downloaded
  root_path
  sha256
  installed_at

resource_activation
  package_id
  active_version_code
  active_location_type
  enabled

resource_preference
  active_edition_package_id
  content_generation

chapter_exercise_cross_ref
  chapter_id
  exercise_package_id
  exercise_id
```

`installed_resource` 使用 `package_id + version_code + location_type` 作为联合主键。APK 内置资源在数据库初始化时登记为 `bundled`，下载资源以版本目录登记为 `downloaded`。`resource_activation` 指定每个包当前解析到的版本；`edition` 由 `active_edition_package_id` 选中当前译注包，`exercise` 由每个包的 `enabled` 状态决定是否参与题库、搜索和章节关联。

存储层提供包级接口：

```kotlin
interface ResourcePackageStorage {
    fun open(packageId: String, relativePath: String): InputStream
    fun exists(packageId: String, relativePath: String): Boolean
    fun list(packageId: String, relativePath: String): List<String>
    fun resolve(packageId: String): ResolvedPackage
}
```

`resolve()` 根据数据库中的活动记录返回指定包的确定版本。Repository 在一次读取过程中持有同一 `ResolvedPackage`，保证目录、索引和详情来自同一版本。

### 4.2 下载、校验与原子安装

`ResourceManager` 负责注册表同步、下载、导入、安装、切换、卸载和恢复。安装状态通过 `StateFlow<ResourceOperationState>` 提供给 UI。

一次完整安装按以下事务执行：

1. 在 `downloads/` 创建 `.part` 文件，通过 OkHttp 流式写入并持续报告进度；服务器支持范围请求时使用 `Range`、`ETag` 和 `If-Range` 续传。
2. 下载完成后计算整包 SHA-256；解压后使用应用内置官方公钥验证 `manifest.sig`，并以清单中的哈希验证全部内容文件。
3. 在唯一的 `staging/<operationId>/` 目录解压，校验路径、文件数量、解压总尺寸和压缩比。
4. 解析 `manifest.json`，校验包格式、内容协议、应用版本、包 ID、文件清单、逐文件哈希和业务数据结构。
5. 将 staging 目录在同一文件系统中重命名为 `packages/<packageId>/<versionCode>/`。
6. 在一个 Room 事务中登记新版本、切换活动版本、重建相关索引并递增 `content_generation`。
7. Repository 收到新资源快照后刷新缓存，界面继续显示同一路由并呈现新内容。
8. 安装成功后清理下载文件和 staging；每个包保留当前版本与前一个可恢复版本。

本地导入通过 Android Storage Access Framework 的 `ACTION_OPEN_DOCUMENT` 取得 URI，随后进入同一校验和安装事务。下载与导入使用相同的安装器和结果模型。

卸载下载版本时，资源管理器在 Room 事务中选择该包的上一有效版本；存在内置版本时自动恢复内置版本。恢复内置资源会统一切换所有具有内置版本的包，并清理相应下载版本。应用的备份规则将 `filesDir/resources/` 排除在系统云备份之外。

### 4.3 Repository 与运行时索引

应用使用以下资源快照驱动业务层：

```kotlin
data class ContentSnapshot(
    val generation: Long,
    val activeEdition: ResolvedPackage,
    val enabledExercisePackages: List<ResolvedPackage>
)
```

`ResourceRepository` 暴露 `StateFlow<ContentSnapshot>`。安装、卸载、切换译注和启停题库都会生成新快照。

业务 Repository 按以下方式接入：

- `AnalectsRepository` 从 `activeEdition` 读取 `catalog.json`、篇、章和译注搜索索引；缓存键包含 `packageId`、`versionCode` 和资源路径。
- `ExerciseRepository` 合并全部已启用题库包的 `index.json` 与 `search.json`；题目详情根据索引记录的 `packageId` 定位。
- 搜索模块查询当前译注及全部已启用题库，在结果对象中保存资源包身份与内容 ID。
- 题库安装、更新、启用或停用时，根据题目材料中的正数 `sourceId` 重建 `chapter_exercise_cross_ref`；章阅读页通过该表获取关联题目。
- ViewModel 收集 Repository 的响应式数据流，使当前页面、筛选项、搜索结果和关联题目随 `content_generation` 更新。

## 5. 用户界面

设置页新增“资源与题库管理”，页面使用两个分组展示译注与试题资源。每一项显示名称、版本、大小、来源和状态，并根据状态提供下载、更新、启用、停用、切换、卸载和重试操作。页面顶部提供检查更新和导入资源包操作，恢复内置资源放在页面末尾的独立操作区。

下载过程显示确定进度、已下载大小和总大小；校验、安装、建立索引分别显示当前步骤。操作完成后，该项状态即时更新，阅读和学习页面同步获得新资源。

章阅读页顶栏的译注名称可打开 Bottom Sheet，列出全部已安装译注。选择后切换 `active_edition_package_id`，尽量保持当前篇章位置；目标译注缺少对应章节时进入其总览页并显示说明。

通过文件管理器打开 `.ilunyupack` 时，应用进入导入确认页，展示名称、类型、版本、来源、签名状态、内容数量和所需空间。用户确认后执行安装，并在完成页提供“开始阅读”或“查看题库”入口。

网络更新检查由用户主动操作和 WorkManager 周期任务共同触发。周期任务只同步注册表并发送可更新通知；资源下载由用户在资源管理页确认。网络请求使用 HTTPS，支持超时、重试、取消和从候选下载地址继续。

## 6. 实施顺序

实现工作按照依赖关系连续完成，并以本文件规定的最终结构交付：

1. 在共享编译工具中实现资源校验、包生成、包检查和可复现构建，迁移现有译注与各学年题库仓库。
2. 为资源仓库和注册表仓库配置 GitHub Actions，发布首批正式 Release 与官方 `registry.json`。
3. 在 Android 工程中加入网络权限、OkHttp、WorkManager、资源数据模型、Room 表和备份规则。
4. 实现 `ResourcePackageStorage`、`ResourceManager`、下载续传、签名校验、受限解压和原子安装事务。
5. 将 APK assets 登记为内置包，按包级接口改造 `AnalectsRepository`、`ExerciseRepository`、搜索和关联题目索引。
6. 将资源状态接入 ViewModel 和现有页面，完成缓存代际切换与即时刷新。
7. 完成资源管理页、译注切换 Bottom Sheet、本地导入确认页、更新通知和错误恢复交互。
8. 使用正式签名资源执行端到端测试，并发布包含动态资源系统的应用版本。

## 7. 验收标准

### 7.1 发布与发现

- 在译注仓库创建版本标签后，GitHub Release 自动生成三个规定文件，包内结构、清单与内嵌签名通过检查命令。
- 在题库仓库创建版本标签后，注册表自动出现对应 `packageId` 和准确的版本、大小、哈希、签名及下载地址。
- 全新安装的应用在离线状态下可以使用 APK 内置译注与题库；联网检查后可以列出注册表中的全部兼容资源。

### 7.2 下载与安装

- 用户下载资源时可以看到持续更新的进度；中断后再次下载可从有效的部分文件继续。
- 下载完成后，应用验证整包 SHA-256、官方签名、manifest、逐文件哈希和内容结构。
- 在下载中断、解压中断、校验失败和索引建立失败的测试中，应用继续读取安装前的完整资源版本。
- 安装成功后，活动版本通过单个数据库事务切换，资源管理页与业务页面即时显示新版本。
- 从系统文件选择器导入同一资源包可以得到与在线下载一致的安装结果。

### 7.3 业务一致性

- 切换译注后，总览、篇阅读、章阅读和阅读搜索均来自所选译注，当前存在的篇章位置得到保持。
- 同时启用多个学年题库后，学习列表、筛选条件、试题搜索和详情页包含全部已启用内容，排序规则保持一致。
- 安装包含新 `sourceId` 关系的题库后，对应章阅读页即时出现关联题目；停用或卸载该题库后关联结果同步更新。
- 更新资源时，收藏、标签和其他以稳定内容 ID 保存的用户数据继续对应到更新后的内容。
- Repository 的缓存随 `content_generation` 切换，同一路由不会混合读取两个资源版本。

### 7.4 管理与恢复

- 资源管理页准确区分内置、已安装、正在使用、已停用和可更新状态。
- 卸载活动译注的下载版本后自动恢复该译注的内置版本；切换、启停和恢复操作在应用重启后保持。
- 恢复内置资源后，阅读、学习、搜索和关联题目均来自内置资源快照。
- 下载目录、staging 目录和旧版本清理符合保留策略，系统备份中不包含可重新下载的资源文件。

### 7.5 安全与兼容

- 修改包内任意文件、包哈希或签名后，安装器拒绝激活该包并显示明确错误。
- 包含越界路径、绝对路径、符号链接、超限文件数、超限展开体积或异常压缩比的包无法进入正式目录。
- `packageFormat`、`contentSchema` 或 `minAppVersionCode` 超出当前应用支持范围时，界面显示所需的应用版本或协议版本。
- 对下载取消、无网络、GitHub 返回错误、磁盘空间不足和应用进程重启分别执行自动化或仪器测试，资源状态与页面内容保持一致。

完成以上验收后，译注和试题资源即可独立于 APK 发布周期持续维护，并同时支持应用内更新、GitHub 手动下载、离线导入、多译注切换和多题库组合使用。
