package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.MineShot.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class GamerMineShotPanel {

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame){
        Color customColorWindow = new Color(19, 19, 21);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(customColorWindow);

        JLabel Incoming = new JLabel("Incoming");
        Incoming.setOpaque(false);
        Incoming.setForeground(Color.RED);
        Incoming.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(Incoming, BorderLayout.CENTER);

        JButton Backbutton = new JButton("Back");
        Backbutton.setFont(new Font("Arial", Font.BOLD, 20));
        Backbutton.setBackground(customColorWindow);
        Backbutton.setForeground(Color.lightGray);
        Backbutton.setBorderPainted(false); // Pas de bordure peinte
        Backbutton.setFocusPainted(false); // Pas de surlignement au focus
        Backbutton.setContentAreaFilled(false); // Pas de remplissage du bouton
        panel.add(Backbutton, BorderLayout.NORTH);

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
