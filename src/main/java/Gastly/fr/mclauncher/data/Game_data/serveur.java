package Gastly.fr.mclauncher.data.Game_data;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class serveur {

    public static String calculerHash(String cheminFichier) throws NoSuchAlgorithmException, IOException {
        // Création d'une instance de MessageDigest pour SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(cheminFichier)) {
            byte[] byteArray = new byte[8192];
            int bytesRead;

            // Lire le fichier par morceaux et mettre à jour le digest
            while ((bytesRead = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
        }

        // Obtenir le hash final
        byte[] hashBytes = digest.digest();

        // Convertir le tableau de bytes en une chaîne hexadécimale
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    public static void getLoadServer() {
        try {
            String fichier1 = "C:/Users/natpe/AppData/Roaming/.minecraft/game-data/1.8.9-1.12.2/servers.dat";
            String fichier2 = "C:/Users/natpe/AppData/Roaming/.minecraft/game-data/1.13.2-1.21/servers.dat";;
            String fichier3 = "C:/Users/natpe/AppData/Roaming/.minecraft/servers.dat";

            // Calculer les hashes des fichiers
            String hashFichier1 = calculerHash(fichier1);
            String hashFichier2 = calculerHash(fichier2);
            String hashFichier3 = calculerHash(fichier3);

            // Afficher les résultats
            System.out.println(fichier1 + ": " + hashFichier1);
            System.out.println(fichier2 + ": " + hashFichier2);
            System.out.println(fichier3 + ": " + hashFichier3);

            // Comparer les fichiers
            if (!hashFichier1.equals(hashFichier2)) {
                System.out.println(fichier1 + " et " + fichier2 + " sont différents.");
            } else {
                System.out.println(fichier1 + " et " + fichier2 + " sont identiques.");
            }

            if (!hashFichier1.equals(hashFichier3)) {
                System.out.println(fichier1 + " et " + fichier3 + " sont différents.");
            } else {
                System.out.println(fichier1 + " et " + fichier3 + " sont identiques.");
            }

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }
}
