package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class Personalize_TextNHoverComponent extends JLayeredPane {

    private Personalize_GamerHoverComponent hoverComponent;
    private Personalize_GamerNavBar butt;

    public Personalize_TextNHoverComponent(String text, String hoverText, String orientation) {
        setLayout(null);

        butt = new Personalize_GamerNavBar(text);
        butt.setBounds(0, 0, 90, 40); // à adapter à ta taille

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (orientation.equals(Personalize_GamerHoverComponent.Right)) {
                    int y = (getHeight() - butt.getHeight()) / 2;
                    butt.setLocation(butt.getX(), y);

                } else if (orientation.equals(Personalize_GamerHoverComponent.Under)) {
                    int x = (getWidth() - butt.getWidth()) / 2;
                    butt.setLocation(x, butt.getY());
                }
            }
        });

        add(butt, JLayeredPane.DEFAULT_LAYER);

        // Création du hoverComponent (non ajouté au bouton !)
        hoverComponent = new Personalize_GamerHoverComponent(
                Personalize_GamerHoverComponent.Under, hoverText
        );
        hoverComponent.setVisible(false);
        hoverComponent.setSize(hoverComponent.getPreferredSize());

        // Ajout à la couche supérieure du layeredPane
        add(hoverComponent, JLayeredPane.POPUP_LAYER);

        // Affichage/masquage au survol du bouton (pas du container)
        butt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {

                // Positionner le hoverComponent à droite du bouton
                Point locationInLayered = SwingUtilities.convertPoint(
                        butt.getParent(), butt.getX(), butt.getY(), Personalize_TextNHoverComponent.this
                );
                if (orientation.equals(Personalize_GamerHoverComponent.Right)){
                    hoverComponent.setLocation(
                            locationInLayered.x + butt.getWidth(),
                            locationInLayered.y
                    );
                } else if (orientation.equals(Personalize_GamerHoverComponent.Under)) {
                    hoverComponent.setLocation(
                            0,
                            locationInLayered.y + butt.getHeight()
                    );
                }

                hoverComponent.setVisible(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverComponent.setVisible(false);
            }
        });
    }
    public Personalize_TextNHoverComponent(Image icon, String hoverText, String orientation, Color customColorWindow, int IconSize, URL ImagePathHover, URL ImagePathBase) {
        setLayout(null);

        butt = new Personalize_GamerNavBar(new ImageIcon(icon), customColorWindow, IconSize, ImagePathHover, ImagePathBase);
        butt.setBounds(0, 0, IconSize, IconSize); // à adapter à ta taille

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (orientation.equals(Personalize_GamerHoverComponent.Right)) {
                    int y = (getHeight() - butt.getHeight()) / 2;
                    butt.setLocation(butt.getX(), y);

                } else if (orientation.equals(Personalize_GamerHoverComponent.Under)) {
                    int x = (getWidth() - butt.getWidth()) / 2;
                    butt.setLocation(x, butt.getY());
                }
            }
        });

        add(butt, JLayeredPane.DEFAULT_LAYER);

        // Création du hoverComponent (non ajouté au bouton !)
        hoverComponent = new Personalize_GamerHoverComponent(
                orientation, hoverText
        );
        hoverComponent.setVisible(false);
        hoverComponent.setSize(hoverComponent.getPreferredSize());

        // Ajout à la couche supérieure du layeredPane
        add(hoverComponent, JLayeredPane.POPUP_LAYER);

        // Affichage/masquage au survol du bouton (pas du container)
        butt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {

                // Positionner le hoverComponent à droite du bouton
                Point locationInLayered = SwingUtilities.convertPoint(
                        butt.getParent(), butt.getX(), butt.getY(), Personalize_TextNHoverComponent.this
                );
                if (orientation.equals(Personalize_GamerHoverComponent.Right)){
                    hoverComponent.setLocation(
                            locationInLayered.x + butt.getWidth(),
                            locationInLayered.y
                    );

                } else if (orientation.equals(Personalize_GamerHoverComponent.Under)) {
                    hoverComponent.setLocation(
                            0,
                            locationInLayered.y + butt.getHeight()
                    );
                }

                hoverComponent.setVisible(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverComponent.setVisible(false);
            }
        });
    }
    public Personalize_TextNHoverComponent(Image icon, String hoverText, String orientation, Color customColorWindow, int IconSize, URL ImagePathHover, URL ImagePathBase, Boolean Animate) {
        setLayout(null);

        butt = new Personalize_GamerNavBar(new ImageIcon(icon), customColorWindow, IconSize, ImagePathHover, ImagePathBase, Animate);
        butt.setBounds(0, 0, IconSize, IconSize); // à adapter à ta taille

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (orientation.equals(Personalize_GamerHoverComponent.Right)) {
                    int y = (getHeight() - butt.getHeight()) / 2;
                    butt.setLocation(butt.getX(), y);

                } else if (orientation.equals(Personalize_GamerHoverComponent.Under)) {
                    int x = (getWidth() - butt.getWidth()) / 2;
                    butt.setLocation(x, butt.getY());
                }
            }
        });

        add(butt, JLayeredPane.DEFAULT_LAYER);

        // Création du hoverComponent (non ajouté au bouton !)
        hoverComponent = new Personalize_GamerHoverComponent(
                orientation, hoverText
        );
        hoverComponent.setVisible(false);
        hoverComponent.setSize(hoverComponent.getPreferredSize());

        // Ajout à la couche supérieure du layeredPane
        add(hoverComponent, JLayeredPane.POPUP_LAYER);

        // Affichage/masquage au survol du bouton (pas du container)
        butt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");

                // Positionner le hoverComponent à droite du bouton
                Point locationInLayered = SwingUtilities.convertPoint(
                        butt.getParent(), butt.getX(), butt.getY(), Personalize_TextNHoverComponent.this
                );
                if (orientation.equals(Personalize_GamerHoverComponent.Right)){
                    hoverComponent.setLocation(
                            locationInLayered.x + butt.getWidth(),
                            locationInLayered.y
                    );

                } else if (orientation.equals(Personalize_GamerHoverComponent.Under)) {
                    hoverComponent.setLocation(
                            0,
                            locationInLayered.y + butt.getHeight()
                    );
                }

                hoverComponent.setVisible(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverComponent.setVisible(false);
            }
        });
    }

    public int getHoverComponentWith(){
        return hoverComponent.getPreferredSize().width;
    }
    public int getHoverComponentHeight(){
        return hoverComponent.getPreferredSize().height;
    }
    public void addButtonActionListener(ActionListener listener) {
        butt.addActionListener(listener);
    }
}
