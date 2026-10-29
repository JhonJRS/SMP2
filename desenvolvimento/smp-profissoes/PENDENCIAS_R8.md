# Rodada 8 (27/10/2026) — lista do usuário

`[x]` = feito. Testes só offline (o usuário pediu para não abrir o Minecraft).
Suíte (`tests\rodar_testes.ps1`): datapack 5947 OK · mixins 103 OK · Java 923 OK · emotes 32 OK.

## Mundo / estruturas / spawn
- [x] Cult of Azazel: sem estruturas no Overworld (tag `overworld_land` vazia; labirinto/monumento/altar sem bioma).
- [x] IDAS: estruturas mais raras (spacing/separation ×2).
- [x] Locks: sem spawn natural (`Lock Generation Chance = 0`).
- [x] AE2: sem meteoros (tag `has_meteorites` vazia); Matter Cannon mais difícil; prensas com receita.
- [x] Tarot Cards: sem spawn natural (`do_loot_generation = false`).
- [x] Relics: relíquias mais raras no saque (`random_chance 0.35`).
- [x] Enhanced Celestials: eventos 3× mais raros, 8 dias entre eventos e ligados às estações (regra `season` no mod).

## Bugs e configuração
- [x] Shaders: escuridão total mantida; com shaderpack ativo o passe final do Hardcore True Darkness é pulado (mixin).
- [x] Hardcore Revival: `/revive` com 0 de vida deixava o jogador "imortal" — mixin + `rescueRespawnHealth = 6`.
- [x] Bloqueio de portais (Access Denied): Nether, End, Otherside, Crystal Chronicles e Neo Vitae fechados;
      `/smpprofissoes dimensao abrir|fechar <dimensão>`.
- [x] Proteção de blocos (ArcadiaGuard): `/smpprofissoes zona <nome>` cria a zona da seleção da varinha com
      quebrar/colocar negado e interagir/baús/placas de pressão liberados. **Falta testar no jogo.**
- [x] Paraglider: sem aumento de vida (`maxHeartContainers = 0`).
- [x] Mods do Barunn: conferidos (3 com atualização; download aguarda autorização).

## Receitas
- [x] Netherite Exo-Plating nas armaduras de Netherite do Protection Pixel.
- [x] Easy NPC: o jar não tem receitas.
- [x] End Remastered: receitas removidas.
- [x] Schematicannon: pesquisa "Schematic Engineering" (nível 3).
- [x] Create Cobblestone: 22 geradores de blocos naturais decorativos.

## Profissões e árvore
- [x] Profissão **Vagabundo** + Guild Letter para escolher a profissão.
- [x] Kit inicial por profissão e especialização.
- [x] Animus (Neo Vitae) + 3 pesquisas.
- [x] Simply More na árvore junto com o Simply Swords.
- [x] Relic Replication (nível 5): duplicar relíquias.

## Ports
- [x] Aetherworks Refracted → 1.21.1: `smp-aetherworks/` (fonte GPLv3 baixada com autorização), `mods/aetherworks-1.21.1-1.1.8-smp.jar`.
      Pesquisas (v16): Aether Refraction (n3), Aetherium Forging (n4), Aetherium Artifacts (n5). **Falta testar no jogo.**
- [ ] Embergetic Expansion: licença "All Rights Reserved" — só com autorização do autor (Morsuk).
- [x] Resource pack Embers Retextured → `resourcepacks/Embers Retextured 1.21.1 (SMP 2).zip`
      (`tools/portar_embers_retextured.py`; partes de Aetherworks/Embergetic/Embers Delight ficam de fora até esses mods existirem).

## Administração
- [x] Tickets: `/ticket <mensagem>`, `/ticket meus`; ADM: `/tickets` (botões TP/Assumir/Fechar), aviso ao entrar.
- [x] Painel de ADM: tecla **F7** (só OP), botões para os comandos mais usados.
- [x] 32 emotes próprios em `emotes/smp2_*.json` (`tools/emotes.py`); validados pelo leitor do PlayerAnimator.
