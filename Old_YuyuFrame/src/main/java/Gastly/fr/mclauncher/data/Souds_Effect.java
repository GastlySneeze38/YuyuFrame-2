package Gastly.fr.mclauncher.data;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.ImageLocations;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Souds_Effect {

    private static final Map<String, Clip> ClipLoad = new HashMap<>();
    private static final Map<String, byte[]> CopyClipLoad = new HashMap<>();
    private static final Map<String, Float> VolumeLoad = new HashMap<>();

    private static final List<Long> callTimestamps = new ArrayList<>();
    private static int maxCallsPerSecond = 1;

    private static boolean isRateExceeded() {
        long currentTime = System.currentTimeMillis();
        long oneSecondAgo = currentTime - 50;

        // Supprimer les appels plus anciens qu'une seconde
        callTimestamps.removeIf(timestamp -> timestamp < oneSecondAgo);

        // Ajouter le timestamp actuel
        callTimestamps.add(currentTime);

        // Vérifier si le nombre d'appels dépasse la limite
        return callTimestamps.size() > maxCallsPerSecond;
    }

    private Clip loadAudio(String filePath) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        File audioFile = new File(filePath);
        Clip audioClip;

        if (!audioFile.exists() || !audioFile.getName().endsWith(".wav")) {
            return null;
        }

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        audioClip = AudioSystem.getClip();
        audioClip.open(audioStream);

        return audioClip;
    }

    private File[] listFiles(String directoryPath) {
        File directory = new File(directoryPath);

        // Vérifie si le chemin spécifié est un répertoire
        if (!directory.isDirectory()) {
            System.out.println("Le chemin spécifié n'est pas un répertoire valide : " + directoryPath);
            return null;
        }

        // Récupère la liste des fichiers et dossiers
        return directory.listFiles();
    }

    /**
     * Joue la piste audio chargée.
     */
    public static void play(String AudioName){
        // Charger les données audio en mémoire
        byte[] audioBytes = CopyClipLoad.get(AudioName);

        if (audioBytes != null) {

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try (BufferedInputStream bufferedInput = new BufferedInputStream(new ByteArrayInputStream(audioBytes))){
                        if (isRateExceeded()) {return null;}

                        AudioInputStream copyStream = AudioSystem.getAudioInputStream(bufferedInput);
                        Clip audioCopy = AudioSystem.getClip();
                        audioCopy.open(copyStream);

                        audioCopy.start();
                        setVolume(audioCopy, VolumeLoad.get(AudioName));

                        audioCopy.addLineListener(event -> {
                            if (event.getType() == LineEvent.Type.STOP) {
                                audioCopy.close();
                            }
                        });

                    } catch (Exception e) {
                        throw new RuntimeException(e);

                    }
                    return null;
                }
            };
            worker.execute();
        } else {
            System.err.println("Aucun fichier audio chargé !");
        }
    }

    /**
    * joue en boucle un son pour par exemple une musique
     */
    public static void playLoop(String AudioName, Boolean setVolume) {

        Clip Audio = ClipLoad.get(AudioName);

        if (Audio != null) {

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {

                    Audio.loop(Clip.LOOP_CONTINUOUSLY); // Lecture en boucle infinie
                    if (setVolume){
                        Souds_Effect.setVolume("musique.wav", LogicParameter.MusicVolume);
                    }
                    System.out.println("Lecture de l'audio en boucle...");
                    return null;
                }
            };
            worker.execute();

        } else {
            System.err.println("Aucun fichier audio chargé !");
        }
    }

    /**
     * Arrête la lecture de la piste audio.
     */
    public static void stop(String AudioName) {

        Clip Audio = ClipLoad.get(AudioName);

        if (Audio != null && Audio.isRunning()) {
            Audio.stop();
            System.out.println("Lecture arrêtée.");
        }
    }

    /**
     * Permet de géré le volume
     */

    public static void setVolume(String AudioName, float volume) {
        if (AudioName.equalsIgnoreCase("hover.wav") || AudioName.equalsIgnoreCase("click.wav")){
            VolumeLoad.replace(AudioName, volume + 0.10f);
            return;
        }

        Clip Audio = ClipLoad.get(AudioName);

        if (Audio != null) {
            FloatControl volumeControl = (FloatControl) Audio.getControl(FloatControl.Type.MASTER_GAIN);
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float gain = min + (max - min) * volume; // Échelle de 0.0 à 1.0
            volumeControl.setValue(gain);
        }
    }

    public static void setVolume(Clip Audio, float volume) {

        if (Audio != null) {
            FloatControl volumeControl = (FloatControl) Audio.getControl(FloatControl.Type.MASTER_GAIN);
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float gain = min + (max - min) * volume; // Échelle de 0.0 à 1.0
            volumeControl.setValue(gain);
        }
    }

    public static void setAllVolume(float volume){

        for (String key : CopyClipLoad.keySet()) {
            VolumeLoad.replace(key, volume + 0.10f);
        }
        for (String key : ClipLoad.keySet()) {
            setVolume(key, volume);
        }
    }

    public static void UpdateVolume(){
        setVolume("musique.wav", LogicParameter.MusicVolume);
        setVolume("click.wav", LogicParameter.ClickVolume);
        setVolume("hover.wav", LogicParameter.HoverVolume);
    }

    /**
     * Permet de récupérer le volume actuelle
     */
    public static float getVolume(String AudioName) {

        Clip Audio = ClipLoad.get(AudioName);

        if (Audio != null) {
            FloatControl volumeControl = (FloatControl) Audio.getControl(FloatControl.Type.MASTER_GAIN);
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float currentGain = volumeControl.getValue();
            return (currentGain - min) / (max - min); // Conversion en échelle 0.0 - 1.0

        } else {
            System.err.println("Aucun fichier audio chargé pour récupérer le volume !");
            return -1.0f; // Retourne -1.0 pour indiquer une erreur
        }
    }

    /**
     * Libère les ressources liées à l'audio.
     */
    public static void close(String AudioName) {

        Clip Audio = ClipLoad.get(AudioName);

        if (Audio != null) {
            Audio.close();
            ClipLoad.remove(Audio);
            System.out.println("Ressources audio libérées.");
        }
    }
    public Clip loadAudioFromInputStream(InputStream inputStream) {
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(bis);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static List<String> listAudioFilesInJar(String pathInJar) {
        List<String> fileNames = new ArrayList<>();

        try {
            URL url = ImageLocations.class.getClassLoader().getResource(pathInJar);
            if (url == null) {
                System.err.println("Ressource non trouvée : " + pathInJar);
                return fileNames;
            }

            if (url.getProtocol().equals("jar")) {
                // On lit à partir du JAR
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(pathInJar) && !entry.isDirectory()) {
                            String fileName = name.substring(pathInJar.length());
                            if (!fileName.isEmpty() && !fileName.contains("/")) {
                                fileNames.add(fileName);
                            }
                        }
                    }
                }
            } else if (url.getProtocol().equals("file")) {
                // Cas d'exécution en IDE
                File folder = new File(url.toURI());
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            fileNames.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileNames;
    }
    public static void Start(JFrame frame) {
        Souds_Effect player = new Souds_Effect();

        try {
            List<String> audioFiles = listAudioFilesInJar("assets/Audio");

            for (String fileName : audioFiles) {
                try (InputStream inputStream = ImageLocations.getAudioStream(fileName)) {
                    if (inputStream == null) {
                        System.err.println("Impossible de charger : " + fileName);
                        continue;
                    }

                    // Charge le clip depuis l'InputStream
                    Clip clip = player.loadAudioFromInputStream(inputStream);
                    if (clip == null) continue;

                    ClipLoad.put(fileName, clip);

                    // Recharge le flux pour les bytes (à cause de lecture unique d'un InputStream)
                    try (InputStream inputStreamCopy = ImageLocations.getAudioStream(fileName)) {
                        if (inputStreamCopy != null) {
                            CopyClipLoad.put(fileName, inputStreamCopy.readAllBytes());
                        }
                    }

                    VolumeLoad.put(fileName, getVolume(fileName));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        frame.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosing(WindowEvent e) {}

            @Override
            public void windowClosed(WindowEvent e) {}

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) { // Fenêtre restaurée
                System.out.println("Fenêtre restaurée : lecture reprise.");
                Souds_Effect.playLoop("musique.wav", false);
            }

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {}
        });
    }
}