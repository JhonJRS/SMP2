"""Migração v20 (28/10/2026), rodada 11: Iron's Spells com Iron's Restrictions na progressão do Arcanista.

- Raridade: todos começam em Comum (config). Os aprimoramentos de raridade saem um por pesquisa:
  Incomum = Scroll Studies (n2), Raro = Arcane Mastery (n3), Épico = Enigmatic Arcana (n4), Lendário = Legendary
  Artifacts (n5). Saem no caldeirão do alquimista (máquina; a guarda de operador vale), por isso sem selo.
- Manuscritos (um por escola) em Scroll Studies, com receitas próprias (ids irons_restrictions:<escola>, que impedem as
  receitas automáticas do mod): manuscrito inacabado + runa da escola + materiais que combinam com ela.
- Escolas: a escolha (/escola) e a regra ficam no mod (EscolasMagia). A pesquisa Cross-School Studies (n3) libera o
  Codex of Schools, que dá direito a mais uma escola.
Roda uma vez.
"""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v20"):
    raise SystemExit("já migrado (v20)")
cfg["_v20"] = True
ix = gerar.indexar()
O = "smpprofissoes:oficios/"
E = {e["licenca"]: e for e in cfg["etapas"]}
P = {p["id"].split("/")[-1]: p for p in cfg["pesquisas"]}


def existe(*itens):
    for i in itens:
        if i not in ix.itens:
            raise SystemExit(f"item inexistente: {i}")
    return list(itens)


R = "irons_restrictions:"
cfg["profissoes"]["arcanista"]["namespaces"].append("irons_restrictions")
SEM_SELO = ["irons_restrictions:*"]


def por(lic, *itens):
    e = E[lic]
    e["itens"] = e.get("itens", []) + existe(*itens)
    e["sem_vedacao"] = sorted(set(e.get("sem_vedacao", [])) | set(SEM_SELO))


por("arc_irons", R + "fragment", R + "unfinished_manuscript", R + "common_upgrade")
por("arc_pergaminhos", R + "uncommon_upgrade", R + "manuscript",
    *sorted(i for i in ix.itens if i.startswith(R) and i.endswith("_manuscript") and i != R + "unfinished_manuscript"))
por("arc_maestria", R + "rare_upgrade")
por("arc_enigmatic", R + "epic_upgrade")
por("c16_arc", R + "legendary_upgrade")

# ---- Cross-School Studies (n3) + Codex of Schools
pai = P["estudos_pergaminhos"]
nova = {"id": O + "estudos_entre_escolas", "nome": "Cross-School Studies",
        "subtitulo": "Iron's Spells: the Codex of Schools lets a mage study one more school of magic",
        "nivel": 3, "icone": "irons_spellbooks:arcane_rune", "edificios": {"minecolonies:enchanter": 3, "minecolonies:library": 3},
        "custos": {"irons_spellbooks:arcane_essence": 64, "irons_spellbooks:arcane_ingot": 8, "minecraft:book": 32}, "pai": pai["id"]}
existe(nova["icone"], *nova["custos"])
lst = cfg["pesquisas"]
lst.insert(max(k for k, x in enumerate(lst) if x["id"] == pai["id"] or x.get("pai") == pai["id"]) + 1, nova)
cfg["etapas"].append({"licenca": "arc_escolas", "nome": "Arcanist · Cross-School Studies", "profissao": "arcanista",
                      "pesquisas": [nova["id"]], "edificios": {"minecolonies:enchanter": 3, "minecolonies:library": 3}, "itens": []})
cfg["itens_especiais"].append({"id": "school_codex", "nome": "Codex of Schools", "forma": "runa", "cor": "#7FB8FF",
                               "licencas": ["arc_escolas"],
                               "descricao": "Right-click to study one more school of magic (choose with /escola)"})
existe("irons_spellbooks:rare_ink", "irons_spellbooks:arcane_ingot", "irons_spellbooks:arcane_rune", "minecraft:writable_book")
cfg["receitas_novas"].append({"id": "smpprofissoes:especial/school_codex", "_comentario": "Codex of Schools: mais uma escola de magia.",
                              "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                                       "key": {"R": {"item": "irons_spellbooks:arcane_rune"}, "T": {"item": "irons_spellbooks:rare_ink"},
                                               "I": {"item": "irons_spellbooks:arcane_ingot"}, "B": {"item": "minecraft:writable_book"}},
                                       "pattern": ["RTR", "IBI", "RTR"], "result": {"count": 1, "id": "smpprofissoes:school_codex"}}})

# ---- manuscritos: ERE / FUF / ESE  (E essência arcana, R runa ou foco da escola, U manuscrito inacabado)
ESCOLAS = {  # escola -> (chave do meio: item ou tag, F, S)
    "irons_spellbooks:fire": ("irons_spellbooks:fire_rune", "minecraft:blaze_rod", "embers:ember_crystal"),
    "irons_spellbooks:ice": ("irons_spellbooks:ice_rune", "minecraft:blue_ice", "irons_spellbooks:permafrost_shard"),
    "irons_spellbooks:lightning": ("irons_spellbooks:lightning_rune", "minecraft:lightning_rod", "create:electron_tube"),
    "irons_spellbooks:holy": ("irons_spellbooks:holy_rune", "minecraft:golden_apple", "irons_spellbooks:divine_pearl"),
    "irons_spellbooks:nature": ("irons_spellbooks:nature_rune", "botania:livingwood_log", "botania:mana_pearl"),
    "irons_spellbooks:ender": ("irons_spellbooks:ender_rune", "minecraft:ender_eye", "minecraft:ender_pearl"),
    "irons_spellbooks:evocation": ("irons_spellbooks:evocation_rune", "minecraft:emerald", "irons_spellbooks:arcane_ingot"),
    "irons_spellbooks:blood": ("irons_spellbooks:blood_rune", "minecraft:fermented_spider_eye", "minecraft:ghast_tear"),
    "aces_spell_utils:hydro": ("#aces_spell_utils:hydro_focus", "minecraft:prismarine_crystals", "minecraft:heart_of_the_sea"),
    "crystal_chronicles:prismatic": ("#crystal_chronicles:prismatic_focus", "minecraft:amethyst_cluster", "irons_spellbooks:arcane_ingot"),
    "iss_magicfromtheeast:dune": ("#iss_magicfromtheeast:dune_focus", "minecraft:sand", "irons_spellbooks:arcane_ingot"),
    "iss_magicfromtheeast:spirit": ("#iss_magicfromtheeast:spirit_focus", "malum:soul_stained_steel_ingot", "minecraft:echo_shard"),
    "iss_magicfromtheeast:symmetry": ("#iss_magicfromtheeast:symmetry_focus", "minecraft:breeze_rod", "irons_spellbooks:arcane_ingot"),
    "tunes_n_tomes:melody": ("#tunes_n_tomes:melody_focus", "minecraft:note_block", "minecraft:jukebox"),
}
for escola, (meio, f, s) in ESCOLAS.items():
    existe(f, s, *([] if meio.startswith("#") else [meio]))
    chave_meio = {"tag": meio[1:]} if meio.startswith("#") else {"item": meio}
    # arquivo de dados com o MESMO id que o Iron's Restrictions procuraria (irons_restrictions:<escola>): assim ele não
    # gera a receita automática (4 fragmentos + foco) daquela escola
    cfg["arquivos_dados"] = [a for a in cfg.get("arquivos_dados", []) if a["caminho"] != f"data/irons_restrictions/recipe/{escola.split(':')[1]}.json"]
    cfg["arquivos_dados"].append({
        "caminho": f"data/irons_restrictions/recipe/{escola.split(':')[1]}.json",
        "_comentario": f"Manuscrito da escola {escola} (substitui a receita automática do Iron's Restrictions).",
        "json": {"type": "minecraft:crafting_shaped", "category": "misc",
                 "key": {"E": {"item": "irons_spellbooks:arcane_essence"}, "R": chave_meio, "F": {"item": f}, "S": {"item": s},
                         "U": {"item": "irons_restrictions:unfinished_manuscript"}},
                 "pattern": ["ERE", "FUF", "ESE"],
                 "result": {"count": 1, "id": "irons_restrictions:manuscript", "components": {"irons_restrictions:school_component": escola}}}})

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"v20 aplicada: raridades em 5 etapas, {len(ESCOLAS)} receitas de manuscrito, Cross-School Studies + Codex of Schools")
