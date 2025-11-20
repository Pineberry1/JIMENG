# JIMENG
jimeng对话app

| 周次               | 核心任务                 | Android 平台                                                 | iOS 平台                                                     | 交付产物与评审点                                             |
| :----------------- | :----------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| **第一周**         | **环境搭建与静态界面**   | 搭建 Android Studio 环境，配置 Kotlin 与 Jetpack Compose。使用 AI 辅助创建项目结构。**独立完成**：使用 Compose 构建一个**静态**的聊天界面，包含输入框、发送按钮和几条写死的聊天气泡。工程架构 MVI 代码组织范式 | 搭建 Xcode 环境，熟悉 Swift 与 SwiftUI。使用 AI 辅助创建项目结构。**独立完成**：使用 SwiftUI 构建一个**静态**的聊天界面，与 Android 版功能对齐。工程架构 MVVM 代码组织范式 | 两个平台的项目骨架。静态聊天界面的截图。**评审点**：环境是否配置成功，静态 UI 是否实现。 |
| **第二周**         | **网络请求与状态管理**   | 使用 Retrofit 集成 API 调用。在 ViewModel 中使用 Kotlin 协程和 `StateFlow` 管理网络请求状态和聊天数据。将界面与 ViewModel 连接，实现一个能与 AI **真实对话**的 MVP 版本。 | 使用 URLSession 和 Combine 集成 API 调用。在 ViewModel 中使用 `Published` 属性和 `sink` 管理网络状态和聊天数据。将界面与 ViewModel 连接，实现与 Android 版功能对齐的 MVP 版本。 | 可运行的 Android 和 iOS App。演示视频，展示两个 App 都能与 AI 对话。**评审点**：功能完整性，**代码对比分析（异步处理部分）**。 |
| **第****三****周** | **本地存储与数据持久化** | 引入 Room 数据库。使用 AI 辅助，编写 `Entity`, `DAO` 和 `Database` 类。将网络请求成功后的对话历史**保存到 Room**，并在 App 启动时加载。 | 引入 Core Data。使用 AI 辅助，创建数据模型 (`.xcdatamodeld`) 和相关的存取代码。将网络请求成功后的对话历史**保存到 Core Data**，并在 App 启动时加载。 | 更新后的 App，支持离线查看历史消息。**AI 指令 Prompt** 列表（用于生成数据库代码）。**评审点**：本地存储功能是否实现，**AI 指令工程质量**。 |
| **第四周**         | **平台适配与最终交付**   | **平台特性**：研究并实现 Android 的权限模型（如网络权限）。**优化**：使用 Profiler 检查并优化性能。准备最终交付物。 | **平台特性**：研究并实现 iOS 的权限模型（如网络权限）。**优化**：使用 Instruments 检查并优化性能。准备最终交付物。 | 两个独立的、最终版的代码仓库。**并排演示**两个 App 的最终视频。**双平台实现对比分析报告**。 |

# WEEK 1

## Android平台

搭建了Android Studio 环境，配置 Kotlin 与 Jetpack Compose。

先用**Gemini**使用 Compose 构建一个静态的聊天界面，包含输入框、发送按钮和几条写死的聊天气泡

![image-20251117210736863](./imgs/image-20251117210736863.png)

熟悉了MVI代码组织架构范式，把代码转换成了MVI的架构

- components -> view层发送intent
- model 定义了数据模型和状态定义
- viewmodel 处理intent然后更新view

![image-20251118153711279](./imgs/image-20251118153711279.png)

#### 对话功能的搭建（vibe coding）

需求是用http post调用大模型api，然后解析它的返回，直接写功能函数即可，然后和页面的message结合。就给出请求体样例和响应体样例让ai写一个http请求即可。

```text
  "model": "qwen-plus",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant."
    },
    {
      "role": "user",
      "content": "你是谁？"
    }
  ],
  "stream": true,
  "stream_options": {
    "include_usage": true
  },
  "top_p": 0.8,
  "temperature": 0.7,
  "enable_thinking": true
}
请求体样例如上，响应体样例如下
data: {"choices":[{"delta":{"content":null,"role":"assistant","reasoning_content":""},"index":0,"logprobs":null,"finish_reason":null}],"object":"chat.completion.chunk","usage":null,"created":1763612375,"system_fingerprint":null,"model":"qwen-plus","id":"chatcmpl-a25dc554-ddb5-4ee5-b33c-cd078fb16954"}

data: {"choices":[{"finish_reason":null,"logprobs":null,"delta":{"content":null,"reasoning_content":"嗯"},"index":0}],"object":"chat.completion.chunk","usage":null,"created":1763612375,"system_fingerprint":null,"model":"qwen-plus","id":"chatcmpl-a25dc554-ddb5-4ee5-b33c-cd078fb16954"}

.......

data: [DONE]```
```

但是和我的app message结合的时候出现了问题

![image-20251120132920838](/imgs/image-20251120132920838.png)

可以看到，第二次回复的时候，我的回复后面跟了ai的回复。询问了ai，反馈是用System.currentTimeMillis()生成id的，可能导致id相同，所以后面改成了uuid来生成。成功解决了问题。

#### 上下文对话功能

直接在viewmodel里实现就行，现在没有上下文管理

![image-20251120143755638](/imgs/image-20251120143755638.png)

![image-20251120151324204](/imgs/image-20251120151324204.png)

#### debug case

为了快速测试我的对话app还是写个debugcase比较省事，让ai写吧![image-20251120153520466](/imgs/image-20251120153520466.png)

测试简单化调用

![image-20251120162612635](/imgs/image-20251120162612635.png)

#### 实现流式传输

这一步调的比较久了，因为它默认写的流式方法是对的，即使我传的参数也没问题，但是它还是阻塞性的返回了流式的结果，修改了日志的阻塞，然后日志不阻塞了，但是反馈到app的ui上还是阻塞的，ai认为这是因为**collect代码收到一次网络返回就要遍历整个消息列表刷新ui**，效率极低导致的。但其实效率低在过长返回中起码ui还能刷几次吧，事实是ui根本没动，所以还是要从viewmodel里找原因。

结论是**更新flow的时候要去dispatch io一下**，之前的flow就是在主线程上直接执行了，那肯定update不了，属于是只声明了suspend但没有真正的分配一个协程处理io。

![image-20251120162500430](/imgs/image-20251120162500430.png)

## IOS平台

XCODE搭建需要MACBOOK支持，所以只能配置一个macos的虚拟机尝试进行开发，网上的资料说intel架构的苹果的m芯片架构比较相似比较容易配置，amd的只能配置12以下的版本，而且还需要开启cpu虚拟化。这里我的电脑是amd的cpu故只能装macos11。

搞了一天装了macos11之后xcode现在根本不支持这么早的版本，眼前一黑，遂放弃

![image-20251118161948739](./imgs/image-20251118161948739.png)
