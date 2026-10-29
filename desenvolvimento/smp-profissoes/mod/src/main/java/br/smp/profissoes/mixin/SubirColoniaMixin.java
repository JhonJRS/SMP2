package br.smp.profissoes.mixin;

import br.smp.profissoes.Colonia;
import br.smp.profissoes.MarcadorProtecao;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pedido de construção/melhoria de um edifício da MineColonies: o nível da colônia depende de licença dada por ADM. */
@Mixin(value = AbstractBuilding.class, remap = false)
public abstract class SubirColoniaMixin implements MarcadorProtecao {

    @Inject(method = "requestUpgrade(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$licencaDeNivel(Player p, BlockPos construtor, CallbackInfo ci) {
        if (!Colonia.podeSubir(p, (IBuilding) (Object) this)) ci.cancel();
    }
}
