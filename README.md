# BBSMCP (Blockbuster Studio MCP)

BBSMCP 是一个连接 Minecraft **Blockbuster Studio(BBS)** 模组的 **Model Context Protocol (MCP)** 插件 。它允许 AI（如 Claude、GPT 等）通过标准的 MCP 接口直接操控游戏内的电影剪辑、动作回放与关键帧，实现自动化的影视创作。

## 项目特性

-   **影片及相关管理**：支持对 Film、Clip、Replay 的增删改查。
-   **关键帧控制**：可批量写入、删除或平移任意 Channel 的 Keyframe。
-   **MCP 生态集成**：整合并封装 BBS 内部 API 以实现标准化的 MCP Tools。
-   **自动化测试套件**：内置 `film_clip_test`、`replay_test` 等工具，可验证从服务端逻辑到客户端渲染的全链路流程。

## 项目架构 (Source Structure)
 
```text
theblocklab.bbsmcp
├── film           # 业务逻辑层：Film、Clip、Replay 的核心管理 API
│   ├── clips      
│   └── replays    
├── mcp            # MCP 接入层：协议实现与服务器生命周期
│   ├── core       # 核心抽象：MCPTool 基类、响应定义接口
│   └── tools      # 具体工具实现：按 Film/Clip/Replay/UI/Test 模块划分
├── network        # 桥接通信层：服务端与客户端的数据同步协议 (Bridge)
└── exception      # 错误定义：统一的异常处理与报错信息提示
```

## 待验证或开发功能

* 能否通过自然语言为 replay 实现理想中的精细动画控制。
* 运动循环 Motion Cycle：通过预先录制的真实动作库来生成 replay 动画。
* 搭建动画环境：
    * 接入 blockbench 来生成 replay（包括模型建模，材质，和动画）。
    * 开发 AI building 组件来生成动画场景。
* 制作动画的全反馈流程：通过自然语言描述调配各组件来生成动画，并设计一个可闭环的反馈流程来达到合格效果。

## 构建说明

项目基于 Fabric 开发框架（Loom），使用 Gradle 进行构建。

1.  **克隆项目**
2.  **编译项目**：
    ```bash
    ./gradlew build
    ```
3.  **运行开发环境**（含 Minecraft 客户端）：
    ```bash
    ./gradlew runClient
    ```
    *构建生成的 jar 文件位于 `build/libs` 目录下。*

## 接入 MCP

项目启动后，后台会开启一个 MCP 服务端（硬编码端口 `8000`）。

### 如何连接
你可以使用 `mcp-inspector` 或其他支持 MCP 的客户端连接到运行中的游戏实例：

```bash
# 使用 Inspector 检查工具列表
npx -y @modelcontextprotocol/inspector http://localhost:8000/mcp
```

## 环境要求
-   **Minecraft**: 1.20.1 (Fabric)
-   **Blockbuster Mod**: 需安装兼容版本的 BBS 模组（本桥梁依赖其底层 API）。
-   **Java**: JDK 17+
