package br.smp.translucidez;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Em RP ninguém fica invisível de verdade: jogadores invisíveis (poção, feitiço, comando, qualquer meio) são desenhados
 * translúcidos para todos. Tudo é feito no cliente pelos mixins; no servidor o mod não faz nada.
 */
@Mod(SmpTranslucidez.ID)
public final class SmpTranslucidez {
    public static final String ID = "smptranslucidez";
    public static final Logger LOG = LoggerFactory.getLogger("SMP Translucidez");

    public SmpTranslucidez() {
        Config.carregar();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(NomeInvisivel.class);
        LOG.info("Invisibilidade de jogadores vira translucidez: opacidade {}%, nome {}",
                Config.opacidade, Config.mostrarNome ? "visível" : "oculto");
    }
}
