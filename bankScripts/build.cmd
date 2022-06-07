dir /S /B ..\java-solutions\info\kgeorgiy\ja\kosolapov\bank\*.java > sources.txt
javac -cp ..\lib\* @sources.txt -d bin
del sources.txt
exit