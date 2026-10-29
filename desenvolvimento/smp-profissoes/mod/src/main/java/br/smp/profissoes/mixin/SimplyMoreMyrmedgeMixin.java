package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Simply More: o Myrmedge faz (ServerLevel) a cada tick no inventário; no cliente o tick é ignorado. */
@Pseudo
@Mixin(targets = "net.rosemarythyme.simplymore.item.uniques.MyrmedgeItem", remap = false)
public abstract class SimplyMoreMyrmedgeMixin implements MarcadorProtecao {

    @Inject(method = "inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$soNoServidor(ItemStack st, Level level, Entity dono, int slot, boolean naMao, CallbackInfo ci) {
        if (level.isClientSide) ci.cancel();
    }
}
