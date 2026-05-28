package com.p2pminecraft.mixin.service;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;

/**
 * Logger minimaliste pour Mixin — redirige vers System.out/err.
 */
public class P2PLogger implements ILogger {

    private final String id;

    public P2PLogger(String id) {
        this.id = id;
    }

    @Override public String getId()   { return id; }
    @Override public String getType() { return "System"; }

    @Override public void catching(Level lvl, Throwable t) { t.printStackTrace(System.err); }
    @Override public void catching(Throwable t)            { t.printStackTrace(System.err); }

    @Override public void debug(String msg, Object... args) { log(Level.DEBUG, msg, args); }
    @Override public void debug(String msg, Throwable t)    { log(Level.DEBUG, msg, t); }

    @Override public void info(String msg, Object... args) { log(Level.INFO, msg, args); }
    @Override public void info(String msg, Throwable t)    { log(Level.INFO, msg, t); }

    @Override public void warn(String msg, Object... args) { log(Level.WARN, msg, args); }
    @Override public void warn(String msg, Throwable t)    { log(Level.WARN, msg, t); }

    @Override public void error(String msg, Object... args) { log(Level.ERROR, msg, args); }
    @Override public void error(String msg, Throwable t)    { log(Level.ERROR, msg, t); }

    @Override public void fatal(String msg, Object... args) { log(Level.FATAL, msg, args); }
    @Override public void fatal(String msg, Throwable t)    { log(Level.FATAL, msg, t); }

    @Override public void trace(String msg, Object... args) {}
    @Override public void trace(String msg, Throwable t)    {}

    @Override
    public void log(Level lvl, String msg, Object... args) {
        String formatted = args.length == 0 ? msg : String.format(msg.replace("{}", "%s"), (Object[]) args);
        java.io.PrintStream out = (lvl.ordinal() >= Level.WARN.ordinal()) ? System.err : System.out;
        out.println("[Mixin/" + lvl + "] [" + id + "] " + formatted);
        writeLog("[" + lvl + "] [" + id + "] " + formatted + "\n");
    }

    @Override
    public void log(Level lvl, String msg, Throwable t) {
        log(lvl, msg);
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        t.printStackTrace(System.err);
        writeLog(sw.toString());
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        t.printStackTrace(System.err);
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        writeLog("THROWING: " + sw.toString());
        return t;
    }

    private static void writeLog(String msg) {
        try {
            java.nio.file.Path f = java.nio.file.Paths.get(
                System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_mixin.txt");
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.write(f, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }
}
