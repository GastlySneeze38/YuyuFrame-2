package Gastly.fr.mclauncher.GraphicPart.Graphic.Animate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.net.URL;

public class Rotation implements MouseListener {

    public static double angle = 0;
    private double totalRotation = 7.5;
    private double progress = 0.0; // Progression de l'animation
    private double speedFactor = 0.0; // Vitesse initiale
    public static BufferedImage image;
    private Timer timer;
    private URL ImagePathHover;
    private URL ImagePathBase;
    private int IconSize;

    public Rotation(Component Target, URL ImagePathHover, URL ImagePathBase, int IconSize) {

        this.ImagePathHover = ImagePathHover;
        this.ImagePathBase = ImagePathBase;
        this.IconSize = IconSize;

        timer = new Timer(16, e -> {
            if (progress < 1.0) {
                progress += 0.001; // Avancer la progression
                speedFactor = calculateSpeed(progress); // Calculer la vitesse

                angle += Math.toRadians(speedFactor); // Appliquer la vitesse à l'angle de rotation

                if (angle >= totalRotation) { // Si on atteint la rotation complète
                    angle = 0; // Fixer l'angle à 180° pour éviter les oscillations
                    progress = 0.0;
                    speedFactor = 0.0;
                    timer.stop(); // Arrêter l'animation
                }
                Target.repaint();
            }
        });
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        ImageIcon icon1 = new ImageIcon(ImagePathHover);
        Image img1 = icon1.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        ImageIcon icon2 = new ImageIcon(img1);

        image = toBufferedImage(icon2.getImage());

        startRotation();
    }
    @Override
    public void mouseExited(MouseEvent e) {

        ImageIcon icon1 = new ImageIcon(ImagePathBase);
        Image img1 = icon1.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        ImageIcon icon2 = new ImageIcon(img1);

        image = toBufferedImage(icon2.getImage());
    }

    // Convertit une Image en BufferedImage
    public static BufferedImage toBufferedImage(Image img) {
        BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return bufferedImage;
    }
    private double calculateSpeed(double progress) {
        double speed;

        if (progress < 0.5) {
            // Première moitié de la rotation : Accélération exponentielle
            speed = Math.exp(progress * 10) - 1; // Fonction exponentielle pour accélération
        } else {
            // Deuxième moitié de la rotation : Décélération exponentielle (réciproque)
            speed = 1 - (Math.exp((1 - progress) * 10) - 1); // Réciproque de la fonction exponentielle
        }

        // Retourner la vitesse en degrés (avec un facteur d'échelle de 180°)
        return speed * 180; // Retourner la vitesse modifiée par l'angle total (180°)
    }
    public void startRotation() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }
}
