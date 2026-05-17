package Gastly.fr.mclauncher.GraphicPart.Graphic.CheckBox;

import javax.swing.*;
import java.awt.*;

public class CheckBox implements Icon {
    private int currentX; // Position actuelle du cercle
    private final int circleSize = 20; // Taille du cercle
    private final int width = 60; // Largeur de l'icône
    private boolean isAnimating = false; // Pour éviter de lancer plusieurs animations en parallèle
    private static Color Backround = new Color(19, 19, 21); // Couleur de fond
    private static int targetX;
    private static Timer timer;

    public CheckBox(boolean isSelected) {
        // Position initiale basée sur l'état isSelected
        this.currentX = isSelected ? width - circleSize - 5 : 5;
    }

    public void animate(final Boolean isSelected, Component c) {
        if (isAnimating) return; // Empêche plusieurs animations en même temps
        isAnimating = true;

        // Calcul de la position cible en fonction de l'état actuel de l'icône
        targetX = !isSelected ? 5 : width - circleSize - 5;
        this.currentX = !isSelected ? width - circleSize - 5 : 5;
        //System.out.println(currentX + "  c1");

        // Lancer l'animation
        timer = new Timer(5, e -> {
            currentX = targetX;

            // Calculer un pas pour l'animation
            if (currentX < targetX) {
                currentX++; // Se rapprocher de targetX

            } else if (currentX > targetX) {
                currentX--; // Se rapprocher de targetX
            }

            c.repaint(); // Rafraîchir l'icône

            // Vérifier si la position cible est atteinte
            if (currentX == targetX) {
                timer.stop();
                isAnimating = false; // Fin de l'animation

                //System.out.println(currentX + "  c2");
            }
        });

        timer.start(); // Démarrer l'animation
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Dessiner le fond de l'icône
        g2d.setColor(Backround);
        g2d.fillRoundRect(x, y, width, getIconHeight(), 20, 20);

        // Dessiner le cercle mobile
        g2d.setColor(new Color(68, 62, 185));
        g2d.fillOval(x + currentX, y + 5, circleSize, circleSize);

        // Dessiner la bordure
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(x, y, width, getIconHeight(), 20, 20);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return 30;
    }

    public static void setBackcolor(Color hover) {
        Backround = hover;
    }
}