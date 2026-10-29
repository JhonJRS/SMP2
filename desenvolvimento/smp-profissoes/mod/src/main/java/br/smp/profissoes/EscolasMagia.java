package br.smp.profissoes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.relimer.ironsrestrictions.registries.ComponentRegistry;
import com.relimer.ironsrestrictions.registries.ItemRegistry;
import com.relimer.ironsrestrictions.util.SchoolContainer;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rp.rpessentials.profession.LicenseManager;

/**
 * Escolas de magia do Iron's Spells (com Iron's Restrictions):
 * - quem tem o Iron's (mod inicial ou a pesquisa Iron's Foundations) escolhe UMA escola com /escola e recebe o
 *   manuscrito dela; só aprende magias das escolas escolhidas (manuscritos de outras escolas e o Unfinished Manuscript,
 *   que ensina uma magia aleatória de qualquer escola, são recusados);
 * - depois da escolha, magias de outras escolas não podem ser conjuradas por nenhum meio (aoConjurar);
 * - a pesquisa Cross-School Studies libera o Codex of Schools: cada codex usado dá direito a mais uma escola.
 * Escolas sem nenhuma magia habilitada não entram nas listas de escolha.
 * Dados no jogador (persistem após a morte). OP ignora a regra. Só é registrado com Iron's Restrictions instalado.
 */
public final class EscolasMagia {
    private EscolasMagia() {}

    static final String ESCOLAS = "smpprofissoes_escolas";
    static final String LIVRES = "smpprofissoes_escolas_livres";
    static final List<String> INICIAIS = EscolasIniciais.IDS;
    private static final Map<UUID, Long> AVISADO = new HashMap<>();

    static void registrar() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(EscolasMagia.class);
    }

    // ---------------------------------------------------------------- dados
    private static CompoundTag dados(Player p) {
        CompoundTag raiz = p.getPersistentData();
        if (!raiz.contains(Player.PERSISTED_NBT_TAG)) raiz.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return raiz.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static List<String> escolas(Player p) {
        List<String> out = new ArrayList<>();
        for (Tag t : dados(p).getList(ESCOLAS, Tag.TAG_STRING)) out.add(t.getAsString());
        return out;
    }

    static int livres(Player p) {
        return dados(p).getInt(LIVRES);
    }

    /** Tem o Iron's (mod inicial ou pesquisa) — então precisa ter escola. */
    static boolean temIrons(ServerPlayer p) {
        List<String> lic = LicenseManager.getLicenses(p.getUUID());
        return lic.stream().anyMatch(l -> l.equalsIgnoreCase("arc_irons") || l.equalsIgnoreCase(Regras.PREFIXO_INICIO + "arc_irons"));
    }

    static boolean podeEscolher(ServerPlayer p) {
        return (temIrons(p) && escolas(p).isEmpty()) || livres(p) > 0;
    }

    static void limpar(Player p) {
        dados(p).remove(ESCOLAS);
        dados(p).remove(LIVRES);
    }

    /** Codex of Schools: mais uma escola. */
    public static void ganharEscola(ServerPlayer p) {
        dados(p).putInt(LIVRES, livres(p) + 1);
        mostrar(p);
    }

    // ---------------------------------------------------------------- menu
    static List<ResourceLocation> oferecidas(ServerPlayer p) {
        List<String> tem = escolas(p);
        List<ResourceLocation> out = new ArrayList<>();
        if (tem.isEmpty()) {
            for (String s : INICIAIS) {  // só as que existem (um addon removido some da lista)
                ResourceLocation id = ResourceLocation.parse(s);
                if (SchoolRegistry.REGISTRY.containsKey(id) && temMagias(id)) out.add(id);
            }
        } else {
            for (ResourceLocation id : SchoolRegistry.REGISTRY.keySet()) if (!tem.contains(id.toString()) && temMagias(id)) out.add(id);
        }
        return out;
    }

    static void mostrar(ServerPlayer p) {
        if (!podeEscolher(p)) {
            p.sendSystemMessage(Component.literal("Your schools of magic: " + String.join(", ", escolas(p))).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        p.sendSystemMessage(Component.literal(escolas(p).isEmpty() ? "✦ Choose your school of magic (you can only learn spells from it):"
                : "✦ Choose another school of magic:").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        MutableComponent linha = Component.literal("  ");
        for (ResourceLocation id : oferecidas(p)) {
            Component nome = Component.translatable("school." + id.getNamespace() + "." + id.getPath());  // ex.: "Melodic"
            linha.append(Component.literal("[").append(nome).append("]").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/escola " + id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("School: ").append(nome)
                            .append(" (" + id + ")")))));
            linha.append(" ");
        }
        p.sendSystemMessage(linha);
    }

    static boolean escolher(ServerPlayer p, ResourceLocation id) {
        if (!podeEscolher(p) || !oferecidas(p).contains(id)) {
            p.sendSystemMessage(Component.literal("✘ You cannot choose that school now.").withStyle(ChatFormatting.RED));
            return false;
        }
        SchoolType escola = SchoolRegistry.getSchool(id);
        if (escola == null) return false;
        boolean primeira = escolas(p).isEmpty();
        ListTag lista = dados(p).getList(ESCOLAS, Tag.TAG_STRING);
        lista.add(StringTag.valueOf(id.toString()));
        dados(p).put(ESCOLAS, lista);
        if (!primeira || livres(p) > 0) dados(p).putInt(LIVRES, Math.max(0, livres(p) - (primeira && temIrons(p) ? 0 : 1)));
        p.getInventory().placeItemBackInInventory(manuscrito(escola));
        p.sendSystemMessage(Component.literal("✔ You are now a student of the School of " + id.getPath()
                + ". You received its manuscript.").withStyle(ChatFormatting.LIGHT_PURPLE));
        SmpProfissoes.LOG.info("{} escolheu a escola de magia {}", p.getGameProfile().getName(), id);
        return true;
    }

    static ItemStack manuscrito(SchoolType escola) {
        ItemStack st = new ItemStack(ItemRegistry.MANUSCRIPT.get());
        st.set(ComponentRegistry.SCHOOL_COMPONENT.get(), new SchoolContainer(escola));
        return st;
    }

    // ---------------------------------------------------------------- regra
    @SubscribeEvent
    public static void aoUsar(PlayerInteractEvent.RightClickItem ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p) || p.hasPermissions(2)) return;
        ItemStack st = ev.getItemStack();
        String escola = null;
        if (st.is(ItemRegistry.UNFINISHED_MANUSCRIPT.get())) {
            recusar(ev, p, "The Unfinished Manuscript teaches a random spell of any school; study your own school's manuscripts instead.");
            return;
        }
        if (st.is(ItemRegistry.MANUSCRIPT.get())) {
            SchoolContainer c = st.get(ComponentRegistry.SCHOOL_COMPONENT.get());
            if (c != null && c.getSchoolType() != null) escola = c.getSchoolId().toString();
        } else if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals("irons_spellbooks:eldritch_manuscript")) {
            escola = "irons_spellbooks:eldritch";
        }
        if (escola == null || escolas(p).contains(escola)) return;
        recusar(ev, p, escolas(p).isEmpty()
                ? "You have not chosen a school of magic (only Iron's Spells arcanists can learn spells). Use /escola."
                : "This manuscript belongs to another school (" + escola + "). Your schools: " + String.join(", ", escolas(p))
                + ". A Codex of Schools lets you study one more.");
    }

    /**
     * Depois de escolher a escola, magias de outras escolas não saem por NENHUM meio: grimório, pergaminho, arma ou item
     * imbuído (o Iron's dispara este evento no servidor antes de qualquer conjuração de jogador).
     */
    @SubscribeEvent
    public static void aoConjurar(SpellPreCastEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p) || p.hasPermissions(2)) return;
        List<String> minhas = escolas(p);
        SchoolType escola = ev.getSchoolType();
        if (minhas.isEmpty() || escola == null || minhas.contains(escola.getId().toString())) return;
        ev.setCanceled(true);
        p.displayClientMessage(Component.literal("✘ You only master the School of ").withStyle(ChatFormatting.RED)
                .append(Component.literal(nomes(minhas)).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(": this spell belongs to ").withStyle(ChatFormatting.RED))
                .append(nome(escola.getId()).copy().withStyle(ChatFormatting.GOLD)), true);
    }

    static Component nome(ResourceLocation id) {
        return Component.translatable("school." + id.getNamespace() + "." + id.getPath());
    }

    private static String nomes(List<String> ids) {
        List<String> out = new ArrayList<>();
        for (String s : ids) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            out.add(id == null ? s : nome(id).getString());
        }
        return String.join(", ", out);
    }

    /** Escola sem nenhuma magia habilitada (Void, Dune, Ritual, Technomancy hoje): o manuscrito sairia vazio. */
    static boolean temMagias(ResourceLocation id) {
        return SpellRegistry.getEnabledSpells().stream().anyMatch(sp -> sp.getSchoolType() != null && sp.getSchoolType().getId().equals(id));
    }

    private static void recusar(PlayerInteractEvent.RightClickItem ev, ServerPlayer p, String msg) {
        ev.setCanceled(true);
        p.sendSystemMessage(Component.literal("✘ " + msg).withStyle(ChatFormatting.RED));
    }

    /** Quem tem o Iron's e ainda não escolheu recebe o lembrete (no máximo a cada 30 s). */
    @SubscribeEvent
    public static void lembrete(ServerTickEvent.Post ev) {
        if (ev.getServer().getTickCount() % 100 != 0) return;
        long agora = ev.getServer().getTickCount();
        for (ServerPlayer p : ev.getServer().getPlayerList().getPlayers()) {
            if (temIrons(p) && escolas(p).isEmpty() && agora - AVISADO.getOrDefault(p.getUUID(), -10000L) >= 600) {
                AVISADO.put(p.getUUID(), agora);
                mostrar(p);
            }
        }
    }

    @SubscribeEvent
    public static void aoSair(PlayerEvent.PlayerLoggedOutEvent ev) {
        AVISADO.remove(ev.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent ev) {
        CommandDispatcher<CommandSourceStack> d = ev.getDispatcher();
        d.register(Commands.literal("escola")
                .executes(c -> {
                    mostrar(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.argument("escola", StringArgumentType.greedyString()).executes(c -> {
                    ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(c, "escola").trim());
                    return id != null && escolher(c.getSource().getPlayerOrException(), id) ? 1 : 0;
                })));
        d.register(Commands.literal(SmpProfissoes.MODID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("escolas").then(Commands.argument("jogador", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "jogador");
                            c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": " + escolas(p)
                                    + " (+" + livres(p) + " to choose)"), false);
                            return 1;
                        })
                        .then(Commands.literal("limpar").executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "jogador");
                            limpar(p);
                            c.getSource().sendSuccess(() -> Component.literal("Schools of magic cleared: " + p.getGameProfile().getName()), true);
                            return 1;
                        })))));
    }
}
