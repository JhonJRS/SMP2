package br.smp.profissoes.mixin;

import br.smp.profissoes.Contexto;
import br.smp.profissoes.MarcadorProtecao;
import br.smp.profissoes.Operador;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tick de toda máquina (bloco com entidade): as receitas buscadas durante o tick valem para o operador dela. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity", remap = false)
public abstract class TickBlocoMixin implements MarcadorProtecao {
    @Shadow(remap = false)
    @Final
    private BlockEntity blockEntity;

    @Unique
    private boolean smpprofissoes$empilhou;

    @Inject(method = "tick()V", at = @At("HEAD"), remap = false)
    private void smpprofissoes$antes(CallbackInfo ci) {
        smpprofissoes$empilhou = blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide
                && Operador.entrar(blockEntity);
    }

    @Inject(method = "tick()V", at = @At("RETURN"), remap = false)
    private void smpprofissoes$depois(CallbackInfo ci) {
        if (smpprofissoes$empilhou) {
            smpprofissoes$empilhou = false;
            Contexto.sair();
        }
    }
}
