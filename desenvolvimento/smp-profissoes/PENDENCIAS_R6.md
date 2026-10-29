# Rodada 6 (26/10/2026) — lista do usuário

`[x]` = feito. Testado só offline (pedido do usuário): datapack 5366 OK, mixins 93 OK, Java 907 OK.
Migração `tools/migracoes/v13_rodada6.py` + `corrigir_adiantados.py`.

- [x] Singularity fora do Chorus Wine e do Teleportation Amulet (Chorus Wine voltou para Fine Wines).
- [x] Singularity: montagem sequencial do Create (base: Matter Ball do AE2) com um item de fim de jogo de cada mod, cada um da sua
      estação: Precision Mechanism (Create), Advanced Electronic Component (bancada do engenheiro, IE), Wildfire Core (alquimia do
      Embers), Legendary Ink (caldeirão do Iron's), Malignant Pewter (altar do Malum), Dragonstone (portal dos elfos), Hellforged
      Resonator (Hellfire Forge), Tyrian Steel (forja de ligas, Silent Gear), Smelter Prism (Forja de Hefesto), Lethal Dose (forja
      alquímica, Toxony), Soul of the World (caldeirão da bruxa), Eiswein (barril, Vinery). Passou para **Quantum Networks** (nível 5),
      que agora exige pesquisas das quatro profissões; o Portal Frame foi junto.
- [x] Protection Pixel: a etapa Netherite Exo-Armor tinha `sem_vedacao: protection_pixel:*` (erro da rodada 4) e Steam Armory era "resto";
      agora as duas são vedadas e as receitas do Create (mechanical crafting e montagem sequencial) pedem um componente feito só por quem
      tem a pesquisa: **Netherite Exo-Plating** e **Steam-Forged Rivets**.
- [x] Gourmet Cuisine: a regra já estava certa (TOML e operador da máquina). O provável motivo do teste ter passado: **OP passa por tudo**
      (`opLevelBypass = 2` no RPEssentials; em mundo single-player com cheats o jogador é OP). Teste com uma conta sem OP.
- [x] Otherwhere Rites: nível 3 (sob Hedge Witchcraft), custo menor (4 pérolas, 8 chorus, 8 ametista); Waystone com Teleposer Focus.
- [x] Megatorch: Rune of Fire e Mana Diamond (Botania), Runewood Log e Hallowed Gold (Malum) — nível 2-3.
- [x] Licença de nível da colônia: o `/rpessentials license give` só aceita profissões cadastradas; agora é
      `/smpprofissoes colonia <jogador> <nível>` (sem nível mostra o atual).
- [x] Simply Bows (não tem receitas): **Legendary Bowyery** (Ferreiro, nível 4, após Ranged Smithing): 8 arcos na Forja de Hefesto e as
      6 melhorias na bancada.
- [x] Simply More (o "More Swords"): crashes `ClientLevel cannot be cast to ServerLevel` (Stun, Mimicry, Glimmerstep e outros): o mod
      usa recursos do servidor também no cliente; mixins fazem esses trechos rodarem só no servidor.
- [x] Quick Skin x MineColonies (crash 26/10 20:32, "No extension for file: quickskin:local/..."): as telas da MineColonies (BlockUI)
      liam o tamanho da skin como arquivo; agora usam o tamanho da textura dinâmica.
