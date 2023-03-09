@rem quicksave_createlink.bat

@echo off

cd %~dp0

set wdir=%~dp0


set appname=______quicksave


rem edit this:
set appSource=C:\___x2_wrk\_autohotkey\simpletools\%appname%.exe



powershell "$s=(New-Object -COM WScript.Shell).CreateShortcut('%appname%.lnk');$s.TargetPath='%appSource%';$s.WorkingDirectory='%wdir%';$s.Save()"



