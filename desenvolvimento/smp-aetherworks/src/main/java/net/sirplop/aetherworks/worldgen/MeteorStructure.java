package net.sirplop.aetherworks.worldgen;

import com.google.common.math.StatsAccumulator;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

// This is basically a heavily modified Applied Energistics 2 meteorite.
public class MeteorStructure extends Structure {

    public static final com.mojang.serialization.MapCodec<MeteorStructure> CODEC = simpleCodec(MeteorStructure::new);
    public static StructureType<MeteorStructure> TYPE = () -> MeteorStructure.CODEC;


    public MeteorStructure(StructureSettings pSettings) {
        super(pSettings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        context.random().setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        if (!context.random().nextBoolean()) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG, (structurePiecesBuilder) -> {
            generatePieces(structurePiecesBuilder, context);
        });
    }

    @Override
    public StructureType<?> type() {
        return TYPE;
    }

    private static void generatePieces(StructurePiecesBuilder piecesBuilder, GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        RandomSource random = context.random();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        ChunkGenerator generator = context.chunkGenerator();

        final int centerX = chunkPos.getMinBlockX() + 4 + random.nextInt(8);
        final int centerZ = chunkPos.getMinBlockZ() + 4 + random.nextInt(8);
        final float meteoriteXSize = random.nextFloat() * 4.0f + 1;
        final float meteoriteYSize = random.nextFloat() * 3.0f + 2; //our meteor is not quite a sphere!
        final float meteoriteZSize = random.nextFloat() * (meteoriteXSize > 2.5f ? 2.0f : 4.0f) + 1;

        var t2 = generator.getBiomeSource().getBiomesWithin(centerX, generator.getSeaLevel(), centerZ, 4,
                context.randomState().sampler());

        boolean hasWateryBiome = t2.stream().anyMatch(b -> b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_DEEP_OCEAN)
                || b.is(BiomeTags.IS_RIVER) || b.is(BiomeTags.IS_BEACH));

        final int yOffset = (int) Math.ceil(meteoriteYSize) + 2;

        final Heightmap.Types heightmapType = Heightmap.Types.OCEAN_FLOOR_WG;

        // Accumulate stats about the surrounding heightmap
        StatsAccumulator stats = new StatsAccumulator();
        int scanRadiusX = (int) Math.max(1, meteoriteXSize * 2);
        int scanRadiusZ = (int) Math.max(1, meteoriteZSize * 2);
        for (int x = -scanRadiusX; x <= scanRadiusX; x++) {
            for (int z = -scanRadiusZ; z <= scanRadiusZ; z++) {
                int h = generator.getBaseHeight(centerX + x, centerZ + z, heightmapType, heightAccessor,
                        context.randomState());
                stats.add(h);
            }
        }

        int centerY = (int) stats.mean();
        // Spawn it down a bit further with a high variance.
        if (stats.populationVariance() > 5) {
            centerY -= (int) ((stats.mean() - stats.min()) * 0.75);
        }
        // Offset caused by the height of the meteor - make it nice n' buried.
        centerY -= yOffset;
        // If we seemingly don't have enough space to spawn (as can happen in flat chunks generators)
        // we snugly generate it on bedrock.
        centerY = Math.max(heightAccessor.getMinBuildHeight() + yOffset, centerY);

        BlockPos actualPos = new BlockPos(centerX, centerY, centerZ);
        boolean hasWater = locateWaterAroundTheCrater(actualPos, (meteoriteXSize + meteoriteZSize), context, hasWateryBiome);
        piecesBuilder.addPiece(new MeteorStructurePiece(actualPos, meteoriteXSize, meteoriteYSize, meteoriteZSize, hasWater));
    }

    private static boolean locateWaterAroundTheCrater(BlockPos pos, float radius, GenerationContext context, boolean hasWateryBiome) {
        //shortcut
        if (hasWateryBiome)
            return true; //this should always be watery

        var generator = context.chunkGenerator();
        var heightAccessor = context.heightAccessor();

        final int seaLevel = generator.getSeaLevel();
        final int maxY = seaLevel - 1;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        blockPos.setY(maxY);
        for (int i = pos.getX() - 40; i <= pos.getX() + 40; i++) {
            blockPos.setX(i);

            for (int k = pos.getZ() - 40; k <= pos.getZ() + 40; k++) {
                blockPos.setZ(k);
                final double dx = i - pos.getX();
                final double dz = k - pos.getZ();
                final double h = pos.getY() - radius + 1;

                final double distanceFrom = dx * dx + dz * dz;

                if (maxY > h + distanceFrom * 0.04 && maxY < h + distanceFrom * 0.08) {
                    int height = generator.getBaseHeight(blockPos.getX(), blockPos.getZ(), Heightmap.Types.OCEAN_FLOOR,
                            heightAccessor, context.randomState());
                    if (height < seaLevel) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
