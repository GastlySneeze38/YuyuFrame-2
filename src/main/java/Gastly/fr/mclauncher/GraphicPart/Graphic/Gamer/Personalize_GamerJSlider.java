package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.JSlider.CustomSliderUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class Personalize_GamerJSlider extends JSlider {

    public Personalize_GamerJSlider(int min, int max, int defaultValue) {
        super(JSlider.HORIZONTAL, min, max, defaultValue);

        // Configurer le slider
        setMajorTickSpacing(4000);
        setMinorTickSpacing(1000);
        setPaintTicks(true);
        setPaintLabels(true);
        setOpaque(false);
        setForeground(Color.WHITE);

        // Ajouter un ChangeListener pour redessiner le composant à chaque modification
        addChangeListener((ChangeEvent e) -> repaint());
    }

    @Override
    public void updateUI() {
        // Créer une nouvelle instance de CustomSliderUI
        CustomSliderUI customSliderUI = new CustomSliderUI(this, new Color(68, 62, 185));

        // Mettre à jour l'UI avec CustomSliderUI
        setUI(customSliderUI);

        // Ajouter le label au slider via l'instance de CustomSliderUI
        customSliderUI.addLabelToSlider();
    }
}


