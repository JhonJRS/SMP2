# Rodada 5 (26/10/2026) — lista do usuário

`[x]` = feito. Pedido do usuário: **não abrir mais o Minecraft; testar internamente** (suíte offline: datapack 5294 OK,
mixins 72 OK, Java 873 OK). Migração `tools/migracoes/v12_rodada5.py` + `corrigir_adiantados.py`.

## Mods novos
- [x] **Enchanted: Witchcraft** (o "enhanced witchcraft"/"Witchery" da lista) na árvore do Naturalista:
      Hedge Witchcraft (n2: altar, forno, destilaria, giz) → Witch's Kettle (n3) e Poppetry (n3) → Otherwhere Rites (n4: waystones,
      giz do outrolugar/nether, vassoura). Caldeirão, roca, destilaria, forno e altar só abrem com a licença da etapa.
- [x] **Serene Seasons** + GlitchCore instalados (não estavam no modpack; só havia o config). `config/sereneseasons/fertility.toml`:
      fora de estação a plantação **não cresce** (estufa de vidro e subsolo continuam férteis). Datapack `smp_estacoes`
      (`tools/estacoes.py`) põe os cultivos de Farmer's Delight, Farm & Charm, Vinery, HerbalBrews, Vital Herbs, Toxony,
      Enchanted, IE e MineColonies nas estações.
- [ ] **Quick Skin x MineColonies**: falta saber qual é o conflito (não há erro nos logs; o jar não colide com mixins da MineColonies).

## Forja de Hefesto / componentes
- [x] Sea Prism sem Heart of the Sea (bloco de cristal arcano + prismarinho, náutilo e escama de tartaruga).
- [x] 97 rituais de armadura/cajado pedem o prisma do elemento (gerador: `prismas_elementais`): fogo → Smelter, natureza → Terrastomp,
      elétrico → Whirlwind, água/gelo → Sea. Sem elemento claro: sem prisma.
- [x] Ashen Weave nas armaduras cinzentas e no Ashen Amulet; Soul Thread nas bijuterias tecidas do Malum (anéis, cinto, colar, broches, bolsa).
- [x] Eternal Stella: Eternity Essence (Naturalista, Eternal Brews) no lugar da Compacted Glow Leaf.
- [x] Shrine: um material de cada escola mágica (Manasteel, Arcane Ingot, Hallowed Gold, Arcane Crystal, Blood Pearl).
- [x] Armas e armaduras do CrystalChronicles no **Altar Espiritual do Malum** (47 infusões; gerador: `conversoes_malum`).
- [x] Mythic Arms: +10 armas míticas (Watcher Claymore, Harbinger, Sunfire, Tempest, Thunderbrand, Star's Edge, Caelestis,
      Awakened Lichblade, Livyatan, Enigma), todas com Mythic Core.

## Progressão
- [x] Nível da colônia: subir a Prefeitura para o nível N exige a licença `colonia_N` dada por ADM (OP não precisa).
- [x] Máquinas com interface: cada máquina tem um **operador** (quem a colocou); o que ela produz respeita a profissão e as etapas
      dele (Cooking Pot, caldeirão do Enchanted, altar do Malum, etc.). Gourmet Cuisine agora libera 50 pratos e os banquetes.
- [x] Apple Wine (o vinho de Resistência) envelhece só até Resistência IV.
- [x] Cataclysm (Ferreiro): Cataclysmic Smithing (n3) → Boss Relic Armory (n4) → Cataclysmic Fusion (n5, bigorna de fusão).
- [x] Applied Energistics: ME Networks (n4) → Quantum Networks (n5). Singularity: receita com itens das 4 profissões (o Condensador
      não a faz mais) e entra em todo item de teletransporte: Portal Frame, Teleportation Amulet, Waystone do Enchanted e Chorus Wine.
