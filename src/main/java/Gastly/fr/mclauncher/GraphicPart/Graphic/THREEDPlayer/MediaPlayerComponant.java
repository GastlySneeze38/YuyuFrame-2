package Gastly.fr.mclauncher.GraphicPart.Graphic.THREEDPlayer;

import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class MediaPlayerComponant extends JPanel {

    private int radius;
    private EmbeddedMediaPlayerComponent mediaPlayerComponent;

    public MediaPlayerComponant(int radius){
        this.radius = radius;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 400));

        // Créer le composant de lecture vidéo
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        add(mediaPlayerComponent, BorderLayout.CENTER);

        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventListener() {
            @Override
            public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {}
            @Override
            public void opening(MediaPlayer mediaPlayer) {}
            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {}
            @Override
            public void playing(MediaPlayer mediaPlayer) {}
            @Override
            public void paused(MediaPlayer mediaPlayer) {}
            @Override
            public void stopped(MediaPlayer mediaPlayer) {}
            @Override
            public void forward(MediaPlayer mediaPlayer) {}
            @Override
            public void backward(MediaPlayer mediaPlayer) {}

            @Override
            public void finished(MediaPlayer mediaPlayer) {

            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {}
            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {}
            @Override
            public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {}
            @Override
            public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {}
            @Override
            public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {}
            @Override
            public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {}
            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {}
            @Override
            public void videoOutput(MediaPlayer mediaPlayer, int newCount) {}
            @Override
            public void scrambledChanged(MediaPlayer mediaPlayer, int newScrambled) {}
            @Override
            public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType type, int id) {}
            @Override
            public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType type, int id) {}
            @Override
            public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType type, int id) {}
            @Override
            public void corked(MediaPlayer mediaPlayer, boolean corked) {}
            @Override
            public void muted(MediaPlayer mediaPlayer, boolean muted) {}
            @Override
            public void volumeChanged(MediaPlayer mediaPlayer, float volume) {}
            @Override
            public void audioDeviceChanged(MediaPlayer mediaPlayer, String audioDevice) {}
            @Override
            public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {}
            @Override
            public void error(MediaPlayer mediaPlayer) {}
            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {}

        });
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Appliquer un clip pour les coins arrondis
        Shape clip = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.setClip(clip);

        // Remplir l'arrière-plan avec des coins arrondis
        g2.setColor(getBackground());
        g2.fill(clip);

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

    // Méthode pour démarrer la vidéo
    public void playVideo(String videoFilePath) {
        mediaPlayerComponent.mediaPlayer().media().start(videoFilePath);
        mediaPlayerComponent.mediaPlayer().controls().setRepeat(true);
    }

    // Méthode pour redémarrer la vidéo au début
    public void restartVideo() {
        mediaPlayerComponent.mediaPlayer().controls().setTime(0);
    }
}
