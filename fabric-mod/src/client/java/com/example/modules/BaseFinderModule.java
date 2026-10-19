package com.example.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Looks through every chunk the client has loaded (up to render distance) and reports chunks that hold a
 * cluster of storage blocks / spawners, which is what a player base usually looks like.
 * Purely passive: it reads chunk data the server already sent, it never sends packets.
 */
public final class BaseFinderModule extends Module {
    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final int MAX_CHUNK_RADIUS = 16;
    private static final int MIN_STORAGE_FOR_BASE = 3;
    private static final int MAX_REPORTS_PER_PASS = 4;

    private static final Set<Block> STORAGE = Set.of(
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.ENDER_CHEST,
            Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER,
            Blocks.SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX
    );
    private static final Set<Block> UTILITY = Set.of(
            Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE, Blocks.BEACON, Blocks.ANVIL
    );

    /** chunk key -> number of storage blocks at the time it was last reported. */
    private final Map<Long, Integer> reported = new HashMap<>();
    private int ticksUntilScan;

    public BaseFinderModule() {
        super("Base Finder", "World", "Reports chunks with clusters of chests, barrels, shulkers or spawners.");
    }

    @Override
    public void onEnable() {
        reported.clear();
        ticksUntilScan = 0;
    }

    @Override
    public void onDisable() {
        reported.clear();
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        if (--ticksUntilScan > 0) return;
        ticksUntilScan = SCAN_INTERVAL_TICKS;

        int radius = Math.min(MAX_CHUNK_RADIUS, Math.max(2, mc.options.getEffectiveRenderDistance()));
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int reports = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                int storage = 0;
                int utility = 0;
                int spawners = 0;
                BlockPos sample = null;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    BlockState state = chunk.getBlockState(pos);
                    Block block = state.getBlock();
                    if (block == Blocks.SPAWNER) {
                        spawners++;
                        if (sample == null) sample = pos;
                    } else if (STORAGE.contains(block)) {
                        storage++;
                        if (sample == null) sample = pos;
                    } else if (UTILITY.contains(block)) {
                        utility++;
                    }
                }

                boolean isBase = storage >= MIN_STORAGE_FOR_BASE || spawners > 0 && storage > 0;
                if (!isBase || sample == null) continue;

                long key = (((long) cx) << 32) ^ (cz & 0xFFFFFFFFL);
                Integer last = reported.get(key);
                // Report new chunks, and re-report if the base has clearly grown.
                if (last != null && storage < last + 4) continue;
                reported.put(key, storage);

                mc.player.displayClientMessage(Component.literal(
                        "[T1 Base Finder] Chunk " + cx + ", " + cz + ": " + storage + " storage"
                                + (spawners > 0 ? ", " + spawners + " spawner(s)" : "")
                                + (utility > 0 ? ", " + utility + " utility" : "")
                                + " near X: " + sample.getX() + " Y: " + sample.getY() + " Z: " + sample.getZ()
                ), false);
                if (++reports >= MAX_REPORTS_PER_PASS) return;
            }
        }

        if (reported.size() > 4096) reported.clear();
    }
}
