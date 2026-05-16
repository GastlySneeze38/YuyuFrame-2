package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.DownloadPage;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerJProgressBar;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerJSeparator;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_StepProgressBar;
import Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox.RoundedBorder;

import javax.swing.*;
import java.awt.*;

public class DownloadPage extends JPanel {

    private Personalize_StepProgressBar Step;
    private Personalize_GamerJProgressBar progress;

    public DownloadPage() {

        setBackground(Color.GRAY);
        setLayout(new BorderLayout());
        setOpaque(false);

        Personalize_GamerJSeparator title = new Personalize_GamerJSeparator("Minecraft Download", 18, new Color(19, 19, 21));
        add(title, BorderLayout.NORTH);

        Step = new Personalize_StepProgressBar(4, 1);
        add(Step, BorderLayout.CENTER);

        progress = new Personalize_GamerJProgressBar();
        add(progress, BorderLayout.SOUTH);

        // Définir la bordure avec coins arrondis
        setBorder(new RoundedBorder(20));

        setSize(400, 150); // taille fixe
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dessiner un fond avec une couleur semi-transparente
        g.setColor(new Color(19, 19, 21));  // Fond gris foncé
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);  // Coins arrondis
    }

    public void showIn(JPanel parentPanel) {
        // Centrer horizontalement en haut
        int x = (parentPanel.getWidth() - getWidth()) / 2;
        int y = 10;
        setBounds(x, y, getWidth(), getHeight());

        parentPanel.add(this);
        parentPanel.setComponentZOrder(this, 0); // l'amener au premier plan
        parentPanel.revalidate();
        parentPanel.repaint();
    }
    public void setProgress(int step, int progress) {
        this.progress.setValue(progress);
        this.Step.setCurrentStep(step);

        if (Step.getCurrentStep() == 4 && this.progress.getValue() == 100) {
            Container parent = getParent();

            if (parent != null) {
                parent.remove(this);

                parent.revalidate();
                parent.repaint();
            }
        }
    }
}
