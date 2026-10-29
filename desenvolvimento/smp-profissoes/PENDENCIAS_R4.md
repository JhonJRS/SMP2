# Rodada 4 (26/10/2026) — lista do usuário

`[x]` = feito (com o que foi feito). Fonte: `smp-profissoes/progressao.json`, migrações `tools/migracoes/v11_rodada4.py`
e `tools/migracoes/corrigir_adiantados.py`.

## Engenheiro
- [x] 1. ComputerCraft fora da progressão (CC:Tweaked livre; a ponte cccbridge saiu da árvore).
- [x] 2. Arcane Aeronautics → **Aeronautics Engineering**.
- [x] 4/5. Steam Armory dividida: armaduras de latão no nível 3 (sob Brass Metallurgy); armaduras de Netherite na nova **Netherite Exo-Armor** (nível 4).
- [x] 6. Torretas e minas do SecurityCraft em Immersive Arsenal (Immersive Arsenal passou a exigir também Logistics & Security, de onde vêm as peças base).
- [x] 7. Precision Scope sai do Railgun e vai para Immersive Arsenal.
- [x] 8. Custo de Alchemy & Artifice com itens do Embers.
- [x] 9. Alchemy & Artifice + Ashen Regalia juntas no nível 3 (componente Ashen Weave).
- [x] 10. The Ember Blade no nível 5 sem pesquisa de nível 4 na linha do Embers: ela fica pendurada em Runic Alloys (n4, que já exigia) e exige Alchemy & Artifice como requisito extra (a MineColonies descarta pesquisa que pula nível — testado em jogo).

## Arcanista
- [x] 3. Arcane Essence mais cara: a mistura dá 1 (não 2) e pede glowstone + mana powder; mithril triturado 25% × 1.
- [x] 11. Expanded Grimoires reanalisado pela regra geral (abaixo): itens que pedem coisas de pesquisas maiores foram para Eldritch Depths, Spellforged Arms ou Legendary Artifacts.
- [x] Megatorch na pesquisa própria **Grand Wards** (nível 3, linha do Warding Arts).
- [x] Receita para a Shrine.
- [x] Portal Frame usa a Singularity do Hazen.
- [x] Mithril e Mithril Weave em Iron's Foundations.
- [x] Armaduras de Iron's Foundations que precisam de itens posteriores foram para Arcane Vestments / Crystal Knights.
- [x] CrystalChronicles na árvore: **Crystal Chronicles** (n2), **Crystal Knights** (n3), **Crystal Arsenal** (n3).
- [x] Blood Foundations sem Ghast Tear (redstone, carne podre, osso).
- [x] Herbal Inks: pigmentos raro/épico/lendário (componentes) com plantas do Botania, Toxony e HerbalBrews.
- [x] Archmage Codex = item das armas e artefatos mais fortes; nos rituais, Deus Essence / Divine Mold pedem o Codex (tier 5).
- [x] Legendary Grimoires → **Legendary Artifacts** (artefatos do Hazen com Deus Essence / Divine Mold).
- [x] Spellbreaker, Boreal Blade e Twilight Gale voltaram para Spellforged Arms com a Arcane Inscription; Dragonskin e Cursed Doll Spell Book foram para Arcane Mastery.
- [x] Malignant Arsenal com uso real (armadura Malignant + Tyrving).
- [x] **Enigmatic Arcana** (nível 3).
- [x] Armadura Malignant em Malignant Arsenal; Malignant Core mais caro.
- [x] Subtítulo de Natural Alchemy & Runes explica que o portal dos elfos é outra pesquisa.
- [x] Terra Blade, Terra Truncator, Terra Shatterer e Rod of the Lands em Terrasteel Synthesis.

## Ferreiro
- [x] Receita de bancada do Obsidian Ingot (Forbidden Arcanus).
- [x] Productive Metalworks junto com o Silent Gear no mod inicial; única pesquisa do PM = **Foundry Upgrades** (as 3 bobinas).

## Geral
- [x] Regra "nada liberado antes de existir a forma de fazê-lo": o gerador agora dá ERRO se algum item for liberado antes da
      pesquisa que libera os ingredientes dele. `corrigir_adiantados.py` moveu 271 itens para a pesquisa certa e deu
      18 pesquisas com requisito extra (lista no HISTORICO). Receitas em ciclo (lingote ↔ bloco) não contam como rota.
- [x] Clérigo não vende mais Echo Shard (troca do Create: Deep Dark removida pelo mod).
- [x] Warden não dropa mais Echo Ingot (tabela de loot vanilla restaurada).
- [x] **Guild Charter**: uso único, dá a segunda profissão (máximo 2) com escolha do mod inicial no chat.
- [x] **Draught of Oblivion**: apaga profissões, mods iniciais e etapas; o menu de escolha volta (Soul Crystal, Soul Dust, Tincture of Forgetfulness).
- [x] Todos os componentes e itens especiais do SMP Profissões têm receita.
