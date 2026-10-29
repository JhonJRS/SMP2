package br.smp.profissoes;

/**
 * Interface adicionada pelos mixins às classes-alvo; serve para confirmar no log que a proteção foi aplicada.
 * Precisa ficar FORA do pacote br.smp.profissoes.mixin: o Mixin proíbe que outras classes referenciem
 * diretamente qualquer classe do pacote de mixins (IllegalClassLoadError ao carregar o jogo).
 */
public interface MarcadorProtecao {}
