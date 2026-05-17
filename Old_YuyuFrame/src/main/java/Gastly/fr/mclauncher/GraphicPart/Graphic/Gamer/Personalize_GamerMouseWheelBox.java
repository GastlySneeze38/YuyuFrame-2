package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.weelBox.weelBox;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class Personalize_GamerMouseWheelBox extends JPanel {

    public Personalize_GamerMouseWheelBox(float DefaultVolume, String DescriptionText, Color Foreground) {

        setLayout(new BorderLayout());
        setOpaque(false); // Rendre le panel transparent

        JLabel description = new JLabel();
        description.setText(DescriptionText);
        description.setOpaque(false);
        description.setFont(new Font("Arial", Font.BOLD, 16));
        description.setForeground(Foreground);

        weelBox ValueBox = new weelBox(DefaultVolume, Foreground);

        add(ValueBox, BorderLayout.EAST);
        add(description, BorderLayout.WEST);
    }
    public Personalize_GamerMouseWheelBox(float DefaultVolume, String DescriptionText, Color Foreground, String NameSound, Consumer<Float> variableUpdater) {

        setLayout(new BorderLayout());
        setOpaque(false); // Rendre le panel transparent

        JLabel description = new JLabel();
        description.setText(">   " + DescriptionText);
        description.setOpaque(false);
        description.setFont(new Font("Arial", Font.BOLD, 16));
        description.setForeground(Foreground);

        weelBox ValueBox = new weelBox(DefaultVolume, Foreground, NameSound, variableUpdater);

        add(ValueBox, BorderLayout.EAST);
        add(description, BorderLayout.WEST);
    }
}
