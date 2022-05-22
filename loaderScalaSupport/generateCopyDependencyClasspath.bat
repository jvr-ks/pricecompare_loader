@rem _generateCopyDependencyClasspath.bat


@echo Generates files "_CopyDependencyClasspath.bat" and "_updaterfiles$_$_$_append.txt"

@echo off

cd %~dp0

cd ..

del /Q _CopyDependencyClasspath.bat
del /Q _updaterfiles$_$_$_append.txt

call sbt printDependencyClasspath

timeout /T 5


