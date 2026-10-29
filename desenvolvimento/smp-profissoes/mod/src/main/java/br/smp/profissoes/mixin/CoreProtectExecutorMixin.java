package br.smp.profissoes.mixin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CoreProtectNeo 1.0: o DatabaseManager é único e o executor dele é final; ao sair de um mundo ele é desligado e, ao
 * abrir outro mundo na mesma sessão, a limpeza periódica (DatabasePurgeService) mandava tarefa para o executor morto e o
 * servidor fechava (crash de 28/10 15:27, RejectedExecutionException). Com o executor desligado, usa um reserva.
 */
@Mixin(targets = "TheAmirtini.coreprotectneo.database.DatabaseManager", remap = false)
public abstract class CoreProtectExecutorMixin {
    @Shadow @Final private ExecutorService asyncExecutor;

    @Unique
    private static ExecutorService smpprofissoes$reserva;

    @Inject(method = "runAsync", at = @At("HEAD"), cancellable = true, remap = false)
    private void smpprofissoes$executorVivo(Runnable tarefa, CallbackInfo ci) {
        if (asyncExecutor.isShutdown()) {
            synchronized (CoreProtectExecutorMixin.class) {
                if (smpprofissoes$reserva == null || smpprofissoes$reserva.isShutdown())
                    smpprofissoes$reserva = Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r, "CoreProtectNeo-reserva");
                        t.setDaemon(true);
                        return t;
                    });
            }
            smpprofissoes$reserva.execute(tarefa);
            ci.cancel();
        }
    }
}
