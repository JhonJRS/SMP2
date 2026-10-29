package br.smp.profissoes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.corgitaco.enhancedcelestials2core.api.EnhancedCelestialsRegistry;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.spawnrule.LunarEventSpawnRule;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.spawnrule.LunarEventSpawnRuleType;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.spawnrule.SpawnRuleContext;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import sereneseasons.api.season.SeasonHelper;

/**
 * Regra de eventos lunares do Enhanced Celestials ligada às estações do Serene Seasons:
 * {"type": "smpprofissoes:season", "settings": {"seasons": ["AUTUMN", "WINTER"]}} só deixa o evento acontecer nessas
 * estações (estação do Overworld). Só é carregada se os dois mods estão presentes (ver SmpProfissoes); os arquivos de
 * evento que a usam têm a condição neoforge:mod_loaded, então sem os mods o Enhanced Celestials segue o padrão.
 */
public final class EstacoesCelestes {
    private EstacoesCelestes() {}

    public record RegraEstacao(List<String> estacoes) implements LunarEventSpawnRule {
        static final MapCodec<RegraEstacao> CODEC = Codec.STRING.listOf().fieldOf("seasons").xmap(RegraEstacao::new, RegraEstacao::estacoes);

        @Override
        public boolean passes(SpawnRuleContext ctx) {
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null || estacoes.isEmpty()) return true;
                String atual = SeasonHelper.getSeasonState(server.overworld()).getSeason().name();
                return estacoes.stream().anyMatch(e -> e.toUpperCase(Locale.ROOT).equals(atual));
            } catch (Throwable t) {
                return true; // falha = não interfere
            }
        }

        @Override
        public LunarEventSpawnRuleType<?> type() {
            return TIPO.get();
        }
    }

    private static final DeferredRegister<LunarEventSpawnRuleType<?>> TIPOS =
            DeferredRegister.create(EnhancedCelestialsRegistry.LUNAR_EVENT_SPAWN_RULE_TYPE_KEY, SmpProfissoes.MODID);
    // o Enhanced Celestials lê as configurações da regra dentro de "settings"
    private static final Supplier<LunarEventSpawnRuleType<RegraEstacao>> TIPO =
            TIPOS.register("season", () -> () -> RegraEstacao.CODEC.codec().fieldOf("settings"));

    static void registrar(IEventBus modBus) {
        TIPOS.register(modBus);
        SmpProfissoes.LOG.info("Regra de eventos lunares por estação (Enhanced Celestials + Serene Seasons) registrada");
    }
}
