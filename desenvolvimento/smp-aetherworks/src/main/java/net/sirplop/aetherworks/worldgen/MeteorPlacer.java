package net.sirplop.aetherworks.worldgen;

import com.google.common.math.StatsAccumulator;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.sirplop.aetherworks.AWRegistry;

import java.util.*;

public class MeteorPlacer {
    public static Map<ChunkPos, Integer> map = new HashMap<>();


    public static void place(LevelAccessor level, PlacedMeteorSettings settings, BoundingBox boundingBox,
                             RandomSource random) {
        var placer = new MeteorPlacer(level, settings, boundingBox, random);

        int moveX = random.nextInt(3);
        moveX = (moveX == 2 ? -1 : moveX);
        int moveZ = random.nextInt(3);
        moveZ = (moveZ == 2 ? -1 : moveZ);
        int moveY = -random.nextInt(2);

        int xStep = random.nextInt(2) + 1;
        int yStep = random.nextInt(2) + 1;
        int zStep = random.nextInt(2) + 1;

        BlockPos betweenTarget = placer.pos.offset((int)Math.floor(moveX * xStep * 0.5),
                (int)Math.floor(moveY * yStep * 0.5), (int)Math.floor(moveZ * zStep * 0.5));
        BlockPos target = placer.pos.offset(moveX * xStep, moveY * yStep, moveZ * zStep);

        //if (!placer.isLandVerySloped() || placer.liquidCrater) //always make a crater if there's water, 'cuz we need to fill it in.
            placer.placeCrater();

        placer.placeMeteor(placer.pos);
        if (target != betweenTarget && betweenTarget != placer.pos)
            placer.placeMeteor(betweenTarget);
        if (target != placer.pos)
            placer.placeMeteor(target);
    }

    private final BlockState aetherium;
    private final BlockState suevite;
    private final BlockPos pos;
    private final int x;
    private final int y;
    private final int z;
    private final double meteoriteSizeX;
    private final double meteoriteSizeY;
    private final double meteoriteSizeZ;
    private boolean liquidCrater;
    private final BoundingBox boundingBox;
    private final LevelAccessor level;
    private final RandomSource random;

    private MeteorPlacer(LevelAccessor level, PlacedMeteorSettings settings, BoundingBox boundingBox,
                            RandomSource random) {
        this.boundingBox = boundingBox;
        this.level = level;
        this.random = random;
        this.pos = settings.getPos();
        this.x = settings.getPos().getX();
        this.y = settings.getPos().getY();
        this.z = settings.getPos().getZ();
        this.meteoriteSizeX = settings.getSizeX();
        this.meteoriteSizeY = settings.getSizeY();
        this.meteoriteSizeZ = settings.getSizeZ();
        this.liquidCrater = settings.getLiquidCrater();


        this.aetherium = AWRegistry.AETHERIUM_ORE.get().defaultBlockState();
        this.suevite = AWRegistry.SUEVITE.get().defaultBlockState();
    }

    private boolean isLandVerySloped() {
        StatsAccumulator stats = new StatsAccumulator();
        int scanRadiusX = (int) Math.max(1, meteoriteSizeX * 3);
        int scanRadiusZ = (int) Math.max(1, meteoriteSizeZ * 3);
        for (int x = -scanRadiusX; x <= scanRadiusX; x++) {
            for (int z = -scanRadiusZ; z <= scanRadiusZ; z++) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX() + x, pos.getZ() + z);
                stats.add(h);
            }
        }
        return stats.populationVariance() > 15; //crater on the side of a mountain == biiiiiig slope, no good, very ugly.
    }

    private void placeCrater() {
        final int maxY = Math.min(y + 50, level.getMaxBuildHeight());
        final int craterAdjust = 5;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        List<BlockPos> touchedPos = new ArrayList<>();
        double meteorSize = (meteoriteSizeX + meteoriteSizeY + meteoriteSizeZ) / 3;
        int end = y + (int)Math.ceil(meteoriteSizeY);
        for (int j = maxY; j >= end; j--) {
            blockPos.setY(j);
            boolean belowSeaLevel = j < level.getSeaLevel();

            for (int i = boundingBox.minX(); i <= boundingBox.maxX(); i++) {
                blockPos.setX(i);

                for (int k = boundingBox.minZ(); k <= boundingBox.maxZ(); k++) {
                    blockPos.setZ(k);
                    final double dx = i - x;
                    final double dz = k - z;
                    final double h = y - meteorSize + craterAdjust;

                    final double distanceFrom = dx * dx + dz * dz;

                    if (j > h + distanceFrom * 0.08) {

                        BlockState origState = level.getBlockState(blockPos);
                        if ((origState.isAir() && !belowSeaLevel) || origState.is(BlockTags.FEATURES_CANNOT_REPLACE)
                            || origState.is(BlockTags.LEAVES)
                            || origState.is(BlockTags.LOGS)) //stop replacing trees!
                            continue;

                        if (belowSeaLevel)
                            touchedPos.add(blockPos);
                        if (origState.getFluidState().is(Fluids.WATER) && origState.getFluidState().isSource())
                            liquidCrater = true; //if it TOUCHES water, then this is FULL OF WATER.

                        //sink the current block so the crater looks old and a part of the landscape.
                        //also make sure we don't sink blocks from overhangs, because that causes really weird formations of floating blocks.
                        if (!origState.isAir()
                                && j > level.getMinBuildHeight() + 1
                                && origState.getFluidState().isEmpty()
                                && !origState.is(BlockTags.REPLACEABLE)
                                && !level.getBlockState(blockPos.offset(0, -1, 0)).isAir())
                        {
                            if (liquidCrater && belowSeaLevel && (origState.is(Blocks.GRASS_BLOCK) || origState.is(Blocks.PODZOL) || origState.is(Blocks.MYCELIUM)))
                                put(level, blockPos, Blocks.DIRT.defaultBlockState()); //no grass underwater!
                            else
                                put(level, blockPos.offset(0, -1, 0), origState);
                        }
                        if (liquidCrater && belowSeaLevel)
                            put(level, blockPos, Blocks.WATER.defaultBlockState());
                        else
                            put(level, blockPos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        if (liquidCrater)
        {
            for (BlockPos pos : touchedPos) {
                BlockState origState = level.getBlockState(pos);
                if (origState.isAir()) {
                    put(level, pos, Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    private void placeMeteor(BlockPos place) {
        int x = place.getX();
        int y = place.getY();
        int z = place.getZ();

        final int xStart = (int)Math.ceil(x - this.meteoriteSizeX);
        final int xEnd = (int)Math.ceil(x + this.meteoriteSizeX);
        final int yStart = (int)Math.ceil(y - this.meteoriteSizeY);
        final int yEnd = (int)Math.ceil(y + this.meteoriteSizeY) - 2;
        final int zStart = (int)Math.ceil(z - this.meteoriteSizeZ);
        final int zEnd = (int)Math.ceil(z + this.meteoriteSizeZ);


        double sizeX = (meteoriteSizeX * meteoriteSizeX) * 0.5;
        double sizeY = (meteoriteSizeY * meteoriteSizeY) * 0.5;
        double sizeZ = (meteoriteSizeZ * meteoriteSizeZ) * 0.5;

        final double size = Math.max(sizeX, Math.max(sizeY, sizeZ));
        final double sizeInternal = size / 10;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = xStart; i <= xEnd; i++) {
            pos.setX(i);
            for (int k = zStart; k <= zEnd; k++) {
                pos.setZ(k);
                ChunkPos chunk = new ChunkPos(i >> 4, k >> 4);
                for (int j = yStart; j < yEnd; j++) {
                    pos.setY(j);

                    var dx = i - x;
                    var dy = j - y;
                    var dz = k - z;
                    double check = dx * dx + dy * dy * (j < y ? 1.6 : 0.4) + dz * dz;
                    if (check < size) {
                        if (check < sizeInternal) {
                            if (put(level, pos, aetherium)) {
                                if (map.containsKey(chunk))
                                    map.put(chunk, map.get(chunk) + 1);
                                else
                                    map.put(chunk, 1);
                            }
                        }
                        else
                            put(level, pos, suevite);
                    }
                }
            }
        }
    }
    public boolean put(LevelAccessor level, BlockPos pos, BlockState blk) {
        final BlockState original = level.getBlockState(pos);

        if (original.getBlock() == Blocks.BEDROCK || original.getBlock() == aetherium.getBlock() || original == blk) {
            return false;
        }

        level.setBlock(pos, blk, Block.UPDATE_ALL);
        return true;
    }
}
