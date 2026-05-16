package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.InformationPanel.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class GamerInformationPanel {

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame){

        Color customColorWindow = new Color(19, 19, 21);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(customColorWindow);

        JButton Backbutton = new JButton("Back");
        Backbutton.setFont(new Font("Arial", Font.BOLD, 20));
        Backbutton.setBounds(2, 0, 596, 30);
        Backbutton.setBackground(customColorWindow);
        Backbutton.setForeground(Color.WHITE);
        Backbutton.setBorderPainted(false); // Pas de bordure peinte
        Backbutton.setFocusPainted(false); // Pas de surlignement au focus
        panel.add(Backbutton);

        String text = "<html>" +
                "<p style='font-size: 12px; text-align: center;'>" +
                "Je suis fier de vous présenter cette application.<br>" +
                "C'est un projet ambitieux sur lequel j’ai beaucoup travaillé.<br><br>" +
                "Je suis un jeune programmeur passionné.<br>" +
                "Ce produit est le fruit de <b>7 années</b> d’expérience.<br>" +
                "Il a été conçu en seulement <b>1 an</b>.<br><br>" +
                "<h2 style='text-align: center;'>État actuel</h2>" +
                "<p style='font-size: 12px; text-align: center;'>" +
                "Il y a un système de quêtes avec une monaie virtuelle.<br>" +
                "Dans le jeu, Résolver des quêtes pour avoir des coins <br>et débloquer des fonctionnalité amusante ou encore des Mod déveloper par moi.<br><br>" +
                "Vous pouver ajouter des texturePack directement depuis l'aplication <br>en glissant-déposant ou en ouvrant le fichier.<br>" +
                "De plus, vous pouvez partager la liste de vos server depuis le press-papier.<br><br>" +
                "Vous n'avez pas de co ?, vous pouvez jouer en hors ligne.<br><br>" +
                "Vous pouvez crée un server Minecraft localement <br>et avec un tuto fourni ouvrez vos port pour jouer en multijoueur.<br><br>" +
                "Pour l’instant, il n’y a pas encore de mods.<br>" +
                "il y aura prochainement un réseau social basé sur Minecraft appelé MineShot.<br>" +
                "Mais ne vous inquiétez pas, ils arriveront bientôt !<br><br>" +
                "L’interface peut sembler imparfaite.<br>" +
                "Des améliorations sont prévues pour la rendre plus agréable.<br><br>" +
                "</p></html>";

        JLabel Incoming = new JLabel(text);
        Incoming.setOpaque(false);
        Incoming.setForeground(Color.LIGHT_GRAY);
        Incoming.setBounds(5, 20, 590, 650);
        panel.add(Incoming);

        Backbutton.addActionListener(e -> {
            cardLayout.show(mainPanel, "MainPage");
            frame.setSize(1100, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        Backbutton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        Backbutton.addMouseListener(new ColorTransformation(Backbutton, customColorWindow, new Color(82, 82, 82), true));

        return panel;
    }
}
