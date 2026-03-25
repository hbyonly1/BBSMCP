# BBS 模组 Replay 系统技术文档

本文档面向 AI（如 MCP Tool 开发者），用于理解 BBS 模组中 `Film` 的 `Replay`（演员/角色轨道）系统的完整运行原理，并基于此编写底层 API。

---

## 一、总体架构概述

一个 `Film` 包含若干个 `Replay`，每个 `Replay` 代表一个角色轨道，定义了一个演员（实体）在整段影片中随时间变化的所有状态：位置、旋转、装备、动作事件等。

```
Film
├── camera: Camera           ← 相机片段（Clips），已有 ClipManagerAPI 覆盖
└── replays: Replays         ← 演员轨道列表（本文档重点）
    ├── Replay[0]
    │   ├── form             ← 外观形态（Form）
    │   ├── keyframes        ← ReplayKeyframes，所有关键帧通道
    │   ├── properties       ← FormProperties，Form 专属动画属性
    │   ├── actions          ← Clips（ActionClip 集合），事件触发
    │   └── ...（各种控制属性）
    └── Replay[1] ...
```

---

## 二、核心类详解

### 2.1 `Replays`（容器）
- **类路径**: `mchorse.bbs_mod.film.replays.Replays`
- **继承**: `ValueList`
- **字段**: `list` (内部，inherited) — 存放 `Replay` 对象的列表

| 方法 | 说明 |
|---|---|
| `addReplay()` | 新建并追加一个 Replay，id 自动取当前 list 的 size |
| `remove(Replay replay)` | 按引用从列表中删除指定 Replay |
| `getList()` | (inherited from ValueList) 返回 `List<Replay>` |
| `get(String id)` | (inherited) 按 id（字符串数字索引）查找 Replay |

> **重要**：Replay 的 `id` 是它被创建时 list 的大小（字符串化），例如 "0", "1", "2"。删除中间项后 id 不会重排，新增时 id 依然取当前 size。因此不能靠 id 推断位置，要用 `getList().indexOf(replay)` 获取真实索引。

---

### 2.2 `Replay`（单个演员轨道）
- **类路径**: `mchorse.bbs_mod.film.replays.Replay`
- **继承**: `ValueGroup`（可序列化/反序列化为 `MapType`）

#### 基本属性字段

| 字段名 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `form` | `ValueForm` | null | 角色的外观形态（Form），决定外观样式 |
| `keyframes` | `ReplayKeyframes` | — | 所有位置/旋转/装备等关键帧数据 |
| `properties` | `FormProperties` | — | Form 本身的动画属性关键帧（如可见性、颜色等） |
| `actions` | `Clips` | — | 使用 ActionClips 工厂，事件动作集合 |
| `enabled` | `ValueBoolean` | true | 是否启用此轨道（false 时服务端不生成实体） |
| `label` | `ValueString` | "" | UI中显示的标签，优先级高于 form 的 displayName |
| `nameTag` | `ValueString` | "" | 角色头顶显示的名字（非空时显示） |
| `shadow` | `ValueBoolean` | true | 是否渲染阴影 |
| `shadowSize` | `ValueFloat` | 0.5F | 阴影大小 |
| `looping` | `ValueInt` | 0 | 循环周期（tick）。0 = 不循环；>0 则 tick % looping |
| `actor` | `ValueBoolean` | false | 是否作为真实实体播放（生成 ActorEntity 到世界中） |
| `fp` | `ValueBoolean` | false | 是否映射到第一人称玩家。全局唯一，一旦设置会清除其他 replay 的此标志 |
| `relative` | `ValueBoolean` | false | 位置是否相对模式（基于 relativeOffset） |
| `relativeOffset` | `ValuePoint` | (0,0,0) | 相对模式时的偏移基准点 |
| `axesPreview` | `ValueBoolean` | false | 是否在 UI 中预览骨骼轴 |
| `axesPreviewBone` | `ValueString` | "" | 预览轴对应的骨骼名称 |

#### 关键方法

| 方法 | 说明 |
|---|---|
| `getName()` | 返回 label（优先）或 form.getDisplayName() 或 "-" |
| `shift(float tick)` | 将所有 keyframes、properties、actions 内的关键帧向后平移 tick |
| `applyActions(actor, fakePlayer, film, tick)` | **服务端调用**。触发此 tick 上所有 ActionClip 的 `apply()` |
| `applyClientActions(tick, entity, film)` | **客户端调用**。触发客户端 ActionClip 的 `applyClient()` |
| `getTick(int tick)` | 处理 looping。若 looping>0 则返回 `tick % looping`，否则原样返回 |

---

### 2.3 `ReplayKeyframes`（关键帧数据）
- **类路径**: `mchorse.bbs_mod.film.replays.ReplayKeyframes`
- **继承**: `ValueGroup`

每个字段都是 `KeyframeChannel<?>` 类型，支持关键帧插值（interpolate）。

#### 通道列表（分组）

**位置/运动**

| 通道字段 | ID | 类型 | 说明 |
|---|---|---|---|
| `x` | "x" | DOUBLE | X 坐标 |
| `y` | "y" | DOUBLE | Y 坐标 |
| `z` | "z" | DOUBLE | Z 坐标 |
| `vX` | "vX" | DOUBLE | X轴速度 |
| `vY` | "vY" | DOUBLE | Y轴速度 |
| `vZ` | "vZ" | DOUBLE | Z轴速度 |
| `fall` | "fall" | DOUBLE | 下落距离 |
| `grounded` | "grounded" | DOUBLE | 是否在地面（0/1） |

**旋转**

| 通道字段 | ID | 类型 | 说明 |
|---|---|---|---|
| `yaw` | "yaw" | DOUBLE | 偏航角（水平朝向） |
| `pitch` | "pitch" | DOUBLE | 俯仰角 |
| `headYaw` | "headYaw" | DOUBLE | 头部偏航角 |
| `bodyYaw` | "bodyYaw" | DOUBLE | 身体偏航角 |

**状态**

| 通道字段 | ID | 类型 | 说明 |
|---|---|---|---|
| `sneaking` | "sneaking" | DOUBLE | 是否潜行（0/1） |
| `sprinting` | "sprinting" | DOUBLE | 是否疾跑（0/1） |
| `damage` | "damage" | DOUBLE | 受伤计时器 |

**装备/物品**（`ITEM_STACK` 类型）

| 通道字段 | ID | 说明 |
|---|---|---|
| `mainHand` | "item_main_hand" | 主手物品 |
| `offHand` | "item_off_hand" | 副手物品 |
| `armorHead` | "item_head" | 头盔 |
| `armorChest` | "item_chest" | 胸甲 |
| `armorLegs` | "item_legs" | 护腿 |
| `armorFeet` | "item_feet" | 靴子 |
| `selectedSlot` | "selected_slot" | INTEGER，当前选中的快捷栏格 |

**手柄/扩展输入**（拓展用途）

| 通道字段 | ID | 说明 |
|---|---|---|
| `stickLeftX/Y` | "stick_lx/ly" | 左摇杆 |
| `stickRightX/Y` | "stick_rx/ry" | 右摇杆 |
| `triggerLeft/Right` | "trigger_l/r" | 扳机键 |
| `extra1X/Y` | "extra1_x/y" | 自定义扩展1 |
| `extra2X/Y` | "extra2_x/y" | 自定义扩展2 |

#### 关键帧通道操作

```java
// 在指定 tick 插入关键帧（值为 double/Object 视工厂类型而定）
channel.insert(float tick, Object value);

// 在指定 tick 进行插值，得到当前状态值
Object value = channel.interpolate(float tick);

// 获取所有关键帧列表
List<Keyframe<?>> kfs = channel.getKeyframes();

// 判断是否为空（没有任何关键帧）
boolean empty = channel.isEmpty();

// 清除所有关键帧
channel.clear(); // 存在于内部实现，可通过 getKeyframes().clear() 操作
```

#### 核心方法

| 方法 | 说明 |
|---|---|
| `record(int tick, IEntity entity, List<String> groups)` | 从实体采样当前状态并写入关键帧 |
| `apply(int tick, IEntity entity)` | 将 tick 对应的插值结果应用到实体上 |
| `apply(int tick, IEntity entity, List<String> groups)` | 带分组排除的 apply，groups 内的通道**不**应用 |
| `copyOver(ReplayKeyframes src, int tick)` | 将另一组关键帧以偏移 tick 复制到本对象 |
| `shift(float tick)` | 将所有通道内的关键帧时间加上 tick |
| `getChannels()` | 返回所有通道的列表 `List<KeyframeChannel<?>>` |

---

### 2.4 `FormProperties`（Form 动画属性）
- **类路径**: `mchorse.bbs_mod.film.replays.FormProperties`
- **继承**: `ValueGroup`

用于对 Form 自身提供的可动画属性（如可见性、颜色、骨骼姿势等）进行关键帧控制。

| 字段/方法 | 说明 |
|---|---|
| `properties: Map<String, KeyframeChannel<?>>` | 所有 Form 属性通道，key 为属性路径（如 "visible", "color"） |
| `getOrCreate(Form form, String key)` | 按 key 获取通道，不存在时从 form 创建 |
| `create(BaseValue property)` | 为某个 form 属性创建对应通道并注册 |
| `applyProperties(Form form, float tick)` | 将当前 tick 的插值结果写入 form 属性（运行时覆盖） |
| `resetProperties(Form form)` | 清除 form 属性的运行时覆盖值（恢复默认） |
| `cleanUp()` | 删除所有空通道，节省内存 |

---

## 三、服务端播放机制（ActionPlayer 视角）

参见 `mchorse.bbs_mod.actions.ActionPlayer`。服务端每 tick 执行：

1. **演员位置更新**：调用 `apply(entity, replay, tick, true)`，仅基于 `replay.keyframes` 的各插值通道写入位置、旋转、装备到实体。
2. **事件/动作触发**：调用 `replay.applyActions(actor, fakePlayer, film, tick)`，触发所有覆盖此 tick 的 `ActionClip`（如使用物品、攻击等事件）。
3. **循环处理**：通过 `Replay.getTick()` 支持 looping 循环。
4. **FP 映射**：`fp == true` 的 Replay 对应 `serverPlayer` 真实玩家，而非 `ActorEntity`。
5. **Actor 模式**：`actor == true` 时才会生成 `ActorEntity` 到世界。否则仅渲染，无实体碰撞。

---

## 四、序列化结构（JSON/MapType）

`Replay.toData()` 产生的 MapType 对应如下结构：

```json
{
  "id": "0",
  "form": { ... },
  "keyframes": {
    "x": { "keyframes": [...] },
    "y": { "keyframes": [...] },
    "z": { "keyframes": [...] },
    "yaw": { ... },
    "item_main_hand": { ... },
    ...
  },
  "properties": {
    "visible": { "keyframes": [...] },
    "color": { ... }
  },
  "actions": {
    "clips": [...]
  },
  "enabled": true,
  "label": "",
  "name_tag": "",
  "shadow": true,
  "shadow_size": 0.5,
  "looping": 0,
  "actor": false,
  "fp": false,
  "relative": false,
  "relativeOffset": { "x": 0, "y": 0, "z": 0 },
  "axes_preview": false,
  "axes_preview_bone": ""
}
```

### KeyframeChannel 序列化结构

```json
{
  "factory": "double",
  "keyframes": [
    {
      "tick": 0.0,
      "value": 100.5,
      "interpolation": "LINEAR"
    }
  ]
}
```

---

## 五、API 设计建议（类比 ClipManagerAPI）

基于上述结构，建议创建 `ReplayManagerAPI.java`，参照 `ClipManagerAPI.java` 的模式：

### 5.1 推荐方法列表

```java
// CRUD
String getReplays(String filmId)                        // 获取所有 Replay（JSON）
String getReplayByIndex(String filmId, int index)       // 按索引获取 Replay（JSON）
int addReplay(ServerPlayerEntity player, String filmId) // 新增 Replay，返回新 index
void removeReplay(ServerPlayerEntity player, String filmId, int index)  // 删除

// 基本属性更新
void setReplayEnabled(player, filmId, index, boolean enabled)
void setReplayLabel(player, filmId, index, String label)
void setReplayNameTag(player, filmId, index, String nameTag)
void setReplayActor(player, filmId, index, boolean actor)
void setReplayFP(player, filmId, index, boolean fp)      // 注意会清除其他 replay 的 fp
void setReplayLooping(player, filmId, index, int looping)
void setReplayForm(player, filmId, index, String formJson)

// 关键帧操作
String getKeyframe(String filmId, int replayIndex, String channel, float tick)
void addKeyframe(player, filmId, index, String channel, float tick, Object value, String interpolation)
void removeKeyframe(player, filmId, index, String channel, float tick)
String getKeyframes(String filmId, int replayIndex, String channel)  // 全通道关键帧
```

### 5.2 关键帧写入示例

```java
// 在 tick=20 处为 Replay[0] 的 x 通道插入关键帧，值为 100.5
Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
Replay replay = film.replays.getList().get(0);
replay.keyframes.x.insert(20.0F, 100.5);

// 保存并同步客户端
FilmManagerAPI.pushFilmToUI(player, filmId, (MapType) film.toData());
FilmManagerAPI.INSTANCE.saveFilm(filmId, (MapType) film.toData());
```

### 5.3 注意事项

1. **写入后必须同步**：每次修改 `Replay` 内容后，需调用 `pushFilmToUI` + `saveFilm`，否则客户端看不到变化。
2. **FP 唯一性**：设置 `fp = true` 时，必须先遍历所有 Replay 取消其他的 `fp`，再设置目标，确保唯一性（参考 `UIReplaysOverlayPanel` 第80-86行）。
3. **Actor vs FP**：`fp` 和 `actor` 都会让 Replay 产生"演员"效果，但 `fp` 绑定真实玩家，`actor` 生成 `ActorEntity`。两者逻辑互斥由服务端 `ActionPlayer.updateReplayEntities()` 处理（如果 `i == exception` 且 `fp == true` 则绑定玩家，否则生成 ActorEntity）。
4. **通道 ID 与字段映射**：序列化 ID（如 `"item_main_hand"`）与 Java 字段名（`mainHand`）不完全一致，API 应接收 ID 字符串，通过 `replay.keyframes.get(channelId)` 动态获取通道。
5. **FormProperties 依赖 Form**：`properties` 中的通道是按需动态创建的，依赖于 `form` 的存在，因此请确保在操作 `properties` 前 form 已被赋值。
