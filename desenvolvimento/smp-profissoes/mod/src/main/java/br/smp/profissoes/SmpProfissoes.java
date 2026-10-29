package br.smp.profissoes;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponte entre as pesquisas da MineColonies e as licenças do RPEssentials.
 *
 * O RPEssentials bloqueia a fabricação pela licença (profissão) do jogador. Este mod concede e remove
 * automaticamente as licenças de etapa (ex.: "eng_latao") conforme a colônia do jogador conclui as
 * pesquisas e mantém os edifícios exigidos. Tudo é calculado no servidor.
 */
@Mod(SmpProfissoes.MODID)
public final class SmpProfissoes {
    public static final String MODID = "smpprofissoes";
    public static final Logger LOG = LoggerFactory.getLogger("SMP Profissões");

    public SmpProfissoes(IEventBus modBus) {
        Componentes.registrar(modBus);
        Especiais.registrar(modBus);
        Operador.registrar(modBus);
        Rede.registrar(modBus);
        if (ModList.get().isLoaded("enhancedcelestials2core") && ModList.get().isLoaded("sereneseasons")) {
            EstacoesCelestes.registrar(modBus);
        }
        if (ModList.get().isLoaded("irons_restrictions") && ModList.get().isLoaded("irons_spellbooks")) {
            EscolasMagia.registrar();
        }
        NeoForge.EVENT_BUS.register(Eventos.class);
        NeoForge.EVENT_BUS.register(Operador.class);
        Regras.recarregar(); // cedo: a guarda de receitas precisa dos namespaces antes do mundo carregar
    }
}
