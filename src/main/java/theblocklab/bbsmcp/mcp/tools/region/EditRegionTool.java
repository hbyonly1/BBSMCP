package theblocklab.bbsmcp.mcp.tools.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.region.RegionBlockStateCodec;
import theblocklab.bbsmcp.region.RegionEditManager;
import theblocklab.bbsmcp.region.RegionSelection;
import theblocklab.bbsmcp.region.RegionServerNetwork;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditRegionTool extends MCPTool {

    public EditRegionTool() {
        super("edit__region",
            "编辑当前选区。mode=preview 时根据 patch_json 生成局部修改预览；mode=apply 时直接应用上一次 preview 结果。该工具只修改世界中的选区方块。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
            {
              "type": "object",
              "properties": {
                "mode": {
                  "type": "string",
                  "enum": ["preview", "apply"],
                  "description": "preview=解析 patch_json 并生成预览；apply=应用上一次 preview 结果"
                },
                "patch_json": {
                  "type": "string",
                  "description": "仅 mode=preview 时需要。局部 patch JSON，支持 blocks/fills，坐标相对于选区 min 点"
                }
              },
              "required": ["mode"]
            }
            """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        RegionSelection selection = RegionEditManager.INSTANCE.getSelection(player);
        if (!selection.isComplete()) {
            return MCPToolResponse.error("当前选区不完整。", "请先使用区域魔杖设置 Pos1 和 Pos2。");
        }
        if (selection.volume() > RegionEditManager.MAX_SELECTION_VOLUME) {
            return MCPToolResponse.error(
                "当前选区体积过大: " + selection.volume(),
                "请缩小选区。当前最大支持体积为 " + RegionEditManager.MAX_SELECTION_VOLUME + " 个方块。"
            );
        }

        String mode = requireString(arguments, "mode");
        return switch (mode) {
            case "preview" -> handlePreview(arguments, player, selection);
            case "apply" -> handleApply(player, selection);
            default -> MCPToolResponse.error("未知 mode: " + mode, "仅支持 preview 或 apply。");
        };
    }

    private MCPToolResponse handlePreview(JsonObject arguments, ServerPlayerEntity player, RegionSelection selection) {
        if (!arguments.has("patch_json") || arguments.get("patch_json").isJsonNull()) {
            return MCPToolResponse.error("mode=preview 时缺少 patch_json。", "请提供局部补丁 JSON。");
        }

        String patchJson = arguments.get("patch_json").getAsString();
        List<RegionEditManager.PatchEntry> patchEntries;
        try {
            patchEntries = parsePatchEntries(patchJson);
        } catch (Exception e) {
            return MCPToolResponse.error("patch_json 解析失败: " + e.getMessage(), "请检查 JSON 格式和 blocks/fills 字段。");
        }

        List<RegionEditManager.BlockChange> changes;
        try {
            changes = RegionEditManager.INSTANCE.buildChanges(player.getWorld(), selection, patchEntries);
        } catch (Exception e) {
            return MCPToolResponse.error("局部补丁构建失败: " + e.getMessage(), "请检查补丁坐标是否落在当前选区内。");
        }

        RegionEditManager.PreviewState preview = new RegionEditManager.PreviewState(
            selection.regionKey(player.getWorld()),
            List.copyOf(changes)
        );
        RegionEditManager.INSTANCE.setPreview(player, preview);
        RegionServerNetwork.sendRegionPreview(player, preview);

        JsonObject result = new JsonObject();
        result.addProperty("regionKey", preview.regionKey());
        result.addProperty("changeCount", preview.changes().size());

        JsonArray changed = new JsonArray();
        for (RegionEditManager.BlockChange change : preview.changes()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", change.pos().getX());
            obj.addProperty("y", change.pos().getY());
            obj.addProperty("z", change.pos().getZ());
            obj.addProperty("before", change.beforeSpec());
            obj.addProperty("after", change.afterSpec());
            changed.add(obj);
        }
        result.add("changes", changed);

        return MCPToolResponse.success(
            result.toString(),
            "预览已发送到客户端。确认后请调用 edit__region(mode=apply) 应用本次预览。"
        );
    }

    private MCPToolResponse handleApply(ServerPlayerEntity player, RegionSelection selection) {
        RegionEditManager.PreviewState preview = RegionEditManager.INSTANCE.getPreview(player);
        if (preview == null) {
            return MCPToolResponse.error("当前没有可应用的预览结果。", "请先调用 edit__region(mode=preview) 生成预览。");
        }

        String currentRegionKey = selection.regionKey(player.getWorld());
        if (!currentRegionKey.equals(preview.regionKey())) {
            return MCPToolResponse.error("当前选区与上一次 preview 不一致。", "请重新生成 preview，或把选区切回原来的区域。");
        }

        int applied = 0;
        for (RegionEditManager.BlockChange change : preview.changes()) {
            BlockState state = RegionBlockStateCodec.parse(change.afterSpec());
            if (state == null) {
                return MCPToolResponse.error("无法解析预览中的目标方块: " + change.afterSpec(), "请重新生成 preview。");
            }
            player.getWorld().setBlockState(change.pos(), state, Block.NOTIFY_ALL);
            applied++;
        }

        RegionEditManager.INSTANCE.pushHistory(player, currentRegionKey,
            new RegionEditManager.HistoryEntry(currentRegionKey, List.copyOf(preview.changes())));
        RegionEditManager.INSTANCE.clearPreview(player);
        RegionServerNetwork.sendRegionClearPreview(player);

        return MCPToolResponse.success("局部区域修改已应用。", "本次实际写入方块数: " + applied);
    }

    private List<RegionEditManager.PatchEntry> parsePatchEntries(String patchJson) {
        JsonObject root = JsonParser.parseString(patchJson).getAsJsonObject();
        Map<BlockPos, String> resolved = new LinkedHashMap<>();

        if (root.has("fills")) {
            for (var elem : root.getAsJsonArray("fills")) {
                JsonArray arr = elem.getAsJsonArray();
                int x1 = arr.get(0).getAsInt();
                int y1 = arr.get(1).getAsInt();
                int z1 = arr.get(2).getAsInt();
                int x2 = arr.get(3).getAsInt();
                int y2 = arr.get(4).getAsInt();
                int z2 = arr.get(5).getAsInt();
                String blockSpec = RegionBlockStateCodec.normalizeSpec(arr.get(6).getAsString());

                int minX = Math.min(x1, x2);
                int maxX = Math.max(x1, x2);
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2);
                int minZ = Math.min(z1, z2);
                int maxZ = Math.max(z1, z2);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            resolved.put(new BlockPos(x, y, z), blockSpec);
                        }
                    }
                }
            }
        }

        if (root.has("blocks")) {
            for (var elem : root.getAsJsonArray("blocks")) {
                JsonArray arr = elem.getAsJsonArray();
                BlockPos pos = new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
                resolved.put(pos, RegionBlockStateCodec.normalizeSpec(arr.get(3).getAsString()));
            }
        }

        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("patch_json 中至少需要 blocks 或 fills 之一");
        }

        List<RegionEditManager.PatchEntry> patchEntries = new ArrayList<>(resolved.size());
        for (Map.Entry<BlockPos, String> entry : resolved.entrySet()) {
            patchEntries.add(new RegionEditManager.PatchEntry(entry.getKey(), entry.getValue()));
        }
        return patchEntries;
    }
}
