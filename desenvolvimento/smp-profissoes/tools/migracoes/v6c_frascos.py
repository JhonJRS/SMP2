import json
from pathlib import Path
SRC = Path("smp-profissoes/progressao.json")
cfg = json.loads(SRC.read_text(encoding="utf-8"))
if not any(c["id"] == "vital_flask_base" for c in cfg["componentes"]):
    cfg["componentes"].append({"id": "vital_flask_base", "nome": "Vital Flask Base", "forma": "frasco", "cor": "#C24A4A", "licencas": ["c12_arc"],
        "quantidade": 2, "descricao": "Herbal-cured glass for Neo Vitae alchemy flasks",
        "ingredientes": ["minecraft:glass_bottle", "minecraft:glass_bottle", "herbalbrews:herbal_infusion", "herbalbrews:lavender"]})
    for e in cfg["etapas"]:
        if e["licenca"] == "c12_arc":
            e["componente_maquina"] = {"id": "vital_flask_base", "quantidade": 1}
    SRC.write_text(json.dumps(cfg, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("vital_flask_base adicionado")
