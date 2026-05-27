package com.p2pminecraft.runtime;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parseur du format ProGuard Mojang.
 *
 * Format :
 *   net.minecraft.world.level.chunk.LevelChunk -> abc:
 *       net.minecraft.world.level.Level level -> b
 *       void tick(java.util.function.BooleanSupplier) -> a
 *       1:5:net.minecraft.world.level.block.state.BlockState setBlockState(...) -> c
 */
public final class MappingsParser {

    public static final class ClassMapping {
        /** Nom obfusqué, format interne (slashes) — ex: "abc" ou "net/minecraft/server/level/aab" */
        public final String obfName;
        /**
         * Clés : "methodName" (fallback) ET "methodName(Lmojang/desc;)V" (prioritaire).
         * Valeur : nom obfusqué de la méthode.
         */
        public final Map<String, String> methods = new HashMap<>();
        /** Clé : nom Mojang du champ. Valeur : nom obfusqué. */
        public final Map<String, String> fields  = new HashMap<>();

        ClassMapping(String obfName) { this.obfName = obfName; }
    }

    private MappingsParser() {}

    /** Retourne : nom interne Mojang (slashes) → ClassMapping */
    public static Map<String, ClassMapping> parse(String path) throws IOException {
        Map<String, ClassMapping> result = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            ClassMapping current = null;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (!line.startsWith("    ")) {
                    // Ligne de classe : "net.minecraft.Foo -> abc:"
                    int arrow = line.indexOf(" -> ");
                    if (arrow < 0 || !line.endsWith(":")) continue;
                    String mojanDot = line.substring(0, arrow).trim();
                    String obfDot   = line.substring(arrow + 4, line.length() - 1).trim();
                    current = new ClassMapping(obfDot.replace('.', '/'));
                    result.put(mojanDot.replace('.', '/'), current);

                } else if (current != null) {
                    String trimmed = line.trim();
                    // Supprimer le préfixe de numéro de ligne "1:5:"
                    if (trimmed.length() > 0 && Character.isDigit(trimmed.charAt(0))) {
                        int colon2 = trimmed.indexOf(':', trimmed.indexOf(':') + 1);
                        if (colon2 >= 0) trimmed = trimmed.substring(colon2 + 1).trim();
                    }
                    int arrow = trimmed.lastIndexOf(" -> ");
                    if (arrow < 0) continue;
                    String memberDecl = trimmed.substring(0, arrow).trim();
                    String obfMember  = trimmed.substring(arrow + 4).trim();

                    int parenOpen = memberDecl.indexOf('(');
                    if (parenOpen >= 0) {
                        // Méthode : "returnType methodName(params)"
                        int nameStart = memberDecl.lastIndexOf(' ', parenOpen);
                        if (nameStart < 0) continue;
                        String methodName = memberDecl.substring(nameStart + 1, parenOpen);
                        String params     = memberDecl.substring(parenOpen + 1, memberDecl.lastIndexOf(')'));
                        String returnType = memberDecl.substring(0, nameStart).trim();

                        // Clé simple (fallback overload)
                        current.methods.put(methodName, obfMember);
                        // Clé avec descripteur Mojang
                        String desc = buildMethodDescriptor(returnType, params);
                        if (!desc.isEmpty()) {
                            current.methods.put(methodName + desc, obfMember);
                        }
                    } else {
                        // Champ : "fieldType fieldName"
                        int nameStart = memberDecl.lastIndexOf(' ');
                        if (nameStart < 0) continue;
                        String fieldName = memberDecl.substring(nameStart + 1);
                        current.fields.put(fieldName, obfMember);
                    }
                }
            }
        }
        return result;
    }

    // ── Conversion types ProGuard → descripteur JVM ───────────────────────────

    static String typeToDesc(String type) {
        type = type.trim();
        int dims = 0;
        while (type.endsWith("[]")) { dims++; type = type.substring(0, type.length() - 2).trim(); }
        String base;
        switch (type) {
            case "boolean": base = "Z"; break;
            case "byte":    base = "B"; break;
            case "char":    base = "C"; break;
            case "short":   base = "S"; break;
            case "int":     base = "I"; break;
            case "long":    base = "J"; break;
            case "float":   base = "F"; break;
            case "double":  base = "D"; break;
            case "void":    base = "V"; break;
            default:        base = "L" + type.replace('.', '/') + ";";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims; i++) sb.append('[');
        sb.append(base);
        return sb.toString();
    }

    static String buildMethodDescriptor(String returnType, String params) {
        try {
            StringBuilder sb = new StringBuilder("(");
            if (!params.isEmpty()) {
                for (String p : params.split(",")) {
                    sb.append(typeToDesc(p.trim()));
                }
            }
            sb.append(")").append(typeToDesc(returnType.trim()));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
