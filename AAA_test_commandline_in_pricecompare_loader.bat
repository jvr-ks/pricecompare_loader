@rem AAA_test_commandline_in_pricecompare_loader.bat
@rem Autohotkey must be in the path

@cd %~dp0

@start loader.ahk debug disableini --executor="java -cp" --classpath="pricecompare.jar;lib\\*" --mainclass="de.jvr.pricecompare.Pricecompare" --parameter="--urlfile=pricecompare_urls.txt"

