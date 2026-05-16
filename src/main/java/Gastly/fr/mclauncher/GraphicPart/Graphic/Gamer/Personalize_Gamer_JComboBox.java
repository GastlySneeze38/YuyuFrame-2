package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.ModernScrollBarUI;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.MyComboBoxEditor;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.MyComboBoxRenderer;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.RoundedBorder;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Personalize_Gamer_JComboBox extends JComboBox{

    Color backgroundColor = new Color(19, 19, 21);
    int radius = 20;

    public Personalize_Gamer_JComboBox(String[] versions) {

        // Ajouter les versions au JComboBox
        for (String version : versions) {
            this.addItem(version); // Ajouter chaque version à la liste
        }

        setEditable(true);
        setRenderer(new MyComboBoxRenderer(Color.WHITE, new Color(173, 216, 230)));
        setEditor(new MyComboBoxEditor(Color.WHITE));
        setOpaque(false);
        this.setOpaque(false);

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
                        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(new Color(68, 62, 185)));
                        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

                        return scrollPane;
                    }

                    @Override
                    protected void paintBorder(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Color.GRAY); // Couleur de la bordure
                        g2.setStroke(new BasicStroke(2));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20); // Coins arrondis
                        g2.dispose();
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Dessiner le fond arrondi
                        g2.setColor(getBackground());
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

                        g2.dispose();
                    }
                    @Override
                    public void show() {
                        setBackground(new Color(30, 30, 30)); // Fond du popup
                        list.setBackground(new Color(30, 30, 30)); // Fond de la liste
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
                        g.setColor(Color.WHITE);
                        g.fillPolygon(xPoints, yPoints, 3);
                    }
                };
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);

                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        setBackcolor(new Color(82, 82, 82)); // Changer la couleur de fond au survol
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        setBackcolor(new Color(19, 19, 21)); // Changer la couleur de fond au desurvol
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
                g2.setColor(Color.GRAY); // Couleur de la bordure
                g2.drawRoundRect(0, 0, comboBox.getWidth() - 1, comboBox.getHeight() - 1, radius, radius); // Bordure arrondie
            }
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(backgroundColor); // Couleur de fond
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius, radius); // Fond arrondi
                g2.setColor(Color.GRAY); // Couleur de la bordure
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, radius, radius); // Bordure arrondie
                super.paint(g, c); // Appeler le dessin du contenu par défaut
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
                setBackcolor(new Color(19, 19, 21)); // Changer la couleur de fond au desurvol
            }
        });
    }
    public void setBackcolor(Color color) {
        backgroundColor = color;
        repaint();
    }
}