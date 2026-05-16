package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import javax.swing.*;
import java.awt.*;

public class Personalize_StepProgressBar extends JComponent {

    private int steps;          // Nombre d'étapes
    private int currentStep = 1;    // Étape actuelle (1 à 4)

    public Personalize_StepProgressBar(int Steps, int currentStep) {
        steps = Steps;
        setCurrentStep(currentStep);
        setPreferredSize(new Dimension(300, 40));
    }
    public void setCurrentStep(int step) {
        this.currentStep = Math.max(1, Math.min(steps, step));
        repaint();
    }
    public int getCurrentStep() {
        return currentStep;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int margin = 20;
        int availableWidth = width - 2 * margin;
        int stepSpacing = availableWidth / (steps - 1);

        int y = height / 2;
        int radius = 12;

        // Dessiner la ligne
        for (int i = 0; i < steps - 1; i++) {
            int x1 = margin + i * stepSpacing;
            int x2 = margin + (i + 1) * stepSpacing;

            g2.setStroke(new BasicStroke(4));
            g2.setColor(i < currentStep - 1 ? new Color(68, 62, 185) : Color.GRAY);
            g2.drawLine(x1, y, x2, y);
        }

        // Dessiner les points
        for (int i = 0; i < steps; i++) {
            int x = margin + i * stepSpacing;
            g2.setColor(i < currentStep ? new Color(68, 62, 185) : Color.GRAY);
            g2.fillOval(x - radius / 2, y - radius / 2, radius, radius);
        }
    }

}
