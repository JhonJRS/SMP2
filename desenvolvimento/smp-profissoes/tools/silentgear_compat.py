"""Gera o datapack smp_silentgear_compat: materiais do Silent Gear feitos com itens de outros mods do pack.

Substitui o SilentGear Compat, que não existe para 1.21.1. O Silent Gear 4.2 lê materiais de
data/<ns>/silentgear_materials/*.json, então um datapack basta (sem textura: HIGH/LOW_CONTRAST pinta a textura genérica).
Uso: python -I smp-profissoes/tools/silentgear_compat.py [--checar]
"""
import json
import shutil
import sys
import zipfile
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
PACK = INST / "moonlight-global-datapacks" / "smp_silentgear_compat"
NS = "smpcompat"

# (nome da tag de "blocos incorretos", nível) das tiers que o Silent Gear já registra
TIER = {
    "iron": ("silentgear:incorrect_for_iron_tools", "2"),
    "diamond": ("silentgear:incorrect_for_diamond_tools", "3"),
    "crimson_steel": ("silentgear:incorrect_for_crimson_steel_tools", "4"),
    "tyrian_steel": ("silentgear:incorrect_for_tyrian_steel_tools", "4"),
}


def t(trait, level, ratio=None):
    cond = [] if ratio is None else [{"type": "silentgear:material_ratio", "ratio": ratio}]
    return {"conditions": cond, "level": level, "trait": f"silentgear:{trait}"}


def add(v):
    return {"operation": "ADD", "value": float(v)}


def main_metal(dur, atk, magic, armor, adur, tough, ench, speed, rarity, marmor, tier, traits, atk_speed=0.0):
    tag, nivel = TIER[tier]
    return {
        "armor": float(armor), "armor/helmet": round(armor * 0.15), "armor/chestplate": round(armor * 0.4),
        "armor/leggings": round(armor * 0.3), "armor/boots": round(armor * 0.15),
        "armor_durability": float(adur), "armor_toughness": float(tough), "attack_damage": float(atk),
        "attack_speed": float(atk_speed), "charging_value": 0.8, "draw_speed": 0.0, "durability": float(dur),
        "enchantment_value": float(ench), "harvest_speed": float(speed),
        "harvest_tier": {"incorrect_blocks_for_tool": tag, "level_hint": nivel, "name": tier},
        "magic_armor": float(marmor), "magic_damage": float(magic), "projectile_accuracy": 1.0, "projectile_speed": 1.0,
        "ranged_damage": round(atk / 2, 1), "rarity": float(rarity), "traits": traits,
    }


def tip(dur, atk, magic, rarity, tier, traits):
    tag, nivel = TIER[tier]
    return {"durability": add(dur), "attack_damage": add(atk), "magic_damage": add(magic), "rarity": add(rarity),
            "harvest_tier": {"incorrect_blocks_for_tool": tag, "level_hint": nivel, "name": tier}, "traits": traits}


def rod(speed_mult, traits, magic=0):
    o = {"harvest_speed": {"operation": "MULTIPLY_TOTAL", "value": speed_mult}, "traits": traits}
    if magic:
        o["magic_damage"] = add(magic)
    return o


def material(nome, item, cor, categorias, partes, textura="HIGH_CONTRAST"):
    return {
        "type": "silentgear:simple",
        "parent": "silentgear:empty",
        "crafting": {"can_salvage": True, "categories": categorias, "gear_type_blacklist": [],
                     "ingredient": {"item": item}, "part_substitutes": {}},
        "display": {"color": cor, "main_texture_type": textura, "name": {"text": nome}, "name_prefix": ""},
        "properties": partes,
    }


# Referência do Silent Gear: ferro 250 dur/2 atk; diamante 1561/3; crimson steel 2400/6; tyrian steel 3652/8.
MATERIAIS = {
    # --- Botania (Arcanista): o traço "renew" regenera a durabilidade, como as ferramentas de mana.
    "manasteel": material("Manasteel", "botania:manasteel_ingot", "#FF3C8CE6", ["metal", "advanced"], {
        "silentgear:main": main_metal(900, 2.5, 3, 17, 18, 1, 18, 7, 45, 8, "iron", [t("renew", 1), t("malleable", 2)]),
        "silentgear:rod": rod(0.1, [t("renew", 1, 0.5)]),
        "silentgear:tip": tip(128, 1, 1, 10, "iron", [t("renew", 1)]),
    }),
    "elementium": material("Elementium", "botania:elementium_ingot", "#FFEB7AC8", ["metal", "advanced"], {
        "silentgear:main": main_metal(1200, 3, 4, 19, 24, 2, 22, 9, 60, 10, "diamond", [t("renew", 1), t("light", 2)]),
        "silentgear:rod": rod(0.15, [t("light", 2, 0.5)], magic=1),
        "silentgear:tip": tip(192, 1, 2, 15, "diamond", [t("renew", 1), t("light", 1)]),
    }),
    "terrasteel": material("Terrasteel", "botania:terrasteel_ingot", "#FF4CC93C", ["metal", "endgame"], {
        "silentgear:main": main_metal(3200, 7.5, 7, 25, 40, 10, 24, 17, 95, 12, "tyrian_steel",
                                      [t("renew", 2), t("sturdy", 2), t("holy", 1)], atk_speed=0.1),
        "silentgear:rod": rod(0.25, [t("renew", 2, 0.5), t("flexible", 2)], magic=2),
        "silentgear:tip": tip(512, 3, 3, 25, "tyrian_steel", [t("renew", 2)]),
    }),
    "mana_diamond": material("Mana Diamond", "botania:mana_diamond", "#FF4FE3F0", ["gem", "advanced"], {
        "silentgear:main": main_metal(1400, 3, 4, 20, 30, 6, 16, 8, 75, 8, "diamond", [t("lustrous", 1), t("renew", 1)]),
        "silentgear:tip": tip(256, 2, 2, 20, "diamond", [t("lustrous", 2), t("renew", 1)]),
    }),
    "dragonstone": material("Dragonstone", "botania:dragonstone", "#FFE84FB8", ["gem", "endgame"], {
        "silentgear:main": main_metal(2000, 5, 6, 22, 36, 8, 20, 12, 85, 10, "crimson_steel", [t("lustrous", 2), t("renew", 1), t("mighty", 1)]),
        "silentgear:tip": tip(384, 2, 3, 25, "crimson_steel", [t("lustrous", 2), t("mighty", 1)]),
    }),
    "livingwood": material("Livingwood", "botania:livingwood_twig", "#FF6B4A2E", ["organic", "wood"], {
        "silentgear:rod": rod(0.05, [t("flexible", 3), t("renew", 1, 0.5)]),
    }, textura="LOW_CONTRAST"),
    "dreamwood": material("Dreamwood", "botania:dreamwood_twig", "#FFD8E4E8", ["organic", "wood"], {
        "silentgear:rod": rod(0.1, [t("flexible", 3), t("renew", 1, 0.5)], magic=2),
    }, textura="LOW_CONTRAST"),
    "manaweave": material("Manaweave", "botania:manaweave_cloth", "#FF6FB7E8", ["organic", "cloth", "intermediate"], {
        "silentgear:main": {"armor": 8.0, "armor/helmet": 1, "armor/chestplate": 3, "armor/leggings": 3, "armor/boots": 1,
                            "armor_durability": 18.0, "charging_value": 0.9, "durability": 0.0, "enchantment_value": 20.0,
                            "magic_armor": 18.0, "rarity": 55.0, "traits": [t("renew", 1)]},
        "silentgear:lining": {"magic_armor": add(4), "traits": [t("flexible", 4), t("renew", 1)]},
        "silentgear:grip": {"harvest_speed": {"operation": "MULTIPLY_BASE", "value": 0.15}, "traits": [t("accelerate", 1)]},
    }, textura="LOW_CONTRAST"),
    # --- Malum (Arcanista)
    "soul_stained_steel": material("Soul Stained Steel", "malum:soul_stained_steel_ingot", "#FF7B4FB3", ["metal", "advanced"], {
        "silentgear:main": main_metal(1600, 5, 4, 20, 30, 4, 16, 10, 70, 9, "diamond", [t("jagged", 2), t("cursed", 1)]),
        "silentgear:rod": rod(0.15, [t("jagged", 1, 0.5)]),
        "silentgear:tip": tip(256, 2, 1, 15, "diamond", [t("jagged", 2)]),
    }),
    "hallowed_gold": material("Hallowed Gold", "malum:hallowed_gold_ingot", "#FFF2D16B", ["metal", "advanced"], {
        "silentgear:main": main_metal(400, 1, 6, 13, 12, 0, 28, 14, 60, 12, "iron", [t("holy", 2), t("soft", 1)]),
        "silentgear:rod": rod(0.2, [t("holy", 1, 0.5)], magic=2),
        "silentgear:tip": tip(64, 0, 3, 15, "iron", [t("holy", 1)]),
    }),
    # --- Forbidden Arcanus (Ferreiro)
    "deorum": material("Deorum", "forbidden_arcanus:deorum_ingot", "#FFF5B342", ["metal", "advanced"], {
        "silentgear:main": main_metal(700, 2, 5, 16, 16, 1, 26, 13, 65, 10, "diamond", [t("fortunate", 1), t("lucky", 1)]),
        "silentgear:rod": rod(0.2, [t("lucky", 1, 0.5)], magic=1),
        "silentgear:tip": tip(128, 1, 2, 15, "diamond", [t("fortunate", 1)]),
    }),
    # --- Embers (Engenheiro)
    "dawnstone": material("Dawnstone", "embers:dawnstone_ingot", "#FFF0A040", ["metal", "advanced"], {
        "silentgear:main": main_metal(1300, 4, 2, 18, 24, 3, 14, 12, 60, 6, "diamond", [t("magmatic", 1), t("heat_resistant", 1)]),
        "silentgear:rod": rod(0.2, [t("heat_resistant", 1, 0.5)]),
        "silentgear:tip": tip(224, 2, 0, 15, "diamond", [t("fiery", 1)]),
    }),
    # --- Iron's Spells (Arcanista)
    "mithril": material("Mithril", "irons_spellbooks:mithril_ingot", "#FF9FD6E8", ["metal", "endgame"], {
        "silentgear:main": main_metal(2200, 4, 8, 22, 34, 6, 30, 14, 90, 14, "crimson_steel", [t("light", 3), t("malleable", 2)]),
        "silentgear:rod": rod(0.2, [t("light", 2, 0.5)], magic=3),
        "silentgear:tip": tip(320, 1, 4, 25, "crimson_steel", [t("light", 2)]),
    }),
    # --- Toxony (Naturalista)
    "silver_steel": material("Silver Steel", "toxony:silver_steel_ingot", "#FFC8D2DC", ["metal", "advanced"], {
        "silentgear:main": main_metal(1100, 4, 2, 18, 22, 2, 18, 9, 55, 7, "diamond", [t("holy", 1), t("venom", 1)]),
        "silentgear:tip": tip(192, 2, 1, 15, "diamond", [t("holy", 1)]),
    }),
    # --- Create: Deep Dark (Engenheiro)
    "echo": material("Echo Alloy", "create_deep_dark:echo_ingot", "#FF0F5C66", ["metal", "endgame"], {
        "silentgear:main": main_metal(2600, 6, 5, 23, 36, 8, 18, 15, 90, 10, "crimson_steel", [t("void_ward", 1), t("sturdy", 1)]),
        "silentgear:tip": tip(384, 2, 2, 20, "crimson_steel", [t("void_ward", 1)]),
    }),
}


def checar():
    """Confere itens, traços e tags contra os jars instalados."""
    itens, tracos, tags_bloco = set(), set(), set()
    for jar in (INST / "mods").glob("*.jar"):
        try:
            with zipfile.ZipFile(jar) as zf:
                for n in zf.namelist():
                    p = n.split("/")
                    if len(p) >= 4 and p[0] == "assets" and p[2] == "models" and p[3] == "item" and n.endswith(".json"):
                        itens.add(f"{p[1]}:{'/'.join(p[4:])[:-5]}")
                    elif len(p) >= 3 and p[0] == "data" and p[2] == "silentgear_traits" and n.endswith(".json"):
                        tracos.add(f"{p[1]}:{'/'.join(p[3:])[:-5]}")
                    elif len(p) >= 5 and p[0] == "data" and p[2:4] == ["tags", "block"] and n.endswith(".json"):
                        tags_bloco.add(f"{p[1]}:{'/'.join(p[4:])[:-5]}")
        except zipfile.BadZipFile:
            pass
    erros = []
    for mid, m in MATERIAIS.items():
        if m["crafting"]["ingredient"]["item"] not in itens:
            erros.append(f"{mid}: item {m['crafting']['ingredient']['item']} não instalado")
        for parte, props in m["properties"].items():
            for tr in props.get("traits", []):
                if tr["trait"] not in tracos:
                    erros.append(f"{mid}/{parte}: traço {tr['trait']} não existe")
            ht = props.get("harvest_tier")
            if ht and ht["incorrect_blocks_for_tool"] not in tags_bloco:
                erros.append(f"{mid}/{parte}: tag {ht['incorrect_blocks_for_tool']} não existe")
    return erros


def main():
    erros = checar()
    for e in erros:
        print("ERRO:", e)
    if erros:
        sys.exit(1)
    print(f"OK: {len(MATERIAIS)} materiais conferidos (itens, traços e tiers).")
    if "--checar" in sys.argv:
        return
    if PACK.exists():
        shutil.rmtree(PACK)
    (PACK / "data" / NS / "silentgear_materials").mkdir(parents=True)
    (PACK / "pack.mcmeta").write_text(json.dumps({"pack": {"pack_format": 48, "description":
        "SMP 2 - Silent Gear materials from other mods (GENERATED by smp-profissoes/tools/silentgear_compat.py)"}}, indent=2), encoding="utf-8")
    for mid, m in MATERIAIS.items():
        (PACK / "data" / NS / "silentgear_materials" / f"{mid}.json").write_text(json.dumps(m, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Escrito: {PACK}")


if __name__ == "__main__":
    main()
