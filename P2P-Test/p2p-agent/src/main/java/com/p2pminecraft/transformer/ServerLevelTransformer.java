package com.p2pminecraft.transformer;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Injecte dans ServerLevel.tick() :
 *   if (!DistributedChunkManager.shouldTickWorld()) return;
 *
 * Noms Mojang-mappés (1.20.x). Pour vanilla obfusqué, le launcher
 * doit appliquer les mappings Mojang avant de lancer Minecraft.
 */
public class ServerLevelTransformer {

    public static byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String descriptor, String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // tick(BooleanSupplier)
                if (name.equals("tick") && descriptor.startsWith("(Ljava/util/function/BooleanSupplier;)")) {
                    return new MethodVisitor(ASM9, mv) {
                        @Override
                        public void visitCode() {
                            // if (!DistributedChunkManager.shouldTickWorld()) return;
                            mv.visitMethodInsn(INVOKESTATIC,
                                "com/p2pminecraft/runtime/DistributedChunkManager",
                                "shouldTickWorld", "()Z", false);
                            Label ok = new Label();
                            mv.visitJumpInsn(IFNE, ok);
                            mv.visitInsn(RETURN);
                            mv.visitLabel(ok);
                            super.visitCode();
                        }
                    };
                }
                return mv;
            }
        }, 0);

        return writer.toByteArray();
    }
}
