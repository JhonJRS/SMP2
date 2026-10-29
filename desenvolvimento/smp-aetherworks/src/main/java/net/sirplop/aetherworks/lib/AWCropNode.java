package net.sirplop.aetherworks.lib;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.datagen.AWBlockTags;
import net.sirplop.aetherworks.network.MessageSyncItemEntityTag;
import net.sirplop.aetherworks.network.PacketHandler;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AWCropNode extends AWHarvestNode {
    public AWCropNode(Player harvester, Level level, BlockPos beginning, int range, Predicate<Player> canHarvest, @Nullable GlowParticleOptions particle, double damageChance) {
        super(harvester, level, beginning, range, canHarvest, particle, damageChance);
    }
    public void traverse(BlockPos from) {
        if (from.distToCenterSqr(this.beginning.getX() + 0.5, this.beginning.getY() + 0.5, this.beginning.getZ() + 0.5) >= (range * range)-1)
            return;
        Stack<BlockPos> check = new Stack<>();
        check.add(from);
        this.toHarvest.add(0, from);
        Set<Block> sameBlocks = AWConfig.getSameBlocks(level.getBlockState(from).getBlock());
        while (!check.isEmpty()) {
            BlockPos pos = check.pop();
            if (pos.distToCenterSqr(this.beginning.getX() + 0.5, this.beginning.getY() + 0.5, this.beginning.getZ() + 0.5) >= (range * range)-1)
                continue;

            for (OctDirection facing : OctDirection.values()) {
                BlockPos offset =  facing.offsetBlock(pos);
                if (this.toHarvest.contains(offset) || !this.isLoaded(offset)) {
                    continue;
                }

                Block block = this.level.getBlockState(offset).getBlock();
                if (block.equals(this.baseState.getBlock()) || sameBlocks.contains(block)) {
                    this.toHarvest.add(0, offset);
                    check.add(offset);
                }
            }
        }
    }

    public void tick()
    {
        if (!(level instanceof ServerLevel)){
            this.invalid = true;
            return;
        }
        if (!canHarvest.test(harvester) || !Utils.hasEnoughDurability(harvester.getMainHandItem(), 1)) {
            this.invalid = true;
            return;
        }
        while (!this.toHarvest.isEmpty()) {
            if (harvest(this.toHarvest.pop()))
                break;
        }
        if (this.toHarvest.isEmpty())
            this.invalid = true;
    }

    protected boolean harvest(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        boolean seedDrop = false;
        boolean replant = false;
        boolean success = false;

        var hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, true);
        if (Utils.blockHasTag(state, AWBlockTags.HOVH_RIGHTCLICK))
        { //this is a right-click harvest block... so do that!
            if (harvester instanceof ServerPlayer player) {
                success = player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                        hitResult).consumesAction();
                //fish around for the items it might have popped off. Is this efficient? No, not really. But the succ demands!
                List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.25));
                drops.forEach(e -> {
                    if (e != null) {
                        e.addTag(Utils.SUCK_ITEM_TAG);
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MessageSyncItemEntityTag(e, Utils.SUCK_ITEM_TAG));
                    }
                });
            }
        }
        else if (isCrop(block)) {
            //check sustaining in all directions - some crops are fun!
            // SMP 2 port: 1.21 dropped IPlantable; "can it be replanted here" is the crop's own survival check
            replant = state.canSurvive(level, pos);

            IntegerProperty age = getAge(state);
            if (isMature(state, age)) {
                seedDrop = dropItems(state, pos, () -> state.getCloneItemStack(hitResult, level, pos, harvester).getItem(), replant);
                level.destroyBlock(pos, false, harvester);
                if (seedDrop) {
                    BlockState freshCrop = state.setValue(age, 0);
                    level.setBlock(pos, freshCrop, 1 | 2);
                }
                success = true;
            }
        }

        if (success) {
            if (!harvester.isCreative() && level.random.nextFloat() <= damageChance)
                harvester.getMainHandItem().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) harvester).level(), (ServerPlayer) harvester, item -> {});
            if (particle != null) {
                ((ServerLevel)level).sendParticles(particle,
                        pos.getX() + 0.5f,
                        pos.getY() + 0.5f,
                        pos.getZ() + 0.5f,
                        10, 0.25f, 0.25f, 0.25f, 0.25f);
            }
        }

        return success;
    }

    protected boolean dropItems(BlockState state, BlockPos pos, Supplier<Item> seed, boolean replant) {
        boolean seedDrop = false;
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, null, harvester, harvester.getMainHandItem());
        Item seedItem = seed.get();
        for (ItemStack drop : drops) {
            if (replant && !seedDrop) {
                if (drop.getItem() == seedItem) {
                    drop.shrink(1);
                    seedDrop = true;
                }
            }
            if (!drop.isEmpty()) {
                List<ItemEntity> extra = Utils.dropItemStack(level, pos.getCenter(), drop);
                extra.forEach(e -> {
                    if (e != null) {
                        e.addTag(Utils.SUCK_ITEM_TAG);
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) harvester), new MessageSyncItemEntityTag(e, Utils.SUCK_ITEM_TAG));
                    }
                });
            }
        }
        return seedDrop;

    }

    public static IntegerProperty getAge(BlockState blockState) throws NullPointerException, NoSuchElementException, ClassCastException {
        return (IntegerProperty) blockState.getProperties().stream().filter(property -> "age".equals(property.getName())).findFirst().orElseThrow();
    }
    public static boolean isMature(BlockState blockState, IntegerProperty age) {
        return blockState.getOptionalValue(age).orElse(0) >= Collections.max(age.getPossibleValues());
    }
    public static boolean isCrop(Block block) {
        return !(block instanceof TorchflowerCropBlock) && (block instanceof CropBlock || block instanceof NetherWartBlock || block instanceof CocoaBlock || block instanceof PitcherCropBlock || Utils.blockHasTag(block.defaultBlockState(), AWBlockTags.HOVH_RIGHTCLICK));
    }
}
