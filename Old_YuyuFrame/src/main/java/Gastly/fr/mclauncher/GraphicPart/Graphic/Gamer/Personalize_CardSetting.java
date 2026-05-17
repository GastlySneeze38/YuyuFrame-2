package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import javax.swing.*;
import java.awt.*;

public class Personalize_CardSetting extends JPanel {

    private int shadowSize = 10; // Taille de l'ombre
    private int Height;
    private int Width;
    private Color CardColor;
    private Boolean PaintedShadow;
    private int Radius = 20;

    public Personalize_CardSetting(int Width, int Height, Color CardColor) {
        this.Height = Height;
        this.Width = Width;
        this.CardColor = CardColor;

        PaintedShadow = true;

        setOpaque(false);
    }
    public Personalize_CardSetting(int Width, int Height, Color CardColor, Boolean PaintedShadow) {
        this.Height = Height;
        this.Width = Width;
        this.CardColor = CardColor;
        this.PaintedShadow = PaintedShadow;

        setOpaque(false);
    }
    public Personalize_CardSetting(int Width, int Height, Color CardColor, Boolean PaintedShadow, int Radius) {
        this.Height = Height;
        this.Width = Width;
        this.CardColor = CardColor;
        this.PaintedShadow = PaintedShadow;
        this.Radius = Radius;

        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();

        // Activer l'anti-aliasing pour des bords lisses
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner l'ombre
        if (PaintedShadow) {
            g2.setColor(new Color(0, 0, 0)); // Ombre noire semi-transparente
            g2.fillRoundRect(shadowSize, shadowSize, width - shadowSize, height - shadowSize, 20, 20);
        }

        g2.setColor(CardColor);
        // Dessiner la carte
        if (PaintedShadow) g2.fillRoundRect(0, 0, width - shadowSize, height - shadowSize, Radius, Radius);
        else g2.fillRoundRect(0, 0, width, height, Radius, Radius);


        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Width, Height); // Taille par défaut de la carte
    }
}
