@echo off
rem Neo Forsaken: The Forsaken Realms with NeoForge's table and deck builder.
rem Everything of yours (saves, decks, settings, card images) goes in the "datos" folder next to this file.
cd /d "%~dp0"
if not exist "jre\bin\javaw.exe" (
  echo The jre folder is missing. Unzip the whole package and run this file from inside it.
  pause
  exit /b 1
)
"jre\bin\java.exe" -Dfile.encoding=UTF-8 -XX:MaxRAMPercentage=50 "-Dforge.assetsDir=%~dp0\" "-Dneo.dataDir=%~dp0datos" --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED -cp "app\*" forge.neo.NeoMain ui
pause
