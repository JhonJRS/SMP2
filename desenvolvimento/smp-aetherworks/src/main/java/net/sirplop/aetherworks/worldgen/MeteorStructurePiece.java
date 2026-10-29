package net.sirplop.aetherworks.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.sirplop.aetherworks.Aetherworks;

public class MeteorStructurePiece  extends StructurePiece {

    public static final StructurePieceType.ContextlessType TYPE = MeteorStructurePiece::new;

    private final PlacedMeteorSettings settings;

    protected MeteorStructurePiece(BlockPos center, float sizeX, float sizeY, float sizeZ, boolean liquidCrater) {
        super(TYPE, 0, createBoundingBox(center, sizeX, sizeY, sizeZ));
        settings = new PlacedMeteorSettings(center, sizeX, sizeY, sizeZ, liquidCrater);
    }
    private static BoundingBox createBoundingBox(BlockPos origin, float sizeX, float sizeY, float sizeZ) {
        //generate the meteor crater in a 3x3 chunk area.
        final int range = 16;
        ChunkPos chunkPos = new ChunkPos(origin);

        return new BoundingBox(chunkPos.getMinBlockX() - range, origin.getY(),
                chunkPos.getMinBlockZ() - range, chunkPos.getMaxBlockX() + range, origin.getY(),
                chunkPos.getMaxBlockZ() + range);
    }
    public MeteorStructurePiece(CompoundTag tag) {
        super(TYPE, tag);

        BlockPos pos = BlockPos.of(tag.getLong(PlacedMeteorSettings.TAG_POS));
        float sizeX = tag.getFloat(PlacedMeteorSettings.TAG_SIZE_X);
        float sizeY = tag.getFloat(PlacedMeteorSettings.TAG_SIZE_Y);
        float sizeZ = tag.getFloat(PlacedMeteorSettings.TAG_SIZE_Z);
        boolean liquidCrater = tag.getBoolean(PlacedMeteorSettings.TAG_CRATER_LIQUID);

        this.settings = new PlacedMeteorSettings(pos, sizeX, sizeY, sizeZ, liquidCrater);
    }

    public PlacedMeteorSettings getSettings() {
        return settings;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong(PlacedMeteorSettings.TAG_POS, settings.getPos().asLong());
        tag.putFloat(PlacedMeteorSettings.TAG_SIZE_X, settings.getSizeX());
        tag.putFloat(PlacedMeteorSettings.TAG_SIZE_Y, settings.getSizeY());
        tag.putFloat(PlacedMeteorSettings.TAG_SIZE_Z, settings.getSizeZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator,
                            RandomSource rand, BoundingBox bounds, ChunkPos chunkPos, BlockPos blockPos) {
        MeteorPlacer.place(level, settings, bounds, rand);
    }
}
