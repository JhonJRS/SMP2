# Compila o mod smptranslucidez contra os jars já instalados (sem Gradle, sem downloads).
# Uso: powershell -ExecutionPolicy Bypass -File smp-translucidez\build.ps1 [-Instalar]
param([switch]$Instalar)
$ErrorActionPreference = "Stop"

$Inst = (Resolve-Path "$PSScriptRoot\..").Path
$Mods = Join-Path $Inst "mods"
$Lib  = Join-Path $env:USERPROFILE "curseforge\minecraft\Install\libraries"
$Jdk  = "C:\Program Files\Java\jdk-21.0.12\bin"
$NeoV = "21.1.253"

function J($rel) { $p = Join-Path $Lib $rel; if (-not (Test-Path $p)) { throw "biblioteca ausente: $p" }; $p }

$cp = @(
  (J "net\neoforged\neoforge\$NeoV\neoforge-$NeoV-client.jar"),
  (J "net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-srg.jar"),
  (J "net\neoforged\neoforge\$NeoV\neoforge-$NeoV-universal.jar"),
  (J "net\neoforged\bus\8.0.5\bus-8.0.5.jar"), (J "net\neoforged\mergetool\2.0.0\mergetool-2.0.0-api.jar"),
  (J "net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar"),
  (J "com\mojang\logging\1.2.7\logging-1.2.7.jar"),
  (J "com\mojang\datafixerupper\8.0.16\datafixerupper-8.0.16.jar"),
  (J "org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar"),
  (J "net\fabricmc\sponge-mixin\0.15.2+mixin.0.8.7\sponge-mixin-0.15.2+mixin.0.8.7.jar")
) -join ";"

$Out = Join-Path $PSScriptRoot "build"
if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Force "$Out\classes" | Out-Null
$src = Get-ChildItem "$PSScriptRoot\src\main\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$Jdk\javac.exe" --release 21 -encoding UTF-8 -proc:none -Xlint:-options -nowarn -cp $cp -d "$Out\classes" $src
if ($LASTEXITCODE -ne 0) { throw "javac falhou" }
Copy-Item "$PSScriptRoot\src\main\resources\*" "$Out\classes" -Recurse -Force

$jar = Join-Path $Out "smptranslucidez-1.0.0.jar"
& "$Jdk\jar.exe" --create --file $jar -C "$Out\classes" .
if ($LASTEXITCODE -ne 0) { throw "jar falhou" }
Write-Host "Compilado: $jar"

if ($Instalar) {
  Copy-Item $jar (Join-Path $Mods "smptranslucidez-1.0.0.jar") -Force
  Write-Host "Instalado em $Mods"
}
