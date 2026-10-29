"""Gera moonlight-global-datapacks/smp_estacoes: cultivos dos mods nas tags de estação do Serene Seasons.
Entradas com required:false (um bloco que suma numa atualização não derruba a tag inteira)."""
import json
import sys
from pathlib import Path

INST = Path(__file__).resolve().parents[2]  # uso: python -I smp-profissoes/tools/estacoes.py
PACK = INST / "moonlight-global-datapacks" / "smp_estacoes"
EST = {
    "spring_crops": [
        "farmersdelight:cabbages", "farmersdelight:onions", "farm_and_charm:lettuce_crop", "farm_and_charm:onion_crop",
        "farm_and_charm:strawberry_crop", "farm_and_charm:oat_crop", "vinery:apple_tree_sapling", "vinery:dark_cherry_sapling",
        "herbalbrews:tea_plant", "minecolonies:cabbage", "minecolonies:onion", "minecolonies:garlic", "enchanted:garlic",
        "enchanted:belladonna", "enchanted:water_artichoke", "vital_herbs:sooth_blossom_plant", "vital_herbs:bleeding_heart_plant",
        "vital_herbs:glow_leaf_plant", "vital_herbs:needle_heart_plant", "toxony:false_berry_bush",
    ],
    "summer_crops": [
        "farmersdelight:tomatoes", "farmersdelight:budding_tomatoes", "farmersdelight:rice", "farmersdelight:rice_panicles",
        "farm_and_charm:tomato_crop", "farm_and_charm:tomato_crop_body", "farm_and_charm:corn_crop", "farm_and_charm:barley_crop",
        "farm_and_charm:strawberry_crop", "vinery:red_grape_bush", "vinery:white_grape_bush", "vinery:jungle_grape_bush_red",
        "vinery:jungle_grape_bush_white", "vinery:savanna_grape_bush_red", "vinery:savanna_grape_bush_white", "vinery:grapevine_stem",
        "herbalbrews:coffee_plant", "herbalbrews:rooibos_plant", "herbalbrews:yerba_mate_plant", "immersiveengineering:hemp",
        "minecolonies:tomato", "minecolonies:corn", "minecolonies:bell_pepper", "minecolonies:eggplant", "minecolonies:rice",
        "vital_herbs:snap_pepper_plant", "vital_herbs:fizz_fruit_plant", "vital_herbs:burst_bud_plant", "vital_herbs:razor_leaf_plant",
        "vital_herbs:crimson_lily_plant", "vital_herbs:tox_kiss_plant", "enchanted:water_artichoke",
    ],
    "autumn_crops": [
        "farmersdelight:cabbages", "farmersdelight:onions", "farm_and_charm:barley_crop", "farm_and_charm:oat_crop",
        "farm_and_charm:onion_crop", "farm_and_charm:lettuce_crop", "vinery:red_grape_bush", "vinery:white_grape_bush",
        "vinery:taiga_grape_bush_red", "vinery:taiga_grape_bush_white", "vinery:grapevine_stem", "herbalbrews:tea_plant",
        "minecolonies:cabbage", "minecolonies:onion", "minecolonies:garlic", "enchanted:mandrake", "enchanted:wolfsbane",
        "enchanted:belladonna", "enchanted:garlic", "vital_herbs:mourning_bloom_plant", "vital_herbs:grim_vine_plant",
        "vital_herbs:iron_root_plant", "toxony:false_berry_bush",
    ],
    "winter_crops": [
        "enchanted:snowbell", "vital_herbs:frost_mint_plant", "vital_herbs:blue_spar_plant", "vinery:taiga_grape_bush_red",
        "vinery:taiga_grape_bush_white",
    ],
    "year_round_crops": [
        "minecolonies:nether_pepper", "enchanted:alder_sapling", "enchanted:hawthorn_sapling", "enchanted:rowan_sapling",
    ],
}
# Rodada 10 (28/10): cultivos que ainda cresciam em qualquer estação
MAIS = {
    "spring_crops": ["farm_and_charm:barley_crop", "minecolonies:peas", "minecolonies:chickpea", "minecolonies:mint", "minecolonies:durum",
                     "silentgear:flax_plant", "silentgear:fluffy_plant", "toxony:bloodroot", "toxony:ocelot_mint", "toxony:water_hemlock",
                     "toxony:whirlsnap", "herbalbrews:yerba_mate_plant", "vital_herbs:burst_bud_plant", "vital_herbs:crimson_lily_plant",
                     "supplementaries:flax", "vinery:apple_leaves", "vinery:dark_cherry_leaves"],
    "summer_crops": ["minecolonies:cotton", "minecolonies:soybean", "minecolonies:chickpea", "minecolonies:mint", "minecolonies:durum",
                     "silentgear:flax_plant", "silentgear:fluffy_plant", "toxony:sunspot", "toxony:nightshade", "toxony:false_berry_bush",
                     "toxony:ocelot_mint", "toxony:water_hemlock", "enchanted:belladonna", "herbalbrews:tea_plant", "vital_herbs:glow_leaf_plant",
                     "vital_herbs:needle_heart_plant", "vinery:apple_leaves", "vinery:dark_cherry_leaves", "vinery:taiga_grape_bush_red",
                     "vinery:taiga_grape_bush_white"],
    "autumn_crops": ["minecolonies:butternut_squash", "minecolonies:durum", "silentgear:fluffy_plant", "toxony:moonlight_hemlock",
                     "toxony:warproot", "toxony:nightshade", "toxony:whirlsnap", "vital_herbs:dried_glow_leaf_plant", "vital_herbs:razor_leaf_plant",
                     "vital_herbs:tox_kiss_plant", "vital_herbs:snap_pepper_plant", "vinery:savanna_grape_bush_red", "vinery:savanna_grape_bush_white",
                     "vinery:apple_leaves", "vinery:dark_cherry_leaves"],
    "winter_crops": ["toxony:coldsnap", "toxony:snow_mint", "enchanted:wolfsbane", "vital_herbs:iron_root_plant"],
    "year_round_crops": ["neovitae:weak_tau", "neovitae:strong_tau", "farmersdelight:brown_mushroom_colony",
                         "farmersdelight:red_mushroom_colony"],
}
for tag, blocos in MAIS.items():
    EST[tag] += blocos

# item da tag (tooltip "fértil em ..."): a semente/muda/fruto que planta o bloco
sys.path.insert(0, str(INST / "smp-profissoes" / "tools"))
import gerar  # noqa: E402
ITENS = gerar.indexar().itens


def itens_do_bloco(bloco):
    ns, nome = bloco.split(":")
    base = nome
    for suf in ("_crop_body", "_crop", "_plant", "_bush", "_stem", "_leaves", "_colony", "_panicles", "s"):
        if base.endswith(suf) and len(base) > len(suf) + 2:
            base = base[: -len(suf)]
            break
    cands = [f"{ns}:{nome}", f"{ns}:{base}_seeds", f"{ns}:{base}_seed", f"{ns}:{nome}_seeds", f"{ns}:{base}"]
    if ns == "vinery" and "grape" in nome:
        cands += [f"vinery:{nome.replace('_bush', '').replace('grape_', '')}_grape", f"vinery:{nome.replace('_bush', '')}"]
    return [c for c in dict.fromkeys(cands) if c in ITENS][:1]


if PACK.exists():
    import shutil
    shutil.rmtree(PACK)
(PACK / "data/sereneseasons/tags/block").mkdir(parents=True)
(PACK / "data/sereneseasons/tags/item").mkdir(parents=True)
(PACK / "pack.mcmeta").write_text(json.dumps({"pack": {"pack_format": 48, "description": "SMP 2 - Serene Seasons: cultivos dos mods por estação"}}, indent=2) + "\n", encoding="utf-8")
sem_item = set()
for tag, blocos in EST.items():
    blocos = list(dict.fromkeys(blocos))
    vals = [{"id": b, "required": False} for b in blocos]
    (PACK / f"data/sereneseasons/tags/block/{tag}.json").write_text(json.dumps({"replace": False, "values": vals}, indent=2) + "\n", encoding="utf-8")
    itens = []
    for b in blocos:
        achado = itens_do_bloco(b)
        itens += achado
        if not achado:
            sem_item.add(b)
    vals = [{"id": i, "required": False} for i in dict.fromkeys(itens)]
    (PACK / f"data/sereneseasons/tags/item/{tag}.json").write_text(json.dumps({"replace": False, "values": vals}, indent=2) + "\n", encoding="utf-8")
print("ok", PACK, f"({sum(len(set(v)) for v in EST.values())} entradas de bloco)")
if sem_item:
    print("sem item de tooltip (só o bloco):", ", ".join(sorted(sem_item)))
