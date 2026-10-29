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

/**
 * Simply More 1.3.0_alpha: as armas únicas fazem (ServerLevel) level também no cliente e o jogo fecha
 * (crashes de 19-21/10: Mimicry, Glimmerstep). No cliente só prevemos o uso (segurar o botão); o efeito roda no servidor.
 */
@Pseudo
@Mixin(targets = {"net.rosemarythyme.simplymore.item.uniques.BoasFangItem", "net.rosemarythyme.simplymore.item.uniques.GlimmerstepItem",
        "net.rosemarythyme.simplymore.item.uniques.mimicry.MimicryItem", "net.rosemarythyme.simplymore.item.uniques.TheVesselBreachItem"}, remap = false)
public abstract class SimplyMoreUsoMixin implements MarcadorProtecao {

    @Inject(method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$soNoServidor(Level level, Player p, InteractionHand mao, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!level.isClientSide) return;
        ItemStack st = p.getItemInHand(mao);
        if (st.getUseDuration(p) > 0) {
            p.startUsingItem(mao);
            cir.setReturnValue(InteractionResultHolder.consume(st));
        } else {
            cir.setReturnValue(InteractionResultHolder.success(st));
        }
    }
}
