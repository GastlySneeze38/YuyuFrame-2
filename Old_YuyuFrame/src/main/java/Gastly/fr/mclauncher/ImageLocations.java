package Gastly.fr.mclauncher;

import java.io.InputStream;
import java.net.URL;

public class ImageLocations {
    private static final ClassLoader CL = ImageLocations.class.getClassLoader();

    // Utilisation de URL (ex: pour ImageIcon)
    public static final URL DiscordEmptyImage = CL.getResource("assets/image/discord/logo-discord-empty.png");
    public static final URL DiscordVideImage = CL.getResource("assets/image/discord/logo-discord-vide.png");

    public static final URL ParameterEmptyImage = CL.getResource("assets/image/parametre/logo-parameter-empty.png");
    public static final URL ParameterVideImage = CL.getResource("assets/image/parametre/logo-parameter-vide.png");

    public static final URL LoginEmptyImage = CL.getResource("assets/image/Login/login-empty.png");
    public static final URL LoginHoverImage = CL.getResource("assets/image/Login/login-Hover.png");

    public static final URL ServerEmptyImage = CL.getResource("assets/image/Server/Server-empty.png");
    public static final URL ServerVideImage = CL.getResource("assets/image/Server/Server-vide.png");

    public static final URL QuestHoverImage = CL.getResource("assets/image/Quetes/Quetes-Hover.png");
    public static final URL QuestEmptyImage = CL.getResource("assets/image/Quetes/Quetes-empty.png");

    public static final URL GetCoinsEmptyImage = CL.getResource("assets/image/GetCoins/GetCoins_Base.png");
    public static final URL GetCoinsHoverImage = CL.getResource("assets/image/GetCoins/GetCoins_Empty.png");

    public static final URL DailyCoinsHoverImage = CL.getResource("assets/image/dailyCoins/calendrier-quotidien.png");
    public static final URL DailyCoinsEmptyImage = CL.getResource("assets/image/dailyCoins/calendrier-quotidien (2).png");

    public static final URL QuestCoinsHoverImage = CL.getResource("assets/image/QuestCoins/quete.png");
    public static final URL QuestCoinsEmptyImage = CL.getResource("assets/image/QuestCoins/quete (1).png");

    public static final URL TimeCoinsHoverImage = CL.getResource("assets/image/TimeCoins/1-heure.png");
    public static final URL TimeCoinsEmptyImage = CL.getResource("assets/image/TimeCoins/1-heure (1).png");

    public static final URL InformationHoverImage = CL.getResource("assets/image/Information/Information-Hover.png");
    public static final URL InformationEmptyImage = CL.getResource("assets/image/Information/Information-Empty.png");

    public static final URL ShaderEmptyImage = CL.getResource("assets/image/Texture.ShaderPack/ShaderPack.png");
    public static final URL TexturePackEmptyImage = CL.getResource("assets/image/Texture.ShaderPack/TexturePack.png");

    public static final URL ModEmptyImage = CL.getResource("assets/image/Mod/Mod-Empty.png");
    public static final URL ModHoverImage = CL.getResource("assets/image/Mod/Mod-Hover.png");

    public static final URL MineShotEmptyImage = CL.getResource("assets/image/MineShot/MineShot.png");
    public static final URL MineShotHoverImage = CL.getResource("assets/image/MineShot/MineShot-Hover.png");

    public static final URL UnconnectedImage = CL.getResource("assets/image/unconnected.jpg");
    public static final URL AccImage = CL.getResource("assets/image/imgAcceuille.jpg");
    public static final URL ServImage = CL.getResource("assets/image/imgservChill.jpg");
    public static final URL MicrosoftImage = CL.getResource("assets/image/Microsoft_logo.png");
    public static final URL CoinsImage = CL.getResource("assets/image/Coins.png");
    public static final URL IconImage = CL.getResource("assets/image/icon.png");
    public static final URL ValidateImage = CL.getResource("assets/image/Validate.png");
    public static final URL TitleClientFont = CL.getResource("assets/font/Super Vibes.ttf");

    // Exemple pour lire une police ou autre fichier via InputStream
    public static InputStream getFontStream() {
        return CL.getResourceAsStream("assets/font/Super Vibes.ttf");
    }

    public static InputStream getAudioStream(String fileName) {
        return CL.getResourceAsStream("assets/Audio/" + fileName);
    }

    public static InputStream getLangStream(String fileName) {
        return CL.getResourceAsStream("assets/lang/" + fileName);
    }
}
