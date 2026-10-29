package net.sirplop.aetherworks.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.particle.SparkParticleOptions;
import com.rekindled.embers.util.EmberInventoryUtil;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.Misc;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.sirplop.aetherworks.network.PacketDistributor;
import net.sirplop.aetherworks.AWConfig;
import net.sirplop.aetherworks.datagen.AWSounds;
import net.sirplop.aetherworks.network.MessageShieldParticle;
import net.sirplop.aetherworks.network.MessageSyncEntityMotion;
import net.sirplop.aetherworks.network.PacketHandler;
import net.sirplop.aetherworks.util.MoonlightRepair;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class AetherShield extends ShieldItem {
    public AetherShield(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        MoonlightRepair.tryRepair(stack, world, entity, AWConfig.AETHERIC_STRENGTH.get());
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        //don't want to make the player unblock because it's repairing itself
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        Level pLevel = entity.level();
        if (entity instanceof Player pPlayer && EmberInventoryUtil.getEmberTotal(pPlayer) > 0) {
            if (pLevel instanceof ServerLevel serverLevel) {
                //only play sounds on client
                serverLevel.playSound(pPlayer, entity.position().x, entity.position().y, entity.position().z,
                       AWSounds.AETHER_SHIELD_LOWER.get(), SoundSource.PLAYERS, 1, 0.75f + pLevel.random.nextFloat() * 0.5f);
            } else if (pLevel instanceof ClientLevel client) {
                client.playSound(pPlayer, entity.position().x, entity.position().y, entity.position().z,
                        AWSounds.AETHER_SHIELD_LOWER.get(), SoundSource.PLAYERS, 1, 0.75f + pLevel.random.nextFloat() * 0.5f);
            }
        }
        super.onStopUsing(stack, entity, count);
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pHand) {
        boolean alt = pLevel.random.nextBoolean();
        if (EmberInventoryUtil.getEmberTotal(pPlayer) > 0) {
            if (pLevel instanceof ServerLevel serverLevel) {
                //only play sounds on client
                serverLevel.playSound(pPlayer, pPlayer.position().x, pPlayer.position().y, pPlayer.position().z,
                        (alt ? AWSounds.AETHER_SHIELD_RAISE_1.get() : AWSounds.AETHER_SHIELD_RAISE_2.get()), SoundSource.PLAYERS, 1, 0.75f + pLevel.random.nextFloat() * 0.5f);
            } else if (pLevel instanceof ClientLevel client) {
                client.playSound(pPlayer, pPlayer.position().x, pPlayer.position().y, pPlayer.position().z,
                        (alt ? AWSounds.AETHER_SHIELD_RAISE_1.get() : AWSounds.AETHER_SHIELD_RAISE_2.get()), SoundSource.PLAYERS, 1, 0.75f + pLevel.random.nextFloat() * 0.5f);
            }
        }
        return super.use(pLevel, pPlayer, pHand);
    }

    public static final String SHIELD_TAG = "aw.touched_shield";

    public static final SparkParticleOptions SPARK = new SparkParticleOptions(EmbersColors.EMBER_ID, 1.0f);
    public static final GlowParticleOptions GLOW = new GlowParticleOptions(Misc.colorFromInt(Utils.AETHERIUM_PROJECTILE_COLOR.getRGB()), 3f, 10);
    @Override
    public void onUseTick(@Nonnull Level lvl, @Nonnull LivingEntity entity, @Nonnull ItemStack pStack, int pRemainingUseDuration) {
        if (!(lvl instanceof ServerLevel level))
            return; //only do this check on client.
        //find the area of effect

        Player player = null;
        if (entity instanceof Player p)
        {
            player = p;
            if (EmberInventoryUtil.getEmberTotal(player) < 2)
                return; //no ember, no service!
        }

        Vec3 dirDeg = entity.getLookAngle();
        Vec3 dir = dirDeg.normalize();
        Vec3 eyePos = entity.getEyePosition();
        Vec3 middlePos = eyePos.add(dir);

        //construct the region the shield is blocking
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setTranslation((float) middlePos.x, (float) middlePos.y, (float) middlePos.z);
        matrix4f.rotate(Axis.ZP.rotationDegrees(0));
        matrix4f.rotate(Axis.YP.rotationDegrees((float)-Math.toRadians(entity.getYRot())));
        matrix4f.rotate(Axis.XP.rotationDegrees((float)Math.toRadians(entity.getXRot())));

        Vector3f lbb = matrix4f.transformPosition(new Vector3f(-1f, -1.5f, 0f));
        Vector3f rtf = matrix4f.transformPosition(new Vector3f(1f, 1.5f, 0.75f));

        PacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(middlePos.x, middlePos.y, middlePos.z,
                64, level.dimension())), new MessageShieldParticle(middlePos.toVector3f(), entity.getXRot(), entity.getYRot(), (byte)64, Misc.intColor(Utils.AETHERIUM_COLOR)));

        AABB aabb = new AABB(lbb.x, lbb.y, lbb.z, rtf.x, rtf.y, rtf.z).inflate(1); //this is our approximation area.

        List<Entity> list = level.getEntities(entity, aabb,
                ent -> ent instanceof Projectile proj && ent.position().closerThan(eyePos, 5) && proj.getOwner() != entity && !ent.getTags().contains(SHIELD_TAG));
        int interact = 0;
        for (Entity projectile : list) {
            if (player != null && !consumeEmbers(player, 2)){
                break; //we're a player, and we're outta ember!
            }

            projectile.addTag(SHIELD_TAG);
            interact++;

            float speed = Math.max(0.1f, (float)projectile.getDeltaMovement().length());
            level.sendParticles(SPARK,
                    projectile.getX(), projectile.getY(), projectile.getZ(),
                    5, 0, 0, 0, speed);
            level.sendParticles(GLOW,
                    projectile.getX(), projectile.getY(), projectile.getZ(),
                    10, 0.1, 0.1, 0.1, speed * 0.5);
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), EmbersSounds.ASHEN_AMULET_BURN.get(), SoundSource.PLAYERS, 0.5f, Misc.random.nextFloat()*0.5f + 0.2f);

            Vec3 deltaMotion = projectile.getDeltaMovement().scale(-1);
            float yRot = projectile.getYRot() + 180.0F;
            float yRotO = projectile.yRotO + 180;

            projectile.setDeltaMovement(deltaMotion);
            projectile.setYRot(yRot);
            projectile.yRotO = yRotO;

            PacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(middlePos.x, middlePos.y, middlePos.z,
                    32, level.dimension())), new MessageSyncEntityMotion(projectile, deltaMotion.toVector3f(), yRot, yRotO));
        }
        if (player != null)
            pStack.hurtAndBreak(interact, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) player).level(), (ServerPlayer) player, item -> {});
    }

    protected boolean consumeEmbers(Player player, double value)
    {
        if (EmberInventoryUtil.getEmberTotal(player) >= value)
        {
            EmberInventoryUtil.removeEmber(player, value);
            return true;
        }
        return false;
    }
}
