La sync est bonne ! Mais la snapshot ne va pas du tout : 
-rien ne s'applique dans le monde
-il se lance au mauvais moment ou il detecte mal le joueur connecter ( timeout)

J'opte pour une version dans le temp pour le guest :
-creer le monde minimal mais erreur : 
Failed to load world

The following issues occurred while trying to read world folder
"p2p_world".
It might be possible to restore the world from an older state or you can
report this issue on the bug tracker.

State from 30/05/2026 23:57: vl: Unknown data version: 0
State from 30/05/2026 23:57: vl: Unknown data version: 0

-tout syncroniser au lancement du monde via les snapshot

build methode : 

{
  "command": "$javac = \"C:\\Program Files\\Java\\jdk-24\\bin\\javac.exe\"\n$agentDir = \"d:\\projets\\YuyuFrame 2\\P2P-Server\\p2p-agent\"\n$lib = \"$agentDir\\lib\"; $outMain = \"$agentDir\\build\\main\"; $outStubs = \"$agentDir\\build\\stubs\"\n$outAsm = \"$agentDir\\build\\_asm_tmp\"; $jar_file = \"$agentDir\\build\\p2p-agent.jar\"\n$jarCmd = \"C:\\Program Files\\Java\\jdk-24\\bin\\jar.exe\"; $res = \"$agentDir\\src\\main\\resources\"\n$srcMain = \"$agentDir\\src\\main\\java\"; $srcStubs = \"$agentDir\\src\\stubs\"\n\n$stubSrc = Get-ChildItem -Recurse -Filter \"*.java\" $srcStubs | ForEach-Object { $_.FullName }\n& $javac --release 17 -d $outStubs @stubSrc 2>&1; if ($LASTEXITCODE -ne 0) { exit 1 }\n$sources = Get-ChildItem -Recurse -Filter \"*.java\" $srcMain | ForEach-Object { $_.FullName }\n$cp = \"$lib\\mixin.jar;$lib\\asm-9.5.jar;$lib\\asm-tree-9.5.jar;$outStubs\"\n& $javac --release 17 -cp $cp -d $outMain @sources 2>&1 | Where-Object { $_ -notmatch \"^Note:\" }\nif ($LASTEXITCODE -ne 0) { Write-Host \"ERREUR compilation\"; exit 1 }\nCopy-Item \"$res\\mixins.p2p.json\" \"$outMain\\\" -Force\nNew-Item -ItemType Directory -Force \"$outMain\\META-INF\\services\\\" | Out-Null\nCopy-Item \"$res\\META-INF\\services\\*\" \"$outMain\\META-INF\\services\\\" -Force\nif (Test-Path $outAsm) { Remove-Item -Recurse -Force $outAsm }\nNew-Item -ItemType Directory -Force $outAsm | Out-Null; Push-Location $outAsm\nforeach ($m in @(\"asm-9.5\",\"asm-tree-9.5\",\"asm-util-9.5\",\"asm-analysis-9.5\",\"asm-commons-9.5\")) { & $jarCmd xf \"$lib\\$m.jar\" }\nif (Test-Path \"module-info.class\") { Remove-Item \"module-info.class\" -Force }\nPop-Location\nrobocopy \"$outAsm\\org\" \"$outMain\\org\" /E /NFL /NDL /NJH /NJS | Out-Null\nRemove-Item -Recurse -Force $outAsm\n& $jarCmd --create --file=\"$jar_file\" --manifest=\"$res\\META-INF\\MANIFEST.MF\" -C \"$outMain\" .\nCopy-Item \"$jar_file\" \"$env:APPDATA\\YuyuFrame\\p2p\\p2p-agent.jar\" -Force\nWrite-Host \"=== Build réussi ===\"",
  "description": "Build p2p-agent.jar avec les deux correctifs snapshot",
  "timeout": 60000
}