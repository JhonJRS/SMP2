# SMP — Lista de configurações pendentes

Lista do usuário (25/10/2026), em ordem de prioridade. `[x]` = feito (com o que foi feito); `[~]` = feito em parte ou depende do usuário.
Fonte das mudanças: `smp-profissoes/progressao.json`, migrações em `smp-profissoes/tools/migracoes/v5…v8`.

## 1. Correções de erros e bloqueios

### Pesquisas e progressão
- [x] Industrial Fuels barrando a Steam Engine → pesquisa nova **Steam Power** (nível 2, depois de Create Foundations).
- [x] Herbal Inks → a Legendary Ink usa o componente **Moonlit Ink Essence** (Moonlight Hemlock do Toxony, sem Vital Herbs); custo da pesquisa trocado.
- [x] Spirit Foundations sem Soul Sand (custo: 16 ametistas).
- [x] Ballistic Engineering com nome duplicado → descrição "Unlocks: X (Profissões)".
- [x] Venomous Alchemy → libera Witching Blade, Flintlock, Iron Round e armadura Professional Hunter.

### Mods e funcionalidades
- [x] Mod de invisibilidade (`smp-translucidez/`).
- [x] CoreProtect → **CoreProtectNeo 1.1** instalado (`mods/coreprotectneo-1.0.jar`, Modrinth, autorizado pelo usuário).
- [x] RegionGuard → trocado pelo **ArcadiaGuard 1.5.8** (o RegionGuard usa WorldEdit, que o usuário não quer). Zona por coordenadas: `/ag zone add <nome> x1 y1 z1 x2 y2 z2`; a zona nasce sem regras, ligue com `/ag zone flag <zona> <flag> deny` (block-break, block-place, container-access, pvp, irons-spell-cast, simplyswords-ability...). Testado em jogo: jogador comum não quebra dentro da zona com block-break deny, quebra fora e quebra de novo quando a flag volta a allow.
- [x] Nome sobre jogador invisível escondido também (inclusive o do RPEssentials). Config: `mostrar_nome` em `config/smptranslucidez.properties`.
- [x] Silent Compact (SilentGear Compat) → datapack `smp_silentgear_compat` com 15 materiais; conferido em jogo (espada de Manasteel 900 dur., Terrasteel 3200 dur./10,5 dano).
- [x] Integrar o Wizardry com o Hazen → Blaze Caster pede Pyrium Nugget do Hazen; Create Wizardry virou pesquisa cruzada (Arcane Engineering).

## 2. Progressão e árvore de pesquisas

### Requisitos e níveis
- [x] Universidade N só exige prédios até N (aplicado a todas as pesquisas e etapas; o validador barra o contrário).
- [x] Nível 4+: receita única, sem selos. Os selos de papel saíram. Receita de bancada recebe os materiais das outras profissões direto; receita de máquina/ritual recebe um **componente** (item do smpprofissoes) que só quem tem a licença da pesquisa fabrica.
- [x] Expanded Grimoires no nível 2.
- [x] **Eldritch Depths** (nível 3, depois de Expanded Grimoires): Deeper and Darker: Spellbooks.
- [x] Pesquisas do Deep Dark custam materiais de lá (Sculk Bone, Soul Crystal do Deeper and Darker, Echo Shard).
- [x] Wizardry no nível 4 (**Arcane Engineering**, Engenheiro + Arcanista).

### Abas e pesquisas
- [x] Malum: Spirit Infusion ampliada (armaduras, foice), **Spirit Crucible** (nível 3), runas na Rites, **Void Arts** (nível 4, com o Ferreiro).
- [x] Protection Pixel → **Steam Armory** (nível 4, Engenheiro + Ferreiro).
- [x] SecurityCraft → básico em Logistics & Security; minas, sentinelas, taser, claymore, IMS e **Rift Stabilizer** (inibidor de teleporte) em Ballistic Engineering.
- [x] Ember Blade na linha do Embers (nível 4, depois de Alchemy & Artifice).
- [x] Kitchen + Cultivation → **Farmstead Foundations** (menu do Naturalista com 2 opções).
- [x] Shrines e Megatocha → **Warding Arts** (nível 2, Arcanista: Torchmaster e Legendary Shrines).
- [x] Flasks do Neo Vitae → **Vital Flasks** (nível 2, Neo Vitae + Herbal Foundations do Naturalista).

## 3. Receitas e balanceamento

### Alterações de receitas
- [x] Upgrades de nível mágico (Iron's Restrictions, Common…Legendary Upgrade) sem receita: só admin/GM entrega (`/give`). Os jogadores começam em RARE.
- [x] Teleposer e Sigil of Teleposition sem receita.
- [x] Tintas: Uncommon = Glow Ink Sac, Rare = Nightshade (Toxony), Epic = Arcane Spirit (Malum), Legendary = Moonlit Ink Essence; atalhos de mistura do Create Wizardry removidos.
- [x] Create Wizardry: Arcane Essence só na mistura superaquecida, 2 por vez (as rotas fáceis com mana foram removidas).
- [x] Echo Shard: nenhuma receita produz (tirado das montagens do Create Wizardry); só o Deep Dark.
- [x] Timeless Slurry na invisibilidade: Invisibility Elixir (caldeirão e Create) e Cloak of Invisibility; o Ring of Invisibility já usava.
- [x] Blaze Caster (Pyrium do Hazen), Mana Siphon (Manasteel do Botania), tintas e Arcane Essence alterados.
- [x] Quantum Catcher: forja tier 2, 2 Ender Runes + 2 Eyes of Ender, mais essências.

### Integração de receitas e combustíveis
- [x] Combustíveis: já integrados por tags `c:` (diesel, biodiesel, gasolina, petróleo). Completado: gerador do IE queima gasolina e plant oil; motores do Create queimam querosene e High-Power Biodiesel.
- [x] Capa e anel de invisibilidade:
  - **Capa do Botania:** funciona, mas gasta mana do inventário (precisa de Mana Tablet carregado); com mana, o jogador fica translúcido.
  - **Ring of Invisibility do Iron's:** apesar do nome, revela criaturas invisíveis. Agora quem usa vê os invisíveis a 80% de opacidade (os outros veem a 45%).

## 4. Forjas, equipamentos e materiais

### Forja de Hefesto
- [x] Crafts da forja: 228 rituais automáticos do Hazen + 9 manuais do Hazen + 8 do Simply Swords + Eternal Stella + Quantum Catcher.
- [x] Utilidade dos tiers:

  | Tier | O que faz |
  |---|---|
  | 1–2 | básicos, prismas, Quantum Catcher |
  | 3 | equipamento base do Hazen |
  | 4 | upgrades do Hazen e armas únicas |
  | 5 | Eternal Stella, Mjolnir e The Devourer |
- [x] Simply Swords na forja: Emberblade, Hearthflame, Frostfall, Stormbringer, Bramblethorn, Soulkeeper, Mjolnir e The Devourer (as únicas continuam no saque).
- [x] Hazen na forja: todas as armas, armaduras, cajados e livros com receita (a receita de bancada/ferraria foi removida).

### Forbidden Arcanus
- [x] Runic Forge e Simply Swords na linha do Forbidden Arcanus:
  - **Martial Weaponsmithing** (nível 2): armas comuns;
  - **Runic Weaponsmithing** (nível 3): Runic Forge, tábuas e gemas.
- [x] Crafts de armas do Simply Swords: as receitas originais ficam, travadas pelas pesquisas.
- [x] Prismas:
  - entram nos rituais das armas únicas;
  - o Whirlwind Prism, que não tinha uso, aplica o modificador **Magnetized**.

### Ferreiros e materiais
- [x] Terrasteel como material do Silent Gear (3200 dur., tier Tyrian Steel).
- [x] Compatibilidade Silent Gear + Botania (datapack de compat).

## 5. Expansão de conteúdo
- [x] Mais armas em Ballistic Engineering (sentinelas, minas, claymore, IMS, taser e Rift Stabilizer do SecurityCraft).

## Testador automático
- `smp-testes/` abre o jogo sozinho e roda roteiros (`python -I smp-testes\rodar.py`). Ver `smp-testes/LEIA-ME.md`.
