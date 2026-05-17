package Gastly.fr.mclauncher.GraphicPart.Graphic.Chill;

import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.ModernScrollBarUI;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.MyComboBoxEditor;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.MyComboBoxRenderer;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.RoundedBorder;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Personalize_ChillJComboBox extends JComboBox {

    Color backgroundColor = ColorMonitor.clairSection1;
    Color ScrollColor = ColorMonitor.Section3;
    Color BorderColor = ColorMonitor.Section3;
    int radius = 20;

    public Personalize_ChillJComboBox(String[] versions, int wigth) {

        // Ajouter les versions au JComboBox
        for (String version : versions) {
            this.addItem(version); // Ajouter chaque version à la liste
        }

        setEditable(true);
        setRenderer(new MyComboBoxRenderer(Color.BLACK, new Color(113, 113, 113)));
        //addPopupMenuListener(new BoundsPopupMenuListener());
        setEditor(new MyComboBoxEditor(Color.BLACK));
        setOpaque(false);

        // Appliquer un UI personnalisé avec barre de défilement moderne
        setUI(new BasicComboBoxUI() {
            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane scrollPane = new JScrollPane(list);

                        // Appliquer des coins arrondis à la JScrollPane
                        scrollPane.setOpaque(false);
                        scrollPane.getViewport().setOpaque(false);
                        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder()); // Retirer la bordure par défaut du viewport
                        scrollPane.setBorder(new RoundedBorder(20)); // Bordure arrondie pour la JScrollPane

                        // Personnaliser la scrollbar
                        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(ScrollColor));
                        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

                        return scrollPane;
                    }

                    @Override
                    protected void paintBorder(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        //composant principale
                        g2.setColor(ColorMonitor.clairSection1); // Couleur de fond
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); // Fond arrondi

                        int width = getWidth();
                        int height = getHeight();
                        int arc = 20; // Taille des coins arrondis

                        // Dessiner la bordure bleue (haut et côté gauche)
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

                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.dispose();
                    }

                    @Override
                    public void show() {
                        // Fond du popup
                        list.setOpaque(false);
                        setOpaque(false);

                        super.show();
                    }
                };
                popup.setOpaque(false);
                popup.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                return popup;
            }

            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton() {
                    @Override
                    public void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        // Dessiner une flèche personnalisée
                        int[] xPoints = {getWidth() / 2 - 5, getWidth() / 2 + 5, getWidth() / 2};
                        int[] yPoints = {getHeight() / 2 - 2, getHeight() / 2 - 2, getHeight() / 2 + 5};
                        g.setColor(Color.BLACK);
                        g.fillPolygon(xPoints, yPoints, 3);
                    }
                };
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);

                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        setBackcolor(ColorMonitor.Section1); // Changer la couleur de fond au survol
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        setBackcolor(ColorMonitor.clairSection1);; // Changer la couleur de fond au desurvol
                    }
                });

                return button;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(backgroundColor); // Couleur de fond
                g2.fillRoundRect(0, 0, comboBox.getWidth(), comboBox.getHeight(), radius, radius); // Bord arrondi
                g2.setColor(BorderColor); // Couleur de la bordure
                g2.drawRoundRect(0, 0, comboBox.getWidth() - 1, comboBox.getHeight() - 1, radius, radius); // Bordure arrondie
            }

            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dessiner l'ombre
                g2.setColor(new Color(0, 0, 0, 60)); // Ombre avec une transparence
                g2.fillRoundRect(55, 135, c.getWidth() - 90, c.getHeight() - 190, 20, 20); // Ombre décalée de 5px et flou

                //composant principale
                g2.setColor(backgroundColor); // Couleur de fond
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius, radius); // Fond arrondi

                int width = getWidth();
                int height = getHeight();
                int arc = 20; // Taille des coins arrondis

                // Dessiner la bordure bleue (haut et côté gauche)
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

                super.paint(g, c); // Appeler le dessin du contenu par défaut
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
}