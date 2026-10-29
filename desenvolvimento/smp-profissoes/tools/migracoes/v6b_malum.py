"""Migração v6b (25/10/2026): componentes de máquina das etapas do Malum (Spirit Altar e Runic Workbench não sabem quem
é o jogador; o componente, feito na bancada só com a licença, garante a pesquisa). Idempotente."""
import json
from pathlib import Path

SRC = Path(__file__).resolve().parents[2] / "progressao.json"
cfg = json.loads(SRC.read_text(encoding="utf-8"))
ids = {c["id"] for c in cfg["componentes"]}
if "spirit_binding" in ids:
    raise SystemExit("já migrado (v6b)")

NOVOS = [
    ("spirit_binding", "Spirit Binding", "frasco", "#9B6BE8", "arc_malum_infusao", 4, "Soulstone resin that steadies spirit infusions",
     ["malum:refined_soulstone", "malum:refined_soulstone", "malum:hex_ash", "malum:arcane_spirit"]),
    ("runic_slate", "Runic Slate", "runa", "#6E6A86", "arc_malum_rituais", 2, "Carved rock ready for runeworking",
     ["malum:tainted_rock", "malum:tainted_rock", "malum:twisted_rock", "malum:twisted_rock", "malum:hex_ash"]),
    ("crucible_primer", "Crucible Primer", "cristal", "#E0703A", "arc_malum_cadinho", 2, "Hallowed alloy that wakes a spirit crucible",
     ["malum:hallowed_gold_nugget", "malum:hallowed_gold_nugget", "malum:hallowed_gold_nugget", "malum:hallowed_gold_nugget",
      "malum:infernal_spirit", "malum:infernal_spirit", "malum:refined_soulstone"]),
    ("void_shard", "Void Shard", "cristal", "#3A2E5C", "c14_arc", 1, "A splinter of the weeping well's emptiness",
     ["malum:void_salts", "malum:void_salts", "malum:null_slate", "forbidden_arcanus:arcane_crystal_dust", "minecraft:netherite_scrap"]),
]
E = {e["licenca"]: e for e in cfg["etapas"]}
for cid, nome, forma, cor, lic, qtd, desc, ings in NOVOS:
    cfg["componentes"].append({"id": cid, "nome": nome, "forma": forma, "cor": cor, "licencas": [lic], "quantidade": qtd,
                               "descricao": desc, "ingredientes": ings})
    E[lic]["componente_maquina"] = {"id": cid, "quantidade": 1}
SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("progressao.json: componentes do Malum adicionados")
