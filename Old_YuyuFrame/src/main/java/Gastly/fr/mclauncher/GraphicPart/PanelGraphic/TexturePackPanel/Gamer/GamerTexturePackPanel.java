package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.TexturePackPanel.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_CardSetting;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerJSeparator;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class GamerTexturePackPanel {

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame){
        Color customColorWindow = new Color(19, 19, 21);
        Color CardColor = new Color(40, 40, 40);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(customColorWindow);

        JButton Backbutton = new JButton("Back");
        Backbutton.setFont(new Font("Arial", Font.BOLD, 20));
        Backbutton.setBackground(customColorWindow);
        Backbutton.setForeground(Color.lightGray);
        Backbutton.setBorderPainted(false); // Pas de bordure peinte
        Backbutton.setFocusPainted(false); // Pas de surlignement au focus
        Backbutton.setContentAreaFilled(false); // Pas de remplissage du bouton
        Backbutton.setOpaque(true);
        Backbutton.setBounds(2, 0, 496, 30);
        panel.add(Backbutton);

        Personalize_GamerJSeparator TextureSeparator = new Personalize_GamerJSeparator("Texture Pack", 20, customColorWindow);
        TextureSeparator.setBounds(10, 40, 480, 40);
        panel.add(TextureSeparator);

        Personalize_CardSetting TextureCard = new Personalize_CardSetting(480, 200, CardColor, false);
        TextureCard.setBounds(20, 100, 460, 200);
        panel.add(TextureCard);

        Personalize_GamerJSeparator ShaderSeparator = new Personalize_GamerJSeparator("Shader Pack", 20, customColorWindow);
        ShaderSeparator.setBounds(10, 320, 480, 40);
        panel.add(ShaderSeparator);

        Personalize_CardSetting ShaderCard = new Personalize_CardSetting(480, 200, CardColor, false);
        ShaderCard.setBounds(20, 380, 460, 200);
        panel.add(ShaderCard);

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
