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
    
    private int nextId = 1;
    private Path savePath;

    private AnchorManager() {}

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

    // ────────── CRUD ──────────

    /** 创建并添加新锚点（自动分配 ID） */
    public Anchor create(BlockPos pos, String name, String description, String color) {
        synchronized (anchors) {
            int id = nextId++;
            Anchor anchor = new Anchor(id, pos, name, description, color);
            addInternal(anchor);
            saveAsync();
            return anchor;
        }
    }

    /** 内部添加（从文件读取时使用） */
    private void addInternal(Anchor anchor) {
        anchors.put(anchor.id, anchor);
        posToId.put(anchor.pos, anchor.id);
    }

    public boolean remove(int id) {
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
    public boolean removeAt(BlockPos pos) {
        synchronized (anchors) {
            Integer id = posToId.get(pos);
            if (id != null) {
                return remove(id);
            }
            return false;
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
    public boolean update(int id, String name, String description, String color) {
        synchronized (anchors) {
            Anchor anchor = anchors.get(id);
            if (anchor == null) return false;
            if (name != null) anchor.name = name;
            if (description != null) anchor.description = description;
            if (color != null) anchor.color = color;
            saveAsync();
            return true;
        }
    }

    /** 加密/兼容层：根据坐标更新 */
    public boolean updateAt(BlockPos pos, String name, String description, String color) {
        Integer id = posToId.get(pos);
        return id != null && update(id, name, description, color);
    }

    public String toJsonString() {
        JsonArray array = new JsonArray();
        for (Anchor anchor : anchors.values()) {
            array.add(anchor.toJson());
        }
        return array.toString();
    }

    // ────────── 持久化 ──────────

    private void load() {
        synchronized (anchors) {
            anchors.clear();
            posToId.clear();
            nextId = 1;
            if (savePath == null || !Files.exists(savePath)) return;
            try {
                String json = Files.readString(savePath);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root.has("nextId")) {
                    nextId = root.get("nextId").getAsInt();
                }
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
        if (savePath == null) return;
        new Thread(() -> {
            try {
                Files.createDirectories(savePath.getParent());
                JsonObject root = new JsonObject();
                List<Anchor> snapshot;
                int currentNextId;
                synchronized (anchors) {
                    snapshot = new ArrayList<>(anchors.values());
                    currentNextId = nextId;
                }
                root.addProperty("nextId", currentNextId);
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
