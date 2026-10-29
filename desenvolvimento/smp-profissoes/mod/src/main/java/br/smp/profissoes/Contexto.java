package br.smp.profissoes;

import java.util.ArrayDeque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Qual jogador está fabricando "agora" nesta thread. Os mixins de estações com jogador (bancada, cliques em
 * menus, terminais AE2, blueprint do Create, halo do Botania, ensino da MineColonies) empilham o jogador antes
 * da busca de receita e desempilham depois. Sem jogador na pilha = fabricação automática.
 * Máquinas empilham o seu {@link Operador} (marcado à parte): ele vale só para receitas de máquina, nunca como
 * jogador de bancada (autocrafter continua sem jogador).
 */
public final class Contexto {
    private Contexto() {}

    private static final Object SEM_JOGADOR = new Object();
    private record DeMaquina(ServerPlayer jogador) {}
    private static final ThreadLocal<ArrayDeque<Object>> PILHA = ThreadLocal.withInitial(ArrayDeque::new);

    public static void entrar(Player p) {
        PILHA.get().push(p instanceof ServerPlayer sp ? sp : SEM_JOGADOR);
    }

    /** Operador de máquina (tick do bloco ou clique nele). */
    public static void entrarOperador(ServerPlayer p) {
        PILHA.get().push(new DeMaquina(p));
    }

    public static void sair() {
        ArrayDeque<Object> d = PILHA.get();
        if (!d.isEmpty()) d.pop();
    }

    /** Jogador do contexto atual, ou null se não houver (ex.: autocrafter ou máquina). */
    public static ServerPlayer jogador() {
        Object o = PILHA.get().peek();
        return o instanceof ServerPlayer sp ? sp : null;
    }

    /** Operador da máquina que está buscando receita agora, ou null. */
    public static ServerPlayer operador() {
        Object o = PILHA.get().peek();
        return o instanceof DeMaquina m ? m.jogador() : null;
    }

    /** Chamado a cada tick do servidor: se alguma exceção deixou a pilha suja, ela não vaza para o tick seguinte. */
    public static void limpar() {
        PILHA.get().clear();
    }
}
