"""Migração v10 (26/10/2026): tudo de poção é do Naturalista.
- O suporte de poções e a Botanical Brewery já são exclusivos (v9: estacoes_exclusivas); a mistura automática de poções
  do Create foi desligada no mod (CreatePocoesMixin).
- O caldeirão do Iron's continua dos dois (decisão do usuário), mas cada elixir pede uma erva dos mods do Naturalista e
  parte de uma poção-base que só sai do suporte de poções. As misturas de elixir do Create Wizardry saem.
Idempotente."""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if cfg.get("_v10"):
    raise SystemExit("já migrado (v10)")
cfg["_v10"] = True

ELIXIR = {  # receita do caldeirão -> ingrediente novo (erva/produto do Naturalista)
    "brew_evasion_elixir": ("toxony:whirlsnap", "Whirlsnap do Toxony"),
    "brew_greater_healing_elixir": ("vital_herbs:compacted_sooth_blossom", "Sooth Blossom compactada do Vital Herbs"),
    "brew_oakskin_elixir": ("farmersdelight:tree_bark", "casca de árvore do Farmer's Delight"),
    "brew_greater_oakskin_elixir": ("vital_herbs:compacted_iron_root", "Iron Root compactada do Vital Herbs"),
    "brew_greater_invisibility_elixir": ("toxony:moonlight_hemlock", "Moonlight Hemlock do Toxony"),
}
for nome, (item, texto) in ELIXIR.items():
    rid = f"irons_spellbooks:alchemist_cauldron/{nome}"
    cfg["receitas_modificadas"].append({
        "id": rid, "caminho": f"data/irons_spellbooks/recipe/alchemist_cauldron/{nome}.json",
        "_comentario": f"Poções são do Naturalista: o elixir pede {texto} (e a poção-base vem do suporte de poções, exclusivo dele).",
        "operacoes": [{"op": "definir", "caminho": "input", "valor": {"item": item}}]})
cfg["receitas_removidas"] += [f"irons_spellbooks:mixing/{x}" for x in ("evasion_elixir", "greater_evasion_elixir", "greater_healing_elixir",
                                                                        "greater_invisibility_elixir", "greater_oakskin_elixir", "invisibility_elixir",
                                                                        "oakskin_elixir")] + ["create_wizardry:mixing/netherward_tincture"]
# a mistura de invisibilidade do Create Wizardry (alterada na v7) sai junto
cfg["receitas_modificadas"] = [m for m in cfg["receitas_modificadas"] if m["id"] != "irons_spellbooks:mixing/invisibility_elixir"]
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json migrado para v10")
