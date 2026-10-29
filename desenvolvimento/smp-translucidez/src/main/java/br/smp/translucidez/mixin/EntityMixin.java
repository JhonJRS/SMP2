package br.smp.translucidez.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No cliente, um jogador nunca é "invisível para" quem olha. Com isso o vanilla (e o Figura) desenham o corpo
 * translúcido em vez de pular, e o Iron's Spells (True Invisibility) não cancela mais o desenho do jogador.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void smptranslucidez$nuncaInvisivel(Player quemOlha, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player && self.level().isClientSide()) {
            cir.setReturnValue(false);
        }
    }
}
