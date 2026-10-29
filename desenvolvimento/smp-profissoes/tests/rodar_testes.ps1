# Roda todas as verificações possíveis sem abrir o jogo.
# Uso (na pasta da instância): powershell -ExecutionPolicy Bypass -File smp-profissoes\tests\rodar_testes.ps1
$ErrorActionPreference = "Stop"
$Inst = (Resolve-Path "$PSScriptRoot\..\..").Path
$Jdk = "C:\Program Files\Java\jdk-21.0.12\bin"
$Lib = Join-Path $env:USERPROFILE "curseforge\minecraft\Install\libraries"
$env:PYTHONIOENCODING = "utf-8"

Write-Host "== 1/6 Validação da árvore de pesquisas e dos itens (gerar.py --checar)"
python -I "$Inst\smp-profissoes\tools\gerar.py" --checar
if ($LASTEXITCODE -ne 0) { throw "validação falhou" }
python -I "$Inst\smp-profissoes\tools\silentgear_compat.py" --checar
if ($LASTEXITCODE -ne 0) { throw "materiais do Silent Gear inválidos" }

Write-Host "== 2/6 Testes do datapack gerado (aba, vedação, loot)"
python -I "$Inst\smp-profissoes\tests\testes_datapack.py"
if ($LASTEXITCODE -ne 0) { throw "testes do datapack falharam" }

Write-Host "== 3/6 Compilação do mod"
& powershell -ExecutionPolicy Bypass -File "$Inst\smp-profissoes\mod\build.ps1"
if ($LASTEXITCODE -ne 0) { throw "compilação falhou" }
python -I "$Inst\smp-profissoes\tests\checar_eventos.py"
if ($LASTEXITCODE -ne 0) { throw "@SubscribeEvent em método que não recebe evento" }

Write-Host "== 4/6 Alvos dos mixins nos jars instalados"
& powershell -ExecutionPolicy Bypass -File "$Inst\smp-profissoes\tests\verificar_mixins.ps1"
if ($LASTEXITCODE -ne 0) { throw "verificação de mixins falhou" }

Write-Host "== 5/6 Testes de permissão e de decisão"
$rpe = (Get-ChildItem "$Inst\mods" -Filter "rpessentials-*.jar" | Select-Object -First 1).FullName
$cp = @("$Inst\smp-profissoes\mod\build\classes", $rpe,
        "$Lib\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar",
        "$Lib\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar") -join ";"
$out = "$Inst\smp-profissoes\tests\build"
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force $out | Out-Null
& "$Jdk\javac.exe" --release 21 -encoding UTF-8 -proc:none -cp $cp -d $out "$PSScriptRoot\java\TestesProgressao.java"
if ($LASTEXITCODE -ne 0) { throw "compilação dos testes falhou" }
& "$Jdk\java.exe" -cp "$out;$cp" TestesProgressao $Inst
if ($LASTEXITCODE -ne 0) { throw "testes falharam" }

Write-Host "== extra: port do Aetherworks (smp-aetherworks) compila e os modelos/texturas resolvem"
& powershell -ExecutionPolicy Bypass -File "$Inst\smp-aetherworks\build.ps1"
if ($LASTEXITCODE -ne 0) { throw "port do Aetherworks não compila" }
python -I "$Inst\smp-aetherworks\tools\checar_modelos.py"
if ($LASTEXITCODE -ne 0) { throw "modelos/texturas do Aetherworks com problema" }

Write-Host "== extra: o jar instalado do smpprofissoes tem os textos gerados (nomes das pesquisas)"
python -I -c "import json,sys,zipfile; inst=sys.argv[1]; z=zipfile.ZipFile(inst+'/mods/smpprofissoes-1.0.0.jar'); a=json.loads(z.read('assets/smpprofissoes/lang/en_us.json')); b=json.load(open(inst+'/smp-profissoes/mod/src/main/resources/assets/smpprofissoes/lang/en_us.json',encoding='utf-8')); f=sorted(set(b)-set(a)); print('faltam no jar instalado:', len(f), f[:5]); sys.exit(1 if f or a!=b else 0)" $Inst
if ($LASTEXITCODE -ne 0) { throw "jar instalado desatualizado: rode smp-profissoes\mod\build.ps1 -Instalar" }

Write-Host "== 6/6 Emotes (lidos pelo mesmo leitor do Emotecraft/PlayerAnimator)"
python -I "$Inst\smp-profissoes\tools\emotes.py"
if ($LASTEXITCODE -ne 0) { throw "geração de emotes falhou" }
$cpe = @((Get-ChildItem "$Inst\mods" -Filter "player-animation-lib-forge-*.jar" | Select-Object -First 1).FullName,
         "$Lib\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar", "$Lib\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar",
         "$Lib\com\google\guava\guava\32.1.2-jre\guava-32.1.2-jre.jar") -join ";"
& "$Jdk\javac.exe" --release 21 -encoding UTF-8 -proc:none -nowarn -cp $cpe -d $out "$PSScriptRoot\java\TesteEmotes.java"
if ($LASTEXITCODE -ne 0) { throw "compilação do teste de emotes falhou" }
& "$Jdk\java.exe" -cp "$out;$cpe" TesteEmotes "$Inst\emotes"
if ($LASTEXITCODE -ne 0) { throw "emotes inválidos" }
