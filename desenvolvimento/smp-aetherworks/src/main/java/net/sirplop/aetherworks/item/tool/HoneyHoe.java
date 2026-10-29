package net.sirplop.aetherworks.item.tool;

import com.rekindled.embers.particle.GlowParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.sirplop.aetherworks.util.AetheriumTiers;
import net.sirplop.aetherworks.util.Utils;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

public class HoneyHoe extends AOEEmberHoeItem {
    public HoneyHoe(Properties pProperties) {
        super(-2f, 0, AetheriumTiers.AETHERIUM, BlockTags.MINEABLE_WITH_HOE, pProperties);
    }

    private final GlowParticleOptions particle = new GlowParticleOptions(getParticleColor(), 1, 15);

    @Override
    public Vector3f getParticleColor() {
        return new Vector3f(0.90f, 0.70F, 0.26F);
    }

    @Override
    public @Nonnull InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target.level() instanceof ServerLevel level) {
            if (hand == InteractionHand.MAIN_HAND && target instanceof Animal animal) {
                if (animal.getAge() == 0 && animal.canFallInLove() && Utils.hasEnoughDurability(stack, 5)) {
                    animal.setInLove(player);
                    if (!player.isCreative())
                        stack.hurtAndBreak(5, (net.minecraft.server.level.ServerLevel) ((ServerPlayer) player).level(), (ServerPlayer) player, item -> {});
                    Vec3 pos = target.position();
                    level.sendParticles(particle,
                            pos.x,
                            pos.y + (animal.getBoundingBox().getYsize() * 0.5),
                            pos.z,
                            32, 0.5f, 0.5f, 0.5f, 0.5f);

                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel level) {
            if (target instanceof Animal animal) {
                animal.setLastHurtByMob(null);//this will kill animals that have half a heart
                animal.heal(6);   //because hurtEnemy is called after death checks (why???)
                moongazeOnStrike = false;
            }
        }
        super.hurtEnemy(stack, target, attacker);
        if (!moongazeOnStrike) {
            moongazeOnStrike = true;
            return false;
        }
        return true;
    }
}
