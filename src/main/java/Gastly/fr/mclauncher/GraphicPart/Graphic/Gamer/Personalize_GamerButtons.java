package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ComponentBounds;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Personalize_GamerButtons extends JButton{

    private int radius;

    public Personalize_GamerButtons(String text, int radius, Color hovercolor) {
        super(text);
        this.radius = radius;
        setBackground(new Color(19, 19, 21));
        setContentAreaFilled(false);  // Removes the button background
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        addMouseListener(new ColorTransformation(this, new Color(19, 19, 21), hovercolor, true));
    }
    public Personalize_GamerButtons(String text, int radius) {
        super(text);
        this.radius = radius;
        setFont(new Font("Arial", Font.BOLD, 15));
        setBackground(new Color(19, 19, 21));
        setContentAreaFilled(false);  // Removes the button background
        setFocusPainted(false);
    }
    public Personalize_GamerButtons(String text, int radius, Color hovercolor, int Width, int Height) {
        super(text);
        this.radius = radius;
        setBackground(new Color(19, 19, 21));
        setPreferredSize(new Dimension(Width, Height));
        setForeground(Color.WHITE);
        setContentAreaFilled(false);  // Removes the button background
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        addMouseListener(new ColorTransformation(this, new Color(19, 19, 21), hovercolor, true));
    }
    public Personalize_GamerButtons(String text, int radius, Color hovercolor, Color back) {
        super(text);
        this.radius = radius;
        setBackground(back);
        setContentAreaFilled(false);  // Removes the button background
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        addMouseListener(new ColorTransformation(this, back, hovercolor, true));
    }

    // Constructor for icon-based button (no return type, name matches the class name)
    public Personalize_GamerButtons(ImageIcon imageIcon, int radius, Color hovercolor) {
        super(imageIcon);
        this.radius = radius;
        setBackground(new Color(19, 19, 21));
        setContentAreaFilled(false);
        setFocusPainted(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hovercolor); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(new Color(19, 19, 21)); // Changer la couleur de fond au desurvol
            }
        });
    }

    public Personalize_GamerButtons(ImageIcon imageIcon, int radius) {
        super(imageIcon);
        this.radius = radius;
        setBackground(new Color(19, 19, 21));
        setContentAreaFilled(false);
        setFocusPainted(false);
    }
    public Personalize_GamerButtons(ImageIcon imageIcon, int radius, Boolean hover) {
        super(imageIcon);
        this.radius = radius;
        setBackground(new Color(19, 19, 21));
        setContentAreaFilled(false);
        setFocusPainted(false);

        if (!hover){return;}

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        addMouseListener(new ComponentBounds(this, hover));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Appliquer un clip pour les coins arrondis
        Shape clip = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.setClip(clip);

        // Remplir l'arrière-plan avec des coins arrondis
        g2.setColor(getBackground());
        g2.fill(clip);

        // Dessiner le texte et/ou l'icône
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner la bordure avec des coins arrondis
        g2.setColor(Color.GRAY);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (ComponentBounds.HoverBorder && this == ComponentBounds.component){
            g2.setStroke(new BasicStroke(5));
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius + 16, radius + 16);
        }
        g2.dispose();
    }

    @Override
    public boolean contains(int x, int y) {
        // Vérifier si le clic est dans la zone arrondie du bouton
        return new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius).contains(x, y);
    }
}
