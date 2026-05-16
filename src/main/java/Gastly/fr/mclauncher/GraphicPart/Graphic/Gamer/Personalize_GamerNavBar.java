package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.Rotation;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.net.URL;

public class Personalize_GamerNavBar extends JButton {

    private Boolean IsRotate = false;
    private Boolean IsRotateComponent = false;
    private Personalize_GamerHoverComponent hoverComponent;

    public Personalize_GamerNavBar(ImageIcon icon2, int IconSize){
        super(icon2);
        IsRotateComponent = false;

        setPreferredSize(new Dimension(IconSize, IconSize));
        setBorderPainted(false); // Pas de bordure peinte
        setFocusPainted(false); // Pas de surlignement au focus
        setContentAreaFilled(false); // Pas de remplissage du bouton
        setOpaque(false);
    }
    public Personalize_GamerNavBar(ImageIcon icon2, Color customColorWindow, int IconSize, URL ImagePathHover, URL ImagePathBase){
        super(icon2);
        IsRotateComponent = false;

        setFont(new Font("Arial", Font.PLAIN, 10));
        setBackground(customColorWindow);
        setBorderPainted(false); // Pas de bordure peinte
        setFocusPainted(false); // Pas de surlignement au focus
        setContentAreaFilled(false); // Pas de remplissage du bouton
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ImageIcon icon1 = new ImageIcon(ImagePathHover);
                Image img1 = icon1.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);

                setIcon(new ImageIcon(img1));

                Souds_Effect.play("hover.wav");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ImageIcon icon1 = new ImageIcon(ImagePathBase);
                Image img1 = icon1.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);

                setIcon(new ImageIcon(img1));
            }
        });
    }
    public Personalize_GamerNavBar(String text){
        super(text);
        IsRotateComponent = false;

        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(new Font("Arial", Font.BOLD, 15));
        setForeground(new Color(200, 200, 200));
        setBorderPainted(false); // Pas de bordure peinte
        setFocusPainted(false); // Pas de surlignement au focus
        setContentAreaFilled(false); // Pas de remplissage du bouton
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        addMouseListener(new ColorTransformation(this, new Color(200, 200, 200), new Color(150, 150, 150), false));
    }
    public Personalize_GamerNavBar(ImageIcon icon2, Color customColorWindow, int IconSize, URL ImagePathHover, URL ImagePathBase, Boolean Rotate){
        IsRotateComponent = true;
        Rotation.image = Rotation.toBufferedImage(icon2.getImage());

        setFont(new Font("Arial", Font.PLAIN, 10));
        setBackground(customColorWindow);
        setBorderPainted(false); // Pas de bordure peinte
        setFocusPainted(false); // Pas de surlignement au focus
        setContentAreaFilled(false); // Pas de remplissage du bouton
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                IsRotate = true;
                Souds_Effect.play("hover.wav");
            }
        });
        addMouseListener(new Rotation(this, ImagePathHover, ImagePathBase, IconSize));

    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (Rotation.image != null && !IsRotate && IsRotateComponent) {
            g2.drawImage(Rotation.image, 0, 0, getWidth(), getHeight(), this);
        }

        if (IsRotate){
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            AffineTransform at = new AffineTransform();
            at.translate(centerX, centerY);
            at.rotate(Rotation.angle);
            at.translate(-Rotation.image.getWidth() / 2, -Rotation.image.getHeight() / 2);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2.drawImage(Rotation.image, at, null);
            g2.dispose();
        }
    }
}