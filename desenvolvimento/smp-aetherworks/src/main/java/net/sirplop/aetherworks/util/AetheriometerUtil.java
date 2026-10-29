package net.sirplop.aetherworks.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.api.capabilities.IAetheriometerCap;
import net.sirplop.aetherworks.capabilities.AetheriometerChunkCapability;

public class AetheriometerUtil {

    public static double getAverageInSurroundings(Level level, BlockPos block, int chunkRadius) {
        /*
            Essentially what this does is kinda what lights do, but it cuts out all the other
            chunks that could get propagated to and only does the 4 neighbor chunks to the player
            because that's all we check. If their standing on the highest point (in the meteorite chunk),
            then it returns that chunk's value instead.
         */

        ChunkPos pos = level.getChunkAt(block).getPos();
        double middle = 0;
        double[] points = new double[4];
        for (int x = pos.x - chunkRadius; x <= pos.x + chunkRadius; x++) {
            for (int z = pos.z - chunkRadius; z <= pos.z + chunkRadius; z++) {
                int am = getData(level, new ChunkPos(x, z));
                propagate(points, x, z, 0, pos.x - 1, pos.z, am, chunkRadius);
                propagate(points, x, z, 1, pos.x + 1, pos.z, am, chunkRadius);
                propagate(points, x, z, 2, pos.x, pos.z - 1, am, chunkRadius);
                propagate(points, x, z, 3, pos.x, pos.z + 1, am, chunkRadius);

                if (z == pos.z && x == pos.x)
                    middle = am * chunkRadius;
            }
        }
        double west = points[0];
        double east = points[1];
        double north = points[2];
        double south = points[3];

        if (middle > west && middle > east && middle > north && middle > south)
            return middle; //we're on top of the highest point

        //must mix into corners for bilinear interpolation to work good.
        double nw = Utils.mix(north, west, 0.5);
        double ne = Utils.mix(north, east, 0.5);
        double sw = Utils.mix(south, west, 0.5);
        double se = Utils.mix(south, east, 0.5);

        double xR = block.getX() == 0 ? 0 : Mth.sign(block.getX()) == 1 ? (block.getX() % 16) / 16d : 1 + (((1 + block.getX()) % 16) / 16d);
        double zR = block.getZ() == 0 ? 0 : Mth.sign(block.getZ()) == 1 ? (block.getZ() % 16) / 16d : 1 + (((1 + block.getZ()) % 16) / 16d);

        double tMix = Utils.mix(nw, ne, xR);
        double bMix = Utils.mix(sw, se, xR);

        return Math.max(0, Utils.mix(tMix, bMix, zR));
    }
    public static int getData(Level level, ChunkPos pos) {
        if (level.hasChunk(pos.x, pos.z)) { //don't load chunks!
            IAetheriometerCap capability = AetheriometerChunkCapability.getData(level, pos).orElseThrow();
            return capability.getData();
        }
        return 0;
    }

    private static void propagate(double[] points, int x, int z, int index, int xT, int zT, double value, int radius) {
        int depth = Math.max(Math.abs(x - xT), Math.abs(z - zT));
        double falloff = value * (radius - depth);
        points[index] = Math.max(falloff, points[index]);
    }
}
