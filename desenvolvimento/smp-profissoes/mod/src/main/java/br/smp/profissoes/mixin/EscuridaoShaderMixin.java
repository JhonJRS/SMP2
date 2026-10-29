package br.smp.profissoes.mixin;

import br.smp.profissoes.MarcadorProtecao;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hardcore True Darkness (cliente): a escuridão total é proposital, mas com alguns shaders o passe final do mod não
 * carrega e ele cai no "blackout fallback", que pinta a tela de preto mesmo ao lado de uma tocha (o passe final não
 * enxerga a luz dos blocos sob shader). Com shaderpack ativo, o passe final (e o blackout) não roda; a escuridão do
 * lightmap — que respeita tochas — continua.
 */
@Pseudo
@Mixin(targets = "com.hyrrx.hardcoretruedarkness.client.FinalDarknessPostProcessor", remap = false)
public abstract class EscuridaoShaderMixin implements MarcadorProtecao {
    @Unique
    private static Method smpprofissoes$shaderAtivo;

    @Inject(method = "apply(F)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void smpprofissoes$semPasseComShader(float parcial, CallbackInfo ci) {
        try {
            if (smpprofissoes$shaderAtivo == null) {
                smpprofissoes$shaderAtivo = Class.forName("com.hyrrx.hardcoretruedarkness.client.ShaderPackState").getMethod("isShaderPathAvailable");
            }
            if (Boolean.TRUE.equals(smpprofissoes$shaderAtivo.invoke(null))) ci.cancel();
        } catch (Throwable t) {
            // sem como saber: deixa o mod seguir como é
        }
    }
}
