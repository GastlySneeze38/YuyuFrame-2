package Gastly.fr.mclauncher.Login;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LoginWithWeb {

    private static final String CLIENT_ID = "TON_CLIENT_ID";
    private static final String REDIRECT_URI = "http://localhost:12345/callback";
    private static final String SCOPE = "XboxLive.signin offline_access";

    public static void openLoginPage() throws IOException {
        String url = "https://login.live.com/oauth20_authorize.srf?" +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);

        Desktop.getDesktop().browse(URI.create(url));
    }

}
