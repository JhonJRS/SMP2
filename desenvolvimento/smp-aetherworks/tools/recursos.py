"""Monta src/main/resources do port (1.20.1 Forge -> 1.21.1 NeoForge) a partir de original/src/{main,generated}/resources.

- pastas de dados renomeadas (recipes->recipe, loot_tables->loot_table, tags/items->tags/item ...); data/forge -> data/c
- "forge:" -> "c:" nas tags (tabela FORGE_C para nomes que mudaram), loaders/condições "forge:" -> "neoforge:"
- resultados de receita {"item": id} -> {"id": id} (formato 1.21; as receitas próprias do Aetherworks aceitam os dois)
- predicados de loot (match_tool com encantamento / "tag") no formato 1.21
- avanços de receita (só destravam o livro de receitas) ficam de fora
- tags incorrect_for_*_tool: ferramentas abaixo do Aetherium não colhem os blocos #aetherworks:needs_aetherium_tool
- META-INF/neoforge.mods.toml no lugar do mods.toml
Uso: python -I smp-aetherworks/tools/recursos.py
"""
import json
import re
import shutil
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ORIG = BASE / "original" / "src"
DEST = BASE / "src" / "main" / "resources"

PASTAS = {"recipes": "recipe", "loot_tables": "loot_table", "advancements": "advancement", "structures": "structure",
          "predicates": "predicate", "item_modifiers": "item_modifier", "functions": "function"}
TAGS = {"items": "item", "blocks": "block", "fluids": "fluid", "entity_types": "entity_type", "game_events": "game_event"}
FORGE_C = {  # nomes que mudaram nas tags comuns de 1.21
    "sandstone": "sandstone/blocks", "netherrack": "netherracks", "obsidian": "obsidians", "cobblestone": "cobblestones",
    "stone": "stones", "glass": "glass_blocks", "glass/blue": "dyed/blue", "tools/shields": "tools/shield",
    "armors/helmets": "armors/head",
}


def tag_c(nome):
    return "c:" + FORGE_C.get(nome, nome)


def converter_texto(t, em_assets):
    if em_assets:  # 1.21: faces com "forge_data" fazem o NeoForge recusar o modelo inteiro (bigorna, estação, minério)
        return t.replace('"forge:', '"neoforge:').replace('"forge_data"', '"neoforge_data"')
    t = re.sub(r'"(#?)forge:(fluid|default-item|item/default|mod_loaded|conditions|not|and|or|tag_empty|item_exists|true|false)"',
               lambda m: f'"{m.group(1)}neoforge:{m.group(2)}"', t)
    t = re.sub(r'"(#?)forge:([a-z0-9_/.-]+)"', lambda m: f'"{m.group(1)}{tag_c(m.group(2))}"', t)
    return t


def resultado_id(o):
    """{"item": x, "count": n} -> {"id": x, "count": n} nos resultados (sem nbt)."""
    if isinstance(o, dict) and "item" in o and "nbt" not in o and "tag" not in o:
        o = dict(o)
        o["id"] = o.pop("item")
    return o


def receita(j):
    if "conditions" in j and "neoforge:conditions" not in j:
        j["neoforge:conditions"] = j.pop("conditions")
    for c in j.get("neoforge:conditions", []):
        if "type" in c and c["type"].startswith("forge:"):
            c["type"] = "neoforge:" + c["type"][6:]
    tipo = j.get("type", "")
    if tipo in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting", "minecraft:blasting",
                "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:smithing_transform"):
        if isinstance(j.get("result"), str):
            j["result"] = {"id": j["result"]}
        j["result"] = resultado_id(j["result"])
    elif tipo == "minecraft:stonecutting":
        r = j.get("result")
        j["result"] = {"id": r, "count": j.pop("count", 1)} if isinstance(r, str) else resultado_id(r)
    elif tipo.startswith("embers:") and "output" in j:
        j["output"] = resultado_id(j["output"])
    saida = j.get("output")
    if isinstance(saida, dict) and "CustomPotionEffects" in str(saida.get("nbt", "")):
        j["output"] = gema_1_21(saida)
    return j


def gema_1_21(saida):
    """Gema de poção com efeitos em NBT antigo -> componentes potion_contents + custom_data (gem_color)."""
    nbt = saida["nbt"]
    efeitos = []
    for e in re.findall(r"\{Ambient:(\d)b,Amplifier:(\d+)b,.*?Duration:(\d+),.*?ShowIcon:(\d)b,ShowParticles:(\d)b,\\?\"forge:id\\?\":\\?\"([^\"\\]+)", nbt):
        amb, amp, dur, icon, part, eid = e
        efeitos.append({"id": eid, "amplifier": int(amp), "duration": int(dur), "ambient": amb == "1",
                        "show_icon": icon == "1", "show_particles": part == "1"})
    cor = re.search(r"gem_color:(-?\d+)", nbt)
    comps = {"minecraft:potion_contents": {"custom_effects": efeitos}}
    if cor:
        comps["minecraft:custom_data"] = {"gem_color": int(cor.group(1))}
    return {"id": saida["item"], "count": saida.get("count", 1), "components": comps}


def loot(o):
    """Predicados de item 1.20 -> 1.21 (match_tool)."""
    if isinstance(o, list):
        return [loot(x) for x in o]
    if not isinstance(o, dict):
        return o
    o = {k: loot(v) for k, v in o.items()}
    if o.get("condition") == "minecraft:match_tool" and isinstance(o.get("predicate"), dict):
        p = dict(o["predicate"])
        if "tag" in p:
            p["items"] = "#" + p.pop("tag")
        if "enchantments" in p:
            ench = [{**{k: v for k, v in e.items() if k != "enchantment"}, "enchantments": e["enchantment"]}
                    if "enchantment" in e else e for e in p.pop("enchantments")]
            p.setdefault("predicates", {})["minecraft:enchantments"] = ench
        o["predicate"] = p
    return o


def destino(rel):
    partes = list(rel.parts)
    if partes[0] != "data":
        return Path(*partes)
    if partes[1] == "forge":
        partes[1] = "c"
        if len(partes) > 4 and partes[2] == "tags":
            sub = "/".join(partes[4:])[:-5]
            novo = FORGE_C.get(sub, sub)
            partes = partes[:4] + (novo + ".json").split("/")
    if len(partes) > 2:
        partes[2] = PASTAS.get(partes[2], partes[2])
        if partes[2] == "tags" and len(partes) > 3:
            partes[3] = TAGS.get(partes[3], partes[3])
    return Path(*partes)


def main():
    if DEST.exists():
        shutil.rmtree(DEST)
    n = pulados = 0
    for raiz in (ORIG / "main" / "resources", ORIG / "generated" / "resources"):
        for p in raiz.rglob("*"):
            if not p.is_file():
                continue
            rel = p.relative_to(raiz)
            if rel.parts[0] == ".cache" or rel.as_posix() in ("META-INF/mods.toml", "META-INF/accesstransformer.cfg",
                                                               "assets/aetherworks/models/item/tool.json"):  # modelo órfão (texturas não existem)
                continue
            out = DEST / destino(rel)
            if rel.parts[0] == "assets":  # 1.21: caminhos de recurso só minúsculos (ex.: crossbow_Base.json sumia)
                out = DEST / destino(rel).as_posix().lower()
            if "advancement" in out.parts and "recipes" in out.parts:
                pulados += 1
                continue
            out.parent.mkdir(parents=True, exist_ok=True)
            if p.suffix in (".json", ".mcmeta"):
                t = converter_texto(p.read_text(encoding="utf-8"), rel.parts[0] == "assets")
                if rel.parts[0] == "data" and p.suffix == ".json":
                    j = json.loads(t)
                    if "recipe" in out.parts:
                        j = receita(j)
                    elif "loot_table" in out.parts:
                        j = loot(j)
                    t = json.dumps(j, indent=2, ensure_ascii=False)
                if rel.as_posix() == "pack.mcmeta":
                    j = json.loads(t)
                    j["pack"]["pack_format"] = 34
                    t = json.dumps(j, indent=2)
                out.write_text(t + "\n", encoding="utf-8")
            else:
                shutil.copyfile(p, out)
            n += 1

    # ferramentas abaixo do Aetherium (até netherite) não colhem blocos que pedem Aetherium
    for tier in ("wooden", "stone", "iron", "gold", "diamond", "netherite"):
        p = DEST / "data" / "minecraft" / "tags" / "block" / f"incorrect_for_{tier}_tool.json"
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps({"values": ["#aetherworks:needs_aetherium_tool"]}, indent=2) + "\n", encoding="utf-8")

    (DEST / "META-INF").mkdir(parents=True, exist_ok=True)
    shutil.copyfile(BASE / "original" / "LICENSE.txt", DEST / "META-INF" / "LICENSE_aetherworks.txt")  # GPLv3 vai junto
    (DEST / "META-INF" / "neoforge.mods.toml").write_text('''modLoader="javafml"
loaderVersion="[4,)"
license="GNU GPLv3"
issueTrackerURL="https://github.com/Sirplop/AetherworksRefracted/issues"

[[mods]]
modId="aetherworks"
version="1.21.1-1.1.8-smp"
displayName="Aetherworks Refracted"
logoFile="aw_logo.png"
authors="Sirplop (port to 1.21.1 NeoForge / Embers Re-Ignited for the SMP 2 modpack)"
description=\'\'\'An addon to Embers focused on processing Aetherium into tools and armor.
Unofficial 1.21.1 port of Aetherworks Refracted 1.1.8 (GPLv3), source in smp-aetherworks/.\'\'\'

[[dependencies.aetherworks]]
    modId="neoforge"
    type="required"
    versionRange="[21.1,)"
    ordering="NONE"
    side="BOTH"
[[dependencies.aetherworks]]
    modId="minecraft"
    type="required"
    versionRange="[1.21.1,1.21.2)"
    ordering="NONE"
    side="BOTH"
[[dependencies.aetherworks]]
    modId="embers"
    type="required"
    versionRange="*"
    ordering="AFTER"
    side="BOTH"
[[dependencies.aetherworks]]
    modId="curios"
    type="optional"
    versionRange="*"
    ordering="NONE"
    side="BOTH"
''', encoding="utf-8")
    print(f"{n} arquivos em {DEST} ({pulados} avanços de receita fora)")


if __name__ == "__main__":
    main()
