package net.sirplop.aetherworks.item.tool;
import net.minecraft.world.item.Item;
import net.sirplop.aetherworks.util.Compat121;

import com.rekindled.embers.item.ClockworkPickaxeItem;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.particle.SparkParticleOptions;
import com.rekindled.embers.util.EmberInventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.api.item.IToggleEmberItem;
import net.sirplop.aetherworks.api.item.IToggleItem;
import net.sirplop.aetherworks.util.MoonlightRepair;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AOEEmberDiggerItem extends DiggerItem implements IToggleEmberItem {

    public TagKey<Block> blocks;
    public boolean moongazeOnStrike = true;

    public AOEEmberDiggerItem(float pAttackDamageModifier, float pAttackSpeedModifier, Tier pTier, TagKey<Block> pBlocks, Properties pProperties) {
        super(pTier, pBlocks, pProperties.attributes(DiggerItem.createAttributes(pTier, pAttackDamageModifier, pAttackSpeedModifier)));
        blocks = pBlocks;
    }

    // SMP 2 port: NeoForge 1.21 has no onBlockStartBreak; the area break runs once the centre block is mined.
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        boolean result = super.mineBlock(stack, level, state, pos, entity);
        if (!level.isClientSide() && entity instanceof Player player && getToggled(stack) == 1) {
            causeAoe((ServerLevel) level, pos, state, stack, player);
        }
        return result;
    }

    @Override
    public byte getToggleMax() { return 1; }

    @Override
    public void clientModeChanged(ItemStack stack, Player player, byte oldValue, byte newValue, byte stateFlag) {
        boolean modeChange = oldValue != newValue;
        Level level =  player.getCommandSenderWorld();
        MutableComponent message;
        if (modeChange && newValue > 0) { //power on successfully
            level.playSound(player, player.getOnPos(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.6f, 0.9F);
            message = Component.translatable(Aetherworks.MODID + ".tooltip.aoe_on");
        } else if (!modeChange || stateFlag == 1) { //power on fail
            level.playSound(player, player.getOnPos(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.6f, 0.0F);
            message = Component.translatable(Aetherworks.MODID + ".tooltip.not_enough_ember");
        } else { //power off
            level.playSound(player, player.getOnPos(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.6f, 0.6F);
            message = Component.translatable(Aetherworks.MODID + ".tooltip.aoe_off");
        }
        if (!Utils.isFakePlayer(player))
            Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    // modified from the JustHammers hammer code:
    // https://github.com/nanite/JustHammers/
    public void causeAoe(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack, Player player) {
        if (!(player instanceof ServerPlayer)) return;

        if (level.isClientSide || state.getDestroySpeed(level, pos) == 0.0F) {
            return;
        }
        HitResult pick = player.pick(20D, 0.0F, false);

        // Hit something that wasn't a block.
        if (!(pick instanceof BlockHitResult blockHitResult)) {
            return;
        }
        //only break blocks that this can actually break.
        if (level.getBlockState(pos).getTags().anyMatch(blockTagKey -> blockTagKey == blocks))
            this.aoeMine(blockHitResult, pos, stack, level, player);
    }
    public void aoeMine(BlockHitResult pick, BlockPos blockPos, ItemStack stack, ServerLevel level, Player player) {
        if (!(player instanceof ServerPlayer)) return;

        Direction direction = pick.getDirection();
        var boundingBox = getAreaOfEffect(blockPos, direction, 2, 1);
        // Don't let the tool break if they don't have enough durability.
        if (!player.isCreative() && (stack.getDamageValue() >= stack.getMaxDamage() - 1)) {
            return;
        }
        GlowParticleOptions glow = getParticle();
        int damage = 0;
        BlockState targetState = level.getBlockState(blockPos);
        Iterator<BlockPos> iterator = BlockPos.betweenClosedStream(boundingBox).iterator();
        Set<BlockPos> removedPos = new HashSet<>();
        while (iterator.hasNext()) {
            var pos = iterator.next();
            // Stop breaking if too much damage has accrued.
            if (!player.isCreative() && (stack.getDamageValue() + (damage + 1)) >= stack.getMaxDamage() - 1) {
                break;
            }
            if (pos.equals(blockPos)) {
                level.sendParticles(glow,
                        pos.getX() + 0.5f,
                        pos.getY() + 0.5f,
                        pos.getZ() + 0.5f,
                        10, 0.25f, 0.25f, 0.25f, 0.1f);
                continue;
            }
            if (removedPos.contains(pos) || !level.getBlockState(pos).getBlock().equals(targetState.getBlock())
                    || !canDestroy(targetState, level, pos)) {
                continue;
            }
            //use embers every block
            if (!consumeEmbers(player, AWConfig.TOOL_EMBER_USE.get())) {
                toggleItem(stack, player, (byte)1);
                break;
            }

            removedPos.add(pos);

            level.sendParticles(glow,
                    pos.getX() + 0.5f,
                    pos.getY() + 0.5f,
                    pos.getZ() + 0.5f,
                    10, 0.25f, 0.25f, 0.25f, 0.1f);
            Utils.breakAndHarvestBlock(level, pos, (ServerPlayer)player, stack, direction,
                    (state) -> state.getBlock().equals(targetState.getBlock()), false, false);
            damage++;
        }
        if (damage != 0 && !player.isCreative()) {
            stack.hurtAndBreak(damage, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) player).level(), (ServerPlayer) player, item -> {});
        }
    }
    public static BoundingBox getAreaOfEffect(BlockPos blockPos, Direction direction, int radius, int depth) {
        int size = (radius / 2);
        int offset = size - 1;

        return switch (direction) {
            case DOWN, UP -> new BoundingBox(blockPos.getX() - size, blockPos.getY() - (direction == Direction.UP ? depth - 1 : 0), blockPos.getZ() - size, blockPos.getX() + size, blockPos.getY() + (direction == Direction.DOWN ? depth - 1 : 0), blockPos.getZ() + size);
            case NORTH, SOUTH -> new BoundingBox(blockPos.getX() - size, blockPos.getY() - size + offset, blockPos.getZ() - (direction == Direction.SOUTH ? depth - 1 : 0), blockPos.getX() + size, blockPos.getY() + size + offset, blockPos.getZ() + (direction == Direction.NORTH ? depth - 1 : 0));
            case WEST, EAST -> new BoundingBox(blockPos.getX() - (direction == Direction.EAST ? depth - 1 : 0), blockPos.getY() - size + offset, blockPos.getZ() - size, blockPos.getX() + (direction == Direction.WEST ? depth - 1 : 0), blockPos.getY() + size + offset, blockPos.getZ() + size);
        };
    }
    private boolean canDestroy(BlockState targetState, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) == null && targetState.getDestroySpeed(level, pos) > 0;
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("aetherworks.tooltip.cycle_mode", Component.keybind("key.aetherworks.mode_change")).withStyle(ChatFormatting.GOLD));

        if (getToggled(pStack) == 1) {
            pTooltipComponents.add(Component.translatable(Aetherworks.MODID + ".tooltip.aoe_on").withStyle(ChatFormatting.GRAY));
        } else {
            pTooltipComponents.add(Component.translatable(Aetherworks.MODID + ".tooltip.aoe_off").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    public GlowParticleOptions getParticle() {
        GlowParticleOptions glow = new GlowParticleOptions(getParticleColor(), 1f, 20);
        return glow;
    }
    public abstract Vector3f getParticleColor();

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        MoonlightRepair.tryRepair(stack, world, entity, AWConfig.AETHERIC_STRENGTH.get());
        if (selected) { //get all items around the player and try to suck them in if they have the succ tag
            SparkParticleOptions suckParticle = new SparkParticleOptions(getParticleColor(), 1f);
            List<ItemEntity> targets = world.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(10.0),
                    ent -> ent.getTags().contains(Utils.SUCK_ITEM_TAG));
            for (ItemEntity ent : targets) {
                double x = ent.position().x - entity.position().x;
                double y = ent.position().y - entity.position().y + (entity.getBbHeight() * 0.1);
                double z = ent.position().z - entity.position().z;
                double w = 1 / Math.sqrt(x * x + y * y + z * z);
                final double adj = 0.3;
                x = -Math.min(0.25, Math.max(-0.25, x * w * adj));
                y = -Math.min(0.25, Math.max(-0.25, y * w * adj - 0.1));
                z = -Math.min(0.25, Math.max(-0.25, z * w * adj));
                ent.setDeltaMovement(x, y, z);
                if (world.isClientSide())
                    world.addParticle(suckParticle,
                            ent.position().x,
                            ent.position().y,
                            ent.position().z,
                            0.05f, 0.1f, 0.05f);
            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public boolean consumeEmbers(Player player, double value)
    {
        if (EmberInventoryUtil.getEmberTotal(player) >= value)
        {
            EmberInventoryUtil.removeEmber(player, value);
            return true;
        }
        return false;
    }
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return true;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        if (!newStack.is(oldStack.getItem()))
            return true;

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem())
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);

        // 1.21: damage is its own component, so the custom data alone tells whether "something that's not damage changed"
        return !Compat121.tag(newStack).getAllKeys().equals(Compat121.tag(oldStack).getAllKeys());
    }
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (Compat121.hasTag(oldStack) && Compat121.hasTag(newStack)) {
            return slotChanged || Compat121.tag(oldStack).getBoolean("poweredOn") != Compat121.tag(newStack).getBoolean("poweredOn") || newStack.getItem() != oldStack.getItem();
        }
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hurt = super.hurtEnemy(stack, target, attacker);;
        if (moongazeOnStrike && hurt && target.isAffectedByPotions()) {
            MobEffectInstance mobeffect = new MobEffectInstance(AWRegistry.EFFECT_MOONFIRE, 100, 1, false, true, true);
            target.addEffect(mobeffect, attacker);
        }
        return hurt;
    }
}
