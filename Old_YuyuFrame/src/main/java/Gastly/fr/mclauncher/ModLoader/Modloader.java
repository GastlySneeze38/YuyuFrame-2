/*package Gastly.fr.mclauncher.ModLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Modloader implements ITweaker {

    private final List<String> modsToLoad = new ArrayList<>();

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        File modsDir = new File(gameDir, "mods");
        if (modsDir.exists()) {
            File[] jars = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    modsToLoad.add(jar.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        try {
            for (String path : modsToLoad) {
                classLoader.addURL(new File(path).toURI().toURL());
            }
            classLoader.registerTransformer("com.tonmodloader.ASMTransformer");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Chargement des mods implémentant IMod
        ServiceLoader<IMod> loader = ServiceLoader.load(IMod.class);
        for (IMod mod : loader) {
            mod.onLoad();
        }
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

}*/
