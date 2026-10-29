"""Testador automático do SMP 2: abre o Minecraft desta instância sozinho, roda um roteiro e fecha.

Uso (na pasta da instância):
    python -I smp-testes/rodar.py                 roda smp-testes/roteiro.txt
    python -I smp-testes/rodar.py outro.txt       roda outro roteiro (caminho relativo a smp-testes/)
    python -I smp-testes/rodar.py --manter-aberto deixa o jogo aberto no fim (para olhar)

O jogo abre em modo offline (jogador "SMPTeste"), cria o mundo novo saves/smp_teste em criativo e executa o roteiro
pelo mod smptestes, que só fica na pasta mods durante o teste. Saídas:
    smp-testes/resultado.txt     cada linha do roteiro com a saída do comando e OK/FALHOU
    smp-testes/erros_log.txt     erros relevantes do logs/latest.log desta execução
    screenshots/smpteste_*.png   fotos pedidas pelo roteiro
"""
import json
import re
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path

INST = Path(__file__).resolve().parents[1]
AQUI = INST / "smp-testes"
INSTALL = Path.home() / "curseforge" / "minecraft" / "Install"
VERSAO = "neoforge-21.1.253"
JAVA = Path(r"C:\Program Files\Java\jdk-21.0.12\bin\java.exe")
JAR_TESTES = AQUI / "mod" / "build" / "smptestes-1.0.0.jar"
JAR_MODS = INST / "mods" / "smptestes-1.0.0.jar"
LIMITE_MIN = 40


def regras_ok(regras):
    if not regras:
        return True
    ok = False
    for r in regras:
        os_ = r.get("os", {})
        casa = (not os_.get("name") or os_["name"] == "windows") and (not os_.get("arch") or os_["arch"] == "x86_64")
        if "features" in r:
            casa = False
        if casa:
            ok = r["action"] == "allow"
    return ok


def comando_java():
    perfil = json.loads((INSTALL / "versions" / VERSAO / f"{VERSAO}.json").read_text(encoding="utf-8"))
    base = json.loads((INSTALL / "versions" / perfil["inheritsFrom"] / f"{perfil['inheritsFrom']}.json").read_text(encoding="utf-8"))
    libdir = INSTALL / "libraries"
    vistos, cp = set(), []
    for lib in perfil["libraries"] + base["libraries"]:
        if not regras_ok(lib.get("rules")):
            continue
        partes = lib["name"].split(":")
        chave = ":".join(partes[:2] + partes[3:])  # sem a versão: o NeoForge vem antes e prevalece
        art = lib.get("downloads", {}).get("artifact")
        if chave in vistos or not art:
            continue
        caminho = libdir / art["path"]
        if not caminho.exists():
            raise SystemExit(f"biblioteca ausente: {caminho}")
        vistos.add(chave)
        cp.append(str(caminho))
    cp.append(str(INSTALL / "versions" / VERSAO / f"{VERSAO}.jar"))
    natives = AQUI / "natives"
    natives.mkdir(exist_ok=True)
    subst = {
        "${library_directory}": str(libdir), "${classpath_separator}": ";", "${version_name}": VERSAO,
        "${natives_directory}": str(natives), "${launcher_name}": "smp-testes", "${launcher_version}": "1",
        "${classpath}": ";".join(cp),
    }

    def troca(s):
        for k, v in subst.items():
            s = s.replace(k, v)
        return s

    jvm = []
    for a in base["arguments"]["jvm"] + perfil["arguments"]["jvm"]:
        if isinstance(a, str):
            jvm.append(troca(a))
        elif regras_ok(a.get("rules")):
            jvm += [troca(v) for v in (a["value"] if isinstance(a["value"], list) else [a["value"]])]
    nome = "SMPTeste"
    uid = uuid.UUID(bytes=uuid.uuid3(uuid.NAMESPACE_OID, "OfflinePlayer:" + nome).bytes).hex
    jogo = ["--username", nome, "--version", VERSAO, "--gameDir", str(INST), "--assetsDir", str(INSTALL / "assets"),
            "--assetIndex", base["assetIndex"]["id"], "--uuid", uid, "--accessToken", "0", "--clientId", "0", "--xuid", "0",
            "--userType", "legacy", "--versionType", "release", "--width", "1280", "--height", "720"]
    jogo += perfil["arguments"]["game"]
    extra = ["-Xms2G", "-Xmx8G", "-Dsmp.teste=1"]
    if "--manter-aberto" in sys.argv:
        extra.append("-Dsmp.teste.fechar=0")
    return [str(JAVA)] + extra + jvm + [perfil["mainClass"]] + jogo


PADROES_ERRO = [
    r"Parsing error loading recipe smpprofissoes", r"Parsing error loading recipe .*(smpcompat|smp_)",
    r"Couldn't parse .*(smpprofissoes|smpcompat)", r"\[SMPTESTE\].*(exceção|ERRO)", r"Mixin apply .* failed",
    r"smpcompat.*(ERROR|Exception|error)", r"MaterialManager.*(ERROR|rror)", r"Exception in server tick loop",
    r"---- Minecraft Crash Report ----",
]


def resumir_log(inicio):
    log = INST / "logs" / "latest.log"
    linhas = log.read_text(encoding="utf-8", errors="replace").splitlines() if log.exists() else []
    achadas = [l for l in linhas if any(re.search(p, l) for p in PADROES_ERRO)]
    crashes = [p for p in (INST / "crash-reports").glob("*.txt") if p.stat().st_mtime > inicio] if (INST / "crash-reports").exists() else []
    texto = "\n".join(achadas[:300])
    if crashes:
        texto += "\n\nCRASH REPORTS:\n" + "\n".join(str(c) for c in crashes)
    (AQUI / "erros_log.txt").write_text(texto + "\n", encoding="utf-8")
    return len(achadas), crashes


def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    roteiro = AQUI / next((a for a in sys.argv[1:] if not a.startswith("--")), "roteiro.txt")
    if not roteiro.exists():
        raise SystemExit(f"roteiro não encontrado: {roteiro}")
    if not JAR_TESTES.exists():
        raise SystemExit("compile antes: powershell -File smp-testes\\mod\\build.ps1")
    res = AQUI / "resultado.txt"
    if res.exists():
        res.unlink()
    shutil.copy(JAR_TESTES, JAR_MODS)
    inicio = time.time()
    try:
        with open(AQUI / "saida_jogo.log", "w", encoding="utf-8", errors="replace") as out:
            cmd = comando_java()
            cmd.insert(1, f"-Dsmp.roteiro={roteiro}")
            proc = subprocess.Popen(cmd, cwd=INST, stdout=out, stderr=subprocess.STDOUT)
            try:
                proc.wait(timeout=LIMITE_MIN * 60)
            except subprocess.TimeoutExpired:
                proc.kill()
                print(f"TEMPO ESGOTADO ({LIMITE_MIN} min): jogo encerrado à força")
    finally:
        try:
            JAR_MODS.unlink()
        except OSError:
            pass
    dur = (time.time() - inicio) / 60
    n_erros, crashes = resumir_log(inicio)
    print(f"Jogo fechado após {dur:.1f} min (código {proc.returncode}).")
    if res.exists():
        texto = res.read_text(encoding="utf-8")
        print(texto[-6000:])
    else:
        print("SEM resultado.txt: o roteiro não terminou (veja smp-testes/saida_jogo.log e logs/latest.log)")
    print(f"Erros relevantes no log: {n_erros} (smp-testes/erros_log.txt)")
    for c in crashes:
        print("CRASH:", c)


if __name__ == "__main__":
    main()
