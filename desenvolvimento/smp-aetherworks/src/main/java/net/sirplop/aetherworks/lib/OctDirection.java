package net.sirplop.aetherworks.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum OctDirection implements StringRepresentable {
    NONE(new Direction[0]),
    LEFT(Direction.EAST),
    RIGHT(Direction.WEST),
    FRONT(Direction.SOUTH),
    BACK(Direction.NORTH),
    LEFT_FRONT(LEFT, FRONT),
    LEFT_BACK(LEFT, BACK),
    RIGHT_FRONT(RIGHT, FRONT),
    RIGHT_BACK(RIGHT, BACK);

    OctDirection(OctDirection... parents)
    {
        List<Direction> facings = new ArrayList<>();
        for (OctDirection parent : parents)
        {
            facings.addAll(Arrays.asList(parent.directionOffsets));
        }

        Direction[] array = new Direction[facings.size()];
        array = facings.toArray(array);
        this.directionOffsets = array;
    }

    OctDirection(Direction... directionOffsets)
    {
        this.directionOffsets = directionOffsets;
    }

    private final Direction[] directionOffsets;

    @Override
    public @Nonnull String getSerializedName() {
        return this.name().toLowerCase();
    }

    public int[] getOffset() {
        return getOffset(this);
    }

    public OctDirection opposite() {
        switch (this) {
            case LEFT -> {
                return RIGHT;
            }
            case RIGHT -> {
                return LEFT;
            }
            case FRONT -> {
                return BACK;
            }
            case BACK -> {
                return FRONT;
            }
            case LEFT_FRONT -> {
                return RIGHT_BACK;
            }
            case LEFT_BACK -> {
                return RIGHT_FRONT;
            }
            case RIGHT_FRONT -> {
                return LEFT_BACK;
            }
            case RIGHT_BACK -> {
                return LEFT_FRONT;
            }
            default -> {
                return NONE;
            }
        }
    }

    public static int[] getOffset(OctDirection direction) {
        switch (direction) {
            case NONE -> {
                return new int[]{0, 0};
            }
            case LEFT -> {
                return new int[]{0, -1};
            }
            case RIGHT -> {
                return new int[]{0, 1};
            }
            case FRONT -> {
                return new int[]{1, 0};
            }
            case BACK -> {
                return new int[]{-1, 0};
            }
            case LEFT_FRONT -> {
                return new int[]{1, -1};
            }
            case LEFT_BACK -> {
                return new int[]{-1, -1};
            }
            case RIGHT_FRONT -> {
                return new int[]{1, 1};
            }
            case RIGHT_BACK -> {
                return new int[]{-1, 1};
            }
        }
        return new int[]{0, 0};
    }

    public static OctDirection getFromOffset(int x, int z) {
        if (x == -1) {
            if (z == -1) return LEFT_BACK;
            else if (z == 0) return BACK;
            else if (z == 1) return RIGHT_BACK;
        } else if (x == 0) {
            if (z == -1) return LEFT;
            else if (z == 0) return NONE;
            else if (z == 1) return RIGHT;
        } else {
            if (z == -1) return LEFT_FRONT;
            else if (z == 0) return FRONT;
            else if (z == 1) return RIGHT_FRONT;
        }
        return NONE;
    }

    public int toBlockRot() {
        switch (this) {
            case LEFT, LEFT_FRONT -> {
                return 270;
            }
            case RIGHT, RIGHT_BACK -> {
                return 90;
            }
            case BACK, LEFT_BACK -> {
                return 180;
            }
            default -> {
                return 0;
            }
        }
    }
    public BlockPos offsetBlock(BlockPos in) {
        int[] offset = getOffset(this);
        return in.east(offset[0]).south(offset[1]);
    }
}