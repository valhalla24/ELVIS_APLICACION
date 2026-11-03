# Pruebas de calendario, reportes, restriccion por rol y seguridad.
# Uso (desde la raiz del repositorio, con un JDK 20+ en el PATH o en JAVA_HOME):
#   powershell -ExecutionPolicy Bypass -File pruebas\correr_pruebas.ps1
# Usa una base temporal en pruebas\datos: NO toca C:\ElvisData. Abre por un momento la ventana de inicio de sesion.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
$bin  = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { Split-Path (Get-Command javac).Source }
$fx   = "$raiz\jar_files\javafx-sdk-20.0.2\lib"
$cp   = (Get-ChildItem "$raiz\jar_files" -Recurse -Filter *.jar | Where-Object { $_.FullName -notmatch 'javafx-sdk|javadoc|examples' }).FullName -join ';'
$app  = "$PSScriptRoot\out\app"
$out  = "$PSScriptRoot\out\pruebas"
$datos = "$PSScriptRoot\datos"

foreach ($d in $app, $out, $datos) { if (Test-Path $d) { Remove-Item -Recurse -Force $d } }
$src = (Get-ChildItem "$raiz\ElvisAccountManager\src" -Recurse -Filter *.java).FullName
& "$bin\javac.exe" -encoding UTF-8 --release 20 --module-path $fx --add-modules javafx.controls,javafx.fxml -cp $cp -d $app $src
if ($LASTEXITCODE -ne 0) { throw 'No compilo la aplicacion' }
Get-ChildItem "$raiz\ElvisAccountManager\src" -Recurse -File | Where-Object Extension -ne '.java' | ForEach-Object {
    $dest = Join-Path $app $_.FullName.Substring((Resolve-Path "$raiz\ElvisAccountManager\src").Path.Length)
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null; Copy-Item $_.FullName $dest }
& "$bin\javac.exe" -encoding UTF-8 --module-path $fx --add-modules javafx.controls,javafx.fxml -cp "$app;$cp" -d $out `
    "$PSScriptRoot\PruebasElvis.java" "$PSScriptRoot\PruebasSeguridad.java"
if ($LASTEXITCODE -ne 0) { throw 'No compilaron las pruebas' }

$ErrorActionPreference = 'Continue'   # java escribe avisos (SLF4J) en stderr
& "$bin\java.exe" "-Delvis.datos=$datos" --module-path $fx --add-modules javafx.controls,javafx.fxml `
    "-Djava.library.path=$raiz\jar_files\javafx-sdk-20.0.2\bin" -cp "$out;$app;$cp" PruebasElvis "$datos\salida" seguridad 2>&1 |
    ForEach-Object { "$_" } | Where-Object { $_ -notmatch 'SLF4J|StatusLogger|SimpleLogger' }
exit $LASTEXITCODE
