package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Simply More: ativar a habilidade da arma (ServerLevel) só no servidor. */
@Pseudo
@Mixin(targets = {"net.rosemarythyme.simplymore.item.uniques.LustrousMoxieItem", "net.rosemarythyme.simplymore.item.uniques.MoundshifterItem",
        "net.rosemarythyme.simplymore.item.uniques.RuyiJinguBangItem", "net.rosemarythyme.simplymore.item.uniques.StasisItem"}, remap = false)
public abstract class SimplyMoreHabilidadeMixin implements MarcadorProtecao {

    @Inject(method = "startPlayerAbility(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$soNoServidor(Level level, Player p, InteractionHand mao, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide) cir.setReturnValue(InteractionResultHolder.pass(p.getItemInHand(mao)));
    }
}
