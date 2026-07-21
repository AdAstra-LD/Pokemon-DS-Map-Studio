$ErrorActionPreference = 'Stop'
$appDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $appDirectory 'Pokemon DS Map Studio.jar'
Start-Process -FilePath 'javaw.exe' -ArgumentList @('-jar', ('"' + $jarPath + '"'))
