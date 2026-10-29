package br.smp.profissoes.mixin;

import br.smp.profissoes.Guarda;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.network.chat.Component;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.core.network.messages.server.colony.building.worker.AddRemoveRecipeMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MineColonies: impede ensinar a um edifício uma receita cujo resultado o jogador que ensina não pode
 * fabricar. Receitas já ensinadas antes deste mod continuam valendo (ver README).
 */
@Mixin(value = AddRemoveRecipeMessage.class, remap = false)
public abstract class AddRemoveRecipeMessageMixin implements MarcadorProtecao {
    @Shadow(remap = false) @Final private boolean remove;
    @Shadow(remap = false) @Final private IRecipeStorage storage;

    @Inject(method = "onExecute(Lnet/neoforged/neoforge/network/handling/IPayloadContext;Lnet/minecraft/server/level/ServerPlayer;Lcom/minecolonies/api/colony/IColony;Lcom/minecolonies/api/colony/buildings/IBuilding;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$verificarEnsino(IPayloadContext ctx, ServerPlayer jogador, IColony colonia, IBuilding predio, CallbackInfo ci) {
        if (remove || storage == null || jogador == null) return;
        ItemStack saida = storage.getPrimaryOutput();
        if (saida == null || saida.isEmpty()) return;
        String recusa = Guarda.recusaParaItem(jogador, saida);
        if (recusa != null) {
            jogador.displayClientMessage(Component.literal(recusa), false);
            ci.cancel();
        }
    }
}
