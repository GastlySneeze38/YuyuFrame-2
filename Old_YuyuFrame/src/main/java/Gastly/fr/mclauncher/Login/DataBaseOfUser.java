package Gastly.fr.mclauncher.Login;

import Gastly.fr.mclauncher.ImageLocations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataBaseOfUser {

    private static final String FILE_PATH = "user_accounts.json";
    private static Map<String, Map<String, String>> userAccounts = new HashMap<>();

    public static String uuid;
    public static String accessToken;
    public static String playerName;
    private static String refreshAccessToken;
    public static int Coins;
    public static int DailyCoins;

    public DataBaseOfUser(String username, String uuid, String accessToken, int Coins, int DailyCoins) {
        playerName = username;
        DataBaseOfUser.uuid = uuid;
        DataBaseOfUser.accessToken = accessToken;
        DataBaseOfUser.Coins = Coins;
        DataBaseOfUser.DailyCoins = DailyCoins;
    }

    public static DataBaseOfUser getInstance() {return new DataBaseOfUser(playerName, uuid, accessToken, Coins, DailyCoins);}

    public static void ReloadAssets(String PlayerName, String Uuid, String AccessToken) {
        uuid = Uuid;
        accessToken = AccessToken;
        playerName = PlayerName;
    }

    // Getters et setters
    public static void setAccessToken(String AccessToken) {
        accessToken = AccessToken;
    }
    public static void setRefreshAccessToken(String RefreshAccessToken) {refreshAccessToken = RefreshAccessToken;}
    public static void setCoins(int coins) {
        Coins = coins;
        saveAccountsToFile();
    }
    public static void setDailyCoins(int Dailycoins){
        Coins = Dailycoins;
        saveAccountsToFile();
    }

    public static void loadAccount(String userId) {

        File file = new File(FILE_PATH);
        Map<String, String> userDetails = userAccounts.get(userId);

        if (file.exists() && userDetails != null) {
            try {
                uuid = userDetails.get("Uuid");
                playerName = userDetails.get("PlayerName");
                accessToken = userDetails.get("AccessToken");
                refreshAccessToken = userDetails.get("RefreshAccessToken");
                Coins = Integer.parseInt(userDetails.get("Coins"));
                DailyCoins = Integer.parseInt(userDetails.get("DailyCoins"));

            }catch (Exception e){
                throw new RuntimeException(e);

            }
        }else {
            uuid = "123e4567-e89b-12d3-a456-426614174000";
            playerName = "Solo_Party";
            accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtaW5lY3JhZnQiLCJhdWQiOiJzZXJ2ZXIiLCJleHAiOjE2ODk1ODQwMDAsImlhdCI6MTY4OTU0ODAwMH0.w7zL-8euh5RdQzA9GzEaQKmfuX1lt-L4Y7Pa56_gJfI";
            refreshAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtaW5lY3JhZnQiLCJhdWQiOiJzZXJ2ZXIiLCJleHAiOjE2ODk1ODQwMDAsImlhdCI6MTY4OTU0ODAwMH0.w7zL-8euh5RdQzA9GzEaQKmfuX1lt-L4Y7Pa56_gJfI";
            Coins = 0;
            DailyCoins = 0;
        }
    }

    public static void saveAcount(String PlayerName, String Uuid, String AccesToken, String refreshAcessToken, int coins, int Dailycoins) {

        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("Uuid", Uuid);
        userDetails.put("PlayerName", PlayerName);
        userDetails.put("AccessToken", AccesToken);
        userDetails.put("RefreshAccessToken", refreshAcessToken);
        userDetails.put("Coins", String.valueOf(coins));
        userDetails.put("DailyCoins", String.valueOf(Dailycoins));

        userAccounts.put(PlayerName, userDetails);

        saveAccountsToFile();
    }

    public static void saveToken(String PlayerName, String AccesToken, String refreshAcessToken) {

        Map<String, String> userDetails = userAccounts.get(PlayerName);
        userDetails.remove("AccessToken");
        userDetails.remove("RefreshAccessToken");

        userDetails.put("AccessToken", AccesToken);
        userDetails.put("RefreshAccessToken", refreshAcessToken);

        saveAccountsToFile();
    }

    public static void saveAccountsToFile() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(userAccounts);

            writer.write(jsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadAccounts() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                userAccounts = gson.fromJson(reader, Map.class);
                if (userAccounts == null) {
                    userAccounts = new HashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Set<String> getAccountIds() {
        loadAccounts();
        return userAccounts.keySet();
    }

    public static void deleteAccount(String playerName) {
        if (userAccounts.containsKey(playerName)) {
            userAccounts.remove(playerName);
            System.out.println("Compte " + playerName + " supprimé.");
            saveAccountsToFile(); // Sauvegarder les modifications dans le fichier

            if (haveAccount()){

                Set<String> accountIds = DataBaseOfUser.getAccountIds();
                String[] accountArray = accountIds.toArray(new String[0]);
                loadAccount(accountArray[0]);

            }else{
                loadAccount(playerName);
            }

        } else {
            System.out.println("Le joueur " + playerName + " n'existe pas.");
        }
    }

    public static boolean isTokenExpired(String accessToken) {

        if (playerName != "Solo_Party"){
            try {
                // Exemple d'URL API avec authentification via access token
                URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Ajouter le token dans l'en-tête Authorization
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                // Envoyer la requête et obtenir la réponse
                int responseCode = connection.getResponseCode();

                // Si le code de réponse est 401, le token est probablement expiré
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    System.out.println("Token expired.");
                    return true; // Token périmé
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false; // Token valide
        }
        return false;
    }

    public static boolean haveAccount() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return false; // Le fichier n'existe pas
        }

        try {
            // Lire le contenu du fichier
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();

            // Vérifier si le contenu est vide ou contient uniquement "{}"
            if(!content.isEmpty() && !content.equals("{}")) {
                return true;
            }

            // Contenu invalide
        } catch (IOException e) {
            return false; // En cas d'erreur de lecture
        }
        return false;
    }

    public static ImageIcon getPlayerImage() throws MalformedURLException {
        URL imageURL;
        ImageIcon icon;

        if (DataBaseOfUser.haveAccount()) {
            imageURL = new URL("https://crafatar.com/avatars/" + DataBaseOfUser.uuid + "?size=100&overlay");
            if (isImageAccessible(imageURL)) {
                icon = new ImageIcon(imageURL);
            } else {
                icon = new ImageIcon(ImageLocations.UnconnectedImage); // Image par défaut en cas d'échec
            }
        } else {
            icon = new ImageIcon(ImageLocations.UnconnectedImage);
        }
        return icon;
    }

    private static boolean isImageAccessible(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // Timeout de 5 secondes
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;

        } catch (IOException e) {
            e.printStackTrace();
            return false; // Considérer comme inaccessible en cas d'exception
        }
    }

    public String getUsername(){return playerName;}
    public String getUuid() {return uuid;}
    public String getAccessToken() {return accessToken;}
    public static String getRefreshAccessToken() {return refreshAccessToken;}

}