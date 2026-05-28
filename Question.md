erreur lors du lancement : 

MC 1.21.11 requiert Java 21 — utilise : C:\Users\natpe\AppData\Roaming\YuyuFrame\.minecraft\runtime\java-runtime-delta\bin\java.exe
[P2P Signaling] En écoute sur ws://127.0.0.1:8765
[P2P] Mappings en cache : C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\cache\client-mappings-1.21.11.txt
[P2P] Mixin    : -javaagent:C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\mixin.jar
[P2P] Agent    : -javaagent:C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\p2p-agent.jar=peerId=7b3c4918-32d1-47f9-a640-a0f6866d14d7,name=GhastlySneeze38,server=ws://127.0.0.1:8765,mappings=C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\cache\client-mappings-1.21.11.txt
[P2P] Mappings : C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\cache\client-mappings-1.21.11.txt
Java 21 détecté — ZGC Generational activé
Exception in thread "main" java.lang.NoClassDefFoundError: org/objectweb/asm/tree/ClassNode
	at java.base/java.lang.Class.getDeclaredMethods0(Native Method)
	at java.base/java.lang.Class.privateGetDeclaredMethods(Class.java:3578)
	at java.base/java.lang.Class.getDeclaredMethod(Class.java:2846)
	at java.instrument/sun.instrument.InstrumentationImpl.loadClassAndStartAgent(InstrumentationImpl.java:521)
	at java.instrument/sun.instrument.InstrumentationImpl.loadClassAndCallPremain(InstrumentationImpl.java:572)
FATAL ERROR in native method: processing of -javaagent failed, processJavaStart failed
Caused by: java.lang.ClassNotFoundException: org.objectweb.asm.tree.ClassNode
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641)
	at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526)
	... 5 more
*** java.lang.instrument ASSERTION FAILED ***: "!errorOutstanding" with message Outstanding error when calling method in invokeJavaAgentMainMethod at s\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 627
*** java.lang.instrument ASSERTION FAILED ***: "success" with message invokeJavaAgentMainMethod failed at s\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 466
*** java.lang.instrument ASSERTION FAILED ***: "result" with message agent load/premain call failed at s\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 429