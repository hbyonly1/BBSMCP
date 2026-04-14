package theblocklab.bbsmcp.building;

import net.fabricmc.loader.api.FabricLoader;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 建筑蓝图文件存取工具。
 * 统一负责 config/bbsmcp/buildings 目录下的名称净化、读写与列表查询。
 */
public class BuildingFileLoader {

    private static final Path BUILDINGS_DIR_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("bbsmcp/buildings");

    private BuildingFileLoader() {}

    public static Path ensureBuildingsDir() {
        try {
            if (!Files.exists(BUILDINGS_DIR_PATH)) {
                Files.createDirectories(BUILDINGS_DIR_PATH);
            }
            return BUILDINGS_DIR_PATH.toAbsolutePath().normalize();
        } catch (java.io.IOException e) {
            throw new BBSMCPException(BBSMCPError.BUILDING_FILE_IO_ERROR, e.getMessage());
        }
    }

    public static String sanitizeBuildingName(String buildingName) {
        if (buildingName == null) {
            throw new BBSMCPException(BBSMCPError.INVALID_BUILDING_NAME, "null");
        }

        String originalName = buildingName;
        String sanitized = buildingName.trim().replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|]+", "_");
        sanitized = sanitized.replaceAll("\\.+", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        sanitized = sanitized.replaceAll("^[_-]+|[_-]+$", "");

        if (sanitized.isBlank()) {
            throw new BBSMCPException(BBSMCPError.INVALID_BUILDING_NAME, originalName);
        }

        return sanitized;
    }

    public static Path resolveBuildingPath(String buildingName) {
        String safeName = sanitizeBuildingName(buildingName);
        return ensureBuildingsDir().resolve(safeName + ".json").toAbsolutePath().normalize();
    }

    public static SaveResult saveBlueprint(String buildingName, String blueprintJson, boolean overwrite) {
        Path fullPath = resolveBuildingPath(buildingName);
        boolean existed = Files.exists(fullPath);

        if (existed && !overwrite) {
            throw new BBSMCPException(BBSMCPError.BUILDING_FILE_ALREADY_EXISTS, fullPath.getFileName());
        }

        try {
            Files.writeString(fullPath, blueprintJson);
            return new SaveResult(fullPath, existed);
        } catch (java.io.IOException e) {
            throw new BBSMCPException(BBSMCPError.BUILDING_FILE_IO_ERROR, e.getMessage());
        }
    }

    public static String readBlueprint(String buildingName) {
        Path fullPath = resolveBuildingPath(buildingName);
        if (!Files.exists(fullPath)) {
            throw new BBSMCPException(BBSMCPError.BUILDING_FILE_NOT_FOUND, fullPath.getFileName());
        }
        try {
            return Files.readString(fullPath);
        } catch (java.io.IOException e) {
            throw new BBSMCPException(BBSMCPError.BUILDING_FILE_IO_ERROR, e.getMessage());
        }
    }

    public static List<BuildingFileInfo> listBuildings() {
        ensureBuildingsDir();
        try (Stream<Path> stream = Files.list(BUILDINGS_DIR_PATH)) {
            return stream
                .filter(Files::isRegularFile)
                .map(path -> path.toAbsolutePath().normalize())
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                .map(path -> {
                    String filename = path.getFileName().toString();
                    String buildingName = filename.substring(0, filename.length() - 5);
                    return new BuildingFileInfo(buildingName, path);
                })
                .sorted(Comparator.comparing(BuildingFileInfo::buildingName))
                .toList();
        } catch (java.io.IOException e) {
            throw new BBSMCPException(BBSMCPError.BUILDING_FILE_IO_ERROR, e.getMessage());
        }
    }

    public record SaveResult(Path fullPath, boolean existedBeforeSave) {}
    public record BuildingFileInfo(String buildingName, Path fullPath) {}
}
