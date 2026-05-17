package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.*;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.Locations;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;

public class GamerParameterPanel {

    private static Color CardColor = new Color(40, 40, 40);

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame) {

        Color customColorWindow = new Color(19, 19, 21);

        // Créer un panneau avec un layout null (sans gestionnaire de disposition)
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(customColorWindow);

        // Ajout du bouton pour revennir au menu principal
        JButton BackButton = new JButton("Back");
        BackButton.setFont(new Font("Arial", Font.BOLD, 16));
        BackButton.setHorizontalAlignment(SwingConstants.CENTER);
        BackButton.setBackground(new Color(19, 19, 21));
        BackButton.setForeground(Color.WHITE);
        BackButton.setBorderPainted(false); // Pas de bordure peinte
        BackButton.setFocusPainted(false); // Pas de surlignement au focus
        BackButton.setContentAreaFilled(false);
        BackButton.setOpaque(true);
        BackButton.setBounds(2, 0, 1096, 30);
        panel.add(BackButton);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator = new Personalize_GamerJSeparator("Config Ram", 20, customColorWindow);
        separator.setBounds(20, 50, 1060, 40);
        panel.add(separator);

        Personalize_GamerJSlider slider = new Personalize_GamerJSlider(1024, 12288, 4096);
        slider.setBounds(50,90,1000,80);
        panel.add(slider);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator1 = new Personalize_GamerJSeparator("UI", 20, CardColor);
        separator1.setBounds(30, 210, 550, 40);
        panel.add(separator1);

        //ajout des boolean
        Personalize_GamerJCheckBox disposeFrameCheck = new Personalize_GamerJCheckBox("Dispose Frame to Launch", LogicParameter.disposeframetolaunch, value -> LogicParameter.disposeframetolaunch = value, 490);
        disposeFrameCheck.setBounds(55, 260, 490, 30);
        panel.add(disposeFrameCheck);

        Personalize_GamerJCheckBox refreshTokenCheck = new Personalize_GamerJCheckBox("Refresh Token", LogicParameter.RefreshTheToken, value -> LogicParameter.RefreshTheToken = value, 490);
        refreshTokenCheck.setBounds(55, 310, 490, 30);
        panel.add(refreshTokenCheck);

        JLabel InComing = new JLabel("In Coming");
        InComing.setOpaque(true);
        InComing.setFont(new Font("Arial", Font.BOLD, 20));
        InComing.setForeground(Color.RED);
        InComing.setBackground(new Color(0, 0, 0, 50));
        InComing.setHorizontalAlignment(SwingConstants.CENTER);
        InComing.setBounds(55, 355, 490, 40);
        panel.add(InComing);

        Personalize_GamerJCheckBox gamerCheck = new Personalize_GamerJCheckBox("Gamer Mode", LogicParameter.Gamer, value -> LogicParameter.Gamer = value, 490);
        for (MouseListener al : gamerCheck.getListeners(MouseListener.class)) {gamerCheck.removeMouseListener(al);}
        for (ActionListener al : gamerCheck.getListeners(ActionListener.class)) {gamerCheck.removeActionListener(al);}
        gamerCheck.setBounds(55, 360, 490, 30);
        panel.add(gamerCheck);

        Personalize_GamerJCheckBox changePadletCheck = new Personalize_GamerJCheckBox("Change Padlet to Hour", LogicParameter.ChangePadletToHour, value -> LogicParameter.ChangePadletToHour = value, gamerCheck, 490);
        changePadletCheck.setBounds(55, 410, 490, 30);
        panel.add(changePadletCheck);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator2 = new Personalize_GamerJSeparator("JDK Folder", 20, CardColor);
        separator2.setBounds(30, 410, 550, 40);
        panel.add(separator2);

        JLabel PathM = new JLabel("Minecraft Path");
        PathM.setOpaque(false);
        PathM.setForeground(Color.WHITE);
        PathM.setFont(new Font("Arial", Font.BOLD, 16));
        PathM.setBounds(65, 470, 120, 30);
        panel.add(PathM);

        Personalize_GamerTextField MTPath = new Personalize_GamerTextField(Locations.rootFolder.getPath());
        MTPath.setBounds(65, 500, 500, 30);
        panel.add(MTPath);

        JLabel PathJ = new JLabel("Java Path");
        PathJ.setOpaque(false);
        PathJ.setForeground(Color.WHITE);
        PathJ.setFont(new Font("Arial", Font.BOLD, 16));
        PathJ.setBounds(65, 550, 120, 30);
        panel.add(PathJ);

        Personalize_GamerTextField JTPath = new Personalize_GamerTextField(Locations.rootFolder.getPath());
        JTPath.setBounds(65, 580, 500, 30);
        panel.add(JTPath);

        Personalize_CardSetting Card1 = new Personalize_CardSetting(575, 450, CardColor);
        Card1.setBounds(20, 200, 575, 450);
        panel.add(Card1);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator3 = new Personalize_GamerJSeparator("Sounds", 20, CardColor);
        separator3.setBounds(615, 210, 465, 40);
        panel.add(separator3);

        Personalize_GamerMouseWheelBox GlobalwheelBox = new Personalize_GamerMouseWheelBox(LogicParameter.GlobalVolume, "Global Sounds", new Color(208, 181, 73));
        GlobalwheelBox.setBounds(650, 260, 400, 30);
        panel.add(GlobalwheelBox);

        Personalize_GamerMouseWheelBox HoverwheelBox = new Personalize_GamerMouseWheelBox(LogicParameter.HoverVolume, "Hover Sound", Color.WHITE, "hover.wav", value -> LogicParameter.HoverVolume = value);
        HoverwheelBox.setBounds(650, 330, 400, 30);
        panel.add(HoverwheelBox);

        Personalize_GamerMouseWheelBox ClickwheelBox = new Personalize_GamerMouseWheelBox(LogicParameter.ClickVolume, "Click Sound", Color.WHITE, "click.wav", value -> LogicParameter.ClickVolume = value);
        ClickwheelBox.setBounds(650, 380, 400, 30);
        panel.add(ClickwheelBox);

        Personalize_GamerMouseWheelBox MusiquewheelBox = new Personalize_GamerMouseWheelBox(LogicParameter.MusicVolume, "Music Sound", Color.WHITE, "musique.wav", value -> LogicParameter.MusicVolume = value);
        MusiquewheelBox.setBounds(650, 430, 400, 30);
        panel.add(MusiquewheelBox);

        Personalize_CardSetting Card2 = new Personalize_CardSetting(575, 300, CardColor);
        Card2.setBounds(605, 200, 485, 300);
        panel.add(Card2);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator4 = new Personalize_GamerJSeparator("Support", 20, CardColor);
        separator4.setBounds(615, 520, 465, 40);
        panel.add(separator4);

        JLabel prevention = new JLabel("If you change the graphic mode, please reload the launcher");
        prevention.setOpaque(false);
        prevention.setForeground(Color.RED);
        prevention.setFont(new Font("Arial", Font.BOLD, 13));
        prevention.setBounds(620, 555, 480, 30);

        Personalize_GamerButtons reloadTheLauncher = new Personalize_GamerButtons("Reload", 15, Color.RED);
        reloadTheLauncher.addActionListener(e -> frame.dispose());
        reloadTheLauncher.setForeground(Color.WHITE);
        reloadTheLauncher.setFont(new Font("Arial", Font.BOLD, 13));
        reloadTheLauncher.setBounds(620, 595, 100, 30);

        panel.add(reloadTheLauncher);
        panel.add(prevention);

        Personalize_CardSetting Card3 = new Personalize_CardSetting(575, 300, CardColor);
        Card3.setBounds(605, 510, 485, 140);
        panel.add(Card3);

        BackButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "MainPage");
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