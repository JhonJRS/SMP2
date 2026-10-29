package br.smp.profissoes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Componentes das pesquisas cruzadas (níveis 4 e 5): itens de verdade, fabricados na bancada por quem tem a licença da
 * pesquisa, com materiais de outras profissões. Entram nas receitas de máquina/ritual (que não sabem quem é o jogador)
 * e garantem que só sai o item final quem concluiu a pesquisa. A lista vem de assets/smpprofissoes/componentes.json,
 * gerado por smp-profissoes/tools/gerar.py.
 */
public final class Componentes {
    private Componentes() {}

    private static final DeferredRegister.Items ITENS = DeferredRegister.createItems(SmpProfissoes.MODID);
    private static final List<DeferredItem<Item>> REGISTRADOS = new ArrayList<>();

    static final class ItemComponente extends Item {
        private final String id;

        ItemComponente(String id) {
            super(new Item.Properties().rarity(Rarity.UNCOMMON));
            this.id = id;
        }

        @Override
        public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> linhas, TooltipFlag flag) {
            linhas.add(Component.translatable("item." + SmpProfissoes.MODID + "." + id + ".desc").withStyle(ChatFormatting.GRAY));
            linhas.add(Component.literal("Guild of Trades component").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        // sem brilho de encantamento: o ícone animado (tools/icones.py) já faz esse papel
    }

    static List<String> ids() {
        List<String> out = new ArrayList<>();
        try (InputStream in = Componentes.class.getResourceAsStream("/assets/" + SmpProfissoes.MODID + "/componentes.json")) {
            if (in == null) return out;
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement e : arr) out.add(e.getAsString());
        } catch (Exception e) {
            SmpProfissoes.LOG.error("Não consegui ler componentes.json", e);
        }
        return out;
    }

    static void registrar(IEventBus modBus) {
        for (String id : ids()) REGISTRADOS.add(ITENS.register(id, () -> new ItemComponente(id)));
        ITENS.register(modBus);
        modBus.addListener(Componentes::abaCriativo);
        SmpProfissoes.LOG.info("Componentes registrados: {}", REGISTRADOS.size());
    }

    private static void abaCriativo(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().location().toString().equals("minecraft:ingredients")) for (DeferredItem<Item> d : REGISTRADOS) e.accept(d.get());
    }
}
