package Gastly.fr.mclauncher.data.Game_data;

import Gastly.fr.mclauncher.LauncherCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Ressourcepack {

    private static File ressource189 = new File("C:/Users/natpe/AppData/Roaming/.minecraft/game-data/1.8.9-1.12.2/resourcepacks");
    private static File ressource113 = new File("C:/Users/natpe/AppData/Roaming/.minecraft/game-data/1.13.2-1.21/resourcepacks");
    private static File ressourceMinecraft = new File("C:/Users/natpe/AppData/Roaming/.minecraft/resourcepacks");

    public static void getLoadPack() {
        // Vérifiez si les chemins sont des dossiers
        if (ressource113.isDirectory() && ressource189.isDirectory() && ressourceMinecraft.isDirectory()) {
            // Lister les fichiers et sous-dossiers
            File[] fichier189 = ressource189.listFiles();
            File[] fichier113 = ressource113.listFiles();
            File[] fichierMinecraft = ressourceMinecraft.listFiles();

            if (fichier189 != null && fichier113 != null && fichierMinecraft != null) {
                boolean identiques = isIdentical(fichier189, fichier113, fichierMinecraft);

                if (!identiques) {
                    LauncherCore.normal("> RessourcePack différents, synchronisation en cours...");
                    synchroniserRessourcePacks(fichier189, ressource189, fichier113, ressource113, fichierMinecraft, ressourceMinecraft);
                } else {
                    LauncherCore.normal("> RessourcePack present");
                }
            } else {
                System.out.println("Impossible de lister les fichiers dans l'un des dossiers.");
            }
        } else {
            System.out.println("Le chemin spécifié n'est pas un dossier.");
        }
    }

    private static boolean isIdentical(File[]... tableaux) {
        if (tableaux.length < 2) return true;

        // Trier le premier tableau comme référence
        File[] tableauReference = tableaux[0].clone();
        Arrays.sort(tableauReference, Comparator.comparing(File::getName)); // Tri par nom de fichier

        for (int i = 1; i < tableaux.length; i++) {
            File[] tableauCourant = tableaux[i].clone();
            Arrays.sort(tableauCourant, Comparator.comparing(File::getName)); // Tri par nom de fichier

            if (!Arrays.equals(tableauReference, tableauCourant)) {
                return false;
            }
        }
        return true;
    }

    public static void synchroniserRessourcePacks(File[] fichier189, File dossier189,
                                                  File[] fichier113, File dossier113,
                                                  File[] fichierMinecraft, File dossierMinecraft) {
        // Créer une liste de tous les noms de fichiers uniques
        Set<String> nomsFichiers = new HashSet<>();
        ajouterNomsFichiers(nomsFichiers, fichier189);
        ajouterNomsFichiers(nomsFichiers, fichier113);
        ajouterNomsFichiers(nomsFichiers, fichierMinecraft);

        // Synchroniser les fichiers dans chaque dossier
        synchroniserDossier(nomsFichiers, fichier189, dossier189, Arrays.asList(fichier113, fichierMinecraft));
        synchroniserDossier(nomsFichiers, fichier113, dossier113, Arrays.asList(fichier189, fichierMinecraft));
        synchroniserDossier(nomsFichiers, fichierMinecraft, dossierMinecraft, Arrays.asList(fichier189, fichier113));
    }

    private static void ajouterNomsFichiers(Set<String> nomsFichiers, File[] fichiers) {
        for (File fichier : fichiers) {
            nomsFichiers.add(fichier.getName());
        }
    }

    private static void synchroniserDossier(Set<String> nomsFichiers, File[] fichiersExistants, File dossier, List<File[]> autresDossiers) {
        Set<String> nomsExistants = new HashSet<>();
        for (File fichier : fichiersExistants) {
            nomsExistants.add(fichier.getName());
        }

        for (String nomFichier : nomsFichiers) {
            if (!nomsExistants.contains(nomFichier)) {
                // Trouver le fichier source dans les autres dossiers
                for (File[] autresFichiers : autresDossiers) {
                    File fichierSource = trouverFichierSource(nomFichier, autresFichiers);
                    if (fichierSource != null) {
                        File destination = new File(dossier, nomFichier);
                        try {
                            Files.copy(fichierSource.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            System.out.println("Erreur lors de la copie de " + fichierSource.getAbsolutePath() + " : " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        }
    }

    private static File trouverFichierSource(String nomFichier, File[] fichiers) {
        if (fichiers == null) return null;
        for (File fichier : fichiers) {
            if (fichier.getName().equals(nomFichier)) {
                return fichier;
            }
        }
        return null;
    }
}