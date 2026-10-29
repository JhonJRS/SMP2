package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Simply More: carregar a habilidade (segurar o botão) só tem efeito no servidor; no cliente o cast para ServerLevel derrubava o jogo. */
@Pseudo
@Mixin(targets = {"net.rosemarythyme.simplymore.item.uniques.CindergorgeItem", "net.rosemarythyme.simplymore.item.uniques.EarthshatterItem",
        "net.rosemarythyme.simplymore.item.uniques.RevvengineItem", "net.rosemarythyme.simplymore.item.uniques.StasisItem"}, remap = false)
public abstract class SimplyMoreUsoContinuoMixin implements MarcadorProtecao {

    @Inject(method = "onUseTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$soNoServidor(Level level, LivingEntity quem, ItemStack st, int resta, CallbackInfo ci) {
        if (level.isClientSide) ci.cancel();
    }
}
