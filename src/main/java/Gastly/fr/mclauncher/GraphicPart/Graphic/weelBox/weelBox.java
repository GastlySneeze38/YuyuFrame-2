package Gastly.fr.mclauncher.GraphicPart.Graphic.weelBox;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.Consumer;

public class weelBox extends JLabel {

    public weelBox(float DefaultVolume, Color ForeGround) {

        setText((int) (DefaultVolume * 100) + " %");
        setOpaque(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setHorizontalAlignment(SwingConstants.CENTER);
        setForeground(ForeGround);

        // Ajouter du padding autour du texte
        setBorder(new EmptyBorder(10, 20, 10, 20)); // Top, Left, Bottom, Right

        addMouseWheelListener(new MouseWheelListener() {
            private int value = (int) (DefaultVolume * 100); // Valeur initiale

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Obtient la direction de la molette (positive = descente, négative = montée)
                int rotation = e.getWheelRotation();
                if (rotation > DefaultVolume) {
                    value -= 5; // Diminue la valeur si on descend la molette
                    if (value <= 0){value = 0;}
                    if (value >= 100){value = 100;}

                } else {
                    value += 5; // Augmente la valeur si on monte la molette
                    if (value <= 0){value = 0;}
                    if (value >= 100){value = 100;}

                }
                LogicParameter.GlobalVolume = (float) value / 100;
                LogicParameter.MusicVolume *= LogicParameter.GlobalVolume;
                LogicParameter.ClickVolume *= LogicParameter.GlobalVolume;
                LogicParameter.HoverVolume *= LogicParameter.GlobalVolume;
                Souds_Effect.UpdateVolume();
                setText(value + " %"); // Met à jour la boîte avec la nouvelle valeur
                LogicParameter.saveSettings("settings.properties");
            }
        });
    }
    public weelBox(float DefaultVolume, Color ForeGround, String NameSound, Consumer<Float> variableUpdater) {

        setText((int) (DefaultVolume * 100) + " %");
        setOpaque(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setHorizontalAlignment(SwingConstants.CENTER);
        setForeground(ForeGround);

        // Ajouter du padding autour du texte
        setBorder(new EmptyBorder(10, 20, 10, 20)); // Top, Left, Bottom, Right

        addMouseWheelListener(new MouseWheelListener() {
            private int value = (int) (DefaultVolume * 100); // Valeur initiale

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Obtient la direction de la molette (positive = descente, négative = montée)
                int rotation = e.getWheelRotation();
                if (rotation >= DefaultVolume) {
                    value -= 5; // Diminue la valeur si on descend la molette
                    if (value <= 0){value = 0;}
                    if (value >= 100){value = 100;}

                } else {
                    value += 5; // Augmente la valeur si on monte la molette
                    if (value <= 0){value = 0;}
                    if (value >= 100){value = 100;}

                }
                variableUpdater.accept((float) value / 100 * LogicParameter.GlobalVolume);
                Souds_Effect.setVolume(NameSound, (float) value / 100 * LogicParameter.GlobalVolume);
                setText(value + " %"); // Met à jour la boîte avec la nouvelle valeur
                LogicParameter.saveSettings("settings.properties");
            }
        });
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