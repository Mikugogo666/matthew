$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mvnExe = "D:\apache-maven-3.9.10\apache-maven-3.9.10\bin\mvn.cmd"

Set-Location $root
& $mvnExe spring-boot:run
