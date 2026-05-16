package Gastly.fr.mclauncher.GraphicPart.Graphic.Chill;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class Personalize_ChillTextfield extends JTextField {

    Color backgroundColor = ColorMonitor.clairSection1;

    public Personalize_ChillTextfield(String textEmpty){

        super(textEmpty);
        setOpaque(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setHorizontalAlignment(SwingConstants.CENTER);

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Effacer le placeholder lorsque le champ gagne le focus
                if (getText().equals(textEmpty)) {
                    setText("");
                    setForeground(Color.BLACK); // Couleur normale du texte
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Remettre le placeholder si le champ est vide
                if (getText().isEmpty()) {
                    setText(textEmpty);
                    setForeground(Color.BLACK);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackcolor(ColorMonitor.Section1); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackcolor(ColorMonitor.clairSection1);; // Changer la couleur de fond au desurvol
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

        int width = getWidth();
        int height = getHeight();
        int arc = 20; // Taille des coins arrondis

        // Dessiner la bordure avec des coins arrondis
        g2.setColor(ColorMonitor.SombreSection3);
        g2.setStroke(new BasicStroke(5));
        g2.drawLine(0, arc / 2, 0, height - arc / 2); // Côté gauche
        g2.drawLine(arc / 2, 0, width - arc / 2, 0); // Haut

        // Dessiner la bordure verte (bas et côté droit)
        g2.setColor(ColorMonitor.SombreSection1);
        g2.drawLine(width - 1, arc / 2, width - 1, height - arc / 2); // Côté droit
        g2.drawLine(arc / 2, height - 1, width - arc / 2, height - 1); // Bas

        // Ajouter les coins arrondis (optionnel, pour un effet cohérent)
        g2.setColor(ColorMonitor.SombreSection3);
        g2.drawArc(0, 0, arc, arc, 90, 90); // Coin supérieur gauche (bleu)
        g2.drawArc(width - arc - 1, 0, arc, arc, 0, 90); // Coin supérieur droit (transition)
        g2.drawArc(0, height - arc - 1, arc, arc, 180, 90); // Coin inférieur gauche (transition)
        g2.setColor(ColorMonitor.SombreSection1);
        g2.drawArc(width - arc - 1, height - arc - 1, arc, arc, 270, 90); // Coin inférieur droit (vert)

        g2.dispose();
    }

}
