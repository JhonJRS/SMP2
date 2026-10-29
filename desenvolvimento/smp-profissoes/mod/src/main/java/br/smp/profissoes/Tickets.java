package br.smp.profissoes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Pedidos de ajuda aos ADMs.
 *   /ticket &lt;mensagem&gt;          (qualquer jogador) abre um ticket com a posição atual; ADMs online são avisados
 *   /ticket meus                   (jogador) os próprios tickets abertos
 *   /tickets                       (ADM) lista os abertos, com botões [TP] [Assumir] [Fechar]
 *   /tickets assumir|fechar &lt;id&gt;  (ADM) avisa o jogador
 * Guardados no mundo (SavedData "smpprofissoes_tickets").
 */
public final class Tickets {
    private Tickets() {}

    static final class Ticket {
        int id;
        UUID autor;
        String nome, texto, dimensao, data, responsavel = "";
        int x, y, z;
    }

    static final class Dados extends SavedData {
        final List<Ticket> abertos = new ArrayList<>();
        int proximo = 1;

        static Dados ler(CompoundTag t, HolderLookup.Provider reg) {
            Dados d = new Dados();
            d.proximo = Math.max(1, t.getInt("proximo"));
            for (Tag e : t.getList("abertos", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) e;
                Ticket k = new Ticket();
                k.id = c.getInt("id");
                k.autor = c.getUUID("autor");
                k.nome = c.getString("nome");
                k.texto = c.getString("texto");
                k.dimensao = c.getString("dimensao");
                k.data = c.getString("data");
                k.responsavel = c.getString("responsavel");
                k.x = c.getInt("x");
                k.y = c.getInt("y");
                k.z = c.getInt("z");
                d.abertos.add(k);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
            t.putInt("proximo", proximo);
            ListTag l = new ListTag();
            for (Ticket k : abertos) {
                CompoundTag c = new CompoundTag();
                c.putInt("id", k.id);
                c.putUUID("autor", k.autor);
                c.putString("nome", k.nome);
                c.putString("texto", k.texto);
                c.putString("dimensao", k.dimensao);
                c.putString("data", k.data);
                c.putString("responsavel", k.responsavel);
                c.putInt("x", k.x);
                c.putInt("y", k.y);
                c.putInt("z", k.z);
                l.add(c);
            }
            t.put("abertos", l);
            return t;
        }
    }

    static Dados dados(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Dados::new, Dados::ler), "smpprofissoes_tickets");
    }

    private static MutableComponent botao(String txt, ChatFormatting cor, String comando, String dica, boolean executar) {
        return Component.literal(txt).withStyle(Style.EMPTY.withColor(cor)
                .withClickEvent(new ClickEvent(executar ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, comando))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dica))));
    }

    private static Component linha(Ticket k) {
        MutableComponent m = Component.literal("#" + k.id + " ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(k.nome + ": ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(k.texto).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  (" + k.data + (k.responsavel.isEmpty() ? "" : ", " + k.responsavel) + ") ").withStyle(ChatFormatting.DARK_GRAY));
        m.append(botao("[TP]", ChatFormatting.AQUA, "/execute in " + k.dimensao + " run tp @s " + k.x + " " + k.y + " " + k.z,
                "Teleport to where the ticket was opened", true)).append(" ");
        m.append(botao("[Assumir]", ChatFormatting.GREEN, "/tickets assumir " + k.id, "Take this ticket", true)).append(" ");
        m.append(botao("[Fechar]", ChatFormatting.RED, "/tickets fechar " + k.id, "Close this ticket", true));
        return m;
    }

    static void avisarAdms(MinecraftServer s, Component msg) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) if (p.hasPermissions(2)) p.sendSystemMessage(msg);
    }

    static void comandos(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ticket")
                .then(Commands.literal("meus").executes(Tickets::meus))
                .then(Commands.argument("mensagem", StringArgumentType.greedyString()).executes(Tickets::abrir)));
        d.register(Commands.literal("tickets").requires(s -> s.hasPermission(2))
                .executes(Tickets::listar)
                .then(Commands.literal("assumir").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> mudar(c, false))))
                .then(Commands.literal("fechar").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> mudar(c, true)))));
    }

    private static int abrir(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Dados d = dados(c.getSource().getServer());
        if (d.abertos.stream().filter(k -> k.autor.equals(p.getUUID())).count() >= 3) {
            c.getSource().sendFailure(Component.literal("You already have 3 open tickets. Wait for an admin (see /ticket meus)."));
            return 0;
        }
        Ticket k = new Ticket();
        k.id = d.proximo++;
        k.autor = p.getUUID();
        k.nome = p.getGameProfile().getName();
        k.texto = StringArgumentType.getString(c, "mensagem");
        k.dimensao = p.level().dimension().location().toString();
        k.x = p.getBlockX();
        k.y = p.getBlockY();
        k.z = p.getBlockZ();
        k.data = new SimpleDateFormat("dd/MM HH:mm").format(new Date());
        d.abertos.add(k);
        d.setDirty();
        p.sendSystemMessage(Component.literal("✔ Ticket #" + k.id + " sent to the admins. You will be notified when someone takes it.").withStyle(ChatFormatting.GREEN));
        avisarAdms(c.getSource().getServer(), Component.literal("✉ New ticket ").withStyle(ChatFormatting.GOLD).append(linha(k)));
        SmpProfissoes.LOG.info("Ticket #{} de {}: {}", k.id, k.nome, k.texto);
        return k.id;
    }

    private static int meus(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<Ticket> meus = dados(c.getSource().getServer()).abertos.stream().filter(k -> k.autor.equals(p.getUUID())).toList();
        if (meus.isEmpty()) p.sendSystemMessage(Component.literal("You have no open tickets.").withStyle(ChatFormatting.GRAY));
        for (Ticket k : meus) p.sendSystemMessage(Component.literal("#" + k.id + " " + k.texto + "  (" + k.data
                + (k.responsavel.isEmpty() ? ", waiting" : ", taken by " + k.responsavel) + ")").withStyle(ChatFormatting.YELLOW));
        return meus.size();
    }

    private static int listar(CommandContext<CommandSourceStack> c) {
        Dados d = dados(c.getSource().getServer());
        c.getSource().sendSystemMessage(Component.literal("── Open tickets: " + d.abertos.size() + " ──").withStyle(ChatFormatting.GOLD));
        for (Ticket k : d.abertos) c.getSource().sendSystemMessage(linha(k));
        return d.abertos.size();
    }

    private static int mudar(CommandContext<CommandSourceStack> c, boolean fechar) {
        int id = IntegerArgumentType.getInteger(c, "id");
        Dados d = dados(c.getSource().getServer());
        Ticket k = d.abertos.stream().filter(t -> t.id == id).findFirst().orElse(null);
        if (k == null) {
            c.getSource().sendFailure(Component.literal("Ticket #" + id + " not found."));
            return 0;
        }
        String adm = c.getSource().getTextName();
        if (fechar) d.abertos.remove(k);
        else k.responsavel = adm;
        d.setDirty();
        ServerPlayer autor = c.getSource().getServer().getPlayerList().getPlayer(k.autor);
        if (autor != null) autor.sendSystemMessage(Component.literal(fechar ? "✔ Your ticket #" + id + " was closed by " + adm + "."
                : "✦ " + adm + " took your ticket #" + id + " and will help you soon.").withStyle(ChatFormatting.GREEN));
        c.getSource().sendSuccess(() -> Component.literal((fechar ? "Closed ticket #" : "You took ticket #") + id), true);
        return 1;
    }

    /** ADM entrando: quantos tickets estão abertos. */
    static void aoEntrar(ServerPlayer p) {
        if (!p.hasPermissions(2)) return;
        int n = dados(p.server).abertos.size();
        if (n > 0) p.sendSystemMessage(Component.literal("✉ " + n + " open ticket(s). ").withStyle(ChatFormatting.GOLD)
                .append(botao("[See]", ChatFormatting.AQUA, "/tickets", "List tickets", true)));
    }
}
