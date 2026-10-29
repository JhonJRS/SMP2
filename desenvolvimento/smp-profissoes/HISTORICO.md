# Histórico de pedidos e decisões — SMP 2 (Alpha 2)

Resumo de tudo o que foi pedido e decidido até 24/10/2026, para continuar o trabalho sem depender do histórico da conversa.

- **Contexto:** modpack NeoForge 1.21.1, com 256 mods, para um servidor SMP de **roleplay medieval-fantástico**. Instância CurseForge `SMP 2 - Alpha 2`.
- **Detalhes técnicos atuais:** [README.md](README.md).
- **Lista gerada de pesquisas, etapas, selos e receitas:** [RELATORIO_GERADO.md](RELATORIO_GERADO.md).

## 1. Pedidos, em ordem

1. **4 profissões e menu de escolha.**
   - Profissões: Engenheiro, Arcanista, Ferreiro e Naturalista.
   - Cada uma só fabrica os mods da sua área; mods fora das profissões são livres.
   - O menu aparece ao entrar no mundo e é obrigatório.
   - Feito com RPEssentials (licenças) mais o datapack `moonlight-global-datapacks/profissoes`.
2. **Progressão pela MineColonies** (pedido longo e formal).
   - Pesquisas liberam etapas, com edifícios como requisito, tudo verificado no servidor.
   - Proteção contra máquinas, autocraft e loot.
   - **Numismatics fora do Engenheiro** (agora é livre para todos).
   - Testes e relatório honesto ao final.
3. **Crash do mixin** (`MarcadorProtecao` estava no pacote de mixins): corrigido.
4. **Bloqueio total por pesquisa.**
   - Nada desbloqueado por pesquisa pode ser obtido antes dela.
   - Pesquisas numa **aba própria** ("Guild of Trades", em inglês); a árvore original da MineColonies fica intocada.
5. **Cada profissão começa com 1 mod** e os addons vêm por pesquisa ("sem ficar chato"). **Toda a aba de pesquisas em inglês.**
6. Mudanças da quinta rodada:
   - **Latão volta às receitas originais.**
   - **No menu, o jogador escolhe o mod inicial** da profissão; os outros vêm por pesquisa na colônia.
   - **Níveis 4 e 5 = pesquisas que juntam profissões.**
   - **Itens game changer exigem várias profissões.** Exemplo pedido: a Eternal Stella na Forja de Hefesto com item mágico do Arcanista e erva do Naturalista.
7. **Crash** ao atualizar mods da MineColonies e colocar o Essential.
   - Causa: a receita da Tyrfing (Embers) recebeu um selo que o Embers não sabe ler.
   - Também corrigidos: nomes dos selos (formato do 1.21.1) e loot do IDAS (variantes de mods não instalados).
8. Mudanças da oitava rodada:
   - **Pesquisas de nível 3 apareciam liberadas sem o nível 2.** Causa: save antigo, com o latão concluído quando ainda era raiz. Agora o servidor exige a cadeia inteira.
   - **Mais receitas fortes que exigem várias profissões.** Eternal Stella mais difícil; Hazen 'n Stuff na Forja de Hefesto; armas lendárias do Iron's no Altar Espiritual do Malum.
9. **(este pedido)**
   - Este resumo.
   - **Invisibilidade vira translucidez:** em RP ninguém fica invisível de verdade. Feito com o mod smp-translucidez/ (cliente, `mods/smptranslucidez-1.0.0.jar`): jogador invisível por qualquer meio (poção, True Invisibility do Iron's, comando) aparece com 45% de opacidade; nome continua oculto. Ajuste em `config/smptranslucidez.properties` (`opacidade`, `mostrar_nome`). Só jogadores; mobs invisíveis seguem o vanilla.

10. **Lista de pendências do usuário** (25/10), salva em [PENDENCIAS.md](PENDENCIAS.md), que é onde se acompanha o andamento.
11. **"Faça tudo o que está pendente; de preferência um agente que teste o Minecraft por mim"** (25/10).
    - Feito quase tudo da lista (ver PENDENCIAS.md); o RegionGuard ficou de fora porque usa o WorldEdit, que o usuário não quer.
    - Decisões do usuário nesta rodada: universidade N só pede prédios até N; nível 4+ com **uma receita, sem selos**; nada de WorldEdit no pack.
    - Testador automático em `smp-testes/` (abre o jogo, roda um roteiro, fecha e gera relatório).
13. **Árvore com mais ramos, nível 5 equilibrado, poções do Naturalista, Railgun 50 de dano** (26/10).
    - Migrações v9 (ramos) e v10 (poções). 88 pesquisas. Visual da árvore: `smp-profissoes/arvore/arvore.html`
      (gerado por `tools/arvore_html.py`; publicado como artifact "Guild of Trades").
    - Nível 5 = só game changers: Railgun, Tyrfing, Legendary Grimoires, Malignant Arsenal, Alfheim & Gaia, Eternal Stella,
      Mythic Arms, Eternal Brews. Regalia do Plaguebringer foi para o nível 4; Terrasteel para o 4 (o portal de Alfheim usa terrasteel).
    - Poções: suporte de poções e Botanical Brewery só abrem para o Naturalista com a pesquisa (`estacoes_exclusivas` →
      containerOpenRestrictions do RPEssentials); mistura de poções do Create desligada (CreatePocoesMixin); caldeirão do
      Iron's é dos dois (decisão do usuário), mas elixires pedem ervas do Naturalista; frascos do Neo Vitae são do Naturalista.
    - Railgun: `damage_modifier = 2.0833333` em `config/immersiveengineering-server.toml` (haste de aço 24 → 50). O servidor dedicado precisa do mesmo ajuste.
12. **Nome oculto em invisíveis e ArcadiaGuard** (25/10): o nome some sobre jogador invisível (inclusive o do RPEssentials); ArcadiaGuard 1.5.8 no lugar do RegionGuard (sem WorldEdit), testado em jogo (`smp-testes/roteiro_arcadia.txt`).
14. **Rodada 4** (26/10): lista em [PENDENCIAS_R4.md](PENDENCIAS_R4.md), toda feita. Migrações `v11_rodada4.py` e `corrigir_adiantados.py`.
    - Regra nova (erro do gerador): **nada é liberado antes da pesquisa que libera os ingredientes dele**. Mod inicial conta como
      livre; receita em ciclo (lingote ↔ bloco/pepita) não é rota. `gerar.py --adiantados` lista os casos;
      `corrigir_adiantados.py [--simular]` move o item para uma etapa mais adiante da mesma linha ou, se não houver, põe o que
      falta como requisito extra da pesquisa dele (tem uma lista `MANUAIS` para os casos em que o automático escolheria mal).
    - Requisitos extras que isso criou: Legendary Weaponsmithing ← Modifier Prisms; Steam Armory ← Steam Power; Spellforged Arms
      ← Spellblade Arsenal; Mythic Arms ← Scroll Studies + Modifier Prisms; Industrial Architecture ← Brass Metallurgy; Crystal
      Arsenal ← Spellblade Arsenal + Arcane Mastery; Immersive Arsenal e Ballistic Engineering ← Logistics & Security; Void Arts ←
      Spirit Crucible + Soulwoven Arts; Crystal Knights ← Scroll Studies + Arcane Vestments; Industrial Fuels ← Steam Power;
      Legendary Artifacts ← Spellblade Arsenal + Arcane Vestments; Petrochemistry ← Immersive Arsenal; Alfheim ← Apothecary
      Brewing (o Alchemy Catalyst pede suporte de poções); Ember Processing ← Ember Logistics; Eldritch Depths ← Spellblade
      Arsenal; Terrasteel Synthesis ← Manasteel Arsenal + Rods & Relics; Arcane Vestments ← Scroll Studies.
    - Mod: **Guild Charter** (`Especiais.java`; uso único, 2ª profissão, escolha no chat → `/guildcharter <etapa>`, marcador nos
      dados persistentes do jogador) e **Draught of Oblivion** (roda `profissoes:admin/resetar`). Agora há um mod inicial **por
      profissão** (`/smpprofissoes inicio` só troca o da mesma profissão). `/progression` mostra as duas profissões.
    - Mod: a troca de Echo Shard do Clérigo (Create: Deep Dark, `BasicItemListing`) é removida no `VillagerTradesEvent` (LOWEST).
    - Testador: comandos novos `/smpteste usaritem`, `licencas`, `trocas <profissão> <item>`.
15. **Rodada 5** (26/10): lista em [PENDENCIAS_R5.md](PENDENCIAS_R5.md). O usuário pediu para **não abrir mais o Minecraft** (só testes offline).
    - Mod: **operador de máquina** (`Operador.java`, anexo no bloco): quem coloca a máquina responde por ela; no tick do bloco
      (`TickBlocoMixin`) e no clique (`CliqueBlocoMixin`) o `RecipeManagerMixin` filtra receitas de máquina (inclusive
      `getAllRecipesFor`, usado pelo Malum/Lodestone) pelo que o operador pode fabricar. Máquina sem operador segue a regra antiga.
    - Mod: `SubirColoniaMixin`/`Colonia.java` (Prefeitura nível N exige licença `colonia_N`), `VinhoResistenciaMixin` (Apple Wine
      até Resistência IV), `CondensadorAE2Mixin` (Condensador não faz Singularity).
    - Gerador: `conversoes_malum` (receita → infusão do Altar Espiritual), `prismas_elementais` (prisma do elemento em
      armadura/cajado da forja), tags do NeoForge no índice (`c:chests` etc.), itens que caem de saque contam como obtíveis e
      `saque_livre` + saque = livre na regra dos adiantados (ex.: Pyrium da Citadel).
    - Serene Seasons: `tools/estacoes.py` gera `moonlight-global-datapacks/smp_estacoes` (cultivos por estação).
16. **Rodada 6** (26/10): lista em [PENDENCIAS_R6.md](PENDENCIAS_R6.md). Migração `v13_rodada6.py`.
    - Gerador: `com_componente` agora põe o componente em `create:sequenced_assembly` (primeiro passo de deploying) e
      `create:mechanical_crafting` (casa vazia da grade ou linha nova) — Protection Pixel vedado com Exo-Plating/Steam Rivets.
    - Mod: `/smpprofissoes colonia <jogador> [nível]` (o `license give` do RPEssentials recusa licenças que não são profissão);
      `ImagemBlockUIMixin` (cliente: skin dinâmica do Quick Skin nas telas da MineColonies); `SimplyMore*Mixin` (Simply More
      fazia cast para ServerLevel no cliente). O verificador de mixins aceita `targets = {...}`.
    - Ao testar permissões: **OP (nível 2+) passa por tudo** — testar com conta sem OP.
    - Correção (26/10, 21h): o ritual do Starweave (`simplybows:cosmic_bow/cosmic_bow`) usava um item que o Simply Bows 0.1.4 não
      registra (só tem modelo/tradução). Ritual é dado de registro: o datapack inteiro foi recusado (mundo novo crashou; o mundo
      "Tests" abriu em modo seguro, sem as receitas). Starweave removido e todo ritual gerado agora leva `neoforge:item_exists`.
17. **Rodada 7** (26/10, `v14_rodada7.py`): Singularity na bancada (Terrasteel, Tyrian Steel, Lethal Dose, Heat Overlocking
    Mechanism, Matter Balls e Nether Star; pesquisa Quantum Networks). A sequência de 12 passos do Create agora faz o item
    provisório **Convergence Matrix** (`itens_especiais`; item especial sem comportamento vira item simples). Receitas do
    Armourer's Workshop, ComputerCraft e CCC Bridge removidas (119). Personagem preso com 0 de vida = nocaute do Hardcore
    Revival que não termina em criativo: `/revive <jogador>`.
18. **Rodada 8** (27/10, `v15_rodada8.py`): lista em [PENDENCIAS_R8.md](PENDENCIAS_R8.md). Profissão Vagabundo + Guild Letter,
    kits iniciais, Animus, Simply More, Relic Replication (nível 5), Schematic Engineering, AE2/Locks/Tarot/Relics/IDAS/Azazel
    mais raros, Enhanced Celestials ligado ao Serene Seasons, Access Denied (`/smpprofissoes dimensao`), ArcadiaGuard
    (`/smpprofissoes zona`), tickets (`/ticket`, `/tickets`), painel de ADM (F7, `cliente/PainelAdm.java`), 32 emotes
    (`tools/emotes.py`), resource pack Embers Retextured portado (`tools/portar_embers_retextured.py`).
    **Aetherworks Refracted portado para 1.21.1** em `smp-aetherworks/` (ver o LEIA-ME de lá) e posto na linha Embers do
    Engenheiro pela migração `v16_aetherworks.py` (n3 Aether Refraction, n4 Aetherium Forging, n5 Aetherium Artifacts).
    Embergetic Expansion não foi portado (licença All Rights Reserved).
19. **Rodada 9** (28/10, `v17_rodada9.py`): Replication Matrix com receita própria (garrafas de experiência de relíquia,
    Heavy Core, Ominous Trial Key, Gaia Ingot, Dark Matter, Totem — nada dos rituais). Componentes do mod agora bloqueados
    na bancada (globalBlockedCrafts) e liberados só para a licença da etapa dona; Carta de Apresentação, Guild Charter e
    Draught of Oblivion seguem livres. Crash ao beber o Draught: bug do RPEssentials (a tela da licença abria na thread do
    servidor ao clicar no item de licença) — `LicencaTelaMixin`. Nomes das pesquisas do Aetherworks: o jar do smpprofissoes
    não tinha sido reinstalado depois da v16 (agora há teste). Texturas do Aetherworks: `crossbow_Base.json` (maiúscula) e
    cores de item sem alfa (1.21 usa ARGB) — corrigidos no port; `tools/checar_modelos.py` confere os modelos.
20. **Rodada 10** (28/10, `v18_rodada10.py`): kits (Novicius Orb no NeoVitae; Iron's sem Arcane Essence/Blank Rune e com a
    Inscription Table — magia inicial a definir; 32 de farinha de osso no Farmer's Delight + Farm & Charm); adagas de
    arremesso do NeoVitae em Blood Foundations; Replication Matrix mais acessível (2 Relic Experience Bottle, Trial Key,
    Mana Diamond, Arcane Crystal, Totem). Sem ponto de renascimento de cama/âncora (`Eventos.semCama`; só Shrine ou o
    spawn do mundo; /spawnpoint de ADM continua). Serene Seasons: `underground_fertility_level` estava no máximo (toda
    plantação era "subterrânea" e crescia o ano todo) → -64; `tools/estacoes.py` com mais cultivos e tags de item.
    Aetherworks: bigorna, Tool Forge e minério usavam `forge_data` nos modelos (1.21 exige `neoforge_data`).
21. **Rodada 11** (28/10, `v19`–`v22`): PlayerRevive (o pack trocou o Hardcore Revival por ele; `bleedInSingleplayer`
    estava false). Crash de 15:27: CoreProtectNeo mandava a limpeza para o executor já desligado ao abrir outro mundo na
    mesma sessão → `CoreProtectExecutorMixin`. Licença colonia_N vale para TODO edifício (antes só a Prefeitura).
    Pré-requisitos: cabana só a partir do nível em que a MineColonies a libera (Mechanic n2, Plantation/Florist n2,
    Alchemist n3...) e pesquisa exigida sempre de nível anterior — o gerar.py agora barra os dois. Algumas pesquisas
    subiram um nível (ficam embaixo da que exigiam) e alguns itens mudaram de etapa (corrigir_adiantados, que agora só
    acrescenta requisitos de nível MENOR). Iron's Spells + Iron's Restrictions: começa em Comum; aprimoramentos Incomum
    (n2) → Raro (n3) → Épico (n4) → Lendário (n5); manuscritos por escola com receitas próprias (arquivos_dados com o id
    irons_restrictions:<escola>); escola única escolhida com /escola (`EscolasMagia`), Cross-School Studies (n3) libera o
    Codex of Schools (+1 escola). Replication Matrix: 2 Hallowed Gold, Nautilus Shell, Mana Diamond, Arcane Crystal, Totem.
22. **Rodada 12** (28/10, `v23_aprimoramentos.py`): primeira entrada só com a licença de Vagabundo, a Carta e uma mochila
    (`profissoes:chegada` limpa o inventário UMA vez, só de quem nunca passou pelo sistema; configs de livro ao entrar
    desligadas: Botania, Silent Gear, War & Taxes). Aprimoramentos de raridade do Iron's Restrictions na bancada (orbe-base
    + 4 tintas + materiais; o caldeirão deixou de ser rota); o orbe-base desceu para Iron's Foundations. Escolha de escola
    inicial com Symmetry, Spirit e Melodic.
23. **Rodada 13** (28/10, `v24`/`v25`): o usuário NÃO quer limite por jogador (a cota da v24 foi tirada do mod). Os
    aprimoramentos de raridade usam ingredientes escassos: orbe-base + 2 tintas + Nautilus Shell (Comum), Heart of the
    Sea (Incomum), Echo Shard (Raro), Heavy Core (Épico), Nether Star + Heavy Core (Lendário).
24. **Rodada 14** (29/10): tela de escolha de profissão (`cliente/TelaProfissoes.java` + fundos animados em
    `cliente/Fundo.java`, tema por profissão e por especialização). Abre pelo pacote `Rede.AbrirEscolha` ao usar a Carta de
    Apresentação ou a Guild Charter; ao confirmar, manda o mesmo comando do chat (`/trigger prof.escolha set 1NN` ou
    `/guildcharter <licença>`), então o servidor decide tudo como antes. O menu do chat fica de reserva (reaparece a cada 30 s).
    Dados em `assets/smpprofissoes/tela_profissoes.json`, gerados pelo `gerar_menu`. Ícones animados para os 32 itens do mod:
    `tools/icones.py` (Pillow; tira de 16 quadros + .mcmeta; prévia em `tools/icones_previa.png`); o brilho de encantamento
    dos itens saiu (a animação faz esse papel). build.ps1 ganhou JOML e netty no classpath.
    Depois: escolhendo Iron's Spells, a tela pede a escola de magia (runa de cada escola, tema por escola em
    `Fundo.ESCOLAS`; lista em `EscolasIniciais`, sem classes do Iron's no cliente). Ela manda `/escola <id>` 1 s depois de
    confirmar a profissão (o servidor precisa ter aplicado o mod inicial antes). O /escola do chat continua valendo.
    Depois: **Vagabundo deixou de ser licença** — é só o estado de quem não tem profissão (tag `prof.vagabundo`). Primeira
    entrada = mochila + Carta de Apresentação. Saiu da lista de profissões do RPEssentials (gerar.py) e dos
    `profissoes:vagabundo`/`finalizar`; quem tinha a licença antiga perde ao entrar (`Eventos.tirarLicencaVagabundo`).
    Depois: magias de outras escolas não podem ser CONJURADAS depois da escolha (`EscolasMagia.aoConjurar`, evento
    SpellPreCastEvent do Iron's: grimório, pergaminho, arma imbuída). Análise das magias por escola em
    `MAGIAS_POR_ESCOLA.md` (`tools/magias_por_escola.py`, lê os jars): 184 magias em 16 escolas. Migração `v26`:
    manuscritos para Cosmic/Radiance/Shadow (hazentouvelib; antes só a receita automática barata) e receita impossível
    para as escolas vazias Void, Dune, Ritual e Technomancy (que também saíram das listas de escolha).

## 2. Regras de design que o usuário fixou

- **Profissão** = Engenheiro, Arcanista, Ferreiro ou Naturalista. A escolha é permanente; só admin troca (`/execute as <jogador> run function profissoes:admin/resetar`), ou o próprio jogador com o Draught of Oblivion. A Guild Charter dá uma segunda profissão (máximo 2).
- **Nada liberado antes da forma de fazê-lo**: um item só fica numa pesquisa se os ingredientes de alguma receita dele já estão liberados na cadeia dela.
- **Mod inicial escolhido no menu** (fica liberado na hora para aquele jogador):

  | Profissão | Opções |
  |---|---|
  | Engenheiro | Create, Immersive Engineering, Embers |
  | Arcanista | Iron's Spells, Malum, Botania, Neo Vitae |
  | Ferreiro | Productive Metalworks, Silent Gear, Forbidden Arcanus |
  | Naturalista | Farmer's Delight + Farm & Charm, Herbalismo (HerbalBrews + Vital Herbs) |

- **O resto é pesquisado na colônia** (aba "Guild of Trades"):
  - nível 1: Foundations de cada mod;
  - níveis 2–3: aprofundam cada linha;
  - **níveis 4–5: cruzados** (exigem pesquisa de outra profissão).
  - pesquisa de nível N não exige edifício acima do nível N (o validador barra).
  - Não usar o nível 6: na MineColonies só 1 pesquisa de nível 6 pode ser concluída por aba.
- **Itens game changer = uma receita só, sem selos** (os selos de papel foram removidos em 25/10).
  - Receita de bancada: pede direto materiais de outras profissões (a bancada confere a licença).
  - Receita de máquina/ritual (não sabe quem é o jogador): pede um **componente**, item registrado pelo smpprofissoes (`@comp:<id>`), feito na bancada só por quem tem a licença da pesquisa.
  - Etapa com `componente_maquina`: o gerador põe o componente em toda receita de máquina daquela etapa (Malum, Embers, Vinery...).
- **Nível 5 só para o que muda o jogo** (super armas, estrela que torna itens eternos, voo, efeitos infinitos). Pesquisas do mesmo nível liberam coisas de poder parecido.
- **Cada linha de mod tem ramos** (não uma linha reta).
- **Tudo de poção é do Naturalista**, mesmo de outros mods; receitas de poção de outros mods usam itens dos mods do Naturalista.
- Etapa pode ter `itens_avulsos` (itens de qualquer mod, ex.: `minecraft:brewing_stand`) e o config tem `estacoes_exclusivas` (bloco que só certas licenças usam).
- **O que é bloqueado é fabricar.** Usar, ter, receber e trocar itens é livre. Materiais naturais (minério, planta, drop de mob) podem ser coletados por qualquer um.
- **Saque:**
  - baús de estrutura perdem itens de etapa;
  - drops de bloco ficam;
  - drops exclusivos de chefes ficam (senão o mod quebraria).
- **OP (nível ≥ 2) ignora todos os bloqueios.** O usuário testa às vezes como OP ou em criativo.

## 3. Onde está cada coisa (o que editar)

| O quê | Arquivo |
|---|---|
| **Fonte única** da progressão (profissões, inícios, pesquisas, etapas, selos, receitas alteradas/novas/removidas, rituais) | `smp-profissoes/progressao.json` |
| Gerador e validador (gera TOML, datapack, regras, lang, menu e relatório) | `smp-profissoes/tools/gerar.py` |
| Datapack gerado (não editar à mão) | `moonlight-global-datapacks/smp_progressao/` |
| Menu de escolha (tick, pendente, menu e finalizar são feitos à mão; selecionar, escolher_inicio, confirmar e inicio/ são gerados) | `moonlight-global-datapacks/profissoes/` |
| Licenças/bloqueio de fabricação (gerado) | `config/rpessentials/rpessentials-professions.toml` |
| Regras lidas pelo mod (gerado) | `config/smpprofissoes/progressao.json` |
| Mod ponte `smpprofissoes` (Java 21, sem Gradle) | `smp-profissoes/mod/` → `mods/smpprofissoes-1.0.0.jar` |
| Mod de translucidez (cliente; `build.ps1 -Instalar`) | `smp-translucidez/` → `mods/smptranslucidez-1.0.0.jar` |
| Materiais do Silent Gear de outros mods (substitui o SilentGear Compat) | `smp-profissoes/tools/silentgear_compat.py` → `moonlight-global-datapacks/smp_silentgear_compat/` |
| Testes (validação, datapack, mixins, permissões) | `smp-profissoes/tests/rodar_testes.ps1` |
| Testador em jogo (abre o Minecraft sozinho e roda roteiros) | `smp-testes/` (`python -I smp-testes\rodar.py`; ver LEIA-ME) |
| Migrações da fonte (histórico das mudanças em lote) | `smp-profissoes/tools/migracoes/` |
| Port do Aetherworks Refracted (1.20.1 → 1.21.1) | `smp-aetherworks/` → `mods/aetherworks-1.21.1-1.1.8-smp.jar` (LEIA-ME lá) |

```powershell
python -I smp-profissoes\tools\gerar.py
powershell -ExecutionPolicy Bypass -File smp-profissoes\mod\build.ps1 -Instalar
powershell -ExecutionPolicy Bypass -File smp-profissoes\tests\rodar_testes.ps1
```

## 4. Lições técnicas (não repetir erros)

- **Classe só do cliente:** fica em `br.smp.profissoes.cliente` com `@Mod(value = MODID, dist = Dist.CLIENT)`; o `build.ps1` precisa do `mergetool-2.0.0-api.jar` (onde está `Dist`).
- **Pacote de mixins:** nenhuma classe fora de `br.smp.profissoes.mixin` pode referenciar classe dentro dele (IllegalClassLoadError).
- **Embers** lê receitas sem registro: **não aceita** ingrediente `neoforge:components` (selo), e o erro derruba o jogo. O gerador recusa (`SEM_COMPONENTES`).
- **Onde o selo funciona:** Create, IE (bancada), Silent Gear, Iron's (caldeirão), Toxony, Botania, rituais do Forbidden Arcanus e Malum (forma plana `{"count":1,"@selo":id}`). Confirmado no log do jogo ou no bytecode do NeoForge.
- **1.21.1:** `custom_name` e `lore` em JSON de receita são **strings** com JSON dentro.
- **Forja de Hefesto:** no máximo 8 itens nos pedestais, `forge_tier` de 1 a 5, essências `aureal`, `blood`, `souls` e `experience`. Rituais novos ficam em `data/<ns>/forbidden_arcanus/hephaestus_forge/ritual/`.
- **MineColonies:** `hasCompletedResearch` devolve **verdadeiro** quando o id não existe; só a tela confere o pai. Por isso o mod trava a cadeia no servidor (`LocalResearchTreeMixin`) e as etapas exigem a cadeia inteira.
- **Loot:** não sobrescrever tabela que cita item de mod não instalado (variantes do IDAS).
- **Mod novo no pack:** rode o gerador. Ele indexa o mod e desativa as receitas que fabricariam itens de etapa por fora (ex.: Create Wizardry). Item de etapa que só deve vir de saque vai em `so_saque` na etapa (ex.: `silver_ring`).
- **Toxony:** o almofariz (`mortar_pestle`) nunca foi testado com ingrediente especial.
- **Receita-laço não é rota** (ex.: "enxaguar" frasco, duplicar template): o validador ignora; o item precisa de uma receita de verdade.
- **Editar arquivos por PowerShell `.Replace` com várias linhas falha em silêncio** (CRLF x LF). Use a ferramenta de edição.
- **Armas únicas que só existem no saque** (Simply Swords): use `saque_livre` na etapa, senão somem dos baús.
- **Testar ao abrir o jogo:**
  - `Research loaded. Located 5 branches, 266 researches`;
  - `Regras carregadas: 4 profissões, 59 etapas`;
  - `Componentes registrados: 13`;
  - 13 linhas `Proteção ativa`;
  - nenhum `Parsing error loading recipe smpprofissoes`;
  - `Invisibilidade de jogadores vira translucidez: opacidade 45%` (só no cliente).

## 5. Pendências e ideias em aberto

- O usuário quer continuar desenvolvendo a árvore de pesquisas (mais receitas cruzadas, mais forjas/altares).
- Ainda não testado em jogo: os rituais do Hazen, as infusões do Malum e a Eternal Stella nova.
- A colônia "Tests" tem pesquisas de versões antigas (o latão concluído como raiz).
