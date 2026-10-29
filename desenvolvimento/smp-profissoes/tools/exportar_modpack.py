"""Exporta a instância no formato de modpack do CurseForge (manifest.json + overrides/) para publicar no GitHub.
Uso: python -I smp-profissoes/tools/exportar_modpack.py <pasta destino>

Gera em <destino>:
- manifest.json   versões exatas (projectID/fileID) de todo mod, shader e resource pack instalado pelo CurseForge
- overrides/      configs, datapacks globais, emotes, resource pack próprio e os jars que não vêm do CurseForge
- MODS.md         lista dos mods com link
- SMP2.zip        manifest + overrides, pronto para "Importar" no CurseForge
- desenvolvimento/ código-fonte dos mods próprios (não entra no zip)
Mundos, logs, caches, banco do CoreProtect e dados pessoais (skins, avatares, blueprints) ficam de fora."""
import json
import os
import shutil
import sys
import zipfile
from pathlib import Path

INST = Path(__file__).resolve().parents[2]
NOME, VERSAO, AUTOR = "SMP 2", "Alpha 2", "JhonJRS"

# pasta da instância -> o que pular dentro dela (nomes de arquivo/pasta ou sufixos)
OVERRIDES = {
    "config": {"database.db", "jeiworldgen-generation-cache.dat", ".bak", "world", "activity.json",  # caches, histórico
               "quickskin-client.json", "quickskin_preferences.json", "firstColonyData.json"},  # dados do jogador local
    "defaultconfigs": set(),
    "moonlight-global-datapacks": set(),
    "emotes": set(),
}
DEV = {  # código-fonte dos mods próprios
    "smp-profissoes": {"__pycache__", "build", "out", ".class", ".jar", ".log"},
    "smp-aetherworks": {"__pycache__", "build", "original", ".class", ".jar"},
    "smp-translucidez": {"build", ".class", ".jar"},
    "smp-testes": {"__pycache__", "natives", "mod", ".log", "resultado.txt"},
}


def pular(rel, regras):
    return any(p in regras for p in rel.parts) or any(rel.name.endswith(s) for s in regras if s.startswith("."))


def copiar(origem, destino, regras):
    n = 0
    for p in origem.rglob("*"):
        rel = p.relative_to(origem)
        if p.is_file() and not pular(rel, regras):
            alvo = destino / rel
            alvo.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(p, alvo)
            n += 1
    return n


def main(dest):
    dest = Path(dest).resolve()
    if os.name == "nt":  # caminhos dos datapacks passam de 260 caracteres
        dest = Path("\\\\?\\" + str(dest))
    inst = json.loads((INST / "minecraftinstance.json").read_text(encoding="utf-8"))
    loader = inst["baseModLoader"]["name"]  # ex.: neoforge-21.1.253
    addons = inst["installedAddons"]
    no_disco = {p.name for d in ("mods", "resourcepacks", "shaderpacks") if (INST / d).exists() for p in (INST / d).iterdir()}
    do_cf = {a["installedFile"]["fileNameOnDisk"] for a in addons}

    for velho in ("overrides", "desenvolvimento", "manifest.json", "MODS.md", "SMP2.zip"):
        alvo = dest / velho
        if alvo.is_dir():
            shutil.rmtree(alvo)
        elif alvo.exists():
            alvo.unlink()
    ov = dest / "overrides"

    arquivos, linhas = [], []
    for a in sorted(addons, key=lambda a: a["name"].strip().lower()):
        f = a["installedFile"]
        nome_disco = f["fileNameOnDisk"]
        ativo = not nome_disco.endswith(".disabled") and a.get("isEnabled", True)
        if nome_disco.removesuffix(".disabled") not in no_disco and nome_disco not in no_disco:
            print("aviso: addon sem arquivo no disco, ignorado:", a["name"])
            continue
        arquivos.append({"projectID": a["addonID"], "fileID": f["id"], "required": bool(ativo)})
        linhas.append(f"| [{a['name'].strip()}]({a.get('webSiteURL') or ''}) | `{f['fileName']}` |")

    manifest = {
        "minecraft": {"version": inst["gameVersion"], "modLoaders": [{"id": loader, "primary": True}]},
        "manifestType": "minecraftModpack", "manifestVersion": 1,
        "name": NOME, "version": VERSAO, "author": AUTOR,
        "files": arquivos, "overrides": "overrides",
    }
    dest.mkdir(parents=True, exist_ok=True)
    (dest / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    n = 0
    for pasta, regras in OVERRIDES.items():
        if (INST / pasta).exists():
            n += copiar(INST / pasta, ov / pasta, regras)
    extras = []
    for d in ("mods", "resourcepacks"):  # jars/packs que não vieram do CurseForge (mods próprios e ports)
        for p in sorted((INST / d).iterdir()):
            if p.is_file() and p.name not in do_cf and not p.name.endswith(".txt"):
                (ov / d).mkdir(parents=True, exist_ok=True)
                shutil.copy2(p, ov / d / p.name)
                extras.append(f"{d}/{p.name}")
    # options.txt só com os resource packs ativos (o resto fica no padrão de cada jogador)
    opts = [l for l in (INST / "options.txt").read_text(encoding="utf-8").splitlines()
            if l.startswith(("resourcePacks:", "incompatibleResourcePacks:")) and not l.endswith(":[]")]
    if opts:
        (ov / "options.txt").write_text("\n".join(opts) + "\n", encoding="utf-8")

    nd = sum(copiar(INST / p, dest / "desenvolvimento" / p, r) for p, r in DEV.items() if (INST / p).exists())

    (dest / "MODS.md").write_text(
        f"# Mods do {NOME} ({VERSAO})\n\nGerado por `desenvolvimento/smp-profissoes/tools/exportar_modpack.py`. "
        f"{len(arquivos)} arquivos do CurseForge (mods, shaders e resource packs) + {len(extras)} incluídos no próprio pack.\n\n"
        "## Incluídos no pack (fora do CurseForge)\n\n" + "\n".join(f"- `{e}`" for e in extras) +
        "\n\n## Do CurseForge\n\n| Projeto | Arquivo |\n|---|---|\n" + "\n".join(linhas) + "\n", encoding="utf-8")

    with zipfile.ZipFile(dest / "SMP2.zip", "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        z.write(dest / "manifest.json", "manifest.json")
        z.write(dest / "MODS.md", "modlist.md")
        for p in sorted(ov.rglob("*")):
            if p.is_file():
                z.write(p, p.relative_to(dest).as_posix())
    print(f"{len(arquivos)} arquivos no manifest, {n} arquivos de override, extras: {extras}, dev: {nd} arquivos, "
          f"zip {(dest / 'SMP2.zip').stat().st_size / 1e6:.1f} MB")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    main(sys.argv[1])
