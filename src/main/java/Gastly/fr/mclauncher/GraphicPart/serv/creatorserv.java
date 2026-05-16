package Gastly.fr.mclauncher.GraphicPart.serv;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static Gastly.fr.mclauncher.LauncherCore.warning;

public class creatorserv {

    public static void Creator(String folderName, String version) {

        if (folderName.equalsIgnoreCase("") || folderName.equals("nom du server")) {
            warning("Mettez un nom de serveur.");

        } else {

            String urlFichier = URL_spigot.getServerJarLink(version);
            File dossier = new File("assets/serveur/servers/" + folderName);

            if (!dossier.exists()) {
                boolean resultat = dossier.mkdir();
                if (resultat) {
                    warning("Dossier créé avec succès : " + folderName);

                } else {
                    warning("Échec de la création du dossier.");
                    return;
                }
            }

            String destinationFichier = "assets/serveur/spigot/" + version + ".jar";
            File spigot = new File(destinationFichier);

            // Using SwingWorker to avoid freezing the UI
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    if (!spigot.exists()) {
                        try {
                            URL url = new URL(urlFichier);
                            try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                                 FileOutputStream fileOutputStream = new FileOutputStream(destinationFichier)) {

                                byte[] dataBuffer = new byte[4096];
                                int bytesRead;
                                long downloadedSize = 0;

                                // Lire et écrire des paquets de 4096 bytes
                                while ((bytesRead = in.read(dataBuffer, 0, 4096)) != -1) {
                                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                                    downloadedSize += bytesRead;

                                    // Afficher la progression (facultatif)
                                    int progress = (int) (downloadedSize * 100 / 49152000);
                                    warning("Progression : " + progress + "%\r");
                                }

                                warning("Téléchargement terminé : " + destinationFichier);

                            } catch (IOException e) {
                                e.printStackTrace();
                                warning("Erreur lors du téléchargement.");
                            }
                        } catch (MalformedURLException e) {
                            warning("L'URL est mal formée : " + e.getMessage());
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    // Copy the file once the download is done
                    Path sourcePath = Paths.get(destinationFichier);
                    Path destinationPath = Paths.get("assets/serveur/servers/" + folderName + "/Spigot.jar");

                    try {
                        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Fichier copié avec succès de " + sourcePath + " vers " + destinationPath);

                        // Create the batch file for server start
                        String cheminFichier = "assets/serveur/servers/" + folderName + "/Start.bat";
                        String contenuSupplementaire = "@echo off\n" + "java -Xms8G -Xmx8G -jar Spigot.jar\n" + "pause\n";

                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(cheminFichier, true))) {
                            bw.write(contenuSupplementaire);
                            bw.newLine();
                            System.out.println("Contenu ajouté avec succès.");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Erreur lors de l'ajout du contenu au fichier.");
                        }

                        // Lancer le fichier batch
                        lunchServ.Lunch(folderName);

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Erreur lors de la copie du fichier.");
                    }
                }
            };

            // Execute the SwingWorker
            worker.execute();
        }
    }
    public static void setEula(String serverFolder, boolean acceptEula) {
        // Construire le chemin vers eula.txt
        String eulaFilePath = "assets/serveur/servers/" + serverFolder + "/eula.txt";
        File eulaFile = new File(eulaFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(eulaFile))) {
            // Écrire la valeur de EULA
            writer.write("# By changing the setting below to TRUE you are indicating your agreement to the EULA (https://account.mojang.com/documents/minecraft_eula)\n");
            writer.write("eula=" + acceptEula + "\n");
            System.out.println("EULA a été défini sur " + acceptEula + " pour le serveur : " + serverFolder);
        } catch (IOException e) {
            System.err.println("Erreur lors de la modification du fichier eula.txt : " + e.getMessage());
        }
    }
}