package Gastly.fr.mclauncher.GraphicPart.Graphic.Chill;

import Gastly.fr.mclauncher.GraphicPart.Graphic.JSlider.CustomSliderUI;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

public class Personalize_ChillJSlider extends JSlider {

    public Personalize_ChillJSlider(int min, int max, int defaultValue) {
        super(JSlider.HORIZONTAL, min, max, defaultValue);

        // Configurer le slider
        setMajorTickSpacing(4000);
        setMinorTickSpacing(1000);
        setPaintTicks(true);
        setPaintLabels(true);
        setOpaque(false);

        // Ajouter un ChangeListener pour redessiner le composant à chaque modification
        addChangeListener((ChangeEvent e) -> repaint());
    }

    @Override
    public void updateUI() {
        // Créer une nouvelle instance de CustomSliderUI
        CustomSliderUI customSliderUI = new CustomSliderUI(this, ColorMonitor.SombreSection1);

        // Mettre à jour l'UI avec CustomSliderUI
        setUI(customSliderUI);

        // Ajouter le label au slider via l'instance de CustomSliderUI
        customSliderUI.addLabelToSlider();
    }
}


