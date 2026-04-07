# BBSMCP (Blockbuster Studio MCP)

BBSMCP 是一个连接 Minecraft **Blockbuster Studio(BBS)** 模组的 **Model Context Protocol (MCP)** 插件 。它允许 AI（如 Claude、GPT 等）通过标准的 MCP 接口直接操控游戏内的电影剪辑、动作回放与关键帧，实现自动化的影视创作。

## 项目特性

-   **MCP 生态集成**：整合并封装 BBS 内部 API 以实现标准化的 MCP Tools。
-   **影片及相关管理**：支持对 Film、Clip、Replay 的增删改查。
-   **关键帧控制**：可批量写入、删除或平移任意 Channel 的 Keyframe。
-   **可视化锚点**：可标记世界中的任意位置并附加信息，帮助 AI 理解世界场景、控制 replay 动作。
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

## 路线图 (Roadmap)

### 当前已实现 ✅
- Film / Clip / Replay 增删改查
- Replay 关键帧的批量写入、平移、删除
- 可视化锚点系统（坐标 + 人工描述）
- MCP Prompts 系统（AI 可读取操作指南）
- 自动化截图验证系统（验证摄像机视野是否覆盖演员、构图是否合理）

### AI 导演闭环系统（核心愿景）

整体目标是让 AI 像一名真正的视频导演一样工作：读懂自然语言描述，规划分镜，执行，看画面，再迭代。

```
用户自然语言描述
     ↓
[阶段一] 场景规划 — AI 以导演视角生成分镜脚本，与用户确认
     ↓
[阶段二] 逐镜执行 — 一个镜头一个镜头执行，截图对比后才推进
     ↓
[阶段三] 视觉验证 — AI 看到画面，判断摄像机与演员是否对齐
     ↓
[阶段四] 场景记忆 — Anchor 积累 AI 视觉描述，形成空间知识库
```

#### 待开发子系统

**① 分镜脚本文件系统**
- AI 在执行前将规划写入 `storyboard` 文件（JSON + Markdown）
- 每个镜头独立存档，支持用户逐步审核和修改
- 执行锁机制：未确认的镜头不执行

**③ 智能 Anchor（空间记忆）**
- AI 围绕 Anchor 点从多个方向截图
- 视觉模型自动识别场景内容，生成描述
- 未来 Anchor 结构：坐标 + 人工描述 + AI 视觉描述 + 截图路径
- 供 AI 导演在规划阶段自主理解世界空间

**④ AI Building（场景搭建）**
- 接入 Blockbench，让 AI 生成或修改 Replay 模型
- 开发 AI 建筑组件，允许 AI 直接修改场景方块
- 配合 Anchor 系统确认场景搭建结果

**⑤ 演员动作系统（最复杂）**
- 近期：仅支持 AI 控制演员的位移路径（x/y/z 关键帧）
- 中期：接入预录制的动作库（Motion Cycle），通过语义匹配复用动作片段
- 远期：探索通过自然语言实现精细的身体姿势控制


## 环境要求
-   **Minecraft**: 1.20.1 (Fabric)
-   **Blockbuster**: 需安装兼容版本的 BBS 模组（本桥梁依赖其底层 API）。
-   **Fabric Language Kotlin**
-   **Java**: JDK 17+

## 如何连接

#### VS Code

**`.vscode/mcp.json`**

```json
{
  "servers": {
    "bbsmcp": {
      "url": "http://localhost:8000/mcp",
      "type": "http"
    }
  }
}
```

#### Claude Desktop

**`claude_desktop_config.json`**

```json
{
  "mcpServers": {
    "bbsmcp": {
      "command": "npx",
      "args": ["mcp-remote", "http://localhost:8000/mcp"]
    }
  }
}
```

#### Claude Code

```bash
claude mcp add blockbench --transport http http://localhost:8000/mcp
```

#### [Antigravity](https://antigravity.google/docs/mcp#connecting-custom-mcp-servers)

```json
{
  "mcpServers": {
    "bbsmcp": {
      "serverUrl": "http://localhost:8000/mcp"
    }
  }
}
```

#### Inspector

```bash
npx -y @modelcontextprotocol/inspector http://localhost:8000/mcp
```

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


