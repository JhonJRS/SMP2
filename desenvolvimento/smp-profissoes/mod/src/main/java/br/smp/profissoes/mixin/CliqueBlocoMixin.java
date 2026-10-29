package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clique direito num bloco: máquinas que procuram receita na hora do clique (ex.: caldeirão do Enchanted) usam o jogador. */
@Mixin(value = ServerPlayerGameMode.class, remap = false)
public abstract class CliqueBlocoMixin implements MarcadorProtecao {

    @Inject(method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), remap = false)
    private void smpprofissoes$antes(ServerPlayer p, Level level, ItemStack st, InteractionHand mao, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        Contexto.entrarOperador(p);
    }

    @Inject(method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"), remap = false)
    private void smpprofissoes$depois(ServerPlayer p, Level level, ItemStack st, InteractionHand mao, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (Contexto.operador() == p) Contexto.sair();
    }
}
