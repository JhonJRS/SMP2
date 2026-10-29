package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create Blueprint na parede: clicar fabrica a receita com o inventário do jogador; o jogador é o contexto. */
@Mixin(value = BlueprintEntity.class, remap = false)
public abstract class BlueprintEntityMixin implements MarcadorProtecao {
    private static final String ALVO = "interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;";

    @Inject(method = ALVO, at = @At("HEAD"), remap = false)
    private void smpprofissoes$entrar(Player p, Vec3 v, InteractionHand m, CallbackInfoReturnable<InteractionResult> cir) { Contexto.entrar(p); }

    @Inject(method = ALVO, at = @At("RETURN"), remap = false)
    private void smpprofissoes$sair(Player p, Vec3 v, InteractionHand m, CallbackInfoReturnable<InteractionResult> cir) { Contexto.sair(); }
}
