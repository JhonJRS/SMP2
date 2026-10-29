# SMP 2 — Profissões, mod inicial e progressão pela MineColonies

A lista completa de cada pesquisa, etapa, selo, receita alterada, receita desativada e tabela de loot está em [RELATORIO_GERADO.md](RELATORIO_GERADO.md). Esse arquivo é gerado pelo `tools/gerar.py`.

## Como o jogador progride

1. **Entrou no servidor → escolhe a profissão e o mod inicial.** O jogador fica preso até concluir os três passos: profissão → mod inicial → confirmar. A escolha é permanente; só admin troca.

   | Profissão | Pode começar por |
   |---|---|
   | Engenheiro | Create · Immersive Engineering · Embers |
   | Arcanista | Iron's Spells · Malum · Botania · Neo Vitae |
   | Ferreiro | Productive Metalworks · Silent Gear · Forbidden Arcanus |
   | Naturalista | Farmer's Delight · Farm & Charm · Herbalismo (HerbalBrews + Vital Herbs) |

   O mod inicial fica liberado **para aquele jogador** na hora, sem precisar de colônia. Só a parte de fundação do mod: as etapas avançadas continuam exigindo pesquisa.
2. **Colônia → aba "Guild of Trades" na Universidade.** São 48 pesquisas, em inglês. Cada mod tem um nó de **Foundations** (nível 1), que libera esse mod para todos os jogadores da profissão na colônia. A partir dele vêm os níveis 2 e 3 da linha.
3. **Níveis 4 e 5 = pesquisas cruzadas.** Cada uma exige pesquisas de **outra profissão**, como requisito real da MineColonies, e libera itens game changer que só saem com o trabalho de 2 ou 3 profissões.
4. **`/progression`** mostra o que o jogador já tem e o que falta.

## Linhas (níveis 1–3)

| Profissão | Linhas |
|---|---|
| Engenheiro | **Create Foundations** → Industrial Architecture (Deco, B&W, Cobblestone) · **Brass Metallurgy** → Logistics & Security / Industrial Fuels / Deep Dark Engineering |
| | **Immersive Foundations** → High-Power Systems (+Posts) → Petrochemistry (Immersive Petroleum) |
| | **Ember Foundations** → Ember Processing → Alchemy & Artifice |
| Arcanista | **Iron's Foundations** → Scroll Studies → Arcane Mastery / Expanded Grimoires (addons) |
| | **Spirit Foundations** (Malum) → Spirit Infusion → Rites & Spirit Convergence |
| | **Botanical Foundations** → Mana Manipulation → Natural Alchemy & Runes |
| | **Blood Foundations** (Neo Vitae) |
| Ferreiro | **Foundry Foundations** (PM) → Heated Foundry → Gear Casting (exige também a fundação do Silent Gear) |
| | **Toolsmith Foundations** (Silent Gear) → Precision Metallurgy (+Transmog) → Master Toolsmith |
| | **Forbidden Foundations** → Arcane Metallurgy → Hephaestus Mastery |
| Naturalista | **Kitchen Foundations** (Farmer's Delight) · **Cultivation Foundations** (Farm & Charm) → Viticulture (Vinery) |
| | **Herbal Foundations** → Toxicology (Toxony) → Natural Alchemy |

## Pesquisas cruzadas e itens game changer

Funcionam com **selos**: papéis com nome, descrição e uma marca interna (`custom_data.smp_selo`), que bigorna e renomeação não conseguem copiar.
- Cada profissão fabrica o seu selo na bancada, e só com a licença da pesquisa cruzada. Isso é conferido no servidor.
- A profissão que opera a máquina final junta os selos num **catalisador**.
- O catalisador entra na receita original, alterada para exigi-lo.

| Nível | Pesquisa | Profissões | Resultado (receita alterada) | Selos |
|---|---|---|---|---|
| 4 | Arcane Aeronautics | Eng + Arc | Levitita (misturador do Create) e toda a linha Aeronautics | Levitation Sigil (Arc) → Levitation Catalyst (Eng) |
| 4 | Ballistic Engineering | Eng + Fer | Railgun do IE (bancada) | Rail-Grade Alloy Writ (Fer) |
| 4 | Runeforged Alloys | Fer + Arc | Tyrian steel (Alloy Forge do Silent Gear) | Spirit Flux (Arc) → Runeforged Catalyst (Fer) |
| 4 | Herbal Inks | Arc + Nat | Tinta lendária do Iron's (caldeirão) | Moonpetal Tincture (Nat) → Legendary Ink Catalyst (Arc) |
| 4 | Venomous Alchemy | Nat + Arc | Eternal Plague e Lethal Dose (forja alquímica do Toxony) | Cursed Essence (Arc) → Plague Catalyst (Nat) |
| 4 | Spellforged Arms | Arc + Fer + Nat | 9 cajados, cetros e grimórios lendários do Hazen **viram rituais da Forja de Hefesto**; Spellbreaker, Boreal Blade e Twilight Gale (Iron's) **viram infusões do Altar Espiritual do Malum** | Arcane Inscription (Arc) → Spellforge Catalyst (Fer) na forja; Runic Blade Blank (Fer) + Arcane Inscription (Arc) no altar; Verdant Sap (Nat) no White Lily Staff |
| 5 | **Stellar Forging** | Fer + Arc + Nat | **Eternal Stella**: Forja de Hefesto **tier 4**, Dark Nether Star no centro, Life Essence (chefe do Botania), stellarite, orbes | Astral Mana Core (Arc) + Starbloom Extract (Nat) → Stellar Catalyst (Fer) |
| 5 | Terrasteel Synthesis | Arc + Nat + Fer | Terrasteel (Terra Plate do Botania) | Living Root Essence (Nat) + Terra Alloy Writ (Fer) → Terra Catalyst (Arc) |
| 5 | The Ember Blade | Eng + Fer + Arc | Tyrfing (alquimia do Embers) | sem selo (o Embers não aceita): tyrian steel (Fer) + placa de soul-stained steel (Arc) sobre a espada de dawnstone (só Eng com a pesquisa) |
| 5 | Plaguebringer Regalia | Nat + Eng + Arc | Armadura Plaguebringer (Toxony) | Alchemical Regulator (Eng) + Cursed Essence (Arc) → Plaguebringer Catalyst (Nat) |

**Árvore:** o servidor só deixa iniciar uma pesquisa da Guild of Trades com **toda a cadeia anterior concluída** (pai, avô…). A MineColonies original só confere o pai, e só na tela. As etapas também exigem a cadeia inteira. Assim, saves de versões antigas da árvore não pulam etapas.

O nível 6 da MineColonies não foi usado: lá só **uma** pesquisa de nível 6 pode ser concluída por aba, o que travaria as quatro profissões.

As rotas alternativas desses itens foram desativadas (fundição do PM e pó do tyrian steel, arc furnace e clibano do obsidiansteel, caldeirão dos orbes). As receitas originais do **latão** voltaram: mistura do Create, IE, PM e Embers.

## Proteções (servidor)

- **Bancada, inventário e ferraria:** só com a profissão e a etapa.
- **Selos:** só com a licença da pesquisa cruzada.
- **Autocraft sem jogador:** Crafter vanilla, Crafty Crate, tartaruga do CC, IE, Neo Vitae e automação do Create **não** fabricam itens de profissão nem selos.
- **AE2 e MineColonies:** codificar padrão e ensinar receita conferem profissão, etapa e selo de quem faz.
- **Saque:**
  - estruturas (baús, arqueologia, presentes de aldeão) perdem os itens de etapa;
  - drops de bloco não mudam;
  - drops exclusivos de chefes (Gaia Spirit, pyrium…) continuam.

## Limitações conhecidas

- **Operadores (OP ≥ 2)** ignoram tudo, inclusive os selos. Teste com uma conta comum ou num mundo sem cheats.
- **Jogadores de mundos antigos** que escolheram a profissão antes desta versão não têm mod inicial. Use `/execute as <jogador> run function profissoes:admin/resetar` para que escolham de novo.
- Materiais naturais dos mods (minério, plantas, drops de mob) continuam coletáveis por qualquer um. O que é bloqueado é fabricar.
- Os formatos das receitas alteradas foram copiados dos originais e validados contra os jars, mas **ainda não foram testados em jogo**. Se alguma não carregar, o log mostra `Couldn't parse data file`.

## Comandos

Rode na pasta da instância:

```powershell
python -I smp-profissoes\tools\gerar.py                                           # regenerar depois de editar progressao.json
powershell -ExecutionPolicy Bypass -File smp-profissoes\mod\build.ps1 -Instalar   # compilar e instalar o mod
powershell -ExecutionPolicy Bypass -File smp-profissoes\tests\rodar_testes.ps1    # todos os testes
```

No jogo:
- **Jogador:** `/progression`
- **Admin:** `/smpprofissoes status <jogador>`, `/smpprofissoes inicio <jogador> <etapa>`, `/smpprofissoes resetar <jogador>`, `/smpprofissoes sincronizar`, `/smpprofissoes recarregar`

Para o servidor, copie:
- `mods\smpprofissoes-1.0.0.jar`
- `config\rpessentials\`
- `config\smpprofissoes\`
- `moonlight-global-datapacks\` (as duas pastas)

**O que conferir no `logs/latest.log`:**
- `Research loaded. Located 5 branches, 266 researches and 154 effects.` A MineColonies original tem 218 pesquisas e 106 efeitos; a aba nova acrescenta 48 de cada.
- `Regras carregadas: 4 profissões, 62 etapas`
- 13 linhas `Proteção ativa` (a nova é a cadeia de pesquisas)
- nenhum `Couldn't parse data file` com `smp_progressao`
