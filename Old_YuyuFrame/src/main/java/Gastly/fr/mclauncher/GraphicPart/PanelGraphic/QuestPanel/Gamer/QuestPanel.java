package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.QuestPanel.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.*;
import Gastly.fr.mclauncher.ImageLocations;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class QuestPanel extends JPanel {

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame) {

        Color customColorWindow = new Color(19, 19, 21);
        Color CardColor = new Color(40, 40, 40);
        Color customColor = new Color(68, 62, 185);
        Color hovercolor = new Color(55, 42, 150);

        // Créer un panneau avec un layout null (sans gestionnaire de disposition)
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(customColorWindow);

        // Ajout du bouton pour revennir au menu principal
        JButton BackButton = new JButton("Back");
        BackButton.setFont(new Font("Arial", Font.BOLD, 16));
        BackButton.setHorizontalAlignment(SwingConstants.CENTER);
        BackButton.setBackground(customColorWindow);
        BackButton.setForeground(Color.WHITE);
        BackButton.setBorderPainted(false); // Pas de bordure peinte
        BackButton.setFocusPainted(false); // Pas de surlignement au focus
        BackButton.setBounds(2, 0, 996, 30);
        panel.add(BackButton);

        Personalize_GamerQuestComboBox QuestComboBox = new Personalize_GamerQuestComboBox(420, 510, CardColor, customColor);
        QuestComboBox.setBounds(30, 52, 430, 510);
        panel.add(QuestComboBox);

        Personalize_CardSetting QuestCard = new Personalize_CardSetting(460, 550, CardColor);
        QuestCard.setBounds(20, 40, 460, 550);
        panel.add(QuestCard);

        Personalize_GamerJSeparator RewardsSeparator = new Personalize_GamerJSeparator("Rewards", 20, customColorWindow);
        RewardsSeparator.setBounds(490, 380, 460, 40);
        panel.add(RewardsSeparator);

        Image DailyIcon = new ImageIcon(ImageLocations.DailyCoinsEmptyImage).getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        Personalize_GamerNavBar DailyCoins = new Personalize_GamerNavBar(new ImageIcon(DailyIcon), 50);
        DailyCoins.setBounds(515, 435, 50, 50);
        panel.add(DailyCoins);

        Personalize_GamerButtons labelDaily = new Personalize_GamerButtons(String.valueOf(DataBaseOfUser.DailyCoins), 10);
        labelDaily.setForeground(Color.WHITE);
        labelDaily.setFont(new Font("Arial", Font.BOLD, 15));
        labelDaily.setBounds(515, 490, 50, 25);
        panel.add(labelDaily);

        Personalize_CardSetting DailyCard = new Personalize_CardSetting(460, 350, new Color(149, 99, 39));
        DailyCard.setBounds(510, 430, 70, 100);
        panel.add(DailyCard);

        Image TimeIcon = new ImageIcon(ImageLocations.TimeCoinsEmptyImage).getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        Personalize_GamerNavBar TimeCoins = new Personalize_GamerNavBar(new ImageIcon(TimeIcon), 50);
        TimeCoins.setBounds(595, 435, 50, 50);
        panel.add(TimeCoins);

        Personalize_GamerButtons labelTime = new Personalize_GamerButtons(String.valueOf(DataBaseOfUser.Coins), 15);
        labelTime.setForeground(Color.WHITE);
        labelTime.setFont(new Font("Arial", Font.BOLD, 15));
        labelTime.setBounds(595, 490, 50, 25);
        panel.add(labelTime);

        Personalize_CardSetting TimeCard = new Personalize_CardSetting(460, 350, new Color(149, 130, 39));
        TimeCard.setBounds(590, 430, 70, 100);
        panel.add(TimeCard);

        Image GetCoinIcon = new ImageIcon(ImageLocations.GetCoinsEmptyImage).getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        Personalize_GamerNavBar GetQuestCoins = new Personalize_GamerNavBar(new ImageIcon(GetCoinIcon), customColorWindow, 100, ImageLocations.GetCoinsHoverImage, ImageLocations.GetCoinsEmptyImage);
        GetQuestCoins.setBounds(670, 425, 100, 100);
        panel.add(GetQuestCoins);

        Image QuestRewardIcon = new ImageIcon(ImageLocations.QuestCoinsEmptyImage).getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        Personalize_GamerNavBar QuestRewardCoins = new Personalize_GamerNavBar(new ImageIcon(QuestRewardIcon), 50);
        QuestRewardCoins.setBounds(785, 435, 50, 50);
        panel.add(QuestRewardCoins);

        Personalize_GamerButtons labelQuest = new Personalize_GamerButtons(String.valueOf(DataBaseOfUser.DailyCoins), 15);
        labelQuest.setForeground(Color.WHITE);
        labelQuest.setFont(new Font("Arial", Font.BOLD, 15));
        labelQuest.setBounds(785, 490, 50, 25);
        panel.add(labelQuest);

        JSeparator QuestSeparator = new JSeparator(SwingConstants.VERTICAL);
        QuestSeparator.setBackground(new Color(19, 19, 21, 150));
        QuestSeparator.setForeground(new Color(19, 19, 21, 150));
        QuestSeparator.setBounds(845, 435, 3, 80);
        panel.add(QuestSeparator);

        Image ValidateIcon = new ImageIcon(ImageLocations.ValidateImage).getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH);
        JButton ValidateQuest = new JButton("15", new ImageIcon(ValidateIcon));
        ValidateQuest.setOpaque(false);
        ValidateQuest.setContentAreaFilled(false);
        ValidateQuest.setBorderPainted(false);
        ValidateQuest.setFocusPainted(false);
        ValidateQuest.setForeground(Color.WHITE);
        ValidateQuest.setFont(new Font("Arial", Font.BOLD, 15));
        ValidateQuest.setHorizontalTextPosition(JLabel.LEADING);
        ValidateQuest.setBounds(850, 435, 70, 40);
        panel.add(ValidateQuest);

        Personalize_GamerButtons QuestPublication = new Personalize_GamerButtons("<html><center>Create<br>Quest</center></html>", 15, hovercolor, customColor);
        QuestPublication.setForeground(Color.WHITE);
        QuestPublication.setBounds(850, 472, 65, 40);
        panel.add(QuestPublication);

        Personalize_CardSetting QuestRewardCard = new Personalize_CardSetting(460, 350, new Color(120, 80, 120));
        QuestRewardCard.setBounds(780, 430, 150, 100);
        panel.add(QuestRewardCard);

        Image CoinsIcon = new ImageIcon(ImageLocations.CoinsImage).getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
        JLabel labelCoins = new JLabel(String.valueOf(DataBaseOfUser.Coins));
        labelCoins.setIcon(new ImageIcon(CoinsIcon));
        labelCoins.setHorizontalTextPosition(JLabel.LEADING); // Positionne le texte à gauche
        labelCoins.setOpaque(false);
        labelCoins.setForeground(Color.ORANGE);
        labelCoins.setFont(new Font("Arial", Font.BOLD, 25));
        labelCoins.setBounds(670, 550, 100, 25);
        panel.add(labelCoins);

        Personalize_GamerButtons LogCoins = new Personalize_GamerButtons("Log", 15, new Color(82, 82, 82));
        LogCoins.setForeground(Color.WHITE);
        LogCoins.setBounds(495, 545, 55, 30);
        panel.add(LogCoins);

        Personalize_CardSetting CoinsCard = new Personalize_CardSetting(460, 350, CardColor);
        CoinsCard.setBounds(490, 540, 460, 50);
        panel.add(CoinsCard);

        BackButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "MainPage");
            frame.setSize(1100, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });

        BackButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        BackButton.addMouseListener(new ColorTransformation(BackButton, customColorWindow, new Color(82, 82, 82), true));

        return panel;
    }
}