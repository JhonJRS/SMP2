package net.sirplop.aetherworks.util;
import net.sirplop.aetherworks.util.Compat121;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.network.MessageSyncItemEntityTag;
import net.sirplop.aetherworks.network.PacketHandler;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Utils {

    public static final String SUCK_ITEM_TAG = "awsucc";
    public static final Vector3f AETHERIUM_COLOR = new Vector3f(0, 0.72F, 0.95F);
    public static final Color AETHERIUM_PROJECTILE_COLOR = new Color(130, 183, 255);

    public static void dropItemIntoWorld(Level level, BlockPos pos, ItemStack item) {
        Random rand = new Random();

        if (item != null && item.getCount() > 0) {
            float rx = rand.nextFloat() * 0.8F + 0.1F;
            float ry = rand.nextFloat() * 0.8F + 0.1F;
            float rz = rand.nextFloat() * 0.8F + 0.1F;

            ItemEntity entityItem = new ItemEntity(level,
                    pos.getX() + rx, pos.getY() + ry, pos.getZ() + rz,
                    item.copy());
            entityItem.setDefaultPickUpDelay();

            if (Compat121.hasTag(item)) {
                Compat121.setTag(entityItem.getItem(), Compat121.tag(item).copy());
            }

            float factor = 0.05F;
            entityItem.lerpMotion(
                    rand.nextGaussian() * factor,
                    rand.nextGaussian() * factor + 0.2F,
                    rand.nextGaussian() * factor);
            level.addFreshEntity(entityItem);
            item.setCount(0);
        }
    }
    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }
    public static Vector3f colorIntToVector(int r, int g, int b) {
        return new Vector3f(r / 255f, g / 255f, b / 255f);
    }
    public static boolean hasEnoughDurability(ItemStack item, int requiredDamage) {
        return item.getDamageValue() < item.getMaxDamage() - requiredDamage;
    }

    /**
     * Tries to harvest the block at the given location using the given item.
     * @return  True if successful
     */
    public static List<ItemStack> breakAndHarvestBlock(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack itemInHand,
                                               @Nullable Direction dropDirection, @Nullable Predicate<BlockState> matchBlock, boolean allowBreakBlockEntities, boolean makeSuck) {
        if (level.isClientSide() || !level.isLoaded(pos))
            return List.of(); //clients can't mine, and unloaded chunks can't have stuff broken in them!

        BlockState targetState = level.getBlockState(pos);
        if (targetState.isAir())
            return List.of(); //can't harvest air!

        if (!allowBreakBlockEntities && level.getBlockEntity(pos) != null)
            return List.of(); //don't break block entities if we don't want to!

        boolean canHarvest = targetState.getBlock().canHarvestBlock(targetState, level, pos, player);
        boolean match = matchBlock == null || matchBlock.test(targetState);

        if (!match || !canHarvest)
            return List.of();

        CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, level.getBlockState(pos));
        level.destroyBlock(pos, false, player);
        List<ItemStack> drops = List.of();
        if (!player.isCreative()) {
            boolean correctToolForDrops = itemInHand.isCorrectToolForDrops(targetState);
            if (correctToolForDrops) {
                targetState.spawnAfterBreak(level, pos, itemInHand, true);
                drops = Block.getDrops(targetState, level, pos, level.getBlockEntity(pos), player, itemInHand);
                if (dropDirection == null) {
                    drops.forEach(e -> {
                        List<ItemEntity> entities = dropItemStack(level, pos.getCenter(), e);
                        for (ItemEntity ent : entities) {
                            if (makeSuck && ent != null) {
                                ent.addTag(SUCK_ITEM_TAG);
                                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MessageSyncItemEntityTag(ent, SUCK_ITEM_TAG));
                            }
                        }
                    });
                } else {
                    drops.forEach(e -> {
                        ItemEntity ent = popResourceFromFace(level, pos, dropDirection, e);
                        if (makeSuck && ent != null) {
                            ent.addTag(SUCK_ITEM_TAG);
                            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MessageSyncItemEntityTag(ent, SUCK_ITEM_TAG));
                        }
                    });
                }
            }
        }
        return drops;
    }

    public static List<Integer> getAllSlotsContaining(Item item, Inventory inventory) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item))
                ret.add(i);
        }
        return ret;
    }
    public static boolean tryRemoveAmount(Item item, Inventory inventory, int amount) {
        if (amount <= 0)
            return true;

        if (inventory.countItem(item) < amount)
            return false;

        int toRemove = amount;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (inventory.getItem(i).is(item)) {
                int stackCount = stack.getCount();
                int rem =  Math.max(0, stackCount- toRemove);
                inventory.removeItem(i, stackCount - rem);
                toRemove -= stackCount - rem;
                if (toRemove <= 0)
                    return true;
            }
        }
        return false;
    }

    public static ItemStack getPlayerInteractionHandItem(Player player, InteractionHand hand) {
        if (Objects.requireNonNull(hand) == InteractionHand.OFF_HAND) {
            return player.getOffhandItem();
        }
        return player.getMainHandItem();
    }

    public static boolean sendPlayerMessage(ServerPlayer player, String message) {
        if (player == null)
            return false;
        PlayerChatMessage chatMessage = PlayerChatMessage.unsigned(player.getUUID(), message);
        player.createCommandSourceStack().sendChatMessage(new OutgoingChatMessage.Player(chatMessage), false, ChatType.bind(ChatType.CHAT, player));
        return true;
    }

    public static Vec3 multiLerp(double weight, Vec3... values) {
        if (weight >= values.length - 1)
            return values[values.length - 1];
        if (weight <= 0)
            return values[0];

        int tFloor = (int)Math.floor(weight);

        Vec3 left = values[tFloor];
        Vec3 right = values[tFloor + 1];

        double t = weight - tFloor;

        double xAvg = mix(left.x(), right.x(), t);
        double yAvg = mix(left.y(), right.y(), t);
        double zAvg = mix(left.z(), right.z(), t);

        return new Vec3(xAvg, yAvg, zAvg);
    }
    public static Vector3f multiLerp(double weight, Vector3f... values) {
        if (weight >= values.length - 1)
            return values[values.length - 1];
        if (weight <= 0)
            return values[0];

        int tFloor = (int)Math.floor(weight);

        Vector3f left = values[tFloor];
        Vector3f right = values[tFloor + 1];

        double t = weight - tFloor;

        float xAvg = (float)mix(left.x(), right.x(), t);
        float yAvg = (float)mix(left.y(), right.y(), t);
        float zAvg = (float)mix(left.z(), right.z(), t);

        return new Vector3f(xAvg, yAvg, zAvg);
    }
    public static Vec3 multiLerpColor(double weight, Vec3... values) {
        if (weight >= values.length - 1)
            return values[values.length - 1];
        if (weight <= 0)
            return values[0];

        int tFloor = (int)Math.floor(weight);

        Vec3 left = values[tFloor];
        Vec3 right = values[tFloor + 1];

        double t = weight - tFloor;

        int rAvg = (int)Math.round(mix(left.x(), right.x(), t));
        int gAvg = (int)Math.round(mix(left.y(), right.y(), t));
        int bAvg = (int)Math.round(mix(left.z(), right.z(), t));

        return new Vec3(rAvg, gAvg, bAvg);
    }
    public static Vector3f multiLerpColor(double weight, Vector3f... values) {
        if (weight >= values.length - 1)
            return values[values.length - 1];
        if (weight <= 0)
            return values[0];

        int tFloor = (int)Math.floor(weight);

        Vector3f left = values[tFloor];
        Vector3f right = values[tFloor + 1];

        double t = weight - tFloor;

        int rAvg = (int)Math.round(mix(left.x(), right.x(), t));
        int gAvg = (int)Math.round(mix(left.y(), right.y(), t));
        int bAvg = (int)Math.round(mix(left.z(), right.z(), t));

        return new Vector3f(rAvg, gAvg, bAvg);
    }

    public static int mix(int a, int b, float weightB)
    {
        return Math.round(((1 - weightB) * a) + (weightB * b));
    }
    public static double mix(double a, double b, double weightB)
    {
        //also written as   a + (b - a) * t = c
        return ((1 - weightB) * a) + (weightB * b);
    }

    public static <T> T[] fisherYatesShuffle(T[] arr, RandomSource random) {
        for (int i = arr.length- 1; i > 0; i--)
        {
            int k = random.nextInt(0, i + 1);
            T value = arr[k];
            arr[k] = arr[i];
            arr[i] = value;
        }
        return arr;
    }

    //copied out of Block because the implementation does not return the created entity.
    public static ItemEntity popResourceFromFace(Level pLevel, BlockPos pPos, Direction pDirection, ItemStack pStack) {
        int i = pDirection.getStepX();
        int j = pDirection.getStepY();
        int k = pDirection.getStepZ();
        double d0 = (double) EntityType.ITEM.getWidth() / 2.0D;
        double d1 = (double)EntityType.ITEM.getHeight() / 2.0D;
        double d2 = (double)pPos.getX() + 0.5D + (i == 0 ? Mth.nextDouble(pLevel.random, -0.25D, 0.25D) : (double)i * (0.5D + d0));
        double d3 = (double)pPos.getY() + 0.5D + (j == 0 ? Mth.nextDouble(pLevel.random, -0.25D, 0.25D) : (double)j * (0.5D + d1)) - d1;
        double d4 = (double)pPos.getZ() + 0.5D + (k == 0 ? Mth.nextDouble(pLevel.random, -0.25D, 0.25D) : (double)k * (0.5D + d0));
        double d5 = i == 0 ? Mth.nextDouble(pLevel.random, -0.1D, 0.1D) : (double)i * 0.1D;
        double d6 = j == 0 ? Mth.nextDouble(pLevel.random, 0.0D, 0.1D) : (double)j * 0.1D + 0.1D;
        double d7 = k == 0 ? Mth.nextDouble(pLevel.random, -0.1D, 0.1D) : (double)k * 0.1D;
        return popResource(pLevel, () -> new ItemEntity(pLevel, d2, d3, d4, pStack, d5, d6, d7), pStack);
    }

    private static ItemEntity popResource(Level pLevel, Supplier<ItemEntity> pItemEntitySupplier, ItemStack pStack) {
        if (!pLevel.isClientSide && !pStack.isEmpty() && pLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !pLevel.restoringBlockSnapshots) {
            ItemEntity itementity = pItemEntitySupplier.get();
            itementity.setDefaultPickUpDelay();
            pLevel.addFreshEntity(itementity);
            return itementity;
        }
        return null;
    }
    //copied out of Container because the implementation does not return the created entities.
    public static List<ItemEntity> dropItemStack(Level pLevel, Vec3 pos, ItemStack pStack) {
        double d0 = EntityType.ITEM.getWidth();
        double d1 = 1.0D - d0;
        double d2 = d0 / 2.0D;
        double d3 = Math.floor(pos.x) + pLevel.random.nextDouble() * d1 + d2;
        double d4 = Math.floor(pos.y) + pLevel.random.nextDouble() * d1;
        double d5 = Math.floor(pos.z) + pLevel.random.nextDouble() * d1 + d2;
        List<ItemEntity> ret = new ArrayList<>();
        while(!pStack.isEmpty()) {
            ItemEntity itementity = new ItemEntity(pLevel, d3, d4, d5, pStack.split(pLevel.random.nextInt(21) + 10));
            itementity.setDeltaMovement(pLevel.random.triangle(0.0D, 0.11485000171139836D), pLevel.random.triangle(0.2D, 0.11485000171139836D), pLevel.random.triangle(0.0D, 0.11485000171139836D));
            pLevel.addFreshEntity(itementity);
            ret.add(itementity);
        }
        return ret;
    }

    public static BlockHitResult getEntityPOVHitResult(Level pLevel, LivingEntity entity, double range, ClipContext.Fluid pFluidMode) {
        float f = entity.getXRot();
        float f1 = entity.getYRot();
        Vec3 vec3 = entity.getEyePosition();
        float f2 = Mth.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = Mth.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -Mth.cos(-f * ((float)Math.PI / 180F));
        float f5 = Mth.sin(-f * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        Vec3 vec31 = vec3.add((double)f6 * range, (double)f5 * range, (double)f7 * range);
        return pLevel.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, pFluidMode, entity));
    }

    public static <T> LivingEntity getClosestEntity(List<LivingEntity> list, Vec3 pos, Function<LivingEntity, Boolean> isValid) {
        LivingEntity result = null;
        double dist = 9999999999D; //big number lmao
        double d;
        for (LivingEntity ent : list) {
            if (!isValid.apply(ent))
                continue;
            d = ent.position().distanceToSqr(pos);
            if (d < dist) {
                result = ent;
                dist = d;
            }
        }
        return result;
    }

    public static Boolean blockHasTag(BlockState state, TagKey<Block> tag) {
        return state.getTags().anyMatch((pred) -> pred == tag);
    }

    public static Vector3f rotateAroundAxis(Vector3f v, Vector3f axis, float deltaRadian) {
        float cosTheta = (float)Math.cos(deltaRadian);
        float sinTheta = (float)Math.sin(deltaRadian);
        float dot = axis.dot(v);

        Vector3f t1 = v.mul(cosTheta, cosTheta, cosTheta);
        Vector3f t2 = axis.cross(v).mul(sinTheta, sinTheta, sinTheta);
                                  //...
        Vector3f t3 = axis.mul(dot, dot, dot).mul(1 - cosTheta, 1 - cosTheta, 1 - cosTheta);

        return t1.add(t2).add(t3);
    }
}