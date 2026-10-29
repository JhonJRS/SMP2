"""Migração v7 (25/10/2026): receitas e balanceamento da lista de pendências.
Uso: python -I smp-profissoes/tools/migracoes/v7_receitas.py   (idempotente)
"""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v7"):
    raise SystemExit("já migrado (v7)")
cfg["_v7"] = True

mods = cfg["receitas_modificadas"]
rem = cfg["receitas_removidas"]
ITEM = lambda i: {"item": i}


def modificar(rid, caminho, comentario, ops):
    mods.append({"id": rid, "caminho": caminho, "_comentario": comentario, "operacoes": ops})


def receita(rid):
    ns, p = rid.split(":", 1)
    return f"data/{ns}/recipe/{p}.json"


# ---- upgrades de nível mágico (Iron's Restrictions): sem receita; só admin/GM entrega
rem += [f"irons_restrictions:alchemist_cauldron/soak_{r}_upgrade" for r in ("common", "uncommon", "rare", "epic", "legendary")]

# ---- Teleposer e Sigil of Teleposition: sem receita
rem += ["neovitae:teleposer", "neovitae:array/teleposition_sigil"]
cfg["sem_receita"] = sorted(set(cfg.get("sem_receita", [])) | {"neovitae:teleposer", "neovitae:sigil_teleposition"})

# ---- Echo Shard: nenhuma receita produz (só o Deep Dark); tira o subproduto das montagens do Create Wizardry
for rid in ("irons_spellbooks:sequenced_assembly/ruined_book", "irons_spellbooks:sequenced_assembly/eldritch_manuscript"):
    modificar(rid, receita(rid), "Echo Shard só vem do Deep Dark: removido dos subprodutos.",
              [{"op": "remover_onde", "caminho": "results", "contem": "minecraft:echo_shard"}])

# ---- Arcane Essence pelo Create Wizardry: só a mistura superaquecida (e rende menos); somem as rotas fáceis com mana
rem += ["irons_spellbooks:filling/arcane_essence", "irons_spellbooks:filling/arcane_essence_cinder_flour_filling",
        "irons_spellbooks:filling/arcane_essence_sugar_filling", "irons_spellbooks:filling/arcane_essence_flour_filling"]
modificar("irons_spellbooks:mixing/arcane_essence", receita("irons_spellbooks:mixing/arcane_essence"),
          "Arcane Essence no Create: superaquecida, 2 por mistura (era 4); as rotas de mana + açúcar/farinha foram removidas.",
          [{"op": "definir", "caminho": "results.0.count", "valor": 2}])

# ---- Blaze Caster (Create Wizardry) integrado ao Hazen 'n Stuff: passo extra com Pyrium
modificar("create_wizardry:sequenced_assembly/blaze_caster", receita("create_wizardry:sequenced_assembly/blaze_caster"),
          "Blaze Caster: passo extra com Pyrium Nugget do Hazen 'n Stuff (integração Wizardry + Hazen).",
          [{"op": "anexar", "caminho": "sequence", "valor": {
              "type": "create:deploying",
              "ingredients": [ITEM("create_wizardry:incomplete_blaze_caster"), ITEM("hazennstuff:pyrium_nugget")],
              "results": [{"id": "create_wizardry:incomplete_blaze_caster"}]}}])

# ---- Mana Siphon: puxa mana, então pede Manasteel do Botania
modificar("create_wizardry:mana_siphon", receita("create_wizardry:mana_siphon"),
          "Mana Siphon: linha de Manasteel (Botania) na crafting mecânica.",
          [{"op": "definir", "caminho": "pattern.2", "valor": "MCM"}, {"op": "definir", "caminho": "key.M", "valor": ITEM("botania:manasteel_ingot")}])

# ---- Quantum Catcher: ritual mais caro (forja tier 2, runas de Ender do Iron's e olhos de Ender)
modificar("forbidden_arcanus:hephaestus_forge/ritual/quantum_catcher",
          "data/forbidden_arcanus/forbidden_arcanus/hephaestus_forge/ritual/quantum_catcher.json",
          "Quantum Catcher: forja tier 2, 2 Ender Runes (Arcanista) + 2 Eyes of Ender, mais essências.",
          [{"op": "anexar", "caminho": "inputs", "valor": {"amount": 2, "ingredient": ITEM("irons_spellbooks:ender_rune")}},
           {"op": "anexar", "caminho": "inputs", "valor": {"amount": 2, "ingredient": ITEM("minecraft:ender_eye")}},
           {"op": "definir", "caminho": "forge_tier", "valor": 2},
           {"op": "definir", "caminho": "essences", "valor": {"aureal": 600, "blood": 2400, "experience": 400, "souls": 10}}])

# ---- invisibilidade usa Timeless Slurry
modificar("irons_spellbooks:alchemist_cauldron/brew_invisibility_elixir", receita("irons_spellbooks:alchemist_cauldron/brew_invisibility_elixir"),
          "Invisibility Elixir: Timeless Slurry no lugar da Shriving Stone.",
          [{"op": "definir", "caminho": "input", "valor": ITEM("irons_spellbooks:timeless_slurry")}])
modificar("irons_spellbooks:mixing/invisibility_elixir", receita("irons_spellbooks:mixing/invisibility_elixir"),
          "Invisibility Elixir no Create: também pede Timeless Slurry.",
          [{"op": "anexar", "caminho": "ingredients", "valor": {"type": "neoforge:single", "amount": 250, "fluid": "irons_spellbooks:timeless_slurry"}}])
modificar("botania:invisibility_cloak", receita("botania:invisibility_cloak"),
          "Cloak of Invisibility: Timeless Slurry (Iron's) no centro.",
          [{"op": "definir", "caminho": "pattern.1", "valor": "GTG"}, {"op": "definir", "caminho": "key.T", "valor": ITEM("irons_spellbooks:timeless_slurry")}])

# ---- tintas do Iron's integradas a outros mods (cada raridade pede um ingrediente de outro mod)
for raridade, item, motivo in (("uncommon", "minecraft:glow_ink_sac", "tinta brilhante de lula"),
                               ("rare", "toxony:nightshade", "Nightshade do Toxony (Naturalista)"),
                               ("epic", "malum:arcane_spirit", "Arcane Spirit do Malum")):
    rid = f"irons_spellbooks:alchemist_cauldron/brew_{raridade}_ink"
    modificar(rid, receita(rid), f"{raridade.title()} Ink: {motivo}.", [{"op": "definir", "caminho": "input", "valor": ITEM(item)}])
rem += [f"irons_spellbooks:mixing/{r}_ink{s}" for r in ("uncommon", "rare", "epic", "legendary") for s in ("", "_mana_recipe")]
rem += [f"minecraft:mixing/cei_{r}_ink_recipe" for r in ("rare", "epic", "legendary")]
for p in cfg["pesquisas"]:
    if p["id"].endswith("/tintas_herbais"):
        p["subtitulo"] = "Legendary ink brewed with moonlight hemlock essence"
        p["custos"] = {"irons_spellbooks:arcane_essence": 64, "toxony:moonlight_hemlock": 16, "minecraft:ghast_tear": 4}

# ---- combustíveis: completa a integração Create Diesel Generators <-> Immersive Engineering/Petroleum
cfg["arquivos_dados"] = cfg.get("arquivos_dados", []) + [
    {"caminho": "data/smpprofissoes/recipe/generator_fuel/gasoline.json",
     "json": {"type": "immersiveengineering:generator_fuel", "burnTime": 300, "fluidTag": "c:gasoline"}},
    {"caminho": "data/smpprofissoes/recipe/generator_fuel/plantoil.json",
     "json": {"type": "immersiveengineering:generator_fuel", "burnTime": 80, "fluidTag": "c:plantoil"}},
    {"caminho": "data/smpprofissoes/createdieselgenerators/fuel_type/high_power_biodiesel.json",
     "json": {"fluid": "#c:high_power_biodiesel", "sound_pitch": 1, "normal": {"speed": 96.0, "strength": 5120.0, "burn_rate": 0.05},
              "modular": {"speed": 96.0, "strength": 7168.0, "burn_rate": 0.05}, "huge": {"speed": 224.0, "strength": 14336.0, "burn_rate": 0.05},
              "burner_multiplier": 0.55}},
    {"caminho": "data/smpprofissoes/createdieselgenerators/fuel_type/kerosene.json",
     "json": {"fluid": "#c:kerosene", "sound_pitch": 0.9, "normal": {"speed": 96.0, "strength": 5120.0, "burn_rate": 0.05},
              "modular": {"speed": 96.0, "strength": 7168.0, "burn_rate": 0.05}, "huge": {"speed": 224.0, "strength": 14336.0, "burn_rate": 0.05},
              "burner_multiplier": 0.55}},
]

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v7")
