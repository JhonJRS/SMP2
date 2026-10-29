"""Confere os assets do port: todo item registrado tem modelo, todo parent/textura citado existe
(no próprio mod, no Embers, no Minecraft ou no NeoForge). Uso: python -I smp-aetherworks/tools/checar_modelos.py"""
import glob
import json
import re
import sys
import zipfile
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
INST = BASE.parent
RES = BASE / "src" / "main" / "resources"
LIB = Path.home() / "curseforge" / "minecraft" / "Install" / "libraries"

existentes = {p.relative_to(RES).as_posix() for p in RES.rglob("*") if p.is_file()}
for jar in [glob.glob(str(INST / "mods" / "embers-reignited-*.jar"))[0],
            str(LIB.parent / "versions" / "1.21.1" / "1.21.1.jar"),
            str(LIB / "net/neoforged/neoforge/21.1.253/neoforge-21.1.253-universal.jar"),
            str(LIB / "net/neoforged/neoforge/21.1.253/neoforge-21.1.253-client.jar")]:
    try:
        existentes |= {n for n in zipfile.ZipFile(jar).namelist() if n.startswith("assets/")}
    except FileNotFoundError:
        pass


def rl(s, padrao_ns="minecraft"):
    ns, _, p = s.partition(":") if ":" in s else (padrao_ns, "", s)
    return ns, p


problemas = []
# itens registrados = chaves item./block. do lang
lang = json.loads((RES / "assets/aetherworks/lang/en_us.json").read_text(encoding="utf-8"))
ids = sorted({k.split(".", 2)[2] for k in lang if k.startswith(("item.aetherworks.", "block.aetherworks.")) and k.count(".") == 2})
SEM_ITEM = {"aether_forge_edge", "aether_gas_block", "aether_gas_impure_block", "aether_gas_painful_block", "alchemic_precursor_block"}
for i in ids:
    if i not in SEM_ITEM and f"assets/aetherworks/models/item/{i}.json" not in existentes:
        problemas.append(f"sem modelo de item: aetherworks:{i}")

for p in sorted(RES.glob("assets/aetherworks/models/**/*.json")):
    t = p.read_text(encoding="utf-8")
    j = json.loads(t)
    nome = p.relative_to(RES).as_posix()
    if '"forge_data"' in t:
        problemas.append(f"{nome}: usa forge_data (1.21 exige neoforge_data; o modelo inteiro some)")
    if "parent" in j and not j["parent"].startswith("builtin/"):
        ns, cam = rl(j["parent"])
        if f"assets/{ns}/models/{cam}.json" not in existentes:
            problemas.append(f"{nome}: parent inexistente {j['parent']}")
    for chave, tex in j.get("textures", {}).items():
        if tex.startswith("#"):
            continue
        ns, cam = rl(tex)
        if f"assets/{ns}/textures/{cam}.png" not in existentes:
            problemas.append(f"{nome}: textura inexistente {tex} ({chave})")
    for sub in ("base", "perspectives"):
        partes = [j[sub]] if sub == "base" and sub in j else list(j.get(sub, {}).values())
        for part in partes:
            if isinstance(part, dict) and "parent" in part:
                ns, cam = rl(part["parent"])
                if f"assets/{ns}/models/{cam}.json" not in existentes:
                    problemas.append(f"{nome}: {sub} parent inexistente {part['parent']}")
    for ov in j.get("overrides", []):
        ns, cam = rl(ov.get("model", ""))
        if f"assets/{ns}/models/{cam}.json" not in existentes:
            problemas.append(f"{nome}: override aponta para modelo inexistente {ov.get('model')}")
    if "loader" in j:
        problemas.append(f"{nome}: loader {j['loader']} (conferir formato 1.21)") if j["loader"] not in (
            "neoforge:separate_transforms", "neoforge:fluid_container", "neoforge:composite", "neoforge:item_layers") else None

for p in sorted(RES.glob("assets/aetherworks/blockstates/*.json")):
    t = p.read_text(encoding="utf-8")
    if "forge_marker" in t:
        continue
    for m in re.findall(r'"model"\s*:\s*"([^"]+)"', t):
        ns, cam = rl(m)
        if f"assets/{ns}/models/{cam}.json" not in existentes:
            problemas.append(f"{p.name}: modelo inexistente {m}")

print(f"{len(ids)} itens; {len(problemas)} problema(s)")
for x in problemas:
    print("  " + x)
sys.exit(1 if problemas else 0)
