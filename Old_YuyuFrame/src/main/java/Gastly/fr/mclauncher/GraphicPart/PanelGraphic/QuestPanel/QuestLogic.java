package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.QuestPanel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class QuestLogic {

    private String id;
    private String JsonPath = "D:/YuyuFrame/Dev evironement/YuyuFrame/Quests.json";
    private int NumberOfQuest;

    private Map<Integer, String> Name = new HashMap<>();
    private Map<Integer, String> Difficulty = new HashMap<>();
    private Map<Integer, String> Coins = new HashMap<>();
    private Map<Integer, String> QuestType = new HashMap<>();

    private Map<Integer, String> Percent = new HashMap<>();

    public QuestLogic(String id){
        this.id = id;
    }
    public QuestLogic(){
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
        Map<String, Map<String, Object>> quests = gson.fromJson(getJson(), type);

        // Affichage des informations
        int index = 0;
        for (Map.Entry<String, Map<String, Object>> entry : quests.entrySet()) {
            Name.put(index, (String) entry.getValue().get("Name"));
            Difficulty.put(index, (String) entry.getValue().get("Difficulty"));
            Coins.put(index, (String) entry.getValue().get("Coins"));
            QuestType.put(index, (String) entry.getValue().get("QuestType"));

            Map<String, Object> Progress = (Map<String, Object>) entry.getValue().get("Progress");
            Percent.put(index, (String) Progress.get("percent"));

            index++;
        }
        NumberOfQuest = index;
    }
    private String getJson(){
        try (BufferedReader reader = new BufferedReader(new FileReader(JsonPath))) {
            // Lire tout le contenu du fichier JSON
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            String json = jsonBuilder.toString();

            return json;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getNumberOfQuest(){return NumberOfQuest;}
    public Map getName(){return Name;}
    public Map getDifficulty(){return Difficulty;}
    public Map getCoins(){return Coins;}
    public Map getQuestType(){return QuestType;}
    public Map getPercent(){return Percent;}
}
