package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel;

import Gastly.fr.mclauncher.data.Souds_Effect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class LogicParameter {

    static public int Ram = 4096;
    static public boolean disposeframetolaunch = false;
    static public boolean RefreshTheToken = true;
    static public boolean Gamer = true;
    public static boolean Accept_Eula = false;
    public static boolean SaveServ = true;
    public static float GlobalVolume = 0.75f;
    public static float HoverVolume = 0.75f;
    public static float ClickVolume = 0.75f;
    public static float MusicVolume = 0.75f;
    static public boolean ChangePadletToHour = false;

    static public int PadletMorning = 1;
    static public int PadletAfternoon = 2;
    static public int PadletNight = 3;
    static public int PadletDefault = 1;

    static public Boolean IsNotStarted = true;
    static public Boolean IsInProgress = true;
    static public Boolean IsFinish = false;

    public static void saveSettings(String filePath) {
        Properties properties = new Properties();
        properties.setProperty("Ram", String.valueOf(Ram));
        properties.setProperty("disposeframetolaunch", String.valueOf(disposeframetolaunch));
        properties.setProperty("RefreshTheToken", String.valueOf(RefreshTheToken));
        properties.setProperty("Gamer", String.valueOf(Gamer));
        properties.setProperty("AcceptEula", String.valueOf(Accept_Eula));
        properties.setProperty("SaveServ", String.valueOf(SaveServ));
        properties.setProperty("GlobalVolume", String.valueOf(GlobalVolume));
        properties.setProperty("HoverVolume", String.valueOf(HoverVolume));
        properties.setProperty("ClickVolume", String.valueOf(ClickVolume));
        properties.setProperty("MusicVolume", String.valueOf(MusicVolume));
        properties.setProperty("ChangePadletToHour", String.valueOf(ChangePadletToHour));
        properties.setProperty("PadletMorning", String.valueOf(PadletMorning));
        properties.setProperty("PadletAfternoon", String.valueOf(PadletAfternoon));
        properties.setProperty("PadletNight", String.valueOf(PadletNight));
        properties.setProperty("PadletDefault", String.valueOf(PadletDefault));
        properties.setProperty("IsNotStarted", String.valueOf(IsNotStarted));
        properties.setProperty("IsInProgress", String.valueOf(IsInProgress));
        properties.setProperty("IsFinish", String.valueOf(IsFinish));

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            properties.store(fos, "LogicParameter Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadSettings(String filePath) {
        Properties properties = new Properties();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Fichier non trouvé : " + filePath + ". Création d'un fichier avec les valeurs par défaut.");
            saveSettings(filePath); // Crée un fichier avec les valeurs par défaut
            return; // Fin de la méthode, les valeurs par défaut sont déjà définies
        }

        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            Ram = Integer.parseInt(properties.getProperty("Ram", "0"));
            disposeframetolaunch = Boolean.parseBoolean(properties.getProperty("disposeframetolaunch", "false"));
            RefreshTheToken = Boolean.parseBoolean(properties.getProperty("RefreshTheToken", "true"));
            Gamer = Boolean.parseBoolean(properties.getProperty("Gamer", "false"));
            Accept_Eula = Boolean.parseBoolean(properties.getProperty("AcceptEula", "false"));
            SaveServ = Boolean.parseBoolean(properties.getProperty("SaveServ", "true"));
            GlobalVolume = Float.parseFloat(properties.getProperty("GlobalVolume", "0.75f"));
            HoverVolume = Float.parseFloat(properties.getProperty("HoverVolume", "0.75f"));
            ClickVolume = Float.parseFloat(properties.getProperty("ClickVolume", "0.75f"));
            MusicVolume = Float.parseFloat(properties.getProperty("MusicVolume", "0.75f"));
            ChangePadletToHour = Boolean.parseBoolean(properties.getProperty("ChangePadletToHour", "true"));
            PadletMorning = Integer.parseInt(properties.getProperty("PadletMorning", "1"));
            PadletAfternoon = Integer.parseInt(properties.getProperty("PadletAfternoon", "2"));
            PadletNight = Integer.parseInt(properties.getProperty("PadletNight", "3"));
            PadletDefault = Integer.parseInt(properties.getProperty("PadletDefault", "2"));
            IsNotStarted = Boolean.parseBoolean(properties.getProperty("IsNotStarted", "true"));
            IsInProgress = Boolean.parseBoolean(properties.getProperty("IsInProgress", "true"));
            IsFinish = Boolean.parseBoolean(properties.getProperty("IsFinish", "false"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadAudioParameter(){
        Souds_Effect.setVolume("hover.wav", HoverVolume * GlobalVolume);
        Souds_Effect.setVolume("click.wav", ClickVolume * GlobalVolume);
    }
}

