package Gastly.fr.mclauncher.GraphicPart.serv;

import java.util.HashMap;
import java.util.Map;

public class URL_spigot {

    public static final Map<String, String> serverJarLinks = new HashMap<>();

    static {
        serverJarLinks.put("1.21", "https://piston-data.mojang.com/v1/objects/450698d1863ab5180c25d7c804ef0fe6369dd1ba/server.jar");
        serverJarLinks.put("1.20.4", "https://piston-data.mojang.com/v1/objects/8dd1a28015f51b1803213892b50b7b4fc76e594d/server.jar");
        serverJarLinks.put("1.20.1", "https://piston-data.mojang.com/v1/objects/84194a2f286ef7c14ed7ce0090dba59902951553/server.jar");
        serverJarLinks.put("1.19.2", "https://piston-data.mojang.com/v1/objects/f69c284232d7c7580bd89a5a4931c3581eae1378/server.jar");
        serverJarLinks.put("1.16.5", "https://launcher.mojang.com/v1/objects/35139deedbd5182953cf1caa23835da59ca3d7cd/server.jar");
        serverJarLinks.put("1.12.2", "https://launcher.mojang.com/mc/game/1.12.2/server/886945bfb2b978778c3a0288fd7fab09d315b25f/server.jar");
        serverJarLinks.put("1.8.9", "https://launcher.mojang.com/mc/game/1.8.9/server/b58b2ceb36e01bcd8dbf49c8fb66c55a9f0676cd/server.jar");
    }

    public static String getServerJarLink(String version) {
        return serverJarLinks.get(version);
    }
}