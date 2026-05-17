package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerButtons;
import Gastly.fr.mclauncher.GraphicPart.Graphic.THREEDPlayer.MediaPlayerComponant;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class VideoPlayer extends JButton{

    private int radius;
    private Color PlayBack = new Color(40, 40, 40);
    private Color PlayHover = new Color(20, 20, 20);
    private Boolean isImage = true;

    public VideoPlayer(ImageIcon imageIcon, int radius) {
        this.radius = radius;
        setLayout(null);
        setContentAreaFilled(false);
        setFocusPainted(false);

        Personalize_GamerButtons playButton = new Personalize_GamerButtons("▶", radius, PlayHover, PlayBack);
        playButton.setForeground(Color.WHITE);
        playButton.setBounds(20, 20, 60, 30);
        add(playButton);

        // Ajouter une image avec JLabel
        Personalize_GamerButtons imageLabel = new Personalize_GamerButtons(imageIcon, radius);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setBounds(0, 0, 800, 400);
        imageLabel.setVisible(true);
        add(imageLabel);

        // Créer un composant de lecture vidéo VLCJ
        MediaPlayerComponant mediaPlayerComponent = new MediaPlayerComponant(radius);
        mediaPlayerComponent.setBounds(0, 0, 800, 400);
        mediaPlayerComponent.setVisible(false);
        add(mediaPlayerComponent);

        playButton.addActionListener(e ->{
            if (isImage){
                mediaPlayerComponent.restartVideo();

                // Démarrer la vidéo
                mediaPlayerComponent.playVideo("assets/video/video.avi");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

                imageLabel.setVisible(false);
                mediaPlayerComponent.setVisible(true);

                isImage = false;

            }else {
                mediaPlayerComponent.restartVideo();

                imageLabel.setVisible(true);
                mediaPlayerComponent.setVisible(false);

                isImage = true;
            }
        });
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
        g2.dispose();
    }
    @Override
    public boolean contains(int x, int y) {
        // Vérifier si le clic est dans la zone arrondie du bouton
        return new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius).contains(x, y);
    }
}


