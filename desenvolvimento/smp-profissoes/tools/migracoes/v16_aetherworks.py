"""Migração v16 (27/10/2026): Aetherworks Refracted (port 1.21.1 em smp-aetherworks/) na linha Embers do Engenheiro.
Roda uma vez. Depois desta, rode tools/migracoes/corrigir_adiantados.py.

  n3 Aether Refraction  (pai Ember Processing)         prisma, amplificador, matriz, aetheriômetro, amálgama, suevita
  n4 Aetherium Forging  (pai Alchemy & Artifice; cruzada Hephaestus Mastery)  a Forja do Aether e as peças
  n5 Aetherium Artifacts (pai Aetherium Forging; cruzadas Legendary Weaponsmithing e Void Arts)  ferramentas e coroa
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v16"):
    raise SystemExit("já migrado (v16)")
cfg["_v16"] = True
ix = gerar.indexar()

O = "smpprofissoes:oficios/"
P = {p["id"].split("/", 1)[1]: p for p in cfg["pesquisas"]}
PROF = cfg["profissoes"]
AW = lambda *n: ["aetherworks:" + x for x in n]


def existe(*itens):
    for i in itens:
        if i not in ix.itens:
            raise SystemExit(f"item inexistente: {i}")
    return list(itens)


def pesquisa(id_, nome, sub, nivel, icone, edificios, custos, pai, extra=()):
    existe(icone, *custos)
    for x in (pai, *extra):
        if x not in P:
            raise SystemExit(f"pesquisa inexistente: {x}")
    p = {"id": O + id_, "nome": nome, "subtitulo": sub, "nivel": nivel, "icone": icone, "edificios": edificios, "custos": custos, "pai": O + pai}
    if extra:
        p["pesquisas_extra"] = [O + x for x in extra]
    lst = cfg["pesquisas"]
    i = max(k for k, x in enumerate(lst) if x["id"] == O + pai or x.get("pai") == O + pai)
    lst.insert(i + 1, p)
    P[id_] = p


def etapa(lic, nome, pesq, edificios, **extra):
    e = {"licenca": lic, "nome": nome, "profissao": "engenheiro", "pesquisas": [O + pesq], "edificios": dict(edificios), "itens": []}
    e.update(extra)
    cfg["etapas"].append(e)


PROF["engenheiro"]["namespaces"].append("aetherworks")

pesquisa("refracao_aether", "Aether Refraction", "Aetherworks: prisms, moonlight amplifiers and the aetheriometer",
         3, "aetherworks:prism", {"minecolonies:mechanic": 3},
         {"minecraft:amethyst_shard": 32, "embers:ember_crystal": 32}, "processamento_ember")
pesquisa("forja_aether", "Aetherium Forging", "Aetherworks: the Aether Forge, metal former, anvil and tool station",
         4, "aetherworks:aether_forge", {"minecolonies:mechanic": 4, "minecolonies:blacksmith": 3},
         {"aetherworks:aether_amalgam": 16, "minecraft:diamond": 8}, "alquimia_artificios", ("refracao_aether", "arsenal_ember", "maestria_hefesto"))
pesquisa("artefatos_aether", "Aetherium Artifacts", "Aetherworks: aetherium tools, crossbows, the aether shield and crown",
         5, "aetherworks:pickaxe_aether", {"minecolonies:mechanic": 5, "minecolonies:blacksmith": 4, "minecolonies:enchanter": 3},
         {"aetherworks:ingot_aether": 32, "aetherworks:gem_aether": 8, "minecraft:nether_star": 1}, "forja_aether",
         ("armaria_lendaria", "artes_vazio"))

suevita = sorted(i for i in ix.itens if i.startswith("aetherworks:suevite"))
naturais = sorted(i for i in ix.itens if i.startswith(("aetherworks:geode_", "aetherworks:ore_aether", "aetherworks:aether_shard")))
# As máquinas do Aetherworks/Embers só produzem o que quem as colocou pode fabricar (guarda de operador, rodada 5);
# a bancada continua presa à licença. Por isso as receitas de máquina ficam sem selo.
LIVRE = {"sem_vedacao": ["aetherworks:*"]}
etapa("eng_aether_refracao", "Engineer · Aether Refraction", "refracao_aether", {"minecolonies:mechanic": 3},
      itens=existe(*AW("prism", "prism_support", "moonlight_amplifier",
                       "heat_dial", "aetherium_lens", "aether_amalgam", "aspectus_aetherium",
                       "glass_aetherium", "glass_aetherium_borderless", "block_shards_raw"), *suevita, *naturais),
      **LIVRE)
etapa("eng_aether_forja", "Engineer · Aetherium Forging", "forja_aether", {"minecolonies:mechanic": 4, "minecolonies:blacksmith": 3},
      itens=existe(*AW("aether_prism_controller_matrix", "aetheriometer", "focus_crystal", "aether_pearl", "aether_forge", "forge_vent", "forge_heater", "forge_cooler", "forge_anvil", "forge_metal_former",
                       "forge_tool_station", "ingot_aether", "plate_aether", "block_aether", "gem_aether",
                       "tool_rod_crude", "tool_rod", "pickaxe_head_crude", "pickaxe_head", "axe_head_crude", "axe_head",
                       "shovel_head_crude", "shovel_head", "hoe_head_crude", "hoe_head", "crossbow_frame_crude", "crossbow_frame",
                       "crossbow_limbs_crude", "crossbow_limbs", "shield_core_crude", "shield_core",
                       "aether_ember_jar", "aether_ember_cartridge", "aether_ember_bulb", "lexicon", "lexicon_receptacle",
                       "potion_gem", "tuning_cylinder", "volant_calcifier", "agrarian_liners", "aetherial_plating")), **LIVRE)
etapa("eng_aether_artefatos", "Engineer · Aetherium Artifacts", "artefatos_aether",
      {"minecolonies:mechanic": 5, "minecolonies:blacksmith": 4, "minecolonies:enchanter": 3}, resto_de=["aetherworks"], **LIVRE)

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("v16 aplicada: 3 pesquisas Aetherworks;", len(suevita), "blocos de suevita na Refração")
