package com.example.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** Highlights selected block types within a modest radius, without changing server traffic or game rules. */
public final class BlockESPModule extends Module {
    private static final int RADIUS = 8;
    private static final int RESCAN_INTERVAL_TICKS = 8;
    private static final int MAX_MARKERS = 64;

    private final List<BlockPos> targets = new ArrayList<>();
    private int ticksUntilScan;

    public BlockESPModule() {
        super("Block ESP", "Render", "Outlines nearby diamond ore, chests and spawners in loaded client chunks.");
    }

    @Override
    public void onEnable() {
        targets.clear();
        ticksUntilScan = 0;
    }

    @Override
    public void onDisable() {
        targets.clear();
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) {
            targets.clear();
            return;
        }

        if (--ticksUntilScan <= 0) {
            rescan();
            ticksUntilScan = RESCAN_INTERVAL_TICKS;
        }

        if (targets.isEmpty() || mc.levelRenderer == null) return;
        try (Gizmos.TemporaryCollection ignored = mc.levelRenderer.collectPerFrameGizmos()) {
            for (BlockPos pos : targets) {
                Block block = mc.level.getBlockState(pos).getBlock();
                int color = colorFor(block);
                Gizmos.cuboid(pos, GizmoStyle.stroke(color, 1.5F)).persistForMillis(120);
            }
        }
    }

    private void rescan() {
        targets.clear();
        BlockPos origin = mc.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        scan:
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!mc.level.hasChunkAt(cursor)) continue;
                    Block block = mc.level.getBlockState(cursor).getBlock();
                    if (isTarget(block)) {
                        targets.add(cursor.immutable());
                        if (targets.size() >= MAX_MARKERS) break scan;
                    }
                }
            }
        }
    }

    private boolean isTarget(Block block) {
        return block == Blocks.DIAMOND_ORE
                || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.SPAWNER;
    }

    private int colorFor(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return 0xFF52E5FF;
        if (block == Blocks.SPAWNER) return 0xFFFF625F;
        return 0xFFF2F5F8;
    }
}
