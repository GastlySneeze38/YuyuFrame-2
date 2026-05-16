package Gastly.fr.mclauncher.GraphicPart.Graphic.Chill;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class Personalize_ChillJCheckBox extends JCheckBox {

    public Personalize_ChillJCheckBox(String proposition, Boolean isSelected, Consumer<Boolean> variableUpdater, int gap){

        super(proposition);
        setSelected(isSelected);

        addActionListener(e -> {
            variableUpdater.accept(isSelected());
            LogicParameter.saveSettings("settings.properties");
        });

        setForeground(Color.WHITE);
        setFont(new Font("Arial", Font.BOLD, 14));
        setOpaque(false);
        setFocusPainted(false);

        // Personnaliser l'icône de la case cochée et décochée
        setIcon(new CustomToggleIcon(false)); // Icône décochée
        setSelectedIcon(new CustomToggleIcon(true)); // Icône cochée

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

    public Personalize_ChillJCheckBox(String proposition, Boolean isSelected, Consumer<Boolean> variableUpdater, JCheckBox dependencies, int gap){

        super(proposition);
        setSelected(isSelected);

        addActionListener(e -> {
            variableUpdater.accept(isSelected());
            LogicParameter.saveSettings("settings.properties");
        });

        setForeground(Color.WHITE);
        setFont(new Font("Arial", Font.BOLD, 14));
        setOpaque(false);
        setFocusPainted(false);

        // Personnaliser l'icône de la case cochée et décochée
        setIcon(new CustomToggleIcon(false)); // Icône décochée
        setSelectedIcon(new CustomToggleIcon(true)); // Icône cochée

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

    // Classe interne pour personnaliser l'icône
    private static class CustomToggleIcon implements Icon {
        private final boolean isSelected;

        public CustomToggleIcon(boolean isSelected) {
            this.isSelected = isSelected;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Activer l'antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); // Améliorer la qualité
            int width = getIconWidth();
            int height = getIconHeight();

            // Dessiner la boîte arrondie
            g2d.setColor(ColorMonitor.clairSection1); // Couleur de fond de la boîte
            g2d.fillRoundRect(x, y, width, height, 20, 20); // Boîte avec coins arrondis

            // Dessiner le cercle qui se déplace
            int circleSize = height - 10;  // Taille du cercle
            int circleX = isSelected ? width - circleSize - 5 : 5;  // Position horizontale en fonction de l'état

            g2d.setColor(ColorMonitor.SombreSection1); // Couleur du rond (vert quand activé)
            g2d.fillOval(x + circleX, y + 5, circleSize, circleSize); // Dessiner le rond

            // Dessiner la bordure arrondie bleue
            g2d.setColor(ColorMonitor.SombreSection3); // Couleur de la bordure
            g2d.setStroke(new BasicStroke(3)); // Largeur de la bordure
            g2d.drawRoundRect(x, y, width, height, 20, 20); // Dessiner la bordure arrondie
        }

        @Override
        public int getIconWidth() {
            return 60; // Largeur de la boîte
        }

        @Override
        public int getIconHeight() {
            return 30; // Hauteur de la boîte
        }


    }
}
