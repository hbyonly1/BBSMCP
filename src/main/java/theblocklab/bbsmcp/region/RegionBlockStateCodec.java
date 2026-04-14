package theblocklab.bbsmcp.region;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 独立于 building 模块的 BlockState 字符串编解码。
 */
public final class RegionBlockStateCodec {

    private RegionBlockStateCodec() {}

    public static String normalizeSpec(String spec) {
        int stateIndex = spec.indexOf('[');
        String blockId = stateIndex >= 0 ? spec.substring(0, stateIndex) : spec;
        String stateSuffix = stateIndex >= 0 ? spec.substring(stateIndex) : "";
        String normalizedId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
        return normalizedId + stateSuffix;
    }

    public static BlockState parse(String spec) {
        String normalized = normalizeSpec(spec);
        int stateIndex = normalized.indexOf('[');
        String blockId = stateIndex >= 0 ? normalized.substring(0, stateIndex) : normalized;
        boolean hasStateSuffix = stateIndex >= 0 && normalized.endsWith("]");
        String stateBody = hasStateSuffix ? normalized.substring(stateIndex + 1, normalized.length() - 1) : "";

        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            return null;
        }

        Block block = Registries.BLOCK.get(id);
        if (block == null) {
            return null;
        }

        BlockState state = block.getDefaultState();
        if (stateBody.isBlank()) {
            return state;
        }

        for (String entry : stateBody.split(",")) {
            String[] pair = entry.split("=", 2);
            if (pair.length != 2) {
                continue;
            }

            String propertyName = pair[0].trim();
            String rawValue = pair[1].trim();
            Property<?> property = block.getStateManager().getProperty(propertyName);
            if (property == null) {
                continue;
            }

            state = applyProperty(state, property, rawValue);
        }

        return state;
    }

    public static String serialize(BlockState state) {
        StringBuilder builder = new StringBuilder(Registries.BLOCK.getId(state.getBlock()).toString());
        if (state.getEntries().isEmpty()) {
            return builder.toString();
        }

        List<Map.Entry<Property<?>, Comparable<?>>> entries = new ArrayList<>(state.getEntries().entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().getName()));

        builder.append('[');
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Property<?>, Comparable<?>> entry = entries.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey().getName())
                .append('=')
                .append(getValueName(entry.getKey(), entry.getValue()));
        }
        builder.append(']');
        return builder.toString();
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String rawValue) {
        Optional<T> parsed = property.parse(rawValue);
        return parsed.map(value -> state.with(property, value)).orElse(state);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getValueName(Property<?> property, Comparable<?> value) {
        return ((Property<T>) property).name((T) value);
    }
}
