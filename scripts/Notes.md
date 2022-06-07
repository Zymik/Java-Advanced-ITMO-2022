PS D:\university\java2022\java-advanced-private\test\__current-repo\scripts> .\build.cmd

D:\university\java2022\java-advanced-private\test\__current-repo\scripts>javac --module-path ..\..\java-advanced-2022\artifacts;..\..\java-advanced-2022\lib ..\java-solutions\module-in
fo.java ..\java-solutions\info\kgeorgiy\ja\kosolapov\implementor\*.java -d bin

D:\university\java2022\java-advanced-private\test\__current-repo\scripts>jar -c -m Manifest.mf  -f implementor.jar --module-path ..\..\java-advanced-2022\artifacts;..\..\java-advanced-
2022\lib -C .\bin .
PS D:\university\java2022\java-advanced-private\test\__current-repo\scripts> ls


    Directory: D:\university\java2022\java-advanced-private\test\__current-repo\scripts


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----          4/1/2022   6:15 PM                bin
-a----          4/1/2022   6:12 PM            321 build.cmd
-a----          4/1/2022   6:15 PM          10689 implementor.jar
-a----          4/1/2022   6:12 PM            225 java_doc.cmd
-a----          4/1/2022   6:12 PM            167 Manifest.mf


PS D:\university\java2022\java-advanced-private\test\__current-repo\scripts> java -jar .\implementor.jar
Error: Could not find or load main class info.kgeorgiy.ja.kosolapov.implementor.Implementor
Caused by: java.lang.NoClassDefFoundError: info/kgeorgiy/java/advanced/implementor/Impler
