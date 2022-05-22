@rem updateupdater$_$_$.bat

@echo off
timeout /t 3 /NOBREAK > nul
copy /Y updater%1.exe.default updater%1.exe  >nul 2>&1

echo Please restart the Updater now!
del /Q updater%1.exe.default >nul 2>&1

timeout /t 3 /NOBREAK > nul
exit