"""Migração v9 (26/10/2026): árvore com mais ramos e níveis equilibrados em poder.
- Linhas que eram retas (IE, Embers, Iron's, Malum, Botania, Neo Vitae, Silent Gear, PM, Forbidden Arcanus, Naturalista)
  ganham ramos nos níveis 2-4.
- Nível 5 = só o que muda o jogo: Railgun (50 de dano), Tyrfing, Grimórios Lendários, Arsenal Maligno, Alfheim/Gaia,
  Eternal Stella, Armas Míticas e Poções Eternas. A Regalia do Plaguebringer desce para o nível 4.
- Tudo de poção passa a ser do Naturalista (suporte de poções, Botanical Brewery, frascos, antídotos).
Uso: python -I smp-profissoes/tools/migracoes/v9_ramos.py   (idempotente)
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402  (índice dos jars: só itens que existem e têm receita de bancada)

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if any(p["id"].endswith("/railgun") for p in cfg["pesquisas"]):
    raise SystemExit("já migrado (v9)")
ix = gerar.indexar()
GRID = gerar.GRID
com_bancada = {s for r in ix.receitas.values() if r["pasta"] == "recipe" and GRID.search(r["tipo"]) for s in r["saidas"]}

O = "smpprofissoes:oficios/"
P = {p["id"].split("/", 1)[1]: p for p in cfg["pesquisas"]}
E = {e["licenca"]: e for e in cfg["etapas"]}
PROF = cfg["profissoes"]
ITEM = lambda i: {"item": i}


def pesquisa(id_, nome, sub, nivel, icone, edificios, custos, pai, extra=(), depois_de=None):
    p = {"id": O + id_, "nome": nome, "subtitulo": sub, "nivel": nivel, "icone": icone, "edificios": edificios, "custos": custos, "pai": O + pai}
    if extra:
        p["pesquisas_extra"] = [O + x for x in extra]
    lst = cfg["pesquisas"]
    ref = O + (depois_de or pai)
    i = next(k for k, x in enumerate(lst) if x["id"] == ref)
    lst.insert(i + 1, p)
    P[id_] = p
    return p


def etapa(lic, nome, prof, pesq, itens=(), resto=(), **extra):
    e = {"licenca": lic, "nome": nome, "profissao": prof, "pesquisas": [O + x for x in pesq], "edificios": {}, "itens": list(itens)}
    if resto:
        e["resto_de"] = list(resto)
    e.update(extra)
    cfg["etapas"].append(e)
    E[lic] = e
    return e


def tirar(lic, *itens):
    E[lic]["itens"] = [i for i in E[lic]["itens"] if i not in itens]


def so_com_bancada(ns, nomes):
    """Só os itens que existem e têm receita de bancada/ferraria (os de saque ficam no saque)."""
    return [f"{ns}:{n}" for n in nomes if f"{ns}:{n}" in com_bancada]


def componente(cid, nome, forma, cor, lic, desc, ings, qtd=1):
    cfg["componentes"].append({"id": cid, "nome": nome, "forma": forma, "cor": cor, "licencas": [lic], "quantidade": qtd,
                               "descricao": desc, "ingredientes": ings})


def pecas(prefixo, nomes=("helmet", "chestplate", "leggings", "boots")):
    return [f"{prefixo}_{n}" for n in nomes]


# ============================================================ ENGENHEIRO
# ---- Immersive Engineering: ferramentas e armas viram ramos; Railgun no nível 5
IE = "immersiveengineering:"
pesquisa("ferramentas_ie", "Powered Tools", "Mining drills, buzzsaws, skyhooks and power packs", 2, IE + "drill",
         {"minecolonies:mechanic": 2}, {IE + "ingot_steel": 16, IE + "component_iron": 8}, "fundacao_ie")
etapa("eng_ferramentas_ie", "Engineer · Powered Tools", "engenheiro", ["ferramentas_ie"],
      [IE + x for x in ("drill", "drillhead_*", "buzzsaw", "sawblade", "rockcutter", "grindingdisk", "skyhook", "powerpack", "maneuver_gear",
                        "glider", "toolupgrade_drill_*", "toolupgrade_buzzsaw_*", "toolupgrade_skyhook_*", "toolupgrade_powerpack_antenna",
                        "toolupgrade_powerpack_induction", "toolupgrade_powerpack_magnet")])
pesquisa("industria_pesada", "Heavy Industry", "Heavy engineering blocks, radiators and arc furnace electrodes", 3, IE + "heavy_engineering",
         {"minecolonies:mechanic": 3}, {IE + "ingot_steel": 48, IE + "component_steel": 16}, "sistemas_alta_potencia")
etapa("eng_industria_pesada", "Engineer · Heavy Industry", "engenheiro", ["industria_pesada"],
      [IE + "heavy_engineering", IE + "radiator", IE + "graphite_electrode"])
P["petroquimica"]["pesquisas_extra"] = [O + "industria_pesada"]
pesquisa("arsenal_ie", "Immersive Arsenal", "Revolvers, cartridges, chemical throwers, turrets and steel armor", 3, IE + "revolver",
         {"minecolonies:mechanic": 3}, {IE + "ingot_steel": 32, "minecraft:gunpowder": 32, IE + "component_steel": 8}, "ferramentas_ie")
etapa("eng_arsenal_ie", "Engineer · Immersive Arsenal", "engenheiro", ["arsenal_ie"],
      [IE + x for x in ("revolver", "gunpart_*", "bullet_*", "empty_casing", "empty_shell", "speedloader", "mold_bullet_casing", "chemthrower",
                        "toolupgrade_chemthrower_*", "toolupgrade_revolver_*", "turret_gun", "turret_chem", "armor_steel_*", "armor_faraday_*",
                        "faraday_suit_*", "shield", "toolupgrade_shield_*")])
b = P["engenharia_balistica"]
b.update(pai=O + "arsenal_ie", subtitulo="Sentries, mines, claymores and the teleport-blocking rift stabilizer")
tirar("c2_eng", IE + "railgun")
tirar("eng_alta_potencia", IE + "toolupgrade_railgun_capacitors")
pesquisa("railgun", "Electromagnetic Railgun", "The railgun: steel rods at 50 damage", 5, IE + "railgun",
         {"minecolonies:mechanic": 5, "minecolonies:blacksmith": 4},
         {IE + "capacitor_hv": 4, "silentgear:crimson_steel_ingot": 16, "minecraft:netherite_ingot": 2}, "engenharia_balistica",
         extra=("industria_pesada", "ligas_runicas"))
etapa("c15_eng", "Engineer · Electromagnetic Railgun", "engenheiro", ["railgun"],
      [IE + "railgun", IE + "toolupgrade_railgun_capacitors", IE + "toolupgrade_railgun_scope"])
for r in cfg["receitas_modificadas"]:
    if r["id"] == "immersiveengineering:crafting/railgun":
        r["_comentario"] = "Electromagnetic Railgun (nível 5): Crimson Steel (Ferreiro) direto na receita; a bancada confere a licença c15_eng."

# ---- Embers: logística, arsenal, regalia cinzenta; Tyrfing no nível 5
EM = "embers:"
logistica = ("item_pipe", "item_extractor", "item_transfer", "item_dropper", "item_vacuum", "item_dial", "fluid_pipe", "fluid_extractor",
             "fluid_transfer", "fluid_dial", "fluid_vessel", "ember_relay", "mirror_relay", "beam_splitter", "ember_funnel", "ember_injector",
             "ember_siphon", "ember_ejector", "bin", "reservoir", "mechanical_pump")
tirar("eng_processamento_ember", *[EM + x for x in logistica])
pesquisa("logistica_ember", "Ember Logistics", "Ember relays, funnels, item and fluid pipes", 2, EM + "ember_relay",
         {"minecolonies:mechanic": 2}, {"minecraft:copper_ingot": 48, EM + "ember_crystal": 8}, "fundacao_embers")
etapa("eng_logistica_ember", "Engineer · Ember Logistics", "engenheiro", ["logistica_ember"], [EM + x for x in logistica])
arsenal = ("blazing_ray", "cinder_staff", "cinder_jet", "grandhammer", "clockwork_axe", "clockwork_pickaxe", "ember_jar", "ember_cartridge")
tirar("eng_alquimia_artificios", *[EM + x for x in arsenal])
pesquisa("arsenal_ember", "Ember Arsenal", "Blazing rays, cinder staves, grandhammers and dawnstone tools", 3, EM + "blazing_ray",
         {"minecolonies:mechanic": 3}, {EM + "dawnstone_ingot": 16, EM + "ember_crystal": 16}, "processamento_ember")
etapa("eng_arsenal_ember", "Engineer · Ember Arsenal", "engenheiro", ["arsenal_ember"],
      [EM + x for x in arsenal] + so_com_bancada("embers", ("flamethrower", "dawnstone_axe", "dawnstone_pickaxe", "dawnstone_shovel",
                                                           "dawnstone_hoe", "dawnstone_mail")))
regalia = ("ashen_amulet", "ashen_boots", "ashen_cloak", "ashen_goggles", "ashen_leggings", "ember_amulet", "ember_belt", "ember_ring",
           "caster_orb", "diffraction_barrel", "resonating_bell", "clockwork_attenuator")
tirar("eng_alquimia_artificios", *[EM + x for x in regalia])
pesquisa("regalia_cinzenta", "Ashen Regalia", "Ashen armor, ember trinkets and tool augments", 4, EM + "ashen_cloak",
         {"minecolonies:mechanic": 4, "minecolonies:enchanter": 3},
         {EM + "dawnstone_ingot": 32, "malum:soul_stained_steel_ingot": 8, EM + "ember_crystal_cluster": 4}, "alquimia_artificios",
         extra=("infusao_espiritos",))
componente("ashen_weave", "Ashen Weave", "runa", "#7A6E66", "c20_eng", "Ash-cured cloth bound with spirit",
           ["embers:ash", "embers:ash", "malum:soul_stained_steel_ingot", "embers:ember_crystal", "minecraft:string"], 2)
etapa("c20_eng", "Engineer · Ashen Regalia", "engenheiro", ["regalia_cinzenta"],
      [EM + x for x in regalia] + so_com_bancada("embers", ("nonbeliever_amulet", "explosion_charm", "eldritch_insignia", "inflictor_gem",
                                                           "focal_lens", "intelligent_apparatus", "engineers_ashen_goggles",
                                                           "physics_ashen_goggles")),
      componente_maquina={"id": "ashen_weave", "quantidade": 1})
lb = P["lamina_ember"]
lb.update(nivel=5, pai=O + "regalia_cinzenta", pesquisas_extra=[O + "ligas_runicas"], subtitulo="The Tyrfing, a blade of living ember",
          edificios={"minecolonies:mechanic": 5, "minecolonies:blacksmith": 4, "minecolonies:enchanter": 3})

# ============================================================ ARCANISTA
# ---- Iron's: vestes, arsenal e grimórios lendários (nível 5)
IR = "irons_spellbooks:"
vestes = [x for esc in ("pyromancer", "cryomancer", "electromancer", "archevoker", "priest", "cultist", "plagued", "shadowwalker")
          for x in pecas(IR + esc)]
pesquisa("vestes_arcanas", "Arcane Vestments", "School robes: pyromancer, cryomancer, electromancer, priest and more", 2, IR + "pyromancer_chestplate",
         {"minecolonies:enchanter": 2}, {IR + "magic_cloth": 16, IR + "arcane_essence": 16}, "fundacao_irons", depois_de="artes_protecao")
etapa("arc_vestes", "Arcanist · Arcane Vestments", "arcanista", ["vestes_arcanas"], [v for v in vestes if v in com_bancada])
tirar("arc_maestria", IR + "graybeard_staff", IR + "pyrium_staff", IR + "artificer_cane", IR + "dragonskin_spell_book", IR + "cursed_doll_spell_book")
armas_irons = so_com_bancada("irons_spellbooks", ("graybeard_staff", "pyrium_staff", "artificer_cane", "ice_staff", "blood_staff", "lightning_rod",
                                                  "staff_of_the_nines", "amethyst_rapier", "keeper_flamberge", "legionnaire_flamberge", "misery",
                                                  "hellrazor", "decrepit_scythe", "dreadsword", "firebrand", "frosted_helve", "icy_fang", "magehunter",
                                                  "truthseeker", "obsidian_katana", "autoloader_crossbow", "claymore"))
pesquisa("arsenal_arcano", "Spellblade Arsenal", "Battle staves, spellblades and netherite mage armor", 3, IR + "graybeard_staff",
         {"minecolonies:enchanter": 3}, {IR + "arcane_ingot": 16, "minecraft:diamond": 8}, "vestes_arcanas")
etapa("arc_arsenal", "Arcanist · Spellblade Arsenal", "arcanista", ["arsenal_arcano"],
      armas_irons + [x for x in pecas(IR + "netherite_mage") if x in com_bancada])
for lic in ("c6_arc",):
    tirar(lic, IR + "spellbreaker", IR + "boreal_blade", IR + "twilight_gale")
pesquisa("grimorios_lendarios", "Legendary Grimoires", "Dragonskin grimoires and the legendary spellblades", 5, IR + "dragonskin_spell_book",
         {"minecolonies:enchanter": 5}, {IR + "legendary_ink": 4, IR + "divine_pearl": 4, "minecraft:nether_star": 1}, "tintas_herbais",
         extra=("armas_encantadas",))
componente("archmage_codex", "Archmage Codex", "runa", "#E8C14A", "c16_arc", "Pages that teach a weapon to hold a spell",
           ["@comp:arcane_inscription", IR + "legendary_ink", IR + "divine_pearl", "minecraft:nether_star"])
etapa("c16_arc", "Arcanist · Legendary Grimoires", "arcanista", ["grimorios_lendarios"],
      so_com_bancada("irons_spellbooks", ("dragonskin_spell_book", "cursed_doll_spell_book", "legendary_spell_book", "necronomicon_spell_book"))
      + [IR + "spellbreaker", IR + "boreal_blade", IR + "twilight_gale"])
for n in cfg["receitas_novas"]:
    if n["id"].startswith("smpprofissoes:spirit_infusion/"):
        n["json"]["extraInputs"] = [({"count": 1, "@comp": "archmage_codex"} if e.get("@comp") == "arcane_inscription" else e)
                                    for e in n["json"]["extraInputs"]]
        n["_comentario"] = "Legendary Grimoires (nível 5): Crimson Steel (Ferreiro) + Archmage Codex no Altar Espiritual."

# ---- Malum: Soulwoven Arts (nível 2) e Arsenal Maligno (nível 5)
ML = "malum:"
tecelagem = ["gilded_ring", "gilded_belt", "ornate_ring", "ornate_necklace", "elaborate_brooch", "runic_brooch", "soulwoven_pouch", "weavers_workbench"]
tirar("arc_malum_infusao", *[ML + x for x in tecelagem])
pesquisa("tecelagem_almas", "Soulwoven Arts", "The weaver's workbench, soulwoven pouches and gilded trinkets", 2, ML + "weavers_workbench",
         {"minecolonies:enchanter": 2}, {ML + "arcane_spirit": 16, "minecraft:string": 32}, "fundacao_malum")
componente("soul_thread", "Soul Thread", "frasco", "#C9A0E8", "arc_tecelagem", "Thread spun from soulwoven silk",
           [ML + "soulwoven_silk", ML + "soulwoven_silk", ML + "arcane_spirit", ML + "refined_soulstone"], 4)
etapa("arc_tecelagem", "Arcanist · Soulwoven Arts", "arcanista", ["tecelagem_almas"],
      [ML + x for x in tecelagem] + so_com_bancada("malum", ("glass_brooch",)), componente_maquina={"id": "soul_thread", "quantidade": 1})
supremas = [ML + x for x in ("weight_of_worlds", "edge_of_deliverance", "mnemonic_hex_staff", "erosion_scepter")]
tirar("c14_arc", *supremas)
pesquisa("arsenal_maligno", "Malignant Arsenal", "Weight of Worlds, Edge of Deliverance and the void scepters", 5, ML + "weight_of_worlds",
         {"minecolonies:enchanter": 5, "minecolonies:blacksmith": 4}, {ML + "malignant_pewter_ingot": 16, ML + "void_salts": 32, "minecraft:nether_star": 1},
         "artes_vazio", extra=("maestria_hefesto",))
componente("malignant_core", "Malignant Core", "nucleo", "#5B1E3D", "c17_arc", "Pewter heart for weapons that bend the world",
           [ML + "malignant_pewter_ingot", ML + "malignant_pewter_ingot", ML + "null_slate", ML + "eldritch_spirit", ML + "eldritch_spirit",
            "forbidden_arcanus:obsidiansteel_ingot"])
etapa("c17_arc", "Arcanist · Malignant Arsenal", "arcanista", ["arsenal_maligno"], supremas,
      componente_maquina={"id": "malignant_core", "quantidade": 1})

# ---- Botania: Manasteel Arsenal (2), Rods & Relics (3), Terrasteel (4), Alfheim e Gaia (5)
BO = "botania:"
mana_arsenal = so_com_bancada("botania", [f"manasteel_{x}" for x in ("axe", "boots", "chestplate", "helmet", "helmet_revealing", "hoe", "leggings",
                                                                     "pick", "shears", "shovel", "sword")]
                              + [f"manaweave_{x}" for x in ("boots", "chestplate", "helmet", "leggings")]
                              + ["travel_belt", "knockback_belt", "ice_pendant", "lava_pendant", "cloud_pendant", "magnet_ring", "aura_ring",
                                 "water_ring", "mining_ring", "swap_ring", "dodge_ring", "reach_ring", "bauble_box"])
pesquisa("arsenal_mana", "Manasteel Arsenal", "Manasteel tools and armor, manaweave robes and the first trinkets", 2, BO + "manasteel_sword",
         {"minecolonies:enchanter": 2}, {BO + "manasteel_ingot": 16, BO + "mana_powder": 16}, "fundacao_botania")
etapa("arc_arsenal_mana", "Arcanist · Manasteel Arsenal", "arcanista", ["arsenal_mana"], mana_arsenal)
reliquias = so_com_bancada("botania", ["cobble_rod", "dirt_rod", "exchange_rod", "fire_rod", "gravity_rod", "missile_rod", "rainbow_rod",
                                       "skydirt_rod", "smelt_rod", "tornado_rod", "water_rod", "divining_rod", "super_travel_belt",
                                       "super_cloud_pendant", "super_lava_pendant", "black_hole_talisman", "holy_cloak", "unholy_cloak",
                                       "balance_cloak", "itemfinder", "monocle", "ender_hand", "slingshot", "crystal_bow", "livingwood_bow",
                                       "thorn_chakram", "flare_chakram", "mana_gun", "astrolabe", "sextant"])
pesquisa("reliquias_mana", "Rods & Relics", "Botanical rods, greater pendants, cloaks and mana weapons", 3, BO + "tornado_rod",
         {"minecolonies:enchanter": 3}, {BO + "mana_pearl": 8, BO + "mana_diamond": 8}, "arsenal_mana", extra=("manipulacao_mana",))
etapa("arc_reliquias", "Arcanist · Rods & Relics", "arcanista", ["reliquias_mana"], reliquias)
tirar("arc_runas", BO + "alfheim_portal", BO + "natura_pylon", BO + "gaia_pylon", BO + "terra_sword", BO + "terra_axe", BO + "terraform_rod",
      BO + "gaia_spreader", BO + "brewery")
tirar("arc_mana", BO + "elven_spreader")
t = P["sintese_terrasteel"]
t.update(nivel=4, pai=O + "alquimia_natural_runas", pesquisas_extra=[O + "ligas_runicas"], subtitulo="The terrestrial agglomeration plate and terrasteel",
         edificios={"minecolonies:enchanter": 4, "minecolonies:plantation": 3, "minecolonies:blacksmith": 3})
pesquisa("portal_alfheim", "Alfheim & the Gaia Trials", "The portal to Alfheim, elementium, the flight tiara and the terra blade", 5,
         BO + "alfheim_portal", {"minecolonies:enchanter": 5, "minecolonies:plantation": 4},
         {BO + "terrasteel_ingot": 4, BO + "mana_diamond": 16, BO + "livingwood": 64}, "sintese_terrasteel", extra=("alquimia_natural",))
etapa("c23_arc", "Arcanist · Alfheim & the Gaia Trials", "arcanista", ["portal_alfheim"],
      [BO + x for x in ("alfheim_portal", "natura_pylon", "gaia_pylon", "gaia_spreader", "elven_spreader", "terra_sword", "terra_axe",
                        "terraform_rod")]
      + so_com_bancada("botania", ["pixie_ring", "flight_tiara", "terra_pick", "star_sword", "thunder_sword"]
                       + [f"elementium_{x}" for x in ("axe", "boots", "chestplate", "helmet", "helmet_revealing", "hoe", "leggings", "pickaxe",
                                                      "shears", "shovel", "sword")]))

# ---- Neo Vitae: sigilos/arranjos (2), rituais e arsenal senciente (3), forja infernal (4, com o Ferreiro)
NV = "neovitae:"
pesquisa("arranjos_vitae", "Sigils & Arrays", "Scribe tools, alchemy arrays, sigils and reagents", 2, NV + "sigil_water",
         {"minecolonies:enchanter": 2}, {"minecraft:redstone": 32, NV + "bloodstone": 16}, "fundacao_neovitae")
etapa("arc_arranjos", "Arcanist · Sigils & Arrays", "arcanista", ["arranjos_vitae"],
      [NV + "*_scribe_tool", NV + "sigil_(?!teleposition)*", NV + "reagent_(?!teleposition)*"], sem_vedacao=[NV + "*"])
pesquisa("rituais_sangue", "Blood Rituals", "Ritual anchors, diviners and master ritual stones", 3, NV + "ritual_diviner",
         {"minecolonies:enchanter": 3}, {"minecraft:gold_block": 4, NV + "bloodstone": 32}, "arranjos_vitae")
etapa("arc_rituais_sangue", "Arcanist · Blood Rituals", "arcanista", ["rituais_sangue"],
      [NV + x for x in ("ritual_anchor", "ritual_designer", "ritual_diviner*", "ritual_ledger", "ritual_reader", "master_core*",
                        "activation_crystal_weak", "activation_crystal_awakened")], sem_vedacao=[NV + "*"])
pesquisa("arsenal_senciente", "Sentient Arsenal", "The hellfire forge, spiritus gems and sentient weapons", 3, NV + "sentient_sword",
         {"minecolonies:enchanter": 3}, {"minecraft:diamond": 8, NV + "bloodstone": 32}, "arranjos_vitae", depois_de="rituais_sangue")
etapa("arc_senciente", "Arcanist · Sentient Arsenal", "arcanista", ["arsenal_senciente"],
      [NV + x for x in ("hellfire_forge", "sentient_*", "spiritus_gem_*", "throwing_dagger*", "amethyst_throwing_dagger", "soul_sword_active")],
      sem_vedacao=[NV + "*"])
pesquisa("forja_infernal", "Hellforged Mastery", "Hellforged ingots and casings, greater anointments and the archmage orb", 4, NV + "ingot_hellforged",
         {"minecolonies:enchanter": 4, "minecolonies:blacksmith": 3}, {NV + "bloodstone": 64, "minecraft:netherite_scrap": 4}, "arsenal_senciente",
         extra=("metalurgia_arcana",))
etapa("c24_arc", "Arcanist · Hellforged Mastery", "arcanista", ["forja_infernal"],
      [NV + x for x in ("hellforged_*", "ingot_hellforged", "swift_sanguine_casing", "*_anointment_xl", "*_anointment_l", "blood_orb_archmage",
                        "blood_orb_transcendent")], sem_vedacao=[NV + "*"])

# ============================================================ FERREIRO
SG = "silentgear:"
ARMAD = ("helmet", "chestplate", "leggings", "boots", "shield", "lining")
DIST = ("bow", "crossbow", "slingshot", "arrow", "fletching", "cord", "trident")
E["fer_precisao"]["itens"] = [("silentgear:(?!(coating|" + "|".join(ARMAD + DIST + ("elytra",)) + ")_)*_blueprint") if i.startswith(SG + "(?!coating_)") else i
                              for i in E["fer_precisao"]["itens"]]
pesquisa("armaria_sg", "Armorsmithing", "Silent Gear helmets, chestplates, leggings, boots and shields", 2, SG + "chestplate",
         {"minecolonies:blacksmith": 2}, {"minecraft:iron_ingot": 48, "minecraft:leather": 16}, "fundacao_silentgear")
etapa("fer_armaria", "Blacksmith · Armorsmithing", "ferreiro", ["armaria_sg"], [f"{SG}{x}_blueprint" for x in ARMAD] + [f"{SG}{x}_template" for x in ARMAD])
pesquisa("armaria_distancia", "Ranged Smithing", "Silent Gear bows, crossbows, slingshots, arrows and tridents", 3, SG + "bow",
         {"minecolonies:blacksmith": 3}, {SG + "flax_string": 32, "minecraft:feather": 32}, "metalurgia_precisao")
etapa("fer_distancia", "Blacksmith · Ranged Smithing", "ferreiro", ["armaria_distancia"], [f"{SG}{x}_blueprint" for x in DIST] + [f"{SG}{x}_template" for x in DIST])
E["c3_fer"]["itens"] += [SG + "elytra_blueprint", SG + "elytra_template"]
P["ligas_runicas"]["subtitulo"] = "Tyrian steel and Silent Gear elytra"

PM = "productivemetalworks:"
pesquisa("arte_moldes", "Casting Arts", "Gear, gem, plate, rod and nugget casts for the foundry", 2, PM + "gear_cast",
         {"minecolonies:blacksmith": 2}, {PM + "fire_clay": 32, "minecraft:gold_ingot": 8}, "fundacao_pm")
etapa("fer_moldes", "Blacksmith · Casting Arts", "ferreiro", ["arte_moldes"], [PM + "*_cast"], sem_vedacao=[PM + "*_cast"])

FA = "forbidden_arcanus:"
prismas = [FA + x for x in ("smelter_prism", "sea_prism", "terrastomp_prism", "whirlwind_prism")]
tirar("fer_hefesto", FA + "apply_modifier_smithing_template")
pesquisa("prismas", "Modifier Prisms", "Prisms of fire, sea, earth and wind, and the modifier template", 3, FA + "smelter_prism",
         {"minecolonies:blacksmith": 3}, {FA + "arcane_crystal": 16, "minecraft:prismarine_crystals": 16}, "metalurgia_arcana", depois_de="maestria_hefesto")
componente("prism_matrix", "Prism Matrix", "cristal", "#9FE3F0", "fer_prismas", "A lattice of arcane crystal that focuses a modifier",
           [FA + "arcane_crystal", FA + "arcane_crystal", "minecraft:glass", "minecraft:glass", "minecraft:amethyst_shard"], 2)
etapa("fer_prismas", "Blacksmith · Modifier Prisms", "ferreiro", ["prismas"], prismas + [FA + "apply_modifier_smithing_template"])
for pr in ("smelter_prism", "sea_prism", "terrastomp_prism", "whirlwind_prism"):
    cfg["receitas_modificadas"].append({
        "id": f"forbidden_arcanus:hephaestus_forge/ritual/{pr}",
        "caminho": f"data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/{pr}.json",
        "_comentario": "Modifier Prisms (nível 3): o ritual do prisma exige a Prism Matrix.",
        "operacoes": [{"op": "anexar", "caminho": "inputs", "valor": {"amount": 1, "ingredient": "@comp:prism_matrix"}}]})
pesquisa("armaria_lendaria", "Legendary Weaponsmithing", "Elemental legendary blades forged with prisms and runefused gems", 4,
         "simplyswords:emberblade", {"minecolonies:blacksmith": 4, "minecolonies:enchanter": 3},
         {FA + "stellarite_piece": 4, "simplyswords:runefused_gem": 4, "minecraft:diamond_block": 2}, "maestria_hefesto",
         extra=("forja_runica", "infusao_espiritos"))
componente("legend_core", "Legend Core", "nucleo", "#E05A3A", "c21_fer", "The heart of a weapon that will be remembered",
           ["simplyswords:runefused_gem", FA + "deorum_ingot", FA + "deorum_ingot", "malum:soul_stained_steel_ingot", "minecraft:diamond"])
etapa("c21_fer", "Blacksmith · Legendary Weaponsmithing", "ferreiro", ["armaria_lendaria"], [])
pesquisa("armas_miticas", "Mythic Arms", "Mjolnir and The Devourer", 5, "simplyswords:mjolnir", {"minecolonies:blacksmith": 5, "minecolonies:enchanter": 4},
         {"minecraft:nether_star": 2, FA + "stellarite_piece": 8, "minecraft:netherite_ingot": 4}, "armaria_lendaria", extra=("artes_vazio",))
componente("mythic_core", "Mythic Core", "nucleo", "#FFD86B", "c22_fer", "Starlight and void, held in a single heart",
           ["@comp:legend_core", "minecraft:nether_star", FA + "stellarite_piece", FA + "stellarite_piece", "malum:void_salts", "malum:void_salts"])
etapa("c22_fer", "Blacksmith · Mythic Arms", "ferreiro", ["armas_miticas"], [])
for r in cfg["rituais_novos"]:
    if r["id"].startswith("simplyswords/"):
        comp = "mythic_core" if r["id"].split("/")[1] in ("mjolnir", "the_devourer") else "legend_core"
        r["json"]["inputs"].append({"amount": 1, "ingredient": f"@comp:{comp}"})

# ============================================================ NATURALISTA
FD, FC, VI = "farmersdelight:", "farm_and_charm:", "vinery:"
pesquisa("pecuaria", "Animal Husbandry", "Chicken coops, troughs, stables, fodder and supply carts", 2, FC + "chicken_coop",
         {"minecolonies:farmer": 2}, {"minecraft:wheat": 64, "minecraft:hay_block": 8}, "fundacao_cozinha")
etapa("nat_pecuaria", "Naturalist · Animal Husbandry", "naturalista", ["pecuaria"],
      so_com_bancada("farm_and_charm", ("chicken_coop", "feeding_trough", "water_trough", "horse_fodder", "cattlegrid", "stable_floor", "supply_cart",
                                        "pet_bowl", "chicken_feed", "cat_food", "plow"))
      + so_com_bancada("farmersdelight", ("horse_feed", "dog_food")))
pesquisa("culinaria", "Gourmet Cuisine", "Feasts: stuffed pumpkin, honey glazed ham, shepherd's pie and roast chicken", 2, FD + "stuffed_pumpkin",
         {"minecolonies:farmer": 2}, {"minecraft:beef": 32, "minecraft:pumpkin": 16}, "fundacao_cozinha", depois_de="pecuaria")
etapa("nat_culinaria", "Naturalist · Gourmet Cuisine", "naturalista", ["culinaria"],
      so_com_bancada("farmersdelight", ("stuffed_pumpkin", "honey_glazed_ham", "shepherds_pie", "roast_chicken"))
      + so_com_bancada("farm_and_charm", ("grandmothers_strawberry_cake",)))
vinhos = ("aegis_wine", "bolvar_wine", "chenet_wine", "clark_wine", "cristel_wine", "eiswein", "glowing_wine", "jellie_wine", "lilitu_wine",
          "magnetic_wine", "mellohi_wine", "noir_wine", "solaris_wine", "stal_wine", "strad_wine", "creepers_crush", "villagers_fright",
          "jo_special_mixture", "chorus_wine", "bottle_mojang_noir")
pesquisa("vinhos_finos", "Fine Wines", "Aged wines with lasting blessings", 3, VI + "aegis_wine", {"minecolonies:farmer": 3},
         {VI + "red_wine": 8, "minecraft:glass_bottle": 32}, "vinicultura")
componente("vintners_yeast", "Vintner's Yeast", "frasco", "#D8B26E", "nat_vinhos", "A starter culture for noble wines",
           [FC + "yeast", FC + "yeast", "minecraft:sugar", "minecraft:sugar", "minecraft:honey_bottle"], 4)
etapa("nat_vinhos", "Naturalist · Fine Wines", "naturalista", ["vinhos_finos"], [VI + x for x in vinhos if VI + x in ix.itens],
      componente_maquina={"id": "vintners_yeast", "quantidade": 1})

# ---- Poções: Apothecary Brewing (2), Vital Flasks e Botanical Brewery (3), Eternal Brews (5)
pesquisa("boticario", "Apothecary Brewing", "The brewing stand, vials, flasks, antidotes and elixirs", 2, "minecraft:brewing_stand",
         {"minecolonies:plantation": 2}, {"minecraft:glass_bottle": 32, "minecraft:nether_wart": 16, "minecraft:blaze_powder": 4}, "fundacao_ervas",
         depois_de="toxicologia")
etapa("nat_boticario", "Naturalist · Apothecary Brewing", "naturalista", ["boticario"],
      itens_avulsos=["minecraft:brewing_stand", BO + "vial", BO + "flask"]
                    + so_com_bancada("sos", ("antidote", "poison_vial")) + so_com_bancada("born_in_chaos_v1", ("transmuting_elixir",)))
fv = P["frascos_vitais"]
fv.update(nivel=3, pai=O + "boticario", pesquisas_extra=[O + "fundacao_neovitae"], edificios={"minecolonies:plantation": 3, "minecolonies:enchanter": 2},
          subtitulo="Neo Vitae alchemy flasks, brewed by the apothecary")
c12 = E.pop("c12_arc")
cfg["etapas"] = [e for e in cfg["etapas"] if e is not c12]
etapa("c12_nat", "Naturalist · Vital Flasks", "naturalista", ["frascos_vitais"], itens_avulsos=["neovitae:alchemy_flask"],
      componente_maquina=c12["componente_maquina"])
for c in cfg["componentes"]:
    if c["id"] == "vital_flask_base":
        c["licencas"] = ["c12_nat"]
pesquisa("cervejaria_botanica", "Botanical Brewery", "Botania's brewery: mana brews in vials and flasks", 3, BO + "brewery",
         {"minecolonies:plantation": 3, "minecolonies:enchanter": 2}, {BO + "mana_powder": 32, "minecraft:nether_wart": 32}, "boticario",
         extra=("manipulacao_mana",), depois_de="frascos_vitais")
etapa("c18_nat", "Naturalist · Botanical Brewery", "naturalista", ["cervejaria_botanica"], itens_avulsos=[BO + "brewery"])
cfg["estacoes_exclusivas"] = [{"bloco": "minecraft:brewing_stand", "licencas": ["nat_boticario"], "_comentario": "só o Naturalista prepara poções"},
                              {"bloco": BO + "brewery", "licencas": ["c18_nat"]}]
rp = P["regalia_praga"]
rp.update(nivel=4, pai=O + "alquimia_natural", edificios={"minecolonies:plantation": 4, "minecolonies:mechanic": 3, "minecolonies:enchanter": 3})
tirar("c5_nat", "toxony:eternal_plague")
pesquisa("pocoes_eternas", "Eternal Brews", "Effects that never end: the Eternal Plague, the Tainted Blood Pendant and incense", 5,
         "toxony:eternal_plague", {"minecolonies:plantation": 5, "minecolonies:enchanter": 3},
         {"minecraft:nether_star": 1, "toxony:toxin": 64, BO + "mana_diamond": 8}, "alquimia_venenosa",
         extra=("cervejaria_botanica", "alquimia_natural_runas"))
componente("eternity_essence", "Essence of Eternity", "frasco", "#7DE0B0", "c19_nat", "A brew that refuses to wear off",
           ["toxony:toxin", "toxony:toxin", "herbalbrews:herbal_infusion", "vital_herbs:compacted_sooth_blossom", "minecraft:ghast_tear"])
etapa("c19_nat", "Naturalist · Eternal Brews", "naturalista", ["pocoes_eternas"], ["toxony:eternal_plague"],
      itens_avulsos=[BO + "blood_pendant", BO + "incense_stick"], componente_maquina={"id": "eternity_essence", "quantidade": 1})
for r in cfg["receitas_modificadas"]:
    if r["id"] == "toxony:alchemical_forge/eternal_plague":
        r["json"] = json.loads(json.dumps(r["json"]).replace("@comp:cursed_toxin", "@comp:eternity_essence"))

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v9")
