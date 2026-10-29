package br.smp.profissoes.cliente;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Painel de ADM: botões para os comandos mais usados. Os que precisam de alvo (jogador, dimensão, nome) abrem o chat
 * já preenchido; os demais rodam direto. Nada aqui dá poder extra: o servidor confere a permissão de cada comando.
 */
public final class PainelAdm extends Screen {
    private record Acao(String rotulo, String comando, boolean executar) {}

    private record Secao(String titulo, List<Acao> acoes) {}

    private static Acao run(String r, String c) { return new Acao(r, c, true); }

    private static Acao chat(String r, String c) { return new Acao(r, c, false); }

    static final List<Secao> SECOES = List.of(
            new Secao("Yourself", List.of(run("Creative", "gamemode creative"), run("Survival", "gamemode survival"),
                    run("Spectator", "gamemode spectator"), run("Heal", "effect give @s minecraft:instant_health 1 10 true"),
                    run("Night vision", "effect give @s minecraft:night_vision infinite 0 true"),
                    run("Invisible", "effect give @s minecraft:invisibility infinite 0 true"), run("Clear effects", "effect clear @s"),
                    run("Zone wand", "ag wand give"))),
            new Secao("World", List.of(run("Day", "time set day"), run("Night", "time set night"), run("Clear sky", "weather clear"),
                    run("Rain", "weather rain"), run("Save world", "save-all"), chat("Reload packs", "reload"))),
            new Secao("Players", List.of(run("Tickets", "tickets"), chat("Teleport to", "tp "), chat("Revive", "revive "),
                    chat("Kick", "kick "), chat("Inventory", "checkinventory "), chat("Progress", "smpprofissoes status "),
                    chat("Reset prof.", "smpprofissoes resetar "), chat("Colony level", "smpprofissoes colonia "))),
            new Secao("Protection & logs", List.of(run("Zones (GUI)", "ag gui"), chat("Protect area", "smpprofissoes zona "),
                    run("Zone list", "ag zone list"), run("Logs nearby", "co near 10"), chat("Rollback", "co rollback "),
                    run("Logs help", "co help"))),
            new Secao("Server", List.of(chat("Open dimension", "smpprofissoes dimensao abrir "),
                    chat("Close dimension", "smpprofissoes dimensao fechar "), run("Sync progress", "smpprofissoes sincronizar"),
                    run("Reload rules", "smpprofissoes recarregar"))));

    private static final int L = 76, A = 20, ESP = 3, TITULO = 11;
    private final List<int[]> titulos = new ArrayList<>(); // {índice da seção, y}

    PainelAdm() {
        super(Component.literal("Admin panel"));
    }

    @Override
    protected void init() {
        titulos.clear();
        int colunas = Math.max(3, Math.min(6, (width - 20) / (L + ESP)));
        int x0 = (width - (colunas * L + (colunas - 1) * ESP)) / 2;
        int y = 24;
        for (int s = 0; s < SECOES.size(); s++) {
            titulos.add(new int[] {s, y});
            y += TITULO;
            List<Acao> acoes = SECOES.get(s).acoes();
            for (int i = 0; i < acoes.size(); i++) {
                Acao a = acoes.get(i);
                int col = i % colunas;
                if (col == 0 && i > 0) y += A + ESP;
                addRenderableWidget(Button.builder(Component.literal(a.rotulo()), b -> agir(a))
                        .bounds(x0 + col * (L + ESP), y, L, A)
                        .tooltip(Tooltip.create(Component.literal("/" + a.comando() + (a.executar() ? "" : "…"))))
                        .build());
            }
            y += A + ESP + 2;
        }
    }

    private void agir(Acao a) {
        if (minecraft == null || minecraft.player == null) return;
        if (a.executar()) {
            minecraft.player.connection.sendCommand(a.comando());
            onClose();
        } else {
            minecraft.setScreen(new ChatScreen("/" + a.comando()));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFD27F);
        for (int[] t : titulos) g.drawCenteredString(font, SECOES.get(t[0]).titulo(), width / 2, t[1] + 1, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
