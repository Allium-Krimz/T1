package com.example.modules;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Scores loaded chunks by how much "unnatural" stuff they contain (redstone/automation parts, storage,
 * storage-style metal blocks, utility blocks) and reports chunks over a threshold.
 * It is a heuristic: villages, bastions and strongholds can score too, so treat results as "worth a look".
 * Passive only: reads chunks the server already sent, sends nothing.
 */
public final class SusChunkFinderModule extends Module {
    private static final int MAX_CHUNK_RADIUS = 16;
    private static final int CHUNKS_PER_TICK = 3;
    private static final int SCORE_THRESHOLD = 14;
    private static final int RESET_TICKS = 2400;

    // Blocks that essentially never generate in bulk naturally.
    private static final Set<Block> UNNATURAL = Set.of(
            Blocks.HOPPER, Blocks.DISPENSER, Blocks.DROPPER, Blocks.OBSERVER,
            Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.REPEATER, Blocks.COMPARATOR,
            Blocks.REDSTONE_LAMP, Blocks.REDSTONE_BLOCK, Blocks.TARGET,
            Blocks.BEACON, Blocks.ENCHANTING_TABLE, Blocks.ANVIL,
            Blocks.NETHERITE_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK,
            Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK, Blocks.LAPIS_BLOCK,
            Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.BARREL, Blocks.ENDER_CHEST, Blocks.TRAPPED_CHEST,
            Blocks.SHULKER_BOX, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX
    );

    private final Set<Long> scanned = new HashSet<>();
    private final Map<Long, Integer> flagged = new HashMap<>();
    private int tickCounter;

    public SusChunkFinderModule() {
        super("Sus Chunk Finder", "World", "Flags loaded chunks with unusually many player-built blocks.");
    }

    @Override
    public void onEnable() {
        scanned.clear();
        flagged.clear();
        tickCounter = 0;
    }

    @Override
    public void onDisable() {
        scanned.clear();
        flagged.clear();
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;
        if (++tickCounter >= RESET_TICKS) {
            tickCounter = 0;
            scanned.clear(); // chunks change over time, look again now and then
        }

        int radius = Math.min(MAX_CHUNK_RADIUS, Math.max(2, mc.options.getEffectiveRenderDistance()));
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int budget = CHUNKS_PER_TICK;

        // Nearest chunks first, so results show up around you before far away.
        for (int ring = 0; ring <= radius && budget > 0; ring++) {
            for (int dx = -ring; dx <= ring && budget > 0; dx++) {
                for (int dz = -ring; dz <= ring && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    long key = (((long) cx) << 32) ^ (cz & 0xFFFFFFFFL);
                    if (scanned.contains(key)) continue;

                    LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    scanned.add(key);
                    budget--;

                    int score = scoreChunk(chunk);
                    if (score >= SCORE_THRESHOLD) {
                        Integer last = flagged.get(key);
                        if (last == null || score >= last + 10) {
                            flagged.put(key, score);
                            mc.player.displayClientMessage(Component.literal(
                                    "[T1 Sus Chunk] Chunk " + cx + ", " + cz + " (score " + score
                                            + ") near X: " + (cx * 16 + 8) + " Z: " + (cz * 16 + 8)
                            ), false);
                        }
                    }
                }
            }
        }

        if (scanned.size() > 8192) scanned.clear();
        if (flagged.size() > 2048) flagged.clear();
    }

    private int scoreChunk(LevelChunk chunk) {
        int score = 0;
        LevelChunkSection[] sections = chunk.getSections();
        for (LevelChunkSection section : sections) {
            if (section == null || section.hasOnlyAir()) continue;
            // Cheap palette check first: skip the 4096-block walk when nothing interesting is in here.
            if (!section.maybeHas(state -> UNNATURAL.contains(state.getBlock()))) continue;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (UNNATURAL.contains(state.getBlock())) score++;
                    }
                }
            }
        }
        return score;
    }
}
