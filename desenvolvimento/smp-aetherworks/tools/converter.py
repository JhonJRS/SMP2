"""Passo mecânico do port (roda UMA vez): copia original/src/main/java para src/main/java trocando o que é
renomeação pura de Forge 1.20.1 -> NeoForge 1.21.1. O resto (capabilities, rede, receitas, componentes) é à mão.
Uso: python -I smp-aetherworks/tools/converter.py
"""
import re
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ORIG = BASE / "original" / "src" / "main" / "java"
DEST = BASE / "src" / "main" / "java"
# datagen: só ficam as classes cujas constantes o jogo usa (viram classes simples à mão)
DATAGEN_MANTER = {"AWItemProperties", "AWSounds", "AWBiomeTags", "AWBlockTags", "AWFluidTags", "AWDamageTypes", "AWItemTags"}

PACOTES = [
    ("net.minecraftforge.api.distmarker", "net.neoforged.api.distmarker"),
    ("net.minecraftforge.eventbus.api", "net.neoforged.bus.api"),
    ("net.minecraftforge.fml", "net.neoforged.fml"),
    ("net.minecraftforge", "net.neoforged.neoforge"),
]
REGISTROS = {  # ForgeRegistries.X -> (chave em Registries, registro em BuiltInRegistries)
    "ITEMS": "ITEM", "BLOCKS": "BLOCK", "FLUIDS": "FLUID", "PARTICLE_TYPES": "PARTICLE_TYPE", "MOB_EFFECTS": "MOB_EFFECT",
    "BLOCK_ENTITY_TYPES": "BLOCK_ENTITY_TYPE", "ENCHANTMENTS": "ENCHANTMENT", "ATTRIBUTES": "ATTRIBUTE",
    "SOUND_EVENTS": "SOUND_EVENT", "RECIPE_TYPES": "RECIPE_TYPE", "RECIPE_SERIALIZERS": "RECIPE_SERIALIZER",
    "ENTITY_TYPES": "ENTITY_TYPE", "POTIONS": "POTION", "BIOMES": "BIOME", "FEATURES": "FEATURE", "MENU_TYPES": "MENU",
}
SIMPLES = [
    (r"\bMinecraftForge\.EVENT_BUS\b", "NeoForge.EVENT_BUS"),
    (r"\bMinecraftForge\b", "NeoForge"),
    (r"\bForgeFlowingFluid\b", "BaseFlowingFluid"),
    (r"\bForgeMod\b", "NeoForgeMod"),
    (r"\bForgeHooks\b", "CommonHooks"),
    (r"\bForgeConfigSpec\b", "ModConfigSpec"),
    (r"\bForgeSpawnEggItem\b", "DeferredSpawnEggItem"),
    (r"\bBlockPathTypes\b", "PathType"),
    (r"\bProperties\.copy\(", "Properties.ofFullCopy("),
    (r"\bMod\.EventBusSubscriber\b", "EventBusSubscriber"),
    (r"org\.jetbrains\.annotations\.NotNull", "javax.annotation.Nonnull"),
    (r"org\.jetbrains\.annotations\.Nullable", "javax.annotation.Nullable"),
    (r"@NotNull\b", "@Nonnull"),
    (r"net\.minecraft\.world\.level\.pathfinder\.BlockPathTypes", "net.minecraft.world.level.pathfinder.PathType"),
]
IMPORTS_EXTRA = {  # usado no texto -> import necessário
    "NeoForge.": "net.neoforged.neoforge.common.NeoForge",
    "BuiltInRegistries.": "net.minecraft.core.registries.BuiltInRegistries",
    "Registries.": "net.minecraft.core.registries.Registries",
    "NeoForgeRegistries.": "net.neoforged.neoforge.registries.NeoForgeRegistries",
    "DeferredHolder<": "net.neoforged.neoforge.registries.DeferredHolder",
    "@EventBusSubscriber": "net.neoforged.fml.common.EventBusSubscriber",
    "BaseFlowingFluid": "net.neoforged.neoforge.fluids.BaseFlowingFluid",
}
IMPORTS_TROCA = {
    "net.neoforged.neoforge.registries.RegistryObject": "net.neoforged.neoforge.registries.DeferredHolder",
    "net.neoforged.neoforge.registries.ForgeRegistries": None,
    "net.neoforged.neoforge.common.MinecraftForge": "net.neoforged.neoforge.common.NeoForge",
    "net.neoforged.neoforge.fluids.ForgeFlowingFluid": "net.neoforged.neoforge.fluids.BaseFlowingFluid",
    "net.neoforged.neoforge.common.ForgeMod": "net.neoforged.neoforge.common.NeoForgeMod",
    "net.neoforged.neoforge.common.ForgeHooks": "net.neoforged.neoforge.common.CommonHooks",
    "net.neoforged.neoforge.common.ForgeConfigSpec": "net.neoforged.neoforge.common.ModConfigSpec",
}


def args_de(t, i):
    """t[i] == '(' -> (lista de argumentos de nível 0, índice depois do ')')."""
    nivel, ini, args = 0, i + 1, []
    j = i
    while True:
        c = t[j]
        if c in "([{":
            nivel += 1
        elif c in ")]}":
            nivel -= 1
            if nivel == 0:
                args.append(t[ini:j])
                return args, j + 1
        elif c == "," and nivel == 1:
            args.append(t[ini:j])
            ini = j + 1
        elif c == '"':
            j = t.index('"', j + 1)
            while t[j - 1] == "\\":
                j = t.index('"', j + 1)
        j += 1


def resource_location(t):
    saida, pos = [], 0
    for m in re.finditer(r"new ResourceLocation\(", t):
        if m.start() < pos:
            continue
        args, fim = args_de(t, m.end() - 1)
        args = [a.strip() for a in args]
        metodo = "fromNamespaceAndPath" if len(args) == 2 else "parse"
        saida.append(t[pos:m.start()] + f"ResourceLocation.{metodo}(" + ", ".join(args) + ")")
        pos = fim
    return "".join(saida) + t[pos:]


def converter(t):
    for velho, novo in PACOTES:
        t = t.replace(velho + ".", novo + ".")
    for velho, novo in IMPORTS_TROCA.items():
        t = re.sub(r"^import " + re.escape(velho) + r";\n", "" if novo is None else f"import {novo};\n", t, flags=re.M)
    for a, b in SIMPLES:
        t = re.sub(a, b, t)
    # registros
    t = re.sub(r"DeferredRegister\.create\(ForgeRegistries\.Keys\.FLUID_TYPES", "DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES", t)
    t = re.sub(r"DeferredRegister\.create\(ForgeRegistries\.(\w+)", lambda m: f"DeferredRegister.create(Registries.{REGISTROS[m.group(1)]}", t)
    linhas = []
    for l in t.split("\n"):
        if "ForgeRegistries." in l:
            l = re.sub(r"ForgeRegistries\.(\w+)", lambda m: f"BuiltInRegistries.{REGISTROS.get(m.group(1), m.group(1))}", l)
            l = l.replace(".getValue(", ".get(")
        linhas.append(l)
    t = "\n".join(linhas)
    t = re.sub(r"RegistryObject<(\w+)<(.+?)>>", r"DeferredHolder<\1<?>, \1<\2>>", t)
    t = re.sub(r"RegistryObject<(\w+)>", r"DeferredHolder<\1, \1>", t)
    t = re.sub(r"RegistryObject<\? extends (\w+)>", r"DeferredHolder<\1, ? extends \1>", t)
    t = resource_location(t)
    # imports que passaram a ser necessários
    pacote = re.search(r"^package [\w.]+;\n", t, re.M)
    faltam = [imp for uso, imp in IMPORTS_EXTRA.items() if uso in t and f"import {imp};" not in t]
    if faltam and pacote:
        t = t[:pacote.end()] + "".join(f"import {i};\n" for i in faltam) + t[pacote.end():]
    return t


def main():
    if DEST.exists() and "--forcar" not in sys.argv:
        sys.exit(f"{DEST} já existe (o port continua à mão). Use --forcar para recomeçar do zero.")
    n = 0
    for f in ORIG.rglob("*.java"):
        rel = f.relative_to(ORIG)
        if "datagen" in rel.parts and f.stem not in DATAGEN_MANTER:
            continue
        out = DEST / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(converter(f.read_text(encoding="utf-8")), encoding="utf-8")
        n += 1
    print(n, "arquivos convertidos ->", DEST)


if __name__ == "__main__":
    main()
