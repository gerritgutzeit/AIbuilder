package cn.kaoyu666.aibuilder.client.schematic;

import cn.kaoyu666.aibuilder.Aibuilder;
import com.google.gson.*;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.SharedConstants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 将 AI 生成的 JSON 建筑数据转换为 Litematic 格式并通过 Litematica 加载投影
 */
public class SchematicHelper {
    private static final String SCHEMATIC_DIR = "schematics";
    private static final String REGION_NAME = "AIRegion";
    /** 建筑尺寸上限，防止 AI 输出过大尺寸导致内存暴涨 */
    private static final int MAX_SIZE = 64;

    /**
     * 解析 AI 输出的 JSON 并加载为 Litematica 投影
     */
    public static boolean loadAISchematic(String jsonData, String schematicName) {
        try {
            JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();

            // 解析尺寸
            JsonObject sizeObj = json.getAsJsonObject("size");
            if (sizeObj == null || !sizeObj.has("x") || !sizeObj.has("y") || !sizeObj.has("z")) {
                Aibuilder.LOGGER.error("AI schematic JSON is missing the 'size' field");
                return false;
            }
            int sizeX = sizeObj.get("x").getAsInt();
            int sizeY = sizeObj.get("y").getAsInt();
            int sizeZ = sizeObj.get("z").getAsInt();

            // 校验尺寸：AI 可能输出非法尺寸；过大则截断，避免位打包数组过大导致内存暴涨
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                Aibuilder.LOGGER.error("AI schematic has invalid size: {}x{}x{}", sizeX, sizeY, sizeZ);
                return false;
            }
            if (sizeX > MAX_SIZE || sizeY > MAX_SIZE || sizeZ > MAX_SIZE) {
                Aibuilder.LOGGER.warn("AI schematic size {}x{}x{} exceeds the limit {}, clamping", sizeX, sizeY, sizeZ, MAX_SIZE);
                sizeX = Math.min(sizeX, MAX_SIZE);
                sizeY = Math.min(sizeY, MAX_SIZE);
                sizeZ = Math.min(sizeZ, MAX_SIZE);
            }

            // 解析方块数据
            JsonArray blocks = json.getAsJsonArray("blocks");
            if (blocks == null) {
                Aibuilder.LOGGER.error("AI schematic JSON is missing the 'blocks' field");
                return false;
            }
            Map<BlockPos, BlockState> blockMap = new HashMap<>();

            for (JsonElement el : blocks) {
                JsonObject block = el.getAsJsonObject();
                int x = block.get("x").getAsInt();
                int y = block.get("y").getAsInt();
                int z = block.get("z").getAsInt();
                String blockId = block.get("block").getAsString();

                // AI 可能输出越界坐标，跳过以避免位打包数组越界导致整个生成失败
                if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
                    Aibuilder.LOGGER.warn("Skipping out-of-bounds block at ({}, {}, {}): {}", x, y, z, blockId);
                    continue;
                }

                BlockState state = parseBlockState(blockId);
                if (state != null) {
                    blockMap.put(new BlockPos(x, y, z), state);
                }
            }

            if (blockMap.isEmpty()) {
                Aibuilder.LOGGER.error("AI schematic contains no valid blocks");
                return false;
            }

            // 构建 Litematic NBT
            NbtCompound nbt = buildLitematicNBT(sizeX, sizeY, sizeZ, blockMap, schematicName);

            // 保存到文件
            Path schematicPath = saveSchematicToFile(nbt, schematicName);
            if (schematicPath == null) return false;

            // 通过 Litematica 加载并放置
            return loadInLitematica(schematicPath.toFile(), schematicName);
        } catch (Exception e) {
            Aibuilder.LOGGER.error("Failed to load AI schematic", e);
            return false;
        }
    }

    private static BlockState parseBlockState(String blockId) {
        try {
            // 处理带命名空间的 ID
            Identifier id = blockId.contains(":") ?
                    new Identifier(blockId) : new Identifier("minecraft", blockId);
            Block block = Registry.BLOCK.get(id);
            if (block != Blocks.AIR || "minecraft:air".equals(blockId) || "air".equals(blockId)) {
                return block.getDefaultState();
            }
            return null;
        } catch (Exception e) {
            Aibuilder.LOGGER.warn("Unknown block: {}", blockId);
            return null;
        }
    }

    private static NbtCompound buildLitematicNBT(int sizeX, int sizeY, int sizeZ,
                                                  Map<BlockPos, BlockState> blockMap, String name) {
        NbtCompound root = new NbtCompound();

        // 收集所有出现的方块状态
        List<BlockState> palette = new ArrayList<>();
        palette.add(Blocks.AIR.getDefaultState()); // 索引0 总是空气
        Map<BlockState, Integer> paletteMap = new HashMap<>();
        paletteMap.put(Blocks.AIR.getDefaultState(), 0);

        for (BlockState state : blockMap.values()) {
            if (!paletteMap.containsKey(state)) {
                paletteMap.put(state, palette.size());
                palette.add(state);
            }
        }

        // 位宽必须与 Litematica 读取时的算法一致: max(2, ceilLog2(paletteSize))
        int paletteSize = palette.size();
        int bitsPerBlock = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));

        // Litematica 使用连续位打包（条目可跨越 long 边界），数组长度 = roundUp(总数*位宽, 64) / 64
        int totalBlocks = sizeX * sizeY * sizeZ;
        int longArraySize = (int) (((long) totalBlocks * bitsPerBlock + 63L) / 64L);
        long[] blockData = new long[longArraySize];
        long maxEntryValue = (1L << bitsPerBlock) - 1L;

        // 填充方块数据 (Litematica 索引顺序: (y*sizeZ + z)*sizeX + x，即 x 变化最快)
        int blockIndex = 0;
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = blockMap.getOrDefault(pos, Blocks.AIR.getDefaultState());
                    int paletteIdx = paletteMap.getOrDefault(state, 0);
                    packEntry(blockData, blockIndex, paletteIdx, bitsPerBlock, maxEntryValue);
                    blockIndex++;
                }
            }
        }

        // 构建方块调色板 NBT
        NbtList paletteNbt = new NbtList();
        for (BlockState state : palette) {
            NbtCompound entry = new NbtCompound();
            Identifier blockId = Registry.BLOCK.getId(state.getBlock());
            entry.putString("Name", blockId.toString());
            paletteNbt.add(entry);
        }

        // 构建区域数据
        NbtCompound regionData = new NbtCompound();
        regionData.put("BlockStatePalette", paletteNbt);
        regionData.put("BlockStates", new NbtLongArray(blockData));
        regionData.put("Entities", new NbtList());
        regionData.put("TileEntities", new NbtList());
        NbtCompound pos = new NbtCompound();
        pos.putInt("x", 0);
        pos.putInt("y", 0);
        pos.putInt("z", 0);
        regionData.put("Position", pos);
        NbtCompound size = new NbtCompound();
        size.putInt("x", sizeX);
        size.putInt("y", sizeY);
        size.putInt("z", sizeZ);
        regionData.put("Size", size);

        NbtCompound regions = new NbtCompound();
        regions.put(REGION_NAME, regionData);

        // 元数据
        NbtCompound metadata = new NbtCompound();
        metadata.putString("Author", "AIbuilder");
        metadata.putString("Description", name);
        metadata.putString("Name", name);
        NbtCompound enclosingBox = new NbtCompound();
        NbtCompound boxPos1 = new NbtCompound();
        boxPos1.putInt("x", 0); boxPos1.putInt("y", 0); boxPos1.putInt("z", 0);
        NbtCompound boxPos2 = new NbtCompound();
        boxPos2.putInt("x", sizeX - 1); boxPos2.putInt("y", sizeY - 1); boxPos2.putInt("z", sizeZ - 1);
        enclosingBox.put("pos1", boxPos1);
        enclosingBox.put("pos2", boxPos2);
        metadata.put("EnclosingBox", enclosingBox);
        NbtCompound regionSizes = new NbtCompound();
        NbtCompound rs = new NbtCompound();
        rs.putInt("x", sizeX); rs.putInt("y", sizeY); rs.putInt("z", sizeZ);
        regionSizes.put(REGION_NAME, rs);
        metadata.put("RegionSizes", regionSizes);
        metadata.putInt("RegionCount", 1);
        metadata.putInt("TotalBlocks", blockMap.size());
        metadata.putInt("TotalVolume", totalBlocks);
        metadata.putLong("TimeCreated", System.currentTimeMillis());
        metadata.putLong("TimeModified", System.currentTimeMillis());

        root.put("Metadata", metadata);
        root.put("Regions", regions);
        root.putInt("MinecraftDataVersion", SharedConstants.WORLD_VERSION); // current game data version
        root.putInt("Version", 7);
        root.putInt("SubVersion", 0);
        root.put("Entities", new NbtList());
        root.put("TileEntities", new NbtList());

        return root;
    }

    private static void packEntry(long[] data, int index, int value, int bits, long maxValue) {
        long bitStart = (long) index * bits;
        int startLong = (int) (bitStart >> 6);
        int offset = (int) (bitStart & 63);
        data[startLong] |= (value & maxValue) << offset;
        if (offset + bits > 64) {
            data[startLong + 1] |= (value & maxValue) >> (64 - offset);
        }
    }

    private static Path saveSchematicToFile(NbtCompound nbt, String name) {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve(SCHEMATIC_DIR);
            Files.createDirectories(dir);
            String safeName = name.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_");
            Path filePath = dir.resolve(safeName + ".litematic");

            NbtIo.write(nbt, filePath.toFile());
            Aibuilder.LOGGER.info("Saved schematic to: {}", filePath);
            return filePath;
        } catch (Exception e) {
            Aibuilder.LOGGER.error("Failed to save schematic file", e);
            return null;
        }
    }

    private static boolean loadInLitematica(File file, String name) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return false;

            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(file.getParentFile(), file.getName());
            if (schematic == null) {
                Aibuilder.LOGGER.error("Failed to load schematic from file: {}", file);
                return false;
            }

            // 计算放置位置（玩家前方5格）
            Direction facing = client.player.getHorizontalFacing();
            BlockPos playerPos = client.player.getBlockPos();
            BlockPos placementPos = playerPos.offset(facing, 5);

            // 创建投影放置
            SchematicPlacement placement = SchematicPlacement.createFor(schematic, placementPos, name, true, true);

            // 添加到 Litematica 管理器
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            manager.addSchematicPlacement(placement, true);
            manager.setSelectedSchematicPlacement(placement);

            Aibuilder.LOGGER.info("Loaded schematic '{}' at {}", name, placementPos);
            return true;
        } catch (Exception e) {
            Aibuilder.LOGGER.error("Failed to load schematic in Litematica", e);
            return false;
        }
    }

    /**
     * 生成 AI 系统提示词
     */
    public static String getSystemPrompt() {
        return """
                You are a Minecraft building AI assistant. The user will describe the building they want, and you must output the building data in JSON format.
                
                Output format requirements (strictly follow):
                ```json
                {
                  "size": {"x": width, "y": height, "z": depth},
                  "blocks": [
                    {"x": 0, "y": 0, "z": 0, "block": "minecraft:stone"},
                    {"x": 1, "y": 0, "z": 0, "block": "minecraft:oak_planks"}
                  ]
                }
                ```
                
                Rules:
                1. All coordinates start from 0 and must stay within the size bounds
                2. Use full Minecraft block IDs (e.g. minecraft:stone, minecraft:oak_planks)
                3. Do not list air blocks; unlisted positions default to air
                4. The building should be reasonable, aesthetically pleasing, and structurally stable
                5. First briefly explain your design approach, then output the JSON data
                6. Wrap the JSON data in a ```json``` code block
                7. Keep the building size within 32x32x32
                8. Gravity-affected blocks (sand, gravel) must be supported
                """;
    }
}
