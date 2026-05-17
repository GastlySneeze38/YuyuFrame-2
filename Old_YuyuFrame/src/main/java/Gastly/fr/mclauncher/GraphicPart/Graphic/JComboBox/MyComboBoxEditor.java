package Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;

public class MyComboBoxEditor extends BasicComboBoxEditor {
    private JLabel label = new JLabel();
    private JPanel panel = new JPanel();
    private Object selectedItem;

    public MyComboBoxEditor(Color foregroundColor) {
        // Configurer le label (texte affiché)
        label.setOpaque(false); // Le label ne gère pas son propre fond
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(foregroundColor);
        label.setHorizontalAlignment(SwingConstants.CENTER); // Aligner le texte à gauche

        // Configurer le panel (contenant principal)
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout()); // Utiliser BorderLayout pour un meilleur alignement
        panel.add(label, BorderLayout.CENTER); // Ajouter le label au centre
    }

    @Override
    public Component getEditorComponent() {
        return this.panel;
    }

    @Override
    public Object getItem() {
        return selectedItem == null ? "" : selectedItem.toString();
    }

    @Override
    public void setItem(Object item) {
        this.selectedItem = item;
        label.setText(item == null ? "" : item.toString());
    }
}