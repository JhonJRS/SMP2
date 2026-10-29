package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import br.smp.profissoes.SmpProfissoes;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.api.research.ILocalResearchTree;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingUniversity;
import com.minecolonies.core.research.LocalResearchTree;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MineColonies só confere o pai direto, e só na tela (cliente). Na aba Guild of Trades o servidor exige a cadeia
 * inteira (pai, avô...) concluída antes de iniciar uma pesquisa. Isso também cobre saves de versões antigas da árvore,
 * em que um nó foi concluído quando ainda não tinha pai.
 */
@Mixin(value = LocalResearchTree.class, remap = false)
public abstract class LocalResearchTreeMixin implements MarcadorProtecao {

    @Inject(method = "attemptBeginResearch(Lnet/minecraft/world/entity/player/Player;Lcom/minecolonies/api/colony/IColony;Lcom/minecolonies/core/colony/buildings/workerbuildings/BuildingUniversity;Lcom/minecolonies/api/research/IGlobalResearch;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$exigirCadeia(Player jogador, IColony colonia, BuildingUniversity universidade, IGlobalResearch pesquisa, CallbackInfo ci) {
        try {
            if (pesquisa == null || !SmpProfissoes.MODID.equals(pesquisa.getBranch().getNamespace())) return;
            ILocalResearchTree arvore = (ILocalResearchTree) (Object) this;
            IGlobalResearchTree global = IGlobalResearchTree.getInstance();
            ResourceLocation pai = pesquisa.getParent();
            int guarda = 0;
            while (pai != null && !pai.getPath().isEmpty() && guarda++ < 32) {
                IGlobalResearch gp = global.getResearch(pesquisa.getBranch(), pai);
                if (gp == null || !arvore.hasCompletedResearch(pai)) {
                    MutableComponent nome = gp == null ? Component.literal(pai.toString()) : MutableComponent.create(gp.getName());
                    jogador.sendSystemMessage(Component.literal("✘ Complete the previous research first: ").withStyle(ChatFormatting.RED)
                            .append(nome.withStyle(ChatFormatting.YELLOW)));
                    ci.cancel();
                    return;
                }
                pai = gp.getParent();
            }
        } catch (Throwable t) {
            SmpProfissoes.LOG.error("Falha ao conferir a cadeia de pesquisa: {}", t.toString());
        }
    }
}
