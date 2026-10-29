package br.smp.profissoes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rp.rpessentials.profession.LicenseManager;

/**
 * Itens com comportamento próprio (lista em assets/smpprofissoes/especiais.json, gerada por gerar.py):
 * - guild_charter: uso único; dá uma segunda profissão (máximo 2). Abre no chat a escolha da profissão + mod inicial,
 *   confirmada por /guildcharter &lt;etapa&gt; (só funciona com uma carta "aberta", guardada nos dados do jogador).
 * - draught_of_oblivion: ao beber, apaga profissões, mods iniciais e etapas (profissoes:admin/resetar) e o menu de
 *   escolha volta a aparecer.
 */
public final class Especiais {
    private Especiais() {}

    private static final DeferredRegister.Items ITENS = DeferredRegister.createItems(SmpProfissoes.MODID);
    private static final List<DeferredItem<Item>> REGISTRADOS = new ArrayList<>();
    /** Marcador (persistente após a morte) de carta usada e escolha ainda não feita. */
    private static final String CARTA_ABERTA = "smpprofissoes_carta_aberta";
    private static final int MAX_PROFISSOES = 2;

    private static final Map<String, ChatFormatting> CORES = Map.of(
            "engenheiro", ChatFormatting.GOLD, "arcanista", ChatFormatting.DARK_PURPLE,
            "ferreiro", ChatFormatting.RED, "naturalista", ChatFormatting.GREEN);

    static List<String> ids() {
        List<String> out = new ArrayList<>();
        try (InputStream in = Especiais.class.getResourceAsStream("/assets/" + SmpProfissoes.MODID + "/especiais.json")) {
            if (in == null) return out;
            JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement e : arr) out.add(e.getAsString());
        } catch (Exception e) {
            SmpProfissoes.LOG.error("Não consegui ler especiais.json", e);
        }
        return out;
    }

    static void registrar(IEventBus modBus) {
        for (String id : ids()) {
            switch (id) {
                case "guild_charter" -> REGISTRADOS.add(ITENS.register(id, () -> new Carta(id)));
                case "draught_of_oblivion" -> REGISTRADOS.add(ITENS.register(id, () -> new Esquecimento(id)));
                case "guild_letter" -> REGISTRADOS.add(ITENS.register(id, () -> new CartaApresentacao(id)));
                case "school_codex" -> REGISTRADOS.add(ITENS.register(id, () -> new CodexEscolas(id)));
                // sem comportamento próprio (ex.: convergence_matrix, provisório): item simples, raro, com a descrição
                default -> REGISTRADOS.add(ITENS.register(id, () -> new Base(id, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)) {}));
            }
        }
        ITENS.register(modBus);
        modBus.addListener(Especiais::abaCriativo);
    }

    private static void abaCriativo(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().location().toString().equals("minecraft:tools_and_utilities")) for (DeferredItem<Item> d : REGISTRADOS) e.accept(d.get());
    }

    private abstract static class Base extends Item {
        final String id;

        Base(String id, Properties p) {
            super(p);
            this.id = id;
        }

        @Override
        public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> linhas, TooltipFlag flag) {
            linhas.add(Component.translatable("item." + SmpProfissoes.MODID + "." + id + ".desc").withStyle(ChatFormatting.GRAY));
        }
        // sem brilho de encantamento: o ícone animado (tools/icones.py) já faz esse papel
    }

    static List<String> profissoesDo(ServerPlayer p) {
        List<String> lic = LicenseManager.getLicenses(p.getUUID());
        return Regras.get().profissoes.stream().filter(x -> lic.stream().anyMatch(l -> l.equalsIgnoreCase(x))).toList();
    }

    private static CompoundTag persistente(Player p) {
        CompoundTag raiz = p.getPersistentData();
        if (!raiz.contains(Player.PERSISTED_NBT_TAG)) raiz.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return raiz.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** Roda um comando do datapack de profissões como o jogador, com permissão de admin e sem eco no chat. */
    private static void comoJogador(ServerPlayer p, String comando) {
        CommandSourceStack src = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        p.server.getCommands().performPrefixedCommand(src, comando);
    }

    // ------------------------------------------------------------------ Codex of Schools (Iron's Spells)
    /** Uso único: dá direito a estudar mais uma escola de magia (escolha em /escola). */
    static final class CodexEscolas extends Base {
        CodexEscolas(String id) {
            super(id, new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
            ItemStack st = jogador.getItemInHand(mao);
            if (level.isClientSide || !(jogador instanceof ServerPlayer p)) return InteractionResultHolder.success(st);
            if (!net.neoforged.fml.ModList.get().isLoaded("irons_restrictions")) return InteractionResultHolder.fail(st);
            if (EscolasMagia.escolas(p).isEmpty()) {
                p.sendSystemMessage(Component.literal("✘ Choose your first school of magic before studying another (/escola).").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(st);
            }
            st.consume(1, p);
            level.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
            EscolasMagia.ganharEscola(p);
            return InteractionResultHolder.consume(st);
        }
    }

    // ------------------------------------------------------------------ Guild Charter
    static final class Carta extends Base {
        Carta(String id) {
            super(id, new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
            ItemStack st = jogador.getItemInHand(mao);
            if (level.isClientSide || !(jogador instanceof ServerPlayer p)) return InteractionResultHolder.success(st);
            List<String> tem = profissoesDo(p);
            if (tem.isEmpty()) {
                p.sendSystemMessage(Component.translatable("smpprofissoes.carta.sem").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(st);
            }
            if (tem.size() >= MAX_PROFISSOES) {
                p.sendSystemMessage(Component.translatable("smpprofissoes.carta.max").withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(st);
            }
            if (!persistente(p).getBoolean(CARTA_ABERTA)) {  // uso único: a carta some ao abrir a escolha
                persistente(p).putBoolean(CARTA_ABERTA, true);
                st.consume(1, p);
                level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
            }
            mostrarEscolha(p, tem);
            Rede.abrirEscolha(p, true, tem);
            return InteractionResultHolder.consume(st);
        }
    }

    static void mostrarEscolha(ServerPlayer p, List<String> tem) {
        p.sendSystemMessage(Component.translatable("smpprofissoes.carta.escolha").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String prof : Regras.get().profissoes) {
            if (tem.contains(prof)) continue;
            ChatFormatting cor = CORES.getOrDefault(prof, ChatFormatting.YELLOW);
            MutableComponent linha = Component.literal("  " + nomeProfissao(prof) + ": ").withStyle(cor, ChatFormatting.BOLD);
            for (Regras.Etapa e : Regras.get().etapas) {
                if (!e.inicio || !e.profissao.equals(prof)) continue;
                linha.append(Component.literal("[" + e.nome + "]").withStyle(Style.EMPTY.withColor(cor).withBold(false)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guildcharter " + e.licenca))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(nomeProfissao(prof) + " · " + e.nome)))));
                linha.append(Component.literal(" "));
            }
            p.sendSystemMessage(linha);
        }
    }

    static String nomeProfissao(String prof) {
        return prof.isEmpty() ? prof : Character.toUpperCase(prof.charAt(0)) + prof.substring(1);
    }

    static void comandos(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("guildcharter").then(Commands.argument("etapa", StringArgumentType.word()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (!persistente(p).getBoolean(CARTA_ABERTA)) {
                c.getSource().sendFailure(Component.literal("Use a Guild Charter first."));
                return 0;
            }
            String lic = StringArgumentType.getString(c, "etapa").toLowerCase();
            Regras.Etapa e = Regras.get().etapas.stream().filter(x -> x.licenca.equals(lic) && x.inicio).findFirst().orElse(null);
            List<String> tem = profissoesDo(p);
            if (e == null || tem.contains(e.profissao)) {
                c.getSource().sendFailure(Component.literal("Not an available starting mod: " + lic));
                return 0;
            }
            if (tem.size() >= MAX_PROFISSOES) {
                persistente(p).remove(CARTA_ABERTA);
                c.getSource().sendFailure(Component.translatable("smpprofissoes.carta.max"));
                return 0;
            }
            persistente(p).remove(CARTA_ABERTA);
            comoJogador(p, "function profissoes:inicio/" + lic);  // licença da profissão + mod inicial + tag
            p.sendSystemMessage(Component.translatable("smpprofissoes.carta.ok", nomeProfissao(e.profissao) + " (" + e.nome + ")")
                    .withStyle(ChatFormatting.GREEN));
            SmpProfissoes.LOG.info("{} usou Guild Charter: {} / {}", p.getGameProfile().getName(), e.profissao, lic);
            return 1;
        })));
    }

    /** Ao entrar: quem fechou o jogo no meio da escolha vê as opções de novo. */
    static void aoEntrar(ServerPlayer p) {
        if (persistente(p).getBoolean(CARTA_ABERTA)) {
            List<String> tem = profissoesDo(p);
            if (tem.isEmpty() || tem.size() >= MAX_PROFISSOES) persistente(p).remove(CARTA_ABERTA);
            else mostrarEscolha(p, tem);
        }
    }

    // ------------------------------------------------------------------ Carta de Apresentação (Vagabundo → escolhe a profissão)
    static final class CartaApresentacao extends Base {
        CartaApresentacao(String id) {
            super(id, new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
            ItemStack st = jogador.getItemInHand(mao);
            if (level.isClientSide || !(jogador instanceof ServerPlayer p)) return InteractionResultHolder.success(st);
            if (!profissoesDo(p).isEmpty()) {
                p.sendSystemMessage(Component.translatable("smpprofissoes.carta_apresentacao.ja").withStyle(ChatFormatting.GRAY));
                return InteractionResultHolder.fail(st);
            }
            // o datapack de profissões mostra o menu para quem tem a marca prof.escolhendo (ver profissoes:tick)
            comoJogador(p, "tag @s add prof.escolhendo");
            // a tela abre na hora; o menu do chat fica de reserva e só reaparece ~30 s depois (prof.timer 640 → 39)
            comoJogador(p, "scoreboard players set @s prof.timer 41");
            Rede.abrirEscolha(p, false, List.of());
            level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
            return InteractionResultHolder.consume(st);
        }
    }

    // ------------------------------------------------------------------ Draught of Oblivion
    static final class Esquecimento extends Base {
        Esquecimento(String id) {
            super(id, new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
        }

        @Override
        public UseAnim getUseAnimation(ItemStack st) {
            return UseAnim.DRINK;
        }

        @Override
        public int getUseDuration(ItemStack st, LivingEntity quem) {
            return 64;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player jogador, InteractionHand mao) {
            return ItemUtils.startUsingInstantly(level, jogador, mao);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack st, Level level, LivingEntity quem) {
            if (!level.isClientSide && quem instanceof ServerPlayer p) {
                persistente(p).remove(CARTA_ABERTA);
                comoJogador(p, "function profissoes:admin/resetar");
                p.sendSystemMessage(Component.translatable("smpprofissoes.esquecer.ok").withStyle(ChatFormatting.DARK_PURPLE));
                level.playSound(null, p.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6f, 1.2f);
                SmpProfissoes.LOG.info("{} bebeu o Draught of Oblivion", p.getGameProfile().getName());
                st.consume(1, p);
            }
            return st;
        }
    }
}
