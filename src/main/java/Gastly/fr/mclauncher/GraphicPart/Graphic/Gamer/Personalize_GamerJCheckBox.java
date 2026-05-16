package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.CheckBox.CheckBox;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class Personalize_GamerJCheckBox extends JCheckBox {

    public Personalize_GamerJCheckBox(String proposition, Boolean isSelected, Consumer<Boolean> variableUpdater, int gap){
        super(proposition);
        setSelected(isSelected);

        addActionListener(e -> {
            variableUpdater.accept(isSelected());
            LogicParameter.saveSettings("settings.properties");
            Souds_Effect.play("click.wav");

            // Lancer l'animation
            CheckBox icon = (CheckBox) (isSelected() ? getSelectedIcon() : getIcon());
            icon.animate(isSelected(), this);
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                CheckBox.setBackcolor(new Color(68, 68, 68)); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                CheckBox.setBackcolor(new Color(19, 19, 21));; // Changer la couleur de fond au desurvol
            }
        });
        /*addMouseListener(new ColorTransformation(this, new Color(19, 19, 21), new Color(68, 68, 68), message -> {
            CheckBox.setBackcolor(new Color(19, 19, 21));
        }));*/

        setForeground(Color.WHITE);
        setFont(new Font("Arial", Font.BOLD, 14));
        setOpaque(false);
        setFocusPainted(false);

        // Personnaliser l'icône de la case cochée et décochée
        setIcon(new CheckBox(isSelected())); // Icône décochée
        setSelectedIcon(new CheckBox(isSelected())); // Icône cochée

        // Placer le texte à gauche et l'icône à droite
        setHorizontalTextPosition(SwingConstants.LEFT); // Texte à gauche
        setHorizontalAlignment(SwingConstants.RIGHT);   // Aligner tout le composant à droite

        // Calculer la largeur du texte
        FontMetrics fontMetrics = getFontMetrics(getFont());
        int textWidth = fontMetrics.stringWidth(proposition);

        // Calculer la largeur de l'icône
        int iconWidth = getIcon().getIconWidth();

        // Définir l'écart entre le texte et l'icône
        int gap1 = gap - (iconWidth + textWidth) - 20; // Calcul dynamique de l'écart

        setIconTextGap(gap1);
    }

    public Personalize_GamerJCheckBox(String proposition, Boolean isSelected, Consumer<Boolean> variableUpdater, JCheckBox dependencies, int gap){
        super(proposition);
        setSelected(isSelected);

        addActionListener(e -> {
            variableUpdater.accept(isSelected());
            LogicParameter.saveSettings("settings.properties");
            Souds_Effect.play("click.wav");

            // Lancer l'animation
            CheckBox icon = (CheckBox) (isSelected() ? getSelectedIcon() : getIcon());
            icon.animate(isSelected(), this);
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                CheckBox.setBackcolor(new Color(68, 68, 68, 150)); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                CheckBox.setBackcolor(new Color(19, 19, 21));; // Changer la couleur de fond au desurvol
            }
        });

        setForeground(Color.WHITE);
        setFont(new Font("Arial", Font.BOLD, 14));
        setOpaque(false);
        setFocusPainted(false);

        // Personnaliser l'icône de la case cochée et décochée
        setIcon(new CheckBox(true)); // Icône décochée
        setSelectedIcon(new CheckBox(true)); // Icône cochée

        // Placer le texte à gauche et l'icône à droite
        setHorizontalTextPosition(SwingConstants.LEFT); // Texte à gauche
        setHorizontalAlignment(SwingConstants.RIGHT);   // Aligner tout le composant à droite

        if (dependencies.isSelected()){
            setVisible(false);
        }

        // Calculer la largeur du texte
        FontMetrics fontMetrics = getFontMetrics(getFont());
        int textWidth = fontMetrics.stringWidth(proposition);

        // Calculer la largeur de l'icône
        int iconWidth = getIcon().getIconWidth();

        // Définir l'écart entre le texte et l'icône
        int gap1 = gap - (iconWidth + textWidth) - 20; // Calcul dynamique de l'écart

        setIconTextGap(gap1);
    }
}
