package Gastly.fr.mclauncher.Login;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;

import java.io.IOException;

public class API_auth {

    public static boolean authenticate(String email, String password) throws IOException {

        try {
            MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
            MicrosoftAuthResult result = authenticator.loginWithCredentials(email, password);
            String AccessToken = result.getAccessToken();
            String RefreshAccessToken = result.getRefreshToken();

            DataBaseOfUser.ReloadAssets(result.getProfile().getName(), result.getProfile().getId(), result.getAccessToken());
            DataBaseOfUser.saveAcount(result.getProfile().getName(), result.getProfile().getId(), AccessToken, RefreshAccessToken, 0, 0);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }
    }
    public static boolean refreshAccessToken() {
        try {
            // Charger le refresh token
            String refreshToken = DataBaseOfUser.getRefreshAccessToken();
            if (refreshToken == null) {
                System.err.println("Le refresh token est introuvable. Veuillez vous reconnecter.");
                return false;
            }

            // Rafraîchir l'access token
            MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
            
            MicrosoftAuthResult result = authenticator.loginWithRefreshToken(refreshToken);

            // Sauvegarder le nouvel access token et éventuellement le nouveau refresh token
            DataBaseOfUser.setAccessToken(result.getAccessToken());
            DataBaseOfUser.setRefreshAccessToken(result.getRefreshToken());
            DataBaseOfUser.saveToken(DataBaseOfUser.playerName, result.getAccessToken(), result.getRefreshToken());

            System.out.printf("Nouveau access token obtenu pour '%s'%n", result.getProfile().getName());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
    public static boolean simulateApiRequest(String accessToken) {
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        try{
            authenticator.loginWithRefreshToken(accessToken);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
