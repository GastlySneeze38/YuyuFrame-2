package com.p2pminecraft.mixin.service;

import com.p2pminecraft.runtime.MappingsRegistry;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Adapte IMixinTransformer → ClassFileTransformer.
 *
 * Pour les classes cibles Mixin standard (axf, eqq, …) :
 *   transformClassBytes(obfDot, mojangDot, bytes) → Mixin applique les mixins.
 *
 * Pour iqa (IntegratedServer) :
 *   @Inject ajoute une nouvelle méthode p2p$preventPause, ce que retransformClasses
 *   interdit (restriction JVM : on ne peut pas ajouter de méthode via retransform).
 *   → Fallback ASM direct : on modifie le corps de b(Z)V sans ajouter de méthode.
 *
 * Patch iqa.b(Z)V :
 *   HEAD:  if (paused) { this.b(false); return; }
 *   Le rappel récursif avec paused=false repasse par le même guard → IFEQ passe la vérification →
 *   exécute le corps original. Aucune nouvelle méthode ajoutée.
 */
public class P2PMixinTransformerWrapper implements ClassFileTransformer {

    private static final java.util.Set<String> asmPatched =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public static boolean isAsmPatched(String className) {
        return asmPatched.contains(className);
    }

    private final IMixinTransformer transformer;

    public P2PMixinTransformerWrapper(IMixinTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain domain,
                            byte[] classfileBuffer) {
        if (classfileBuffer == null || className == null) return null;

        String obfDot    = className.replace('/', '.');
        String mojangDot = MappingsRegistry.INSTANCE.unmap(className).replace('/', '.');

        byte[] result = transformer.transformClassBytes(obfDot, mojangDot, classfileBuffer);

        // Pour iqa : si Mixin n'a pas pu injecter p2p$preventPause (car retransformClasses
        // refuse d'ajouter de nouvelles méthodes), on applique un patch ASM direct qui
        // modifie uniquement b(Z)V — sans ajouter de méthode.
        if ("iqa".equals(className)) {
            byte[] src = (result != null) ? result : classfileBuffer;
            if (!hasPauseGuard(src)) {
                System.out.println("[P2P ASM] Patch direct iqa.b(Z)V (fallback Mixin)");
                byte[] patched = patchProcessPacketsAndTick(src);
                if (patched != null) return patched;
            }
        }

        return result;
    }

    /** Vérifie si le guard anti-pause est déjà présent dans les bytes de iqa. */
    private static boolean hasPauseGuard(byte[] bytes) {
        try {
            // Si la méthode p2p$preventPause existe, Mixin a réussi.
            boolean[] found = {false};
            new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                                                String sig, String[] exc) {
                    if ("p2p$preventPause".equals(name) || "__p2p_pause_patched".equals(name))
                        found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return found[0];
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Patch ASM de iqa.b(Z)V :
     * Insère en tête de la méthode :
     *   if (paused) { this.b(false); return; }
     * Aucune nouvelle méthode ajoutée → compatible avec retransformClasses.
     */
    private static byte[] patchProcessPacketsAndTick(byte[] classBytes) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            // COMPUTE_FRAMES recalcule tous les stackmap frames depuis zéro.
            // Nécessaire car on insère un saut IFEQ qui crée un nouveau bloc basique.
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String t1, String t2) {
                    // Fallback sûr : Object est toujours un ancêtre commun valide.
                    // La résolution de hiérarchie complète échouerait ici car les
                    // classes MC ne sont pas toutes accessibles depuis ce classloader.
                    try {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Class<?> c1 = Class.forName(t1.replace('/', '.'), false, cl);
                        Class<?> c2 = Class.forName(t2.replace('/', '.'), false, cl);
                        if (c1.isAssignableFrom(c2)) return t1;
                        if (c2.isAssignableFrom(c1)) return t2;
                        if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";
                        do { c1 = c1.getSuperclass(); } while (!c1.isAssignableFrom(c2));
                        return c1.getName().replace('.', '/');
                    } catch (Exception e) {
                        return "java/lang/Object";
                    }
                }
            };

            boolean[] patched = {false};
            // EXPAND_FRAMES est requis avec COMPUTE_FRAMES pour lire les frames compressées
            cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    // Patch 1 : b(Z)V = processPacketsAndTick(boolean paused)
                    // Force paused=false vers MinecraftServer pour qu'il crée le bon BooleanSupplier
                    if ("b".equals(name) && "(Z)V".equals(descriptor)) {
                        System.out.println("[P2P ASM] Patching iqa.b(Z)V");
                        patched[0] = true;
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                /*
                                 * HEAD: if (paused) { this.b(false); return; }
                                 * L_skip: [corps original → super.b(paused)]
                                 */
                                Label skipLabel = new Label();
                                visitVarInsn(Opcodes.ILOAD, 1);
                                visitJumpInsn(Opcodes.IFEQ, skipLabel);
                                visitVarInsn(Opcodes.ALOAD, 0);
                                visitInsn(Opcodes.ICONST_0);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "iqa", "b", "(Z)V", false);
                                visitInsn(Opcodes.RETURN);
                                visitLabel(skipLabel);
                            }
                        };
                    }

                    // Patch 2 : a(BooleanSupplier) = tickServer(BooleanSupplier)
                    // Après que this.q est défini depuis Minecraft.isPaused(),
                    // on force this.q = false pour que le tick complet soit toujours exécuté.
                    // Sans ce patch : if (this.q) { processPackets(); return; } empêche le tick.
                    if ("a".equals(name) && "(Ljava/util/function/BooleanSupplier;)V".equals(descriptor)) {
                        System.out.println("[P2P ASM] Patching iqa.a(BooleanSupplier)");
                        patched[0] = true;
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitFieldInsn(int opcode, String owner,
                                                       String fieldName, String fieldDesc) {
                                super.visitFieldInsn(opcode, owner, fieldName, fieldDesc);
                                // Après PUTFIELD iqa.q:Z (= this.q = isPaused()), forcer this.q = false
                                if (opcode == Opcodes.PUTFIELD
                                        && "iqa".equals(owner)
                                        && "q".equals(fieldName)
                                        && "Z".equals(fieldDesc)) {
                                    visitVarInsn(Opcodes.ALOAD, 0);
                                    visitInsn(Opcodes.ICONST_0);
                                    super.visitFieldInsn(Opcodes.PUTFIELD, "iqa", "q", "Z");
                                }
                            }
                        };
                    }

                    return mv;
                }
            }, ClassReader.EXPAND_FRAMES);

            if (patched[0]) {
                System.out.println("[P2P ASM] iqa patché avec succès (b+a)");
                asmPatched.add("iqa");
                return cw.toByteArray();
            }
            System.err.println("[P2P ASM] WARN: b(Z)V non trouvée dans iqa");
            return null;
        } catch (Exception e) {
            System.err.println("[P2P ASM] Erreur patch: " + e);
            return null;
        }
    }
}
