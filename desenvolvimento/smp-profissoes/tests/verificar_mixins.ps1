# Verifica, sem abrir o jogo, que cada alvo de mixin do smpprofissoes existe com o descritor exato nos jars
# instalados, que cada @Shadow existe e que nenhuma classe fora do pacote de mixins referencia esse pacote.
# Uso (na pasta da instância): powershell -ExecutionPolicy Bypass -File smp-profissoes\tests\verificar_mixins.ps1
$ErrorActionPreference = "Stop"
$Inst = (Resolve-Path "$PSScriptRoot\..\..").Path
$Mods = Join-Path $Inst "mods"
$Lib  = Join-Path $env:USERPROFILE "curseforge\minecraft\Install\libraries"
$jp   = "C:\Program Files\Java\jdk-21.0.12\bin\javap.exe"
$src  = Join-Path $Inst "smp-profissoes\mod\src\main\java\br\smp\profissoes\mixin"

$cp = @(
  "$Lib\net\neoforged\neoforge\21.1.253\neoforge-21.1.253-client.jar",
  "$Lib\net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-srg.jar",
  (Get-ChildItem $Mods -Filter "appliedenergistics2-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "create-1.21.1-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "Botania Revived-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "minecolonies-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "letsdo-vinery-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "blockui-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "simplymore-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "coreprotectneo-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "hardcore_true_darkness-*.jar")[0].FullName,
  (Get-ChildItem $Mods -Filter "rpessentials-*.jar")[0].FullName
) -join ";"

$falhas = 0; $ok = 0
foreach ($f in Get-ChildItem $src -Filter *Mixin.java) {
  $t = Get-Content $f.FullName -Raw -Encoding UTF8
  $alvos = @()
  if ($t -match '@Mixin\(targets\s*=\s*\{([^}]+)\}') { $alvos = [regex]::Matches($Matches[1], '"([^"]+)"') | % { $_.Groups[1].Value } }
  elseif ($t -match '@Mixin\(targets\s*=\s*"([^"]+)"') { $alvos = @($Matches[1]) }
  elseif ($t -match '@Mixin\(value\s*=\s*(\w+)\.class') {
    $simples = $Matches[1]
    if ($t -match "import\s+([\w\.]+\.$simples);") { $alvos = @($Matches[1]) }
  }
  if (-not $alvos) { Write-Host "FALHOU: $($f.Name): alvo não identificado"; $falhas++; continue }
  foreach ($alvo in $alvos) {
  $sig = & $jp -p -s -cp $cp $alvo 2>&1 | Out-String
  if ($sig -match "Error: class not found") { Write-Host "FALHOU: $($f.Name): classe $alvo não existe"; $falhas++; continue }
  # constantes ALVO = "..."
  $consts = @{}
  foreach ($m in [regex]::Matches($t, 'String\s+(\w+)\s*=\s*"([^"]+)"')) { $consts[$m.Groups[1].Value] = $m.Groups[2].Value }
  foreach ($m in [regex]::Matches($t, 'method\s*=\s*("([^"]+)"|(\w+))')) {
    $alvoMetodo = if ($m.Groups[2].Success) { $m.Groups[2].Value } else { $consts[$m.Groups[3].Value] }
    if ($alvoMetodo -notmatch '^(\w+)(\(.*)$') {
      # só o nome: basta existir um método com esse nome
      if ($sig -match "\s$([regex]::Escape($alvoMetodo))\(") { $ok++ } else { Write-Host "FALHOU: $($f.Name): método $alvoMetodo não existe em $alvo"; $falhas++ }
      continue
    }
    $nome = $Matches[1]; $desc = $Matches[2]
    $linhas = $sig -split "`r?`n"
    $achou = $false
    for ($i = 0; $i -lt $linhas.Count; $i++) {
      if ($linhas[$i] -match "\s$([regex]::Escape($nome))\(" -and $i + 1 -lt $linhas.Count -and $linhas[$i + 1].Trim() -eq "descriptor: $desc") { $achou = $true; break }
    }
    if ($achou) { $ok++ } else { Write-Host "FALHOU: $($f.Name): $nome$desc não existe em $alvo"; $falhas++ }
  }
  foreach ($m in [regex]::Matches($t, '@Shadow[^;]*?\s(\w+)\s*;')) {
    $campo = $m.Groups[1].Value
    if ($sig -match "\s$campo;") { $ok++ } else { Write-Host "FALHOU: $($f.Name): @Shadow $campo não existe em $alvo"; $falhas++ }
  }
  }
}
# regra do Mixin: nada fora do pacote mixin pode referenciar classes do pacote
$cls = Join-Path $Inst "smp-profissoes\mod\build\classes\br\smp\profissoes"
foreach ($c in Get-ChildItem $cls -Filter *.class) {
  if ([IO.File]::ReadAllText($c.FullName, [Text.Encoding]::GetEncoding(28591)).Contains("br/smp/profissoes/mixin/")) { Write-Host "FALHOU: $($c.Name) referencia o pacote de mixins"; $falhas++ } else { $ok++ }
}
Write-Host "Mixins: $ok verificações OK, $falhas falha(s)."
if ($falhas -gt 0) { exit 1 }
