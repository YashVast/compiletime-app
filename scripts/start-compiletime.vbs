' CompileTime - Silent Startup Launcher
' Starts companion + process monitor only if not already running.

Dim shell, exec
Set shell = CreateObject("WScript.Shell")

' Check if port 9999 is already in use (companion already running)
Set exec = shell.Exec("cmd /c netstat -ano | findstr :9999")
Dim output
output = exec.StdOut.ReadAll()

If InStr(output, ":9999") = 0 Then
    ' Port is free - start the companion
    shell.Run "cmd /c cd /d E:\Compiletime\companion && mvn spring-boot:run > ""%USERPROFILE%\.compiletime\startup.log"" 2>&1", 0, False
End If

' Always start the process monitor (it handles being run twice gracefully)
shell.Run "powershell -WindowStyle Hidden -ExecutionPolicy Bypass -File ""E:\Compiletime\scripts\process-monitor.ps1""", 0, False
