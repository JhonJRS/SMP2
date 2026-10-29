package br.smp.profissoes.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Fundos animados e temáticos da tela de profissões. Tudo é função do tempo (sem estado): partículas com posição
 * pseudoaleatória fixa por índice, que só se movem com t. {@code alfa} multiplica todas as cores (para o cruzamento
 * entre um tema e outro).
 */
final class Fundo {
    private Fundo() {}

    enum Motivo { ENGRENAGENS, ELETRICO, BRASAS, RUNAS, ALMAS, PETALAS, SANGUE, FAISCAS, ARCANO, TRIGO, FOLHAS, ESTRELAS, NEVE, NOTAS }

    record Tema(int topo, int base, int destaque, Motivo motivo, String icone) {}

    static final Map<String, Tema> PROFISSOES = Map.of(
            "engenheiro", new Tema(0xFF2A1E10, 0xFF5A3A16, 0xFFE8A33A, Motivo.ENGRENAGENS, "⚙"),
            "arcanista", new Tema(0xFF140C2A, 0xFF3A1A5E, 0xFFB57BFF, Motivo.ESTRELAS, "✦"),
            "ferreiro", new Tema(0xFF1A0E0C, 0xFF5E1A10, 0xFFFF6A2E, Motivo.BRASAS, "⚒"),
            "naturalista", new Tema(0xFF0E1E10, 0xFF24502A, 0xFF7BE36A, Motivo.FOLHAS, "❀"));

    /** Tema de cada especialização (licença do mod inicial); as que faltarem usam o da profissão. */
    static final Map<String, Tema> ESPECIALIZACOES = Map.ofEntries(
            Map.entry("eng_create", new Tema(0xFF2A1E10, 0xFF6A4A1E, 0xFFF0C060, Motivo.ENGRENAGENS, "⚙")),
            Map.entry("eng_ie", new Tema(0xFF101820, 0xFF2E3E52, 0xFFFFB040, Motivo.ELETRICO, "⚡")),
            Map.entry("eng_embers", new Tema(0xFF1E0C08, 0xFF5A2410, 0xFFFF8A2E, Motivo.BRASAS, "☀")),
            Map.entry("arc_irons", new Tema(0xFF0A1028, 0xFF1E3A78, 0xFF7FD0FF, Motivo.RUNAS, "✧")),
            Map.entry("arc_malum", new Tema(0xFF180A22, 0xFF4A1650, 0xFFFF7BE0, Motivo.ALMAS, "☽")),
            Map.entry("arc_botania", new Tema(0xFF0C1E1A, 0xFF1E5A46, 0xFF9FF0FF, Motivo.PETALAS, "✿")),
            Map.entry("arc_neovitae", new Tema(0xFF1A0608, 0xFF5A0E16, 0xFFFF3A4A, Motivo.SANGUE, "♥")),
            Map.entry("fer_silentgear", new Tema(0xFF14161A, 0xFF3A4048, 0xFFFFD27A, Motivo.FAISCAS, "⚒")),
            Map.entry("fer_arcanus", new Tema(0xFF120A1A, 0xFF3A2242, 0xFFF0C24A, Motivo.ARCANO, "✠")),
            Map.entry("nat_cozinha", new Tema(0xFF20160A, 0xFF5A4218, 0xFFFFD86B, Motivo.TRIGO, "❦")),
            Map.entry("nat_ervas", new Tema(0xFF0A1A12, 0xFF1E4A30, 0xFF9AF07A, Motivo.FOLHAS, "☘")));

    /** Tema de cada escola de magia do Iron's (id da escola); as que faltarem usam o do Iron's Spells. */
    static final Map<String, Tema> ESCOLAS = Map.ofEntries(
            Map.entry("irons_spellbooks:fire", new Tema(0xFF200804, 0xFF6A1A08, 0xFFFF6A2E, Motivo.BRASAS, "✹")),
            Map.entry("irons_spellbooks:ice", new Tema(0xFF081422, 0xFF1E4A6A, 0xFFBFEFFF, Motivo.NEVE, "❄")),
            Map.entry("irons_spellbooks:lightning", new Tema(0xFF0C0E22, 0xFF2A2E5A, 0xFFFFE45C, Motivo.ELETRICO, "⚡")),
            Map.entry("irons_spellbooks:holy", new Tema(0xFF221C0C, 0xFF6A5A2A, 0xFFFFF0B0, Motivo.ESTRELAS, "✚")),
            Map.entry("irons_spellbooks:nature", new Tema(0xFF0A1A0C, 0xFF24502A, 0xFF7BE36A, Motivo.FOLHAS, "❦")),
            Map.entry("irons_spellbooks:ender", new Tema(0xFF0E0618, 0xFF2E1450, 0xFFC060FF, Motivo.ALMAS, "◈")),
            Map.entry("irons_spellbooks:evocation", new Tema(0xFF101418, 0xFF34404A, 0xFFE0F0E8, Motivo.RUNAS, "✧")),
            Map.entry("irons_spellbooks:blood", new Tema(0xFF1A0406, 0xFF5A0A12, 0xFFFF3A4A, Motivo.SANGUE, "♥")),
            Map.entry("iss_magicfromtheeast:symmetry", new Tema(0xFF1A1408, 0xFF4A3A1A, 0xFFF0D890, Motivo.ARCANO, "☯")),
            Map.entry("iss_magicfromtheeast:spirit", new Tema(0xFF061A18, 0xFF14483E, 0xFF7FFFE0, Motivo.ALMAS, "☁")),
            Map.entry("tunes_n_tomes:melody", new Tema(0xFF1A0A18, 0xFF4A1E46, 0xFFFF9AD8, Motivo.NOTAS, "♫")));

    static final Tema NEUTRO =new Tema(0xFF0E0C14, 0xFF241E2C, 0xFFE8C878, Motivo.ESTRELAS, "✦");

    static Tema daEspecializacao(String licenca, String profissao) {
        return ESPECIALIZACOES.getOrDefault(licenca, PROFISSOES.getOrDefault(profissao, NEUTRO));
    }

    // ------------------------------------------------------------------ utilidades de cor
    static int a(int cor, float f) {
        int al = Math.round(((cor >>> 24) & 0xFF) * Math.max(0, Math.min(1, f)));
        return (al << 24) | (cor & 0xFFFFFF);
    }

    static int mistura(int c1, int c2, float f) {
        f = Math.max(0, Math.min(1, f));
        int r = 0;
        for (int s = 0; s <= 24; s += 8) {
            int x = (c1 >>> s) & 0xFF, y = (c2 >>> s) & 0xFF;
            r |= (Math.round(x + (y - x) * f) & 0xFF) << s;
        }
        return r;
    }

    static int claro(int cor, float f) {
        return mistura(cor, cor | 0xFFFFFF, f);
    }

    /** Número pseudoaleatório fixo em [0,1) para (i, canal). */
    static float h(int i, int canal) {
        double v = Math.sin(i * 12.9898 + canal * 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    /** Texto visível só com alfa >= 4 (abaixo disso a fonte desenha opaco). */
    static void texto(GuiGraphics g, Font f, Component c, int x, int y, int cor, float alfa, boolean sombra) {
        if (alfa < 0.03f) return;
        g.drawString(f, c, x, y, a(cor, alfa), sombra);
    }

    // ------------------------------------------------------------------ fundo
    static void desenhar(GuiGraphics g, Font fonte, Tema tm, int x, int y, int w, int hh, float t, float alfa) {
        if (alfa <= 0.01f || w <= 0 || hh <= 0) return;
        g.enableScissor(x, y, x + w, y + hh);
        g.fillGradient(x, y, x + w, y + hh, a(tm.topo, alfa), a(tm.base, alfa));
        // luz que respira, vinda de baixo
        float resp = 0.5f + 0.5f * (float) Math.sin(t * 1.3);
        for (int k = 0; k < 5; k++) {
            int m = (int) (hh * (0.18f + k * 0.08f));
            g.fillGradient(x, y + hh - m, x + w, y + hh, 0, a(tm.destaque, alfa * (0.05f + 0.04f * resp)));
        }
        int n = Math.max(8, Math.min(70, w * hh / 1500));
        switch (tm.motivo) {
            case ENGRENAGENS -> {
                engrenagem(g, x + w * 0.12f, y + hh * 0.86f, Math.max(14, hh * 0.22f), t * 25, a(mistura(tm.base, tm.destaque, 0.28f), alfa));
                engrenagem(g, x + w * 0.88f, y + hh * 0.16f, Math.max(12, hh * 0.17f), -t * 33, a(mistura(tm.base, tm.destaque, 0.22f), alfa));
                engrenagem(g, x + w * 0.80f, y + hh * 0.92f, Math.max(9, hh * 0.12f), t * 48, a(mistura(tm.base, tm.destaque, 0.18f), alfa));
                vapor(g, x, y, w, hh, t, n / 3, alfa);
                subindo(g, x, y, w, hh, t, n / 2, tm.destaque, alfa * 0.6f, 0.12f, 1);
            }
            case ELETRICO -> {
                raio(g, x, y, w, hh, t, tm.destaque, alfa);
                subindo(g, x, y, w, hh, t, n / 2, tm.destaque, alfa, 0.35f, 1);
                estrelas(g, x, y, w, hh, t, n / 2, 0xFF9FC8FF, alfa * 0.5f);
            }
            case BRASAS -> {
                subindo(g, x, y, w, hh, t, n, tm.destaque, alfa, 0.2f, 2);
                subindo(g, x, y, w, hh, t + 7, n / 2, 0xFFFFE08A, alfa, 0.3f, 1);
            }
            case FAISCAS -> {
                faiscas(g, x, y, w, hh, t, tm.destaque, alfa);
                subindo(g, x, y, w, hh, t, n / 2, 0xFFFFA040, alfa * 0.7f, 0.15f, 1);
            }
            case RUNAS -> {
                estrelas(g, x, y, w, hh, t, n, 0xFFFFFFFF, alfa * 0.6f);
                circulo(g, fonte, x + w / 2f, y + hh / 2f, Math.min(w, hh) * 0.36f, t, tm.destaque, alfa);
            }
            case ARCANO -> {
                estrelas(g, x, y, w, hh, t, n, tm.destaque, alfa * 0.5f);
                circulo(g, fonte, x + w / 2f, y + hh * 0.55f, Math.min(w, hh) * 0.30f, -t * 0.7f, 0xFFB57BFF, alfa * 0.8f);
                subindo(g, x, y, w, hh, t, n / 2, tm.destaque, alfa, 0.12f, 1);
            }
            case ALMAS -> {
                estrelas(g, x, y, w, hh, t, n / 2, 0xFFFFFFFF, alfa * 0.4f);
                almas(g, x, y, w, hh, t, Math.max(4, n / 6), tm.destaque, alfa);
            }
            case PETALAS -> {
                caindo(g, x, y, w, hh, t, n / 2, 0xFFFFB0E0, alfa, true);
                subindo(g, x, y, w, hh, t, n / 2, tm.destaque, alfa, 0.1f, 1);
            }
            case SANGUE -> {
                gotas(g, x, y, w, hh, t, n / 2, tm.destaque, alfa);
                estrelas(g, x, y, w, hh, t, n / 3, 0xFFFF8090, alfa * 0.4f);
            }
            case TRIGO -> {
                caindo(g, x, y, w, hh, t, n / 2, tm.destaque, alfa, false);
                vapor(g, x, y, w, hh, t, n / 4, alfa);
            }
            case FOLHAS -> {
                caindo(g, x, y, w, hh, t, n / 2, tm.destaque, alfa, true);
                vagalumes(g, x, y, w, hh, t, n / 3, 0xFFF0FF80, alfa);
            }
            case ESTRELAS -> {
                estrelas(g, x, y, w, hh, t, n * 2, 0xFFFFFFFF, alfa);
                subindo(g, x, y, w, hh, t, n / 3, tm.destaque, alfa * 0.8f, 0.08f, 1);
            }
            case NEVE -> {
                caindo(g, x, y, w, hh, t, n, 0xFFFFFFFF, alfa * 0.9f, true);
                estrelas(g, x, y, w, hh, t, n / 2, tm.destaque, alfa * 0.6f);
            }
            case NOTAS -> {
                notas(g, fonte, x, y, w, hh, t, Math.max(4, n / 4), tm.destaque, alfa);
                estrelas(g, x, y, w, hh, t, n / 2, 0xFFFFFFFF, alfa * 0.5f);
            }
        }
        // vinheta
        int v = Math.max(6, Math.min(w, hh) / 6);
        g.fillGradient(x, y, x + w, y + v, a(0xC0000000, alfa), 0);
        g.fillGradient(x, y + hh - v, x + w, y + hh, 0, a(0xA0000000, alfa));
        g.disableScissor();
    }

    // ------------------------------------------------------------------ motivos
    static void engrenagem(GuiGraphics g, float cx, float cy, float r, float ang, int cor) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(ang));
        int dentes = 10, d = Math.max(2, Math.round(r * 0.17f));
        for (int i = 0; i < dentes; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(i * 360f / dentes));
            g.fill(-d, -Math.round(r * 1.22f), d, -Math.round(r * 0.75f), cor);
            ps.popPose();
        }
        int s = Math.round(r * 0.84f);
        for (int k = 0; k < 3; k++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(k * 30f));
            g.fill(-s, -s, s, s, cor);
            ps.popPose();
        }
        int furo = Math.max(2, Math.round(r * 0.28f));
        g.fill(-furo, -furo, furo, furo, mistura(cor, 0xFF000000, 0.45f));
        ps.popPose();
    }

    /** Partículas que sobem (brasas, faíscas, motes). */
    static void subindo(GuiGraphics g, int x, int y, int w, int hh, float t, int n, int cor, float alfa, float vel, int tam) {
        for (int i = 0; i < n; i++) {
            float f = (t * vel * (0.6f + h(i, 3)) + h(i, 1)) % 1f;
            float px = x + h(i, 0) * w + (float) Math.sin(t * 1.7 + i) * 5;
            float py = y + hh + 4 - f * (hh + 8);
            float vida = (float) Math.sin(Math.PI * f);
            int s = tam + (h(i, 4) > 0.8f ? 1 : 0);
            g.fill((int) px, (int) py, (int) px + s, (int) py + s, a(cor, alfa * vida));
        }
    }

    static void caindo(GuiGraphics g, int x, int y, int w, int hh, float t, int n, int cor, float alfa, boolean folha) {
        PoseStack ps = g.pose();
        for (int i = 0; i < n; i++) {
            float f = (t * 0.07f * (0.6f + h(i, 3)) + h(i, 1)) % 1f;
            float px = x + h(i, 0) * w + (float) Math.sin(t * 1.2 + i * 1.7) * 12;
            float py = y - 6 + f * (hh + 12);
            int c = a(mistura(cor, 0xFF000000, h(i, 5) * 0.35f), alfa * (float) Math.min(1, Math.sin(Math.PI * f) * 2));
            ps.pushPose();
            ps.translate(px, py, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(t * 70 * (h(i, 2) - 0.5f) * 2 + i * 40));
            if (folha) {
                g.fill(-2, -1, 2, 1, c);
                g.fill(-1, -2, 1, 2, c);
            } else {
                g.fill(-1, -2, 1, 2, c);
            }
            ps.popPose();
        }
    }

    /** Notas musicais que sobem balançando. */
    static void notas(GuiGraphics g, Font fonte, int x, int y, int w, int hh, float t, int n, int cor, float alfa) {
        String[] simbolos = {"♪", "♫", "♩", "♬"};
        for (int i = 0; i < n; i++) {
            float f = (t * 0.12f * (0.6f + h(i, 3)) + h(i, 1)) % 1f;
            float px = x + h(i, 0) * w + (float) Math.sin(t * 2 + i) * 8;
            float py = y + hh + 6 - f * (hh + 14);
            texto(g, fonte, Component.literal(simbolos[i % simbolos.length]), (int) px, (int) py,
                    mistura(cor, 0xFFFFFFFF, h(i, 5) * 0.4f), alfa * (float) Math.sin(Math.PI * f), false);
        }
    }

    static void estrelas(GuiGraphics g,int x, int y, int w, int hh, float t, int n, int cor, float alfa) {
        for (int i = 0; i < n; i++) {
            int px = x + (int) (h(i, 0) * w), py = y + (int) (h(i, 1) * hh);
            float k = 0.5f + 0.5f * (float) Math.sin(t * (1 + h(i, 2) * 2.5f) + i);
            g.fill(px, py, px + 1, py + 1, a(cor, alfa * (0.15f + 0.85f * k * k)));
            if (h(i, 3) > 0.9f && k > 0.8f) {
                g.fill(px - 1, py, px + 2, py + 1, a(cor, alfa * 0.4f * k));
                g.fill(px, py - 1, px + 1, py + 2, a(cor, alfa * 0.4f * k));
            }
        }
    }

    static void vagalumes(GuiGraphics g, int x, int y, int w, int hh, float t, int n, int cor, float alfa) {
        for (int i = 0; i < n; i++) {
            float px = x + (h(i, 0) * w + (float) Math.sin(t * 0.5 + i) * 18 + w) % w;
            float py = y + hh * 0.35f + h(i, 1) * hh * 0.6f + (float) Math.cos(t * 0.7 + i * 2) * 8;
            float k = (float) Math.max(0, Math.sin(t * 1.5 + i * 2.3));
            g.fill((int) px - 1, (int) py - 1, (int) px + 2, (int) py + 2, a(cor, alfa * 0.18f * k));
            g.fill((int) px, (int) py, (int) px + 1, (int) py + 1, a(cor, alfa * k));
        }
    }

    static void vapor(GuiGraphics g, int x, int y, int w, int hh, float t, int n, float alfa) {
        for (int i = 0; i < n; i++) {
            float f = (t * 0.09f + h(i, 1)) % 1f;
            float px = x + h(i, 0) * w + (float) Math.sin(t + i) * 6;
            float py = y + hh - f * hh * 0.8f;
            int s = 2 + (int) (f * 7);
            g.fill((int) px - s, (int) py - s, (int) px + s, (int) py + s, a(0xFFE0E0E0, alfa * 0.07f * (1 - f)));
        }
    }

    static void almas(GuiGraphics g, int x, int y, int w, int hh, float t, int n, int cor, float alfa) {
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < 6; k++) {
                float tt = t - k * 0.08f;
                float px = x + w * (0.5f + 0.42f * (float) Math.sin(tt * (0.4 + h(i, 0) * 0.3) + i * 2.1));
                float py = y + hh * (0.5f + 0.38f * (float) Math.sin(tt * (0.55 + h(i, 1) * 0.3) * 1.3 + i));
                int s = k == 0 ? 2 : 1;
                g.fill((int) px, (int) py, (int) px + s, (int) py + s, a(k == 0 ? claro(cor, 0.5f) : cor, alfa * (1 - k / 6f)));
            }
        }
    }

    static void gotas(GuiGraphics g, int x, int y, int w, int hh, float t, int n, int cor, float alfa) {
        for (int i = 0; i < n; i++) {
            float f = (t * 0.25f * (0.5f + h(i, 3)) + h(i, 1)) % 1f;
            int px = x + (int) (h(i, 0) * w);
            int py = y + (int) (f * f * (hh + 8)) - 4;
            g.fill(px, py, px + 1, py + 3, a(cor, alfa * 0.9f));
            g.fill(px, py + 3, px + 1, py + 4, a(claro(cor, 0.4f), alfa));
        }
    }

    static void raio(GuiGraphics g, int x, int y, int w, int hh, float t, int cor, float alfa) {
        int ciclo = (int) (t / 1.4f);
        float fase = t - ciclo * 1.4f;
        if (fase > 0.22f) return;
        float k = 1 - fase / 0.22f;
        float px = x + w * (0.2f + 0.6f * h(ciclo, 7));
        float py = y;
        int seg = 0;
        while (py < y + hh && seg++ < 40) {
            float nx = px + (h(ciclo * 31 + seg, 8) - 0.5f) * 12;
            float ny = py + 4 + h(ciclo * 17 + seg, 9) * 6;
            linha(g, px, py, nx, ny, a(0xFFE8F4FF, alfa * k));
            px = nx;
            py = ny;
        }
        g.fill(x, y, x + w, y + hh, a(0xFFCFE4FF, alfa * 0.06f * k));
    }

    static void faiscas(GuiGraphics g, int x, int y, int w, int hh, float t, int cor, float alfa) {
        int ciclo = (int) (t / 0.9f);
        float f = (t - ciclo * 0.9f) / 0.9f;
        float cx = x + w * (0.3f + 0.4f * h(ciclo, 5)), cy = y + hh * 0.78f;
        for (int i = 0; i < 18; i++) {
            float ang = (float) (Math.PI * (1.05 + 0.9 * h(ciclo * 19 + i, 6)));
            float v = 30 + 50 * h(ciclo * 7 + i, 4);
            float px = cx + (float) Math.cos(ang) * v * f;
            float py = cy + (float) Math.sin(ang) * v * f + 40 * f * f;
            g.fill((int) px, (int) py, (int) px + 1, (int) py + 1, a(mistura(0xFFFFFFFF, cor, f), alfa * (1 - f)));
        }
    }

    static void circulo(GuiGraphics g, Font fonte, float cx, float cy, float r, float t, int cor, float alfa) {
        int pontos = Math.max(24, (int) (r * 2.2f));
        for (int i = 0; i < pontos; i++) {
            double ang = 2 * Math.PI * i / pontos + t * 0.25;
            float k = 0.5f + 0.5f * (float) Math.sin(t * 2 + i * 0.4);
            g.fill((int) (cx + Math.cos(ang) * r), (int) (cy + Math.sin(ang) * r),
                    (int) (cx + Math.cos(ang) * r) + 1, (int) (cy + Math.sin(ang) * r) + 1, a(cor, alfa * (0.25f + 0.5f * k)));
            double ang2 = -2 * Math.PI * i / pontos - t * 0.18;
            g.fill((int) (cx + Math.cos(ang2) * r * 0.62f), (int) (cy + Math.sin(ang2) * r * 0.62f),
                    (int) (cx + Math.cos(ang2) * r * 0.62f) + 1, (int) (cy + Math.sin(ang2) * r * 0.62f) + 1, a(cor, alfa * 0.3f));
        }
        // runas do alfabeto galáctico (fonte "alt" do encantamento) girando entre os dois anéis
        Style galactico = Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("alt"));
        String letras = "abcdefghijklmnop";
        int qtd = 12;
        for (int i = 0; i < qtd; i++) {
            double ang = 2 * Math.PI * i / qtd + t * 0.25;
            float px = cx + (float) Math.cos(ang) * r * 0.81f, py = cy + (float) Math.sin(ang) * r * 0.81f;
            float k = 0.5f + 0.5f * (float) Math.sin(t * 2.2 - i * 0.7);
            Component c = Component.literal(String.valueOf(letras.charAt(i % letras.length()))).withStyle(galactico);
            texto(g, fonte, c, (int) px - 3, (int) py - 4, claro(cor, 0.3f * k), alfa * (0.35f + 0.6f * k), false);
        }
        // linhas de um hexagrama
        for (int i = 0; i < 6; i++) {
            double a1 = 2 * Math.PI * i / 6 - t * 0.12, a2 = 2 * Math.PI * (i + 2) / 6 - t * 0.12;
            linha(g, cx + (float) Math.cos(a1) * r * 0.6f, cy + (float) Math.sin(a1) * r * 0.6f,
                    cx + (float) Math.cos(a2) * r * 0.6f, cy + (float) Math.sin(a2) * r * 0.6f, a(cor, alfa * 0.22f));
        }
    }

    static void linha(GuiGraphics g, float x0, float y0, float x1, float y1, int cor) {
        int passos = (int) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= passos; i++) {
            float f = passos == 0 ? 0 : i / (float) passos;
            int px = Math.round(x0 + (x1 - x0) * f), py = Math.round(y0 + (y1 - y0) * f);
            g.fill(px, py, px + 1, py + 1, cor);
        }
    }

    /** Moldura com brilho que corre pela borda. */
    static void moldura(GuiGraphics g, int x, int y, int w, int hh, int cor, float t, float alfa, float realce) {
        int base = a(mistura(cor, 0xFF000000, 0.35f - 0.3f * realce), alfa);
        g.fill(x, y, x + w, y + 1, base);
        g.fill(x, y + hh - 1, x + w, y + hh, base);
        g.fill(x, y, x + 1, y + hh, base);
        g.fill(x + w - 1, y, x + w, y + hh, base);
        int per = 2 * (w + hh);
        float pos = (t * 60) % per;
        for (int k = 0; k < 24; k++) {
            float p = (pos - k + per) % per;
            int px, py;
            if (p < w) { px = x + (int) p; py = y; }
            else if (p < w + hh) { px = x + w - 1; py = y + (int) (p - w); }
            else if (p < 2 * w + hh) { px = x + w - 1 - (int) (p - w - hh); py = y + hh - 1; }
            else { px = x; py = y + hh - 1 - (int) (p - 2 * w - hh); }
            g.fill(px, py, px + 1, py + 1, a(claro(cor, 0.5f), alfa * (0.3f + 0.7f * realce) * (1 - k / 24f)));
        }
        // cantos
        int c = a(claro(cor, 0.3f + 0.4f * realce), alfa);
        g.fill(x - 1, y - 1, x + 3, y, c);
        g.fill(x - 1, y - 1, x, y + 3, c);
        g.fill(x + w - 3, y - 1, x + w + 1, y, c);
        g.fill(x + w, y - 1, x + w + 1, y + 3, c);
        g.fill(x - 1, y + hh, x + 3, y + hh + 1, c);
        g.fill(x - 1, y + hh - 3, x, y + hh + 1, c);
        g.fill(x + w - 3, y + hh, x + w + 1, y + hh + 1, c);
        g.fill(x + w, y + hh - 3, x + w + 1, y + hh + 1, c);
    }
}
