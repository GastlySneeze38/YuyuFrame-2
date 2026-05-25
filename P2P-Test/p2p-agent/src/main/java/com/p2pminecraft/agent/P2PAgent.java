package com.p2pminecraft.agent;

import com.p2pminecraft.transformer.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Point d'entrée du Java Agent.
 * Ajouté aux args JVM par YuyuFrame : -javaagent:p2p-agent.jar=peerId=xxx,name=Alice,server=ws://...
 */
public class P2PAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[P2P Agent] Démarrage...");

        AgentConfig config = AgentConfig.parse(agentArgs);
        System.out.println("[P2P Agent] peerId=" + config.peerId + " name=" + config.peerName);

        inst.addTransformer(new UnifiedTransformer(), false);

        System.out.println("[P2P Agent] Transformers enregistrés — en attente du chargement Minecraft");
    }

    // Requis pour retransformation dynamique (optionnel, bonne pratique)
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }

    static class UnifiedTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className,
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {

            if (className == null || !className.startsWith("net/minecraft/")) return null;

            try {
                return switch (className) {
                    case "net/minecraft/server/level/ServerLevel"
                        -> ServerLevelTransformer.transform(classfileBuffer);

                    case "net/minecraft/world/level/chunk/LevelChunk"
                        -> ChunkTransformer.transform(classfileBuffer);

                    case "net/minecraft/world/entity/Entity"
                        -> EntityTransformer.transform(classfileBuffer);

                    case "net/minecraft/client/Minecraft"
                        -> MinecraftTransformer.transform(classfileBuffer);

                    default -> null;
                };
            } catch (Exception e) {
                System.err.println("[P2P Agent] Erreur transformation " + className + " : " + e.getMessage());
                return null; // Fallback sur classe originale
            }
        }
    }
}
