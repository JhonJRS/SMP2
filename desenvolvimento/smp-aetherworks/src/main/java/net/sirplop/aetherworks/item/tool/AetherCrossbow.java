package net.sirplop.aetherworks.item.tool;

import com.rekindled.embers.api.item.IProjectileWeapon;
import com.rekindled.embers.util.EmberInventoryUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.util.MoonlightRepair;
import net.sirplop.aetherworks.util.Compat121;

public class AetherCrossbow extends CrossbowItem implements IProjectileWeapon {
    public AetherCrossbow(Properties pProperties) {
        super(pProperties);
    }
    /** Set to {@code true} when the crossbow is 20% charged. */
    private boolean startSoundPlayed = false;
    /** Set to {@code true} when the crossbow is 50% charged. */
    private boolean midLoadSoundPlayed = false;

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        MoonlightRepair.tryRepair(stack, world, entity, AWConfig.AETHERIC_STRENGTH.get());
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (isCharged(itemstack)) {
            performShooting(pLevel, pPlayer, itemstack, pHand);
            setCharged(itemstack, false);
            return InteractionResultHolder.consume(itemstack);
        } else if (pPlayer.getAbilities().instabuild || EmberInventoryUtil.getEmberTotal(pPlayer) > AWConfig.CROSSBOW_EMBER_USE.get()) {
            if (!isCharged(itemstack)) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
                pPlayer.startUsingItem(pHand);
            }

            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
    }
    // SMP 2 port: "charged" is the CHARGED_PROJECTILES component in 1.21; ember shots carry no real projectile,
    // so a placeholder arrow marks the crossbow as loaded.
    public static void setCharged(ItemStack stack, boolean charged) {
        stack.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES, charged
                ? net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(net.minecraft.world.item.Items.ARROW))
                : net.minecraft.world.item.component.ChargedProjectiles.EMPTY);
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft) {
        int i = this.getUseDuration(pStack, pEntityLiving) - pTimeLeft;
        float f = getPowerForTime(i, pStack, pEntityLiving);
        if (f >= 1.0F && !isCharged(pStack) && tryLoadProjectiles(pEntityLiving, pStack)) {
            setCharged(pStack, true);
            SoundSource soundsource = pEntityLiving instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            pLevel.playSound(null, pEntityLiving.getX(), pEntityLiving.getY(), pEntityLiving.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, soundsource, 1.0F, 1.0F / (pLevel.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }

    protected boolean tryLoadProjectiles(LivingEntity pShooter, ItemStack pCrossbowStack) {
        int i = Compat121.enchantmentLevel(Enchantments.MULTISHOT, pCrossbowStack);
        int j = i == 0 ? 1 : 3;
        boolean playerFlag = pShooter instanceof Player;
        boolean creativeFlag = pShooter instanceof Player && ((Player)pShooter).getAbilities().instabuild;
        if (playerFlag && !loadProjectile(pShooter, AWConfig.CROSSBOW_EMBER_USE.get() * j,
                EmberInventoryUtil.getEmberTotal((Player)pShooter) > AWConfig.CROSSBOW_EMBER_USE.get() * j, creativeFlag))
            return false;
        else if (!loadProjectile(pShooter, 0,true, creativeFlag))
            return false; //hostiles don't require ember to shoot.

        return true;
    }

    private static boolean loadProjectile(LivingEntity pShooter, double drain, boolean pHasAmmo, boolean pIsCreative) {
        if (!pHasAmmo && !pIsCreative)
            return false;

        if (pShooter instanceof Player player && !pIsCreative)
            EmberInventoryUtil.removeEmber(player, drain);
        return true;
    }

    private static float getPowerForTime(int pUseTime, ItemStack pCrossbowStack, LivingEntity shooter) {
        float f = (float)pUseTime / (float)getChargeDuration(pCrossbowStack, shooter);
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (!level.isClientSide) {
            int i = Compat121.enchantmentLevel(Enchantments.QUICK_CHARGE, stack);
            SoundEvent soundevent = this.getStartSound(i);
            SoundEvent soundevent1 = i == 0 ? SoundEvents.CROSSBOW_LOADING_MIDDLE.value() : null;
            float f = (float)(stack.getUseDuration(livingEntity) - count) / (float)getChargeDuration(stack, livingEntity);
            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), soundevent, SoundSource.PLAYERS, 0.5F, 1.0F);
            }

            if (f >= 0.5F && soundevent1 != null && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), soundevent1, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }
    }
    private SoundEvent getStartSound(int pEnchantmentLevel) {
        return switch (pEnchantmentLevel) {
            case 1 -> SoundEvents.CROSSBOW_QUICK_CHARGE_1.value();
            case 2 -> SoundEvents.CROSSBOW_QUICK_CHARGE_2.value();
            case 3 -> SoundEvents.CROSSBOW_QUICK_CHARGE_3.value();
            default -> SoundEvents.CROSSBOW_LOADING_START.value();
        };
    }

    //Implemented in child classes.
    public void shootProjectile(Level level, LivingEntity entity, ItemStack stack, InteractionHand hand, float pSoundPitch, float rotation) {

    }

    public static void performShooting(Level pLevel, LivingEntity pShooter, ItemStack pCrossbowStack, InteractionHand hand) {
        int i = Compat121.enchantmentLevel(Enchantments.MULTISHOT, pCrossbowStack);
        ((AetherCrossbow)pCrossbowStack.getItem()).shootProjectile(pLevel, pShooter, pCrossbowStack, hand,0.0F, 0);
        if (i > 0) {
            ((AetherCrossbow)pCrossbowStack.getItem()).shootProjectile(pLevel, pShooter, pCrossbowStack, hand,0.0F, -10);
            ((AetherCrossbow)pCrossbowStack.getItem()).shootProjectile(pLevel, pShooter, pCrossbowStack, hand,0.0F, 10);
        }
        onCrossbowShot(pLevel, pShooter, pCrossbowStack);
    }

    private static void onCrossbowShot(Level pLevel, LivingEntity pShooter, ItemStack pCrossbowStack) {
        if (pShooter instanceof ServerPlayer serverplayer) {
            if (!pLevel.isClientSide) {
                CriteriaTriggers.SHOT_CROSSBOW.trigger(serverplayer, pCrossbowStack);
            }

            serverplayer.awardStat(Stats.ITEM_USED.get(pCrossbowStack.getItem()));
        }
    }
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return true;
    }

    //Piercing does not work with ember projectiles, so disable the enchantment.
    // 1.21: the anvil/books ask supportsEnchantment, the enchanting table isPrimaryItemFor.
    private static Boolean allowed(net.minecraft.core.Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.FLAME) || enchantment.is(Enchantments.POWER)
                || enchantment.is(Enchantments.PUNCH) || enchantment.is(Enchantments.MULTISHOT))
            return true;
        if (enchantment.is(Enchantments.PIERCING))
            return false;
        return null;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment) {
        Boolean a = allowed(enchantment);
        return a != null ? a : super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment) {
        Boolean a = allowed(enchantment);
        return a != null ? a : super.isPrimaryItemFor(stack, enchantment);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.matches(oldStack, newStack);
    }

}
