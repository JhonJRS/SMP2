"""Migração v26 (29/10/2026): análise das magias por escola (tools/magias_por_escola.py → MAGIAS_POR_ESCOLA.md).
- Cosmic, Radiance e Shadow (Hazen 'n Stuff, escolas do hazentouvelib) tinham magias mas nenhuma receita nossa: o Iron's
  Restrictions criava a automática (4 fragmentos + foco). Ganham receita no mesmo padrão das outras (ERE / FUF / ESE).
- Void (Iron's), Dune (Magic from the East), Ritual e Technomancy (Ace's Spell Utils) não têm NENHUMA magia: o manuscrito
  sairia vazio. A receita com o id que o Iron's Restrictions procura passa a pedir uma Barreira (impossível em
  sobrevivência), o que também impede a receita automática. O mod tira essas escolas das listas de escolha (temMagias).
Roda uma vez."""
import json
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SRC = RAIZ / "progressao.json"
sys.path.insert(0, str(RAIZ / "tools"))
import gerar  # noqa: E402

cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v26"):
    raise SystemExit("já migrado (v26)")
cfg["_v26"] = True
ix = gerar.indexar()

COM_MAGIAS = {  # escola -> (runa da escola, F, S)
    "hazentouvelib:cosmic": ("hazentouvelib:cosmic_rune", "minecraft:glowstone", "irons_spellbooks:arcane_ingot"),
    "hazentouvelib:radiance": ("hazentouvelib:radiance_rune", "minecraft:sea_lantern", "irons_spellbooks:divine_pearl"),
    "hazentouvelib:shadow": ("hazentouvelib:shadow_rune", "minecraft:phantom_membrane", "minecraft:echo_shard"),
}
VAZIAS = ["irons_spellbooks:void", "iss_magicfromtheeast:dune", "aces_spell_utils:ritual", "aces_spell_utils:technomancy"]


def trocar(escola, receita, comentario):
    caminho = f"data/irons_restrictions/recipe/{escola.split(':')[1]}.json"
    cfg["arquivos_dados"] = [a for a in cfg["arquivos_dados"] if a["caminho"] != caminho]
    cfg["arquivos_dados"].append({"caminho": caminho, "_comentario": comentario, "json": receita})


for escola, (runa, f, s) in COM_MAGIAS.items():
    for i in (runa, f, s):
        if i not in ix.itens:
            raise SystemExit(f"item inexistente: {i}")
    trocar(escola, {"type": "minecraft:crafting_shaped", "category": "misc",
                    "key": {"E": {"item": "irons_spellbooks:arcane_essence"}, "R": {"item": runa}, "F": {"item": f},
                            "S": {"item": s}, "U": {"item": "irons_restrictions:unfinished_manuscript"}},
                    "pattern": ["ERE", "FUF", "ESE"],
                    "result": {"count": 1, "id": "irons_restrictions:manuscript", "components": {"irons_restrictions:school_component": escola}}},
           f"Manuscrito da escola {escola} (substitui a receita automática do Iron's Restrictions).")

for escola in VAZIAS:
    trocar(escola, {"type": "minecraft:crafting_shapeless", "category": "misc",
                    "ingredients": [{"item": "minecraft:barrier"}, {"item": "irons_restrictions:unfinished_manuscript"}],
                    "result": {"count": 1, "id": "irons_restrictions:manuscript", "components": {"irons_restrictions:school_component": escola}}},
           f"Escola {escola} sem nenhuma magia: manuscrito vazio, receita impossível (Barreira) só para barrar a automática.")

SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"v26 aplicada: {len(COM_MAGIAS)} manuscritos novos (Hazen), {len(VAZIAS)} escolas vazias barradas")
