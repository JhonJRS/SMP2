"""Reduz as classes de datagen mantidas (tags, damage types) às constantes que o jogo usa.
Uso: python -I smp-aetherworks/tools/so_constantes.py"""
import re
from pathlib import Path

DG = Path(__file__).resolve().parents[1] / "src" / "main" / "java" / "net" / "sirplop" / "aetherworks" / "datagen"
for nome in ["AWItemTags", "AWBlockTags", "AWFluidTags", "AWBiomeTags", "AWDamageTypes"]:
    f = DG / f"{nome}.java"
    t = f.read_text(encoding="utf-8")
    if "SMP 2 port: constants only" in t:
        continue
    imports = re.findall(r"^import [\w.]+;$", t, re.M)
    consts = re.findall(r"^\s*public static final [^;]+;", t, re.M | re.S)
    corpo = "\n".join("    " + c.strip() for c in consts)
    novo = ("package net.sirplop.aetherworks.datagen;\n\n" + "\n".join(imports) +
            f"\n\n// SMP 2 port: constants only (the data provider ran at build time; its output ships in the jar)\n"
            f"public final class {nome} {{\n    private {nome}() {{}}\n\n{corpo}\n}}\n")
    f.write_text(novo, encoding="utf-8")
    print(nome, len(consts), "constantes")
