package com.p2pminecraft.transformer;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Injecte dans LevelChunk.tick() :
 *   int cx = this.getPos().x;
 *   int cz = this.getPos().z;
 *   if (!DistributedChunkManager.shouldTickChunk(cx, cz)) return;
 *
 * Et après le tick :
 *   DistributedChunkManager.afterChunkTick(cx, cz);
 */
public class ChunkTransformer {

    // Noms Mojang-mappés 1.20.x
    private static final String CHUNK_CLASS   = "net/minecraft/world/level/chunk/LevelChunk";
    private static final String CHUNKPOS_CLASS = "net/minecraft/world/level/ChunkPos";
    private static final String DCM_CLASS      = "com/p2pminecraft/runtime/DistributedChunkManager";

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

                if (name.equals("tick") && descriptor.startsWith("(Ljava/util/function/BooleanSupplier;)")) {
                    return new ChunkTickInjector(mv);
                }
                return mv;
            }
        }, 0);

        return writer.toByteArray();
    }

    static class ChunkTickInjector extends MethodVisitor {

        ChunkTickInjector(MethodVisitor mv) { super(ASM9, mv); }

        @Override
        public void visitCode() {
            // int cx = this.getPos().x;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, CHUNK_CLASS, "getPos",
                    "()L" + CHUNKPOS_CLASS + ";", false);
            mv.visitFieldInsn(GETFIELD, CHUNKPOS_CLASS, "x", "I");
            mv.visitVarInsn(ISTORE, 2); // local var slot 2

            // int cz = this.getPos().z;
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, CHUNK_CLASS, "getPos",
                    "()L" + CHUNKPOS_CLASS + ";", false);
            mv.visitFieldInsn(GETFIELD, CHUNKPOS_CLASS, "z", "I");
            mv.visitVarInsn(ISTORE, 3); // local var slot 3

            // if (!DistributedChunkManager.shouldTickChunk(cx, cz)) return;
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKESTATIC, DCM_CLASS, "shouldTickChunk", "(II)Z", false);
            Label ok = new Label();
            mv.visitJumpInsn(IFNE, ok);
            mv.visitInsn(RETURN);
            mv.visitLabel(ok);

            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            // Avant chaque RETURN : afterChunkTick(cx, cz)
            if (opcode == RETURN) {
                mv.visitVarInsn(ILOAD, 2);
                mv.visitVarInsn(ILOAD, 3);
                mv.visitMethodInsn(INVOKESTATIC, DCM_CLASS, "afterChunkTick", "(II)V", false);
            }
            super.visitInsn(opcode);
        }
    }
}
