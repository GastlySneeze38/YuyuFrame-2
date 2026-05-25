import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes;

/**
 * Remappe un client.jar Minecraft obfusqué → noms Mojang (deobf).
 * Utilise les mappings ProGuard fournis par Mojang (client_mappings.txt).
 *
 * Usage: MojangRemapper <input.jar> <mappings.txt> <output.jar>
 */
public class MojangRemapper {

    // obfInternalName (slashes) → deobfInternalName (slashes)
    private static final Map<String, String> classMap = new HashMap<>();
    // deobfDotName → obfDotName  (pour reconstruire les descripteurs obf depuis les types ProGuard)
    private static final Map<String, String> deobfToObf = new HashMap<>();
    // Map complète pour le remapper (classes + méthodes + champs)
    private static final Map<String, String> asmMap = new HashMap<>();
    // obfInternalName → obfSuperInternalName (hiérarchie pour résolution des méthodes héritées)
    private static final Map<String, String> superClassMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: MojangRemapper <input.jar> <mappings.txt> <output.jar>");
            System.exit(1);
        }
        System.out.println("[MojangRemapper] Parsing mappings...");
        parseMappings(args[1]);
        System.out.println("[MojangRemapper] " + classMap.size() + " classes, "
                + (asmMap.size() - classMap.size()) + " membres");

        System.out.println("[MojangRemapper] Lecture de la hiérarchie de classes...");
        buildHierarchy(args[0]);
        System.out.println("[MojangRemapper] " + superClassMap.size() + " relations parent-enfant");

        System.out.println("[MojangRemapper] Remapping JAR...");
        remapJar(args[0], args[2]);
        System.out.println("[MojangRemapper] Termine !");
    }

    // -------------------------------------------------------------------------
    // Parsing ProGuard
    // -------------------------------------------------------------------------

    private static void parseMappings(String path) throws IOException {
        // Passe 1 : classes uniquement → construit deobfToObf pour la résolution des descripteurs
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("#") || t.isEmpty()) continue;
                if (!isIndented(line) && t.endsWith(":")) {
                    String[] parts = t.split(" -> ", 2);
                    if (parts.length != 2) continue;
                    String deobf = parts[0].trim();
                    String obf   = parts[1].replaceAll(":$", "").trim();
                    classMap.put(obf.replace('.', '/'), deobf.replace('.', '/'));
                    deobfToObf.put(deobf, obf);
                    asmMap.put(obf.replace('.', '/'), deobf.replace('.', '/'));
                }
            }
        }

        // Passe 2 : membres (méthodes + champs)
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            String currentObfOwner = null;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("#") || t.isEmpty()) continue;

                if (!isIndented(line) && t.endsWith(":")) {
                    String[] parts = t.split(" -> ", 2);
                    if (parts.length == 2) {
                        String obf = parts[1].replaceAll(":$", "").trim();
                        currentObfOwner = obf.replace('.', '/');
                    }
                    continue;
                }

                if (!isIndented(line) || currentObfOwner == null) continue;

                String[] parts = t.split(" -> ", 2);
                if (parts.length != 2) continue;

                String lhs     = parts[0].trim();
                String obfName = parts[1].trim();

                // Supprimer le préfixe de numéros de ligne "1:200:"
                lhs = lhs.replaceAll("^\\d+:\\d+:", "").trim();

                if (lhs.contains("(")) {
                    parseMember(currentObfOwner, lhs, obfName);
                } else {
                    int sp = lhs.lastIndexOf(' ');
                    if (sp < 0) continue;
                    String deobfField = lhs.substring(sp + 1).trim();
                    asmMap.put(currentObfOwner + "." + obfName, deobfField);
                }
            }
        }
    }

    private static void parseMember(String obfOwner, String lhs, String obfMethodName) {
        int parenOpen  = lhs.indexOf('(');
        int parenClose = lhs.lastIndexOf(')');
        if (parenClose < parenOpen) return;

        String beforeParen = lhs.substring(0, parenOpen).trim();
        String params      = lhs.substring(parenOpen + 1, parenClose);

        int sp = beforeParen.lastIndexOf(' ');
        if (sp < 0) return;
        String retType         = beforeParen.substring(0, sp).trim();
        String deobfMethodName = beforeParen.substring(sp + 1).trim();

        String desc = buildObfDesc(retType, params);
        asmMap.put(obfOwner + "." + obfMethodName + desc, deobfMethodName);
    }

    private static String buildObfDesc(String retType, String params) {
        StringBuilder sb = new StringBuilder("(");
        if (!params.isBlank()) {
            for (String p : params.split(",")) {
                sb.append(toObfDescriptor(p.trim()));
            }
        }
        sb.append(")").append(toObfDescriptor(retType));
        return sb.toString();
    }

    private static String toObfDescriptor(String javaType) {
        int dims = 0;
        String t = javaType;
        while (t.endsWith("[]")) {
            dims++;
            t = t.substring(0, t.length() - 2).trim();
        }
        String base = switch (t) {
            case "void"    -> "V";
            case "boolean" -> "Z";
            case "byte"    -> "B";
            case "char"    -> "C";
            case "short"   -> "S";
            case "int"     -> "I";
            case "long"    -> "J";
            case "float"   -> "F";
            case "double"  -> "D";
            default -> {
                String obf = deobfToObf.getOrDefault(t, t);
                yield "L" + obf.replace('.', '/') + ";";
            }
        };
        return "[".repeat(dims) + base;
    }

    // -------------------------------------------------------------------------
    // Hiérarchie de classes (pour résolution des méthodes héritées)
    // -------------------------------------------------------------------------

    // SimpleRemapper échoue quand une méthode est héritée : l'appel dans le
    // bytecode utilise la sous-classe comme owner, mais le mapping est défini
    // sur la super-classe. On lit la hiérarchie depuis le JAR pour propager
    // les lookups vers les parents.
    private static void buildHierarchy(String jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath, false)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                try (InputStream is = jar.getInputStream(entry)) {
                    ClassReader cr = new ClassReader(is.readAllBytes());
                    String child = cr.getClassName();
                    String superName = cr.getSuperName();
                    if (superName != null && !superName.equals("java/lang/Object")) {
                        superClassMap.put(child, superName);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // Remapping du JAR
    // -------------------------------------------------------------------------

    private static void remapJar(String inputPath, String outputPath) throws Exception {
        HierarchyRemapper remapper = new HierarchyRemapper();
        Set<String> written = new HashSet<>();

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarFile input    = new JarFile(inputPath, false);
             JarOutputStream output = new JarOutputStream(new FileOutputStream(outputPath), manifest)) {

            Enumeration<JarEntry> entries = input.entries();
            int count = 0, skipped = 0;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("META-INF/") && (
                        name.equals("META-INF/MANIFEST.MF") ||
                        name.endsWith(".SF") || name.endsWith(".RSA") ||
                        name.endsWith(".DSA") || name.endsWith(".EC"))) {
                    continue;
                }

                try (InputStream is = input.getInputStream(entry)) {
                    byte[] data   = is.readAllBytes();
                    String outName = name;

                    if (name.endsWith(".class")) {
                        try {
                            data = remapClass(data, remapper);
                        } catch (Exception e) {
                            System.err.println("\n  Warning: remap " + name + " : " + e.getMessage());
                        }
                        String internalName = name.substring(0, name.length() - 6);
                        String mapped = classMap.get(internalName);
                        if (mapped != null) outName = mapped + ".class";
                    }

                    if (!written.contains(outName)) {
                        output.putNextEntry(new JarEntry(outName));
                        output.write(data);
                        output.closeEntry();
                        written.add(outName);
                        count++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    System.err.println("\n  Warning: skip " + name + " : " + e.getMessage());
                }

                if ((count + skipped) % 1000 == 0 && count + skipped > 0)
                    System.out.print(".");
            }
            System.out.println();
            System.out.println("  Entrees ecrites: " + count + ", doublons ignores: " + skipped);
        }
    }

    private static byte[] remapClass(byte[] bytecode, HierarchyRemapper remapper) {
        ClassReader cr = new ClassReader(bytecode);
        ClassWriter cw = new ClassWriter(0);
        cr.accept(new ClassRemapper(cw, remapper), 0);
        return cw.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Remapper avec résolution de l'héritage
    // -------------------------------------------------------------------------

    private static class HierarchyRemapper extends org.objectweb.asm.commons.Remapper {

        @Override
        public String map(String internalName) {
            return asmMap.getOrDefault(internalName, internalName);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            String result = asmMap.get(owner + "." + name);
            if (result != null) return result;
            String parent = superClassMap.get(owner);
            if (parent != null) return mapFieldName(parent, name, descriptor);
            return name;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            String result = asmMap.get(owner + "." + name + descriptor);
            if (result != null) return result;
            // Méthode non trouvée sur owner → chercher dans la super-classe
            String parent = superClassMap.get(owner);
            if (parent != null) return mapMethodName(parent, name, descriptor);
            return name;
        }
    }

    private static boolean isIndented(String line) {
        return !line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t');
    }
}
