package Gastly.fr.mclauncher.GraphicPart.serv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class lunchServ {

    public static void Lunch(String folderName) {

        String cheminFichier = "assets/serveur/servers/" + folderName + "/Start.bat";

        // Lancer le fichier batch
        File batchFile = new File(cheminFichier); // Fichier batch
        if (batchFile.exists()) {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", batchFile.getAbsolutePath());
            pb.directory(batchFile.getParentFile()); // Définit le dossier de travail
            pb.inheritIO(); // Hérite de l'entrée/sortie de la console

            try {
                creatorserv.setEula(folderName, true);
                Process process = pb.start();

            } catch (IOException e) {

            e.printStackTrace();
            System.out.println("Erreur lors de l'ajout du contenu au fichier.");

            }
        }
    }
    public static String[] getServer() {
        List<String> nameServ = new ArrayList<>();

        String chemin = "assets/serveur/servers"; // Remplacez par le chemin réel
        File dossier = new File(chemin);

        // Vérifie si le chemin est un dossier
        if (dossier.isDirectory()) {
            // Liste les fichiers et dossiers à l'intérieur
            File[] fichiers = dossier.listFiles();

            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (fichier.isDirectory()) {
                        // Vérifie la présence des fichiers requis
                        boolean contientStartBat = new File(fichier, "Start.Bat").exists();
                        boolean contientSpigotJar = new File(fichier, "Spigot.jar").exists();
                        boolean contientEulaTxt = new File(fichier, "Eula.txt").exists();

                        // Affiche le résultat pour ce dossier
                        if (contientStartBat && contientSpigotJar && contientEulaTxt) {
                            // Ajoute le nom du serveur à la liste
                            nameServ.add(fichier.getName());

                        } else if (!contientEulaTxt && contientSpigotJar && contientStartBat) {
                            // Appeler une méthode pour créer le fichier Eula.txt
                            creatorserv.setEula(fichier.getName(), true);
                            nameServ.add(fichier.getName());
                        }
                    }
                }
            } else {
                System.out.println("Impossible de lire le contenu du dossier.");
            }
        } else {
            System.out.println("Le chemin spécifié n'est pas un dossier.");
        }
        return nameServ.toArray(new String[0]);
    }
}
