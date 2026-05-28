package com.p2pminecraft.runtime;

import org.spongepowered.asm.mixin.extensibility.IRemapper;

import java.io.IOException;
import java.util.*;

/**
 * Registre statique des mappings Mojang → obfusqué.
 *
 * Implémente {@link IRemapper} pour être enregistré dans la chaîne de remapping Mixin via :
 *   MixinEnvironment.getDefaultEnvironment().getRemappers().add(MappingsRegistry.INSTANCE)
 *
 * Quand les mappings ne sont pas chargés, tous les accesseurs retournent le nom Mojang tel quel
 * (comportement correct avec le JAR mappé où les noms Mojang sont réels).
 */
public final class MappingsRegistry implements IRemapper {

    /** Instance singleton pour l'enregistrement Mixin. */
    public static final MappingsRegistry INSTANCE = new MappingsRegistry();

    // ── État statique ─────────────────────────────────────────────────────────

    private static volatile boolean loaded = false;

    /** Nom interne Mojang (slashes) → ClassMapping */
    private static Map<String, MappingsParser.ClassMapping> byMojang = Collections.emptyMap();
    /** Nom interne obfusqué (slashes) → ClassMapping */
    private static Map<String, MappingsParser.ClassMapping> byObf    = Collections.emptyMap();
    /** Nom interne obfusqué → nom interne Mojang (pour unmap) */
    private static Map<String, String> obfToMojang = Collections.emptyMap();

    private MappingsRegistry() {}

    // ── Chargement ────────────────────────────────────────────────────────────

    public static void load(String path) throws IOException {
        Map<String, MappingsParser.ClassMapping> parsed = MappingsParser.parse(path);
        Map<String, MappingsParser.ClassMapping> obfMap = new HashMap<>(parsed.size() * 2);
        Map<String, String> reverse = new HashMap<>(parsed.size() * 2);
        for (Map.Entry<String, MappingsParser.ClassMapping> e : parsed.entrySet()) {
            obfMap.put(e.getValue().obfName, e.getValue());
            reverse.put(e.getValue().obfName, e.getKey());
        }
        byMojang   = parsed;
        byObf      = obfMap;
        obfToMojang = reverse;
        loaded     = true;
        System.out.println("[P2P] Mappings chargés : " + parsed.size() + " classes");
    }

    public static boolean isLoaded() { return loaded; }

    // ── API statique (runtime) ────────────────────────────────────────────────

    /** Nom pointillé obfusqué pour Class.forName(). Retourne nom Mojang si inconnu. */
    public static String getObfClassDot(String mojanSlash) {
        if (!loaded) return mojanSlash.replace('/', '.');
        MappingsParser.ClassMapping cm = byMojang.get(mojanSlash);
        return cm != null ? cm.obfName.replace('/', '.') : mojanSlash.replace('/', '.');
    }

    /**
     * Charge la classe via son nom Mojang interne.
     * Tente le classloader de contexte (MC URLClassLoader, accès aux libs non obfusquées
     * comme ResourceLocation) puis le classloader système (classes obfusquées du jar MC).
     */
    public static Class<?> loadClass(String mojanSlash) throws ClassNotFoundException {
        String name = getObfClassDot(mojanSlash);
        // Classloader de contexte en premier : il a accès aux JARs de bibliothèque MC
        // (ResourceLocation, etc.) qui ne sont pas dans le classpath système de l'agent.
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        if (ctx != null) {
            try { return Class.forName(name, false, ctx); } catch (ClassNotFoundException ignored) {}
        }
        // Fallback : classloader système (contient les classes obfusquées du jar MC principal)
        return Class.forName(name);
    }

    /** Vérifie si obj est une instance de la classe identifiée par son nom Mojang interne. */
    public static boolean isInstance(Object obj, String mojanSlash) {
        if (obj == null) return false;
        try {
            return loadClass(mojanSlash).isInstance(obj);
        } catch (ClassNotFoundException e) {
            // Fallback : vérification sur le nom simple de la classe
            String simpleName = mojanSlash.substring(mojanSlash.lastIndexOf('/') + 1);
            return obj.getClass().getName().contains(simpleName);
        }
    }

    /**
     * Nom obfusqué d'une méthode identifiée par la classe Mojang (slash) + nom de méthode.
     * Priorité : classe exacte → retour Mojang si inconnu.
     */
    public static String getObfMethodName(String mojanClassSlash, String mojanMethod) {
        if (!loaded) return mojanMethod;
        MappingsParser.ClassMapping cm = byMojang.get(mojanClassSlash);
        if (cm != null) {
            String r = cm.methods.get(mojanMethod);
            if (r != null) return r;
        }
        return mojanMethod;
    }

    /**
     * Nom obfusqué d'un champ identifié par la classe Mojang (slash) + nom du champ.
     */
    public static String getObfFieldName(String mojanClassSlash, String mojanField) {
        if (!loaded) return mojanField;
        MappingsParser.ClassMapping cm = byMojang.get(mojanClassSlash);
        if (cm != null) {
            String r = cm.fields.get(mojanField);
            if (r != null) return r;
        }
        return mojanField;
    }

    // ── IRemapper — utilisé par Mixin pour remap les cibles ──────────────────

    @Override
    public String map(String typeName) {
        if (!loaded || typeName == null) return typeName;
        MappingsParser.ClassMapping cm = byMojang.get(typeName);
        return cm != null ? cm.obfName : typeName;
    }

    @Override
    public String unmap(String typeName) {
        if (!loaded || typeName == null) return typeName;
        String mojang = obfToMojang.get(typeName);
        return mojang != null ? mojang : typeName;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        if (!loaded || name == null) return name;
        // owner peut être Mojang OU obf selon le stade de traitement Mixin
        MappingsParser.ClassMapping cm = byObf.containsKey(owner) ? byObf.get(owner) : byMojang.get(owner);
        if (cm == null) return name;
        // 1. Tentative avec descripteur exact (Mojang form)
        if (desc != null) {
            String r = cm.methods.get(name + desc);
            if (r != null) return r;
        }
        // 2. Fallback : nom seul
        return cm.methods.getOrDefault(name, name);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        if (!loaded || name == null) return name;
        MappingsParser.ClassMapping cm = byObf.containsKey(owner) ? byObf.get(owner) : byMojang.get(owner);
        if (cm == null) return name;
        return cm.fields.getOrDefault(name, name);
    }

    @Override
    public String mapDesc(String desc) {
        if (!loaded || desc == null) return desc;
        return remapDesc(desc, true);
    }

    @Override
    public String unmapDesc(String desc) {
        if (!loaded || desc == null) return desc;
        return remapDesc(desc, false);
    }

    // ── Remapping des descripteurs ────────────────────────────────────────────

    private static String remapDesc(String desc, boolean mojangToObf) {
        StringBuilder sb = new StringBuilder(desc.length());
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i++);
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi < 0) { sb.append('L').append(desc.substring(i)); break; }
                String cls = desc.substring(i, semi);
                String mapped;
                if (mojangToObf) {
                    MappingsParser.ClassMapping cm = byMojang.get(cls);
                    mapped = cm != null ? cm.obfName : cls;
                } else {
                    mapped = obfToMojang.getOrDefault(cls, cls);
                }
                sb.append('L').append(mapped).append(';');
                i = semi + 1;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
