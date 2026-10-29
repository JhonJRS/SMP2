# Compila o mod smpprofissoes contra os jars já instalados (sem Gradle, sem downloads).
# Uso: powershell -ExecutionPolicy Bypass -File smp-profissoes\mod\build.ps1 [-Instalar]
param([switch]$Instalar)
$ErrorActionPreference = "Stop"

$Inst = (Resolve-Path "$PSScriptRoot\..\..").Path
$Mods = Join-Path $Inst "mods"
$Lib  = Join-Path $env:USERPROFILE "curseforge\minecraft\Install\libraries"
$Jdk  = "C:\Program Files\Java\jdk-21.0.12\bin"
$NeoV = "21.1.253"   # mesma versão de NeoForge do minecraftinstance.json

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
  (J "org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar"),
  (J "it\unimi\dsi\fastutil\8.5.12\fastutil-8.5.12.jar"),
  (J "org\joml\joml\1.10.5\joml-1.10.5.jar"),
  (J "io\netty\netty-buffer\4.1.97.Final\netty-buffer-4.1.97.Final.jar"),
  (J "io\netty\netty-common\4.1.97.Final\netty-common-4.1.97.Final.jar"),
  (J "net\fabricmc\sponge-mixin\0.15.2+mixin.0.8.7\sponge-mixin-0.15.2+mixin.0.8.7.jar"),
  (M "minecolonies-*.jar"), (M "structurize-*.jar"), (M "blockui-*.jar"), (M "domum-ornamentum-*.jar"),
  (M "rpessentials-*.jar"), (M "appliedenergistics2-*.jar"), (M "guideme-*.jar"),
  (M "create-1.21.1-*.jar"), (M "Botania Revived-*.jar"),
  (M "Enhanced-Celestials-2-Core-*.jar"), (M "SereneSeasons-*.jar"), (M "GlitchCore-*.jar"),
  (M "irons_spellbooks-*.jar"), (M "irons_restrictions-*.jar")
) -join ";"

$Out = Join-Path $PSScriptRoot "build"
if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Force "$Out\classes" | Out-Null
$src = Get-ChildItem "$PSScriptRoot\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$Jdk\javac.exe" --release 21 -encoding UTF-8 -proc:none -Xlint:-options -nowarn -cp $cp -d "$Out\classes" $src
if ($LASTEXITCODE -ne 0) { throw "javac falhou" }
Copy-Item "$PSScriptRoot\src\main\resources\*" "$Out\classes" -Recurse -Force

$jar = Join-Path $Out "smpprofissoes-1.0.0.jar"
& "$Jdk\jar.exe" --create --file $jar -C "$Out\classes" .
if ($LASTEXITCODE -ne 0) { throw "jar falhou" }
Write-Host "Compilado: $jar"

if ($Instalar) {
  Copy-Item $jar (Join-Path $Mods "smpprofissoes-1.0.0.jar") -Force
  Write-Host "Instalado em $Mods"
}
