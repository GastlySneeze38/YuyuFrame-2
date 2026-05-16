package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import javax.swing.*;
import java.awt.*;

public class Personalize_GamerJProgressBar extends JProgressBar {

    public Personalize_GamerJProgressBar(){
        super(HORIZONTAL, 0, 100);

        setForeground(Color.GREEN); // Couleur de remplissage par défaut
        setBackground(Color.DARK_GRAY); // Couleur de fond
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Ajoute un padding
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Lissage

        int width = getWidth();
        int height = getHeight();
        int arc = 15; // Rayon d'arrondi

        // Définir la couleur de la bordure en fonction de la valeur
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        // Dessiner la barre de progression
        int progressWidth = (int) ((getValue() / 100.0) * width);
        g2.setColor(new Color(68, 62, 185));
        g2.fillRoundRect(0, 0, progressWidth, height, arc, arc);

        /*// Dessiner la bordure colorée
        Color borderColor = getBorderColor(getValue());
        g2.setStroke(new BasicStroke(1)); // Épaisseur de la bordure
        g2.setColor(borderColor);
        g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);*/

        // Dessiner le texte au centre
        String text = getValue() + "%";
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, textX, textY);
    }

    // Méthode pour obtenir la couleur de la bordure en fonction de la valeur
    private Color getBorderColor(int value) {
        if (value <= 30) {
            return Color.RED;
        } else if (value <= 60) {
            return Color.ORANGE;
        } else {
            return Color.GREEN;
        }
    }
}
