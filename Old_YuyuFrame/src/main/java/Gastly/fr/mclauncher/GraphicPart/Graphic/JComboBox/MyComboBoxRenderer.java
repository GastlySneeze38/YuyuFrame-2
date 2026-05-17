package Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox;

import javax.swing.*;
import java.awt.*;

public class MyComboBoxRenderer extends JLabel implements ListCellRenderer<Object> {

    Color foregroundColor;
    Color hoverColor;

    public MyComboBoxRenderer(Color foregroundColor, Color hover) {
        setFont(new Font("Arial", Font.BOLD, 14));
        setOpaque(false);
        this.foregroundColor = foregroundColor;
        hoverColor = hover;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(value == null ? "" : value.toString());

        // Gérer les couleurs
        if (isSelected) {
            setBackground(new Color(45, 45, 45)); // Fond sélectionné
            setForeground(hoverColor); // Texte sélectionné
        } else {
            setBackground(new Color(30, 30, 30)); // Fond non sélectionné
            setForeground(foregroundColor); // Texte non sélectionné
        }

        // Supprimer les marges inutiles
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8)); // Marges internes réduites

        return this;
    }
}