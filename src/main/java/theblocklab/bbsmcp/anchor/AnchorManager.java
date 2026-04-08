package theblocklab.bbsmcp.anchor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.BBSMCP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 锚点管理器（单例）。
 * 负责锚点的内存存储、CRUD 操作以及 JSON 文件的持久化。
 */
public class AnchorManager {

    public static final AnchorManager INSTANCE = new AnchorManager();

    private static final Gson GSON = new Gson();
    private static final String FILE_NAME = "bbsmcp_anchors.json";

    /** 主存储：ID -> Anchor */
    private final Map<Integer, Anchor> anchors = new LinkedHashMap<>();
    /** 辅助索引：BlockPos -> ID */
    private final Map<BlockPos, Integer> posToId = new HashMap<>();

    private Path savePath;

    private AnchorManager() {
    }

    // ────────── 生命周期 ──────────

    public void onServerStarted(MinecraftServer server) {
        savePath = FabricLoader.getInstance().getConfigDir()
                .resolve("bbsmcp/anchor")
                .resolve(FILE_NAME);
        load();
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(INSTANCE::onServerStarted);
    }

    /** 内部添加（从文件读取时使用） */
    private void addInternal(Anchor anchor) {
        anchors.put(anchor.id, anchor);
        posToId.put(anchor.pos, anchor.id);
    }

    protected String toJsonString() {
        JsonArray array = new JsonArray();
        for (Anchor anchor : anchors.values()) {
            array.add(anchor.toJson());
        }
        return array.toString();
    }

    // ────────── CRUD ──────────

    /** 创建并添加新锚点（自动分配 ID：搜索最小可用正整数） */
    protected Anchor create(BlockPos pos, String name, String description, String color) {
        synchronized (anchors) {
            int id = 1;
            while (anchors.containsKey(id)) {
                id++;
            }
            Anchor anchor = new Anchor(id, pos, name, description, color);
            addInternal(anchor);
            saveAsync();
            return anchor;
        }
    }

    protected boolean remove(int id) {
        synchronized (anchors) {
            Anchor removed = anchors.remove(id);
            if (removed != null) {
                posToId.remove(removed.pos);
                saveAsync();
                return true;
            }
            return false;
        }
    }

    /** 根据坐标删除 */
    protected boolean removeAt(BlockPos pos) {
        synchronized (anchors) {
            Integer id = posToId.get(pos);
            if (id != null) {
                return remove(id);
            }
            return false;
        }
    }

    /** 删除所有锚点 */
    protected void removeAll() {
        synchronized (anchors) {
            List<Integer> ids = new ArrayList<>(anchors.keySet());
            for (Integer id : ids) {
                remove(id);
            }
        }
    }

    public Anchor get(int id) {
        return anchors.get(id);
    }

    public Anchor getAt(BlockPos pos) {
        Integer id = posToId.get(pos);
        return id != null ? anchors.get(id) : null;
    }

    public boolean has(int id) {
        return anchors.containsKey(id);
    }

    public boolean hasAt(BlockPos pos) {
        return posToId.containsKey(pos);
    }

    public Collection<Anchor> getAll() {
        return anchors.values();
    }

    /** 更新锚点属性 */
    protected boolean update(int id, String name, String description, String color) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(id);
            if (anchor == null)
                return false;
            if (name != null)
                anchor.name = name;
            if (description != null)
                anchor.description = description;
            if (color != null)
                anchor.color = color;
            saveAsync();
            return true;
        }
    }

    // ────────── camera hint ──────────

    /** 添加一个 camera hint 到指定锚点，并自动分配 hintId */
    protected boolean addCameraHint(int anchorId, JsonObject hint) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(anchorId);
            if (anchor == null)
                return false;
            // 计算最大现有 id+1 作为新 id
            int maxId = 0;
            for (var elem : anchor.cameraHints) {
                if (elem.isJsonObject() && elem.getAsJsonObject().has("id")) {
                    maxId = Math.max(maxId, elem.getAsJsonObject().get("id").getAsInt());
                }
            }
            hint.addProperty("id", maxId + 1);
            if (!hint.has("preferred"))
                hint.addProperty("preferred", false);
            anchor.cameraHints.add(hint);
            saveAsync();
            return true;
        }
    }

    /** 从指定锚点删除一个 hint */
    protected boolean removeCameraHint(int anchorId, int hintId) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(anchorId);
            if (anchor == null)
                return false;
            for (int i = 0; i < anchor.cameraHints.size(); i++) {
                var elem = anchor.cameraHints.get(i);
                if (elem.isJsonObject() && elem.getAsJsonObject().get("id").getAsInt() == hintId) {
                    anchor.cameraHints.remove(i);
                    saveAsync();
                    return true;
                }
            }
            return false;
        }
    }

    /** 标记某个 hint 为首选（并自动清除其余） */
    protected boolean setPreferredHint(int anchorId, int hintId) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(anchorId);
            if (anchor == null)
                return false;
            boolean found = false;
            for (var elem : anchor.cameraHints) {
                if (!elem.isJsonObject())
                    continue;
                var obj = elem.getAsJsonObject();
                boolean isTarget = obj.has("id") && obj.get("id").getAsInt() == hintId;
                obj.addProperty("preferred", isTarget);
                if (isTarget)
                    found = true;
            }
            if (found)
                saveAsync();
            return found;
        }
    }

    /** 更新锚点上已有 hint 的内容（新增 hint 请用 addCameraHint） */
    protected boolean updateCameraHint(int anchorId, int hintId, com.google.gson.JsonObject updatedHint) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(anchorId);
            if (anchor == null)
                return false;
            for (int i = 0; i < anchor.cameraHints.size(); i++) {
                var elem = anchor.cameraHints.get(i);
                if (elem.isJsonObject() && elem.getAsJsonObject().get("id").getAsInt() == hintId) {
                    updatedHint.addProperty("id", hintId); // 保持 id 不变
                    anchor.cameraHints.set(i, updatedHint);
                    saveAsync();
                    return true;
                }
            }
            return false;
        }
    }

    /** 删除当前锚点所有的 camera hints */
    protected boolean clearCameraHints(int anchorId) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(anchorId);
            if (anchor == null)
                return false;
            anchor.cameraHints = new JsonArray();
            saveAsync();
            return true;
        }
    }

    /** 加密/兼容层：根据坐标更新 */
    protected boolean updateAt(BlockPos pos, String name, String description, String color) {
        Integer id = posToId.get(pos);
        return id != null && update(id, name, description, color);
    }

    // ────────── 持久化 ──────────

    private void load() {
        synchronized (anchors) {
            anchors.clear();
            posToId.clear();
            if (savePath == null || !Files.exists(savePath))
                return;
            try {
                String json = Files.readString(savePath);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root.has("anchors")) {
                    JsonArray array = root.getAsJsonArray("anchors");
                    for (var elem : array) {
                        try {
                            Anchor anchor = Anchor.fromJson(elem.getAsJsonObject());
                            addInternal(anchor);
                        } catch (Exception e) {
                            BBSMCP.LOGGER.warn("[AnchorManager] 跳过无效锚点: {}", e.getMessage());
                        }
                    }
                }
                BBSMCP.LOGGER.info("[AnchorManager] 已加载 {} 个锚点", anchors.size());
            } catch (Exception e) {
                BBSMCP.LOGGER.error("[AnchorManager] 加载锚点文件失败", e);
            }
        }
    }

    private void saveAsync() {
        if (savePath == null)
            return;
        new Thread(() -> {
            try {
                Files.createDirectories(savePath.getParent());
                JsonObject root = new JsonObject();
                List<Anchor> snapshot;
                synchronized (anchors) {
                    snapshot = new ArrayList<>(anchors.values());
                }
                JsonArray array = new JsonArray();
                for (Anchor anchor : snapshot) {
                    array.add(anchor.toJson());
                }
                root.add("anchors", array);
                Files.writeString(savePath, GSON.toJson(root));
            } catch (IOException e) {
                BBSMCP.LOGGER.error("[AnchorManager] 保存锚点文件失败", e);
            }
        }).start();
    }
}
