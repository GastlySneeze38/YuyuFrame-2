package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.ModernScrollBarUI;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.QuestPanel.QuestLogic;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class Personalize_GamerQuestComboBox extends JPanel {

    private JPanel list = new JPanel();

    public Personalize_GamerQuestComboBox(int Width, int Height, Color WindowColor, Color ScrollBarColor) {
        setLayout(new BorderLayout());

        JPanel Filter = new JPanel();
        Filter.setOpaque(false);
        Filter.setPreferredSize(new Dimension(Width, 30));
        Filter.setLayout(new BorderLayout(30, 0));

        Personalize_GamerButtons NotStartedFilter = new Personalize_GamerButtons("Not started", 15);
        NotStartedFilter.setBackground((LogicParameter.IsNotStarted) ? new Color(68, 62, 185) : new Color(19, 19, 21));
        NotStartedFilter.setForeground(Color.WHITE);
        NotStartedFilter.setBounds(5, 0, 120, 25);
        Filter.add(NotStartedFilter, BorderLayout.WEST);

        NotStartedFilter.addActionListener(e -> {
            LogicParameter.IsNotStarted = !LogicParameter.IsNotStarted;
            reloadList();
            LogicParameter.saveSettings("settings.properties");
            Souds_Effect.play("click.wav");
        });
        NotStartedFilter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                NotStartedFilter.setBackground(new Color(55, 42, 150));
                Souds_Effect.play("hover.wav");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                Color backround = (LogicParameter.IsNotStarted) ? new Color(68, 62, 185) : new Color(19, 19, 21);
                NotStartedFilter.setBackground(backround);
            }
        });

        Personalize_GamerButtons InProgressFilter = new Personalize_GamerButtons("In Progress", 15);
        InProgressFilter.setBackground((LogicParameter.IsInProgress) ? new Color(68, 62, 185) : new Color(19, 19, 21));
        InProgressFilter.setForeground(Color.WHITE);
        InProgressFilter.setBounds(Width / 2 - 40, 0, 120, 25);
        Filter.add(InProgressFilter, BorderLayout.CENTER);

        InProgressFilter.addActionListener(e -> {
            LogicParameter.IsInProgress = !LogicParameter.IsInProgress;
            reloadList();
            LogicParameter.saveSettings("settings.properties");
            Souds_Effect.play("click.wav");
        });
        InProgressFilter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                InProgressFilter.setBackground(new Color(55, 42, 150));
                Souds_Effect.play("hover.wav");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                Color backround = (LogicParameter.IsInProgress) ? new Color(68, 62, 185) : new Color(19, 19, 21);
                InProgressFilter.setBackground(backround);
            }
        });

        Personalize_GamerButtons FinishFilter = new Personalize_GamerButtons("Finish", 15);
        FinishFilter.setBackground((LogicParameter.IsFinish) ? new Color(68, 62, 185) : new Color(19, 19, 21));
        FinishFilter.setForeground(Color.WHITE);
        FinishFilter.setBounds(Width - 80, 0, 120, 25);
        Filter.add(FinishFilter, BorderLayout.EAST);

        FinishFilter.addActionListener(e -> {
            LogicParameter.IsFinish = !LogicParameter.IsFinish;
            reloadList();
            LogicParameter.saveSettings("settings.properties");
            Souds_Effect.play("click.wav");
        });
        FinishFilter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                FinishFilter.setBackground(new Color(55, 42, 150));
                Souds_Effect.play("hover.wav");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                Color backround = (LogicParameter.IsFinish) ? new Color(68, 62, 185) : new Color(19, 19, 21);
                FinishFilter.setBackground(backround);
            }
        });

        add(Filter, BorderLayout.NORTH);

        // Créer un JPanel pour le contenu
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); // Permet d'ajouter les composants verticaux

        // Ajouter des composants au JPanel list (exemple)
        reloadList();

        // Créer le JScrollPane pour contenir le JPanel
        JScrollPane scrollPane = new JScrollPane(list);
        list.setBackground(WindowColor);

        // Définir la taille du JScrollPane en fonction de la fenêtre
        scrollPane.setPreferredSize(new Dimension(Width, Height - 50));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(ScrollBarColor));
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Empêcher la barre de défilement horizontale si non nécessaire

        // Obtenir la barre de défilement verticale
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

        // Ajouter un MouseWheelListener pour personnaliser la vitesse de la molette
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Récupérer la quantité de défilement de la molette
                int notches = e.getWheelRotation();

                // Facteur de vitesse de défilement (plus grand pour un défilement plus rapide)
                int speedFactor = 25;  // Par exemple, multiplier par 3 la distance de la molette

                // Calculer la nouvelle valeur de la position de la barre de défilement
                int newValue = verticalScrollBar.getValue() + notches * speedFactor;

                // Limiter la valeur à l'intérieur des bornes de la barre de défilement
                if (newValue < verticalScrollBar.getMinimum()) {
                    newValue = verticalScrollBar.getMinimum();
                } else if (newValue > verticalScrollBar.getMaximum()) {
                    newValue = verticalScrollBar.getMaximum();
                }

                // Appliquer la nouvelle position à la barre de défilement
                verticalScrollBar.setValue(newValue);
            }
        });

        // Ajouter le JScrollPane au panel principal
        setBackground(WindowColor);
        add(scrollPane, BorderLayout.SOUTH);
    }
    private void reloadList(){
        list.removeAll();
        QuestLogic Quests = new QuestLogic();
        list.add(Box.createRigidArea(new Dimension(0, 15)));

        for (int i = 0; i < Quests.getNumberOfQuest(); i++) {
            Personalize_QuestComponent Quest = new Personalize_QuestComponent(380, (String) Quests.getName().get(i), (String) Quests.getDifficulty().get(i), (String) Quests.getCoins().get(i), (String) Quests.getQuestType().get(i), (String) Quests.getPercent().get(i));
            Quest.setMaximumSize(new Dimension(380, 130));

            if (!LogicParameter.IsNotStarted && Quest.getAdvancementQuest() == 0) continue;
            else if (!LogicParameter.IsInProgress && Quest.getAdvancementQuest() > 0 && Quest.getAdvancementQuest() < 100) continue;
            else if (!LogicParameter.IsFinish && Quest.getAdvancementQuest() == 100) continue;

            list.add(Quest);
            list.add(Box.createRigidArea(new Dimension(0, 25)));
        }
        list.revalidate();
        list.repaint();
    }
}