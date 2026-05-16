package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Personalize_GamerHoverComponent extends JButton {

    public static String Under = "Under";
    public static String Right = "Right";
    private String orientation;

    public Personalize_GamerHoverComponent(String orientation, String text){
        super(text);
        this.orientation = String.valueOf(orientation);

        setForeground(Color.WHITE);
        setOpaque(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setFocusPainted(false);
        setBorderPainted(false);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int width = getWidth();
        int height = getHeight();

        // Antialiasing for smooth edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background rounded rectangle
        g2.setColor(new Color(50, 50, 50)); // Dark gray background
        g2.fill(new RoundRectangle2D.Float(8, 4, width - 16, height - 8, 8, 8));

        /*// Draw the brackets
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(200, 200, 200)); // Light gray for the brackets

        int midY = height / 2;
        int bracketSize = height / 3;

        // Left bracket
        QuadCurve2D.Float leftTop = new QuadCurve2D.Float(4, midY - bracketSize, 0, midY, 4, midY + bracketSize);
        g2.draw(leftTop);

        // Right bracket
        QuadCurve2D.Float rightTop = new QuadCurve2D.Float(width - 4, midY - bracketSize, width, midY, width - 4, midY + bracketSize);
        g2.draw(rightTop);*/

        g2.dispose();
        super.paintComponent(g);
    }
}
