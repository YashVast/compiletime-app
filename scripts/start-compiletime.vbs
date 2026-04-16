' CompileTime — Silent Startup Launcher
' Starts the Spring Boot companion app with no terminal window.
' Placed in Windows Startup folder by Install-CompileTime.ps1

Set shell = CreateObject("WScript.Shell")
shell.Run "cmd /c cd /d E:\Compiletime\companion && mvn spring-boot:run > %USERPROFILE%\.compiletime\startup.log 2>&1", 0, False
