package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ServerPanel.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.*;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.GraphicPart.serv.creatorserv;
import Gastly.fr.mclauncher.GraphicPart.serv.lunchServ;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ServerPanel {

    public static Boolean lauchOrCreateServ;

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame) {

        lauchOrCreateServ = true;

        Color customColorWindow = new Color(19, 19, 21);
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
        BackButton.setBackground(new Color(19, 19, 21));
        BackButton.setForeground(Color.WHITE);
        BackButton.setBorderPainted(false); // Pas de bordure peinte
        BackButton.setFocusPainted(false); // Pas de surlignement au focus
        BackButton.setBounds(2, 0, 496, 30);
        panel.add(BackButton);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator1 = new Personalize_GamerJSeparator("Settings", 20, customColorWindow);
        separator1.setBounds(20, 50, 460, 40);
        panel.add(separator1);

        Personalize_GamerJCheckBox acceptEula = new Personalize_GamerJCheckBox("Accept Eula", LogicParameter.Accept_Eula, value -> LogicParameter.Accept_Eula = value, 380);
        panel.add(acceptEula);

        Personalize_GamerJCheckBox Save_Serv = new Personalize_GamerJCheckBox("Save Server", LogicParameter.SaveServ, value -> LogicParameter.SaveServ = value, 380);
        panel.add(Save_Serv);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator2 = new Personalize_GamerJSeparator("Server", 20, customColorWindow);
        separator2.setBounds(20, 200, 460, 40);
        panel.add(separator2);

        List<JComponent> create = new ArrayList<>();;

        //liste déroulante pour la creation serv
        String[] versions_serv = { "1.21", "1.20.4", "1.20.1", "1.19.2", "1.16.5", "1.12.2", "1.8.9" };
        Personalize_Gamer_JComboBox versionservComboBox = new Personalize_Gamer_JComboBox(versions_serv);
        create.add(versionservComboBox);

        //ajouter l'entrer de text de creation de serveur
        Personalize_GamerTextField textField = new Personalize_GamerTextField("Server Name");
        create.add(textField);

        //ajouter le bouton de creation de serveur
        Personalize_GamerButtons createserv = new Personalize_GamerButtons("Create a server", 30, hovercolor, customColor);
        createserv.setBackground(customColor);
        createserv.setForeground(Color.BLACK);
        createserv.setFont(new Font("Arial", Font.BOLD, 16));
        createserv.setBorder(null);
        create.add(createserv);

        for (JComponent obj : create) {obj.setVisible(true);}

        List<JComponent> launchserv = new ArrayList<>();;

        //ajouter le bouton de creation de serveur
        Personalize_GamerButtons Launchbuttserv = new Personalize_GamerButtons("Lancer le server", 30, hovercolor, customColor);
        Launchbuttserv.setBackground(customColor);
        Launchbuttserv.setForeground(Color.BLACK);
        Launchbuttserv.setFont(new Font("Arial", Font.BOLD, 16));
        Launchbuttserv.setBorder(null);
        panel.add(Launchbuttserv);

        launchserv.add(Launchbuttserv);

        //ajouter l'entrer de text de creation de serveur
        Personalize_Gamer_JComboBox NameServ = new Personalize_Gamer_JComboBox(lunchServ.getServer());
        panel.add(NameServ);
        launchserv.add(NameServ);

        for (JComponent obj : launchserv) {obj.setVisible(false);}

        Personalize_GamerButtons changeServ = new Personalize_GamerButtons("▼", 20, new Color(82, 82, 82));
        changeServ.setForeground(Color.WHITE);
        changeServ.setFont(new Font("Arial", Font.BOLD, 24));
        changeServ.setBorder(null);

        Launchbuttserv.addActionListener(e -> {
            lunchServ.Lunch((String) NameServ.getSelectedItem());
            Souds_Effect.play("click.wav");
        });
        createserv.addActionListener(e -> {
            // Lancer Minecraft lorsque le bouton est cliqué
            creatorserv.Creator(textField.getText(), (String) versionservComboBox.getSelectedItem());
            Souds_Effect.play("click.wav");
        });
        changeServ.addActionListener(e ->{
            if (lauchOrCreateServ){
                for (JComponent obj : create) {obj.setVisible(false);}
                for (JComponent obj : launchserv) {obj.setVisible(true);}
                lauchOrCreateServ = false;

            }else {
                for (JComponent obj : create) {obj.setVisible(true);}
                for (JComponent obj : launchserv) {obj.setVisible(false);}
                lauchOrCreateServ = true;
            }
            NameServ.removeAllItems();
            for (String item : lunchServ.getServer()) {NameServ.addItem(item);}
            Souds_Effect.play("click.wav");
        });
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

        acceptEula.setBounds(50, 110, 400, 30);
        Save_Serv.setBounds(50, 150, 400, 30);
        versionservComboBox.setBounds(50, 280, 400, 50);
        textField.setBounds(50, 380, 400, 50);
        createserv.setBounds(50, 480, 400, 50);
        Launchbuttserv.setBounds(50, 330, 400, 50);
        NameServ.setBounds(50, 430, 400, 50);
        changeServ.setBounds(215,580, 70,70);

        panel.add(createserv);
        panel.add(textField);
        panel.add(versionservComboBox);
        panel.add(changeServ);

        return panel;
    }

}
