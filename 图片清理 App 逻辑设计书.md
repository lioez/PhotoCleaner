
一、 用户操作流程 (User Journey)
这是用户看到的表面逻辑：

冷启动 (Cold Start)：

用户打开 App。

权限检查： App 检查是否有“读取/修改相册”权限。

无权限时： 弹出系统弹窗申请权限（必须通过才能继续）。

初始化： 出现一个短暂的 Loading 界面（后台正在扫描所有图片 ID 并进行洗牌）。

主界面交互 (The Tinder Loop)：

屏幕中央显示一张图片（随机抽取的）。

左滑 (手势)： 图片飞出屏幕左侧 -> 标记为“待删除” -> 立即显示下一张。

右滑 (手势)： 图片飞出屏幕右侧 -> 标记为“保留” -> 立即显示下一张。

误触悔棋 (可选功能)： 提供一个“撤销”按钮，恢复上一张刚刚滑走的照片。

缓冲机制 (用户无感)：

当用户滑得很快时，App 会自动在后台默默加载后续图片，保证永远不会出现“白屏等待”。

确认与清理 (Final Review)：

用户随时可以点击角落的“垃圾桶图标”。

进入“待删除列表”页面，以网格形式展示所有刚刚左滑过的图片。

用户可以在这里把误删的图片“捞回来”（移出删除列表）。

最终点击“确认删除”：

App 弹出一个系统级的确认框：“允许 PhotoCleaner 删除这 50 张照片吗？”

用户点“允许” -> 手机相册中的文件真正消失。

二、 后台数据流转 (Data Logic)
这是你看不到的“大脑”运作逻辑（核心技术点）：

1. 初始化阶段：全量 ID 洗牌策略
Step 1: 查询 Android MediaStore 数据库。

Step 2: 只获取所有图片的 _ID (Long 类型)。不获取图片详情，速度极快（1万张图耗时约 200ms）。

Step 3: 得到一个 List<Long> (例如 [1, 2, 3, ... 10000])。

Step 4: 调用 Collections.shuffle()。

结果: 得到一个全局乱序 ID 池 GlobalShuffledList (例如 [599, 2, 8848, ...])。

2. 运行阶段：双缓冲队列策略
我们需要维护两个核心列表：

A. 全局乱序池 (Global ID Pool): 存着所有未看过的图片 ID。

B. 显示缓存池 (Display Buffer): 这是一个 MutableList<PhotoModel>，容量维持在 20 张左右。

运作逻辑：

从 A 中取出前 20 个 ID，转换成 Uri，填入 B。

UI 永远只渲染 B 中的第 0 张。

Coil (图片库) 负责根据 Uri 异步加载图片像素到内存。

当用户滑走一张：

将该图片放入 PendingDeleteList (如果是左滑)。

从 B 中移除该图片。

检查： 如果 B 的剩余数量 < 5 张？

补货： 从 A 中再取 20 个 ID，填入 B 的尾部。

3. 删除阶段：批量执行
遍历 PendingDeleteList。

构建一个 PendingIntent (Android 10+ 的要求)。

调用 MediaStore.createDeleteRequest(uris)。

注意： Android 系统可能限制一次请求的数量（例如部分机型限制一次只能删几十张），所以如果删除数量巨大，后台逻辑需要自动分批次请求（例如每 50 张弹一次窗，或者循环处理）。

三、 核心架构设计 (Technical Architecture)
基于 MVVM 模式的代码结构：

1. Data Layer (数据层)
PhotoRepository (类):

方法 getAllImageIds(): 返回 List<Long>。

方法 deleteImages(uris): 调用系统 API 删除文件。

方法 idToUri(id): 辅助工具，把数字 ID 12345 变成 content://.../12345。

2. ViewModel Layer (逻辑层)
CleanerViewModel (类):

变量 uiState: 包含当前显示的图片、等待列表长度等信息。

变量 pendingDeleteList: 暂存要删除的 Uri。

方法 initPhotoList(): 启动协程，读取 ID 并洗牌。

方法 swipeLeft(photo): 加入删除列表，移除当前显示。

方法 swipeRight(photo): 仅移除当前显示。

方法 checkBuffer(): 监控是否需要补货。

3. UI Layer (界面层 - Jetpack Compose)
MainActivity: App 入口。

SwipeScreen (主界面):

包含一个自定义的 SwipeableCard 组件。

监听 ViewModel 的 uiState 变化。

TrashReviewScreen (确认界面):

显示网格列表。