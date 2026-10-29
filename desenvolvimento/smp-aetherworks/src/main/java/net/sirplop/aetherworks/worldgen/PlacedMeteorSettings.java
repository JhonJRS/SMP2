package net.sirplop.aetherworks.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class PlacedMeteorSettings {
    public static String TAG_POS = "aw.pos";
    public static String TAG_SIZE_X = "aw.size.x";
    public static String TAG_SIZE_Y = "aw.size.y";
    public static String TAG_SIZE_Z = "aw.size.z";
    public static String TAG_CRATER_LIQUID = "aw.liquid_crater";

    private final BlockPos pos;
    private final float meteorSizeX;
    private final float meteorSizeY;
    private final float meteorSizeZ;
    private final boolean liquidCrater;

    public PlacedMeteorSettings(BlockPos pos, float sizeX, float sizeY, float sizeZ, boolean liquidCrater) {
        this.pos = pos;
        this.meteorSizeX = sizeX;
        this.meteorSizeY = sizeY;
        this.meteorSizeZ = sizeZ;
        this.liquidCrater = liquidCrater;
    }

    public BlockPos getPos() {
        return pos;
    }
    public float getSizeX() { return meteorSizeX; }
    public float getSizeY() { return meteorSizeY; }
    public float getSizeZ() { return meteorSizeZ; }
    public boolean getLiquidCrater() { return liquidCrater; }

    public CompoundTag write(CompoundTag tag) {
        tag.putLong(TAG_POS, pos.asLong());
        tag.putFloat(TAG_SIZE_X, meteorSizeX);
        tag.putFloat(TAG_SIZE_Y, meteorSizeY);
        tag.putFloat(TAG_SIZE_Z, meteorSizeZ);
        return tag;
    }

    public static PlacedMeteorSettings read(CompoundTag tag) {
        BlockPos pos = BlockPos.of(tag.getLong(TAG_POS));
        float sizeX = tag.getFloat(TAG_SIZE_X);
        float sizeY = tag.getFloat(TAG_SIZE_Y);
        float sizeZ = tag.getFloat(TAG_SIZE_Z);
        boolean liquidCrater = tag.getBoolean(TAG_CRATER_LIQUID);

        return new PlacedMeteorSettings(pos, sizeX, sizeY, sizeZ, liquidCrater);
    }

    @Override
    public String toString() {
        return "PlacedMeteorSettings [pos=" + pos + ", meteorite size= { " +meteorSizeX + ", "+meteorSizeY+", "+meteorSizeZ+" }, isLiquid="+liquidCrater+"]";
    }
}
