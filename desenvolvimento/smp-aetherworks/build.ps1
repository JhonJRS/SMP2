# Compila o port do Aetherworks Refracted (1.21.1 NeoForge) contra os jars instalados (sem Gradle, sem downloads).
# Uso: powershell -ExecutionPolicy Bypass -File smp-aetherworks\build.ps1 [-Instalar] [-Erros 60]
param([switch]$Instalar, [int]$Erros = 100)
$ErrorActionPreference = "Stop"

$Inst = (Resolve-Path "$PSScriptRoot\..").Path
$Mods = Join-Path $Inst "mods"
$Lib  = Join-Path $env:USERPROFILE "curseforge\minecraft\Install\libraries"
$Jdk  = "C:\Program Files\Java\jdk-21.0.12\bin"
$NeoV = "21.1.253"

function J($rel) { $p = Join-Path $Lib $rel; if (-not (Test-Path $p)) { throw "biblioteca ausente: $p" }; $p }
function M($pat) { $f = Get-ChildItem $Mods -Filter $pat | Select-Object -First 1; if (-not $f) { throw "mod ausente: $pat" }; $f.FullName }

$cp = @(
  (J "net\neoforged\neoforge\$NeoV\neoforge-$NeoV-client.jar"),
  (J "net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-srg.jar"),
  (J "net\neoforged\neoforge\$NeoV\neoforge-$NeoV-universal.jar"),
  (J "net\neoforged\bus\8.0.5\bus-8.0.5.jar"),
  (J "net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar"),
  (J "net\neoforged\mergetool\2.0.0\mergetool-2.0.0-api.jar"),
  (J "com\mojang\brigadier\1.3.10\brigadier-1.3.10.jar"),
  (J "com\mojang\datafixerupper\8.0.16\datafixerupper-8.0.16.jar"),
  (J "com\mojang\authlib\6.0.54\authlib-6.0.54.jar"),
  (J "com\mojang\logging\1.2.7\logging-1.2.7.jar"),
  (J "com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"),
  (J "com\google\guava\guava\32.1.2-jre\guava-32.1.2-jre.jar"),
  (J "com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar"),
  (J "com\electronwill\night-config\core\3.8.3\core-3.8.3.jar"),
  (J "org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar"),
  (J "org\joml\joml\1.10.5\joml-1.10.5.jar"),
  (J "org\lwjgl\lwjgl\3.3.3\lwjgl-3.3.3.jar"),
  (J "org\apache\commons\commons-lang3\3.14.0\commons-lang3-3.14.0.jar"),
  (J "it\unimi\dsi\fastutil\8.5.12\fastutil-8.5.12.jar"),
  (J "io\netty\netty-buffer\4.1.97.Final\netty-buffer-4.1.97.Final.jar"),
  (J "io\netty\netty-common\4.1.97.Final\netty-common-4.1.97.Final.jar"),
  (J "io\netty\netty-transport\4.1.97.Final\netty-transport-4.1.97.Final.jar"),
  (J "io\netty\netty-codec\4.1.97.Final\netty-codec-4.1.97.Final.jar"),
  (M "embers-reignited-*.jar"), (M "jei-1.21.1-neoforge-*.jar"), (M "curios-neoforge-*.jar")
) -join ";"

$Out = Join-Path $PSScriptRoot "build"
if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Force "$Out\classes" | Out-Null
$src = Get-ChildItem "$PSScriptRoot\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$args_file = Join-Path $Out "fontes.txt"
$src | ForEach-Object { '"' + $_.Replace('\', '/') + '"' } | Set-Content $args_file -Encoding ascii
$opts = Join-Path $Out "opcoes.txt"
@("--release", "21", "-encoding", "UTF-8", "-proc:none", "-nowarn", "-Xmaxerrs", "$Erros",
  "-cp", ('"' + $cp.Replace('\', '/') + '"'), "-d", ('"' + "$Out\classes".Replace('\', '/') + '"')) | Set-Content $opts -Encoding ascii
cmd /c "`"$Jdk\javac.exe`" -J-Duser.language=en @`"$opts`" @`"$args_file`" > `"$Out\erros.txt`" 2>&1"
$ok = $LASTEXITCODE -eq 0
if (-not $ok) {
  $txt = Get-Content (Join-Path $Out "erros.txt") -Encoding utf8
  $txt | Select-Object -Last 1
  throw "javac falhou (ver smp-aetherworks\build\erros.txt)"
}
Copy-Item "$PSScriptRoot\src\main\resources\*" "$Out\classes" -Recurse -Force
$jar = Join-Path $Out "aetherworks-1.21.1-1.1.8-smp.jar"
& "$Jdk\jar.exe" --create --file $jar -C "$Out\classes" .
if ($LASTEXITCODE -ne 0) { throw "jar falhou" }
Write-Host "Compilado: $jar"
if ($Instalar) {
  Copy-Item $jar (Join-Path $Mods (Split-Path $jar -Leaf)) -Force
  Write-Host "Instalado em $Mods"
}
