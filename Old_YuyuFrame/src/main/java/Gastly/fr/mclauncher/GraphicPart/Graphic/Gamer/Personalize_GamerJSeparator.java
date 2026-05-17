package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import javax.swing.*;
import java.awt.*;

public class Personalize_GamerJSeparator extends JPanel {

    public Personalize_GamerJSeparator(String text, int TextSize, Color WindowsColor){
        setLayout(new GridBagLayout()); // Permet de centrer les éléments proprement
        setOpaque(false); // Garde le fond transparent

        JLabel MidleText = new JLabel(text);
        MidleText.setFont(new Font("Arial", Font.BOLD, TextSize));
        MidleText.setForeground(Color.WHITE);
        MidleText.setBackground(WindowsColor);
        MidleText.setOpaque(true);
        MidleText.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 5, 0, 5);

        JSeparator leftSeparator = new JSeparator(SwingConstants.HORIZONTAL);
        leftSeparator.setForeground(new Color(64, 64, 64, 150));
        leftSeparator.setBackground(new Color(64, 64, 64, 150));
        JSeparator rightSeparator = new JSeparator(SwingConstants.HORIZONTAL);
        rightSeparator.setForeground(new Color(64, 64, 64, 150));
        rightSeparator.setBackground(new Color(64, 64, 64, 150));

        add(leftSeparator, gbc);
        add(MidleText);
        add(rightSeparator, gbc);
    }

}
