package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Personalize_GamerTextField extends JTextField {

    Color backgroundColor = new Color(19, 19, 21);

    public Personalize_GamerTextField(String textEmpty){

        super(textEmpty);
        setOpaque(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setHorizontalAlignment(SwingConstants.CENTER);
        setForeground(Color.WHITE);

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Effacer le placeholder lorsque le champ gagne le focus
                if (getText().equals(textEmpty)) {
                    setText("");
                    setForeground(Color.WHITE); // Couleur normale du texte
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Remettre le placeholder si le champ est vide
                if (getText().isEmpty()) {
                    setText(textEmpty);
                    setForeground(Color.WHITE);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackcolor(new Color(82, 82, 82)); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackcolor(new Color(19, 19, 21));; // Changer la couleur de fond au desurvol
            }
        });
    }

    public void setBackcolor(Color color) {
        backgroundColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Appliquer un clip pour les coins arrondis
        Shape clip;
        clip = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        g2.setClip(clip);

        // Remplir l'arrière-plan avec des coins arrondis
        g2.setColor(backgroundColor);
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
        g2.setStroke(new BasicStroke(1));

        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        g2.dispose();
    }

}
