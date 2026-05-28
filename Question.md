nouvelle erreur :

MC 1.21.11 requiert Java 21 — utilise : C:\Users\natpe\AppData\Roaming\YuyuFrame\.minecraft\runtime\java-runtime-delta\bin\java.exe
[P2P Signaling] En écoute sur ws://127.0.0.1:8765
[P2P] Mappings en cache : C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\cache\client-mappings-1.21.11.txt
[P2P] Mixin    : -javaagent:C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\mixin.jar
[P2P] Agent    : -javaagent:C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\p2p-agent.jar=peerId=aa17566f-0cd2-4042-993e-5068d2f1fef8,name=GhastlySneeze38,server=ws://127.0.0.1:8765,mappings=C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\cache\client-mappings-1.21.11.txt
[P2P] Mappings : C:\Users\natpe\AppData\Roaming\YuyuFrame\p2p\cache\client-mappings-1.21.11.txt
Java 21 détecté — ZGC Generational activé
[Mixin/DEBUG] [mixin] Mixin bootstrap service org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapperBootstrap is not available: LaunchWrapper is not available
[Mixin/DEBUG] [mixin] Mixin bootstrap service org.spongepowered.asm.service.modlauncher.MixinServiceModLauncherBootstrap is not available: ModLauncher is not available
[Mixin/DEBUG] [mixin] MixinService [P2PJavaAgent] was successfully booted in jdk.internal.loader.ClassLoaders$AppClassLoader@68fb2c38
Exception in thread "main" java.lang.reflect.InvocationTargetException
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:118)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.instrument/sun.instrument.InstrumentationImpl.loadClassAndStartAgent(InstrumentationImpl.java:560)
	at java.instrument/sun.instrument.InstrumentationImpl.loadClassAndCallPremain(InstrumentationImpl.java:572)
Caused by: org.spongepowered.asm.service.ServiceNotAvailableError: No mixin global property service is available
	at org.spongepowered.asm.service.MixinService.initPropertyService(MixinService.java:269)
	at org.spongepowered.asm.service.MixinService.getGlobalPropertyServiceInstance(MixinService.java:249)
	at org.spongepowered.asm.service.MixinService.getGlobalPropertyService(MixinService.java:239)
	at org.spongepowered.asm.launch.GlobalProperties.getService(GlobalProperties.java:108)
	at org.spongepowered.asm.launch.GlobalProperties.get(GlobalProperties.java:121)
	at org.spongepowered.asm.launch.MixinBootstrap.isSubsystemRegistered(MixinBootstrap.java:206)
	at org.spongepowered.asm.launch.MixinBootstrap.start(MixinBootstrap.java:139)
	at org.spongepowered.asm.launch.MixinBootstrap.init(MixinBootstrap.java:128)
	at com.p2pminecraft.agent.P2PAgent.premain(P2PAgent.java:45)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	... 3 more
*** java.lang.instrument ASSERTION FAILED ***: "!errorOutstanding" with message Outstanding error when calling method in invokeJavaAgentMainMethod at s\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 627
*** java.lang.instrument ASSERTION FAILED ***: "success" with message invokeJavaAgentMainMethod failed at s\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 466
*** java.lang.instrument ASSERTION FAILED ***: "result" with message agent load/premain call failed at s\src\java.instrument\share\native\libinstrument\JPLISAgent.c line: 429