package com.p2pminecraft.transformer;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Injecte dans Entity.tick() :
 *   if (EntitySyncSystem.isGhost(this.getId())) { EntitySyncSystem.tickGhost(this); return; }
 *
 * Phase 3 — pour l'instant le transformer est minimal (stub prêt à étoffer).
 */
public class EntityTransformer {

    private static final String ENTITY_CLASS = "net/minecraft/world/entity/Entity";

    public static byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };

        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name,
                    String descriptor, String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                if (name.equals("tick") && descriptor.equals("()V")) {
                    return new MethodVisitor(ASM9, mv) {
                        @Override
                        public void visitCode() {
                            // if (EntitySyncSystem.isGhost(this.getId())) { EntitySyncSystem.tickGhost(this); return; }
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKEVIRTUAL, ENTITY_CLASS, "getId", "()I", false);
                            mv.visitMethodInsn(INVOKESTATIC,
                                "com/p2pminecraft/runtime/EntitySyncSystem",
                                "isGhost", "(I)Z", false);
                            Label notGhost = new Label();
                            mv.visitJumpInsn(IFEQ, notGhost);
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKESTATIC,
                                "com/p2pminecraft/runtime/EntitySyncSystem",
                                "tickGhost", "(Ljava/lang/Object;)V", false);
                            mv.visitInsn(RETURN);
                            mv.visitLabel(notGhost);
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
