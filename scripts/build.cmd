javac --module-path ..\..\java-advanced-2022\artifacts;..\..\java-advanced-2022\lib ..\java-solutions\module-info.java ..\java-solutions\info\kgeorgiy\ja\kosolapov\implementor\*.java -d bin
jar -c -m Manifest.mf  -f implementor.jar --module-path ..\..\java-advanced-2022\artifacts;..\..\java-advanced-2022\lib -C .\bin .