package com.p2pminecraft.transformer;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Injecte dans Minecraft (client) pour démarrer le serveur intégré P2P.
 *
 * Cible : net.minecraft.client.Minecraft.startGame()
 * Injection : appel à RuntimeInitializer.onGameStart() en fin de méthode.
 *
 * RuntimeInitializer démarre ensuite le réseau P2P dans un thread séparé.
 */
public class MinecraftTransformer {

    public static byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String descriptor, String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // Injecter au retour de startGame()
                if (name.equals("startGame") && descriptor.equals("()V")) {
                    return new MethodVisitor(ASM9, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == RETURN) {
                                mv.visitMethodInsn(INVOKESTATIC,
                                    "com/p2pminecraft/runtime/RuntimeInitializer",
                                    "onGameStart", "()V", false);
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        }, 0);

        return writer.toByteArray();
    }
}
