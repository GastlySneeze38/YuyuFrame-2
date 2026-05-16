package Gastly.fr.mclauncher.GraphicPart.Graphic.Animate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public class ColorTransformation implements MouseListener {

    private final int steps = 15;
    private final int delay = 15;
    private Timer animationTimer = null;
    private int step = 0;

    private Component Target;
    private Color Base;
    private Color Hover;

    private Boolean isBackround;
    private final Consumer<Color> setBackColor;

    public ColorTransformation(Component Target, Color Base, Color Hover, Boolean isBackround){
        this.Target = Target;
        this.Base = Base;
        this.Hover = Hover;
        this.isBackround = isBackround;

        this.setBackColor = null;
    }
    public ColorTransformation(Component Target, Color Base, Color Hover, Consumer<Color> setBackColor){
        this.Target = Target;
        this.Base = Base;
        this.Hover = Hover;
        this.isBackround = null;

        this.setBackColor = setBackColor;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        startColorTransition(Base, Hover, isBackround);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        startColorTransition(Hover, Base, isBackround);
    }

    private void startColorTransition(Color start, Color end, Boolean isBackround) {

        // Stoppe l'ancienne animation si elle est encore en cours
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        step = 0;

        animationTimer = new Timer(delay, e -> {
            float ratio = (float) step / steps;
            int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            if (isBackround == null) setBackColor.accept(new Color(r, g, b));
            else if (isBackround) Target.setBackground(new Color(r, g, b));
            else if (!isBackround) Target.setForeground(new Color(r, g, b));

            step++;
            if (step > steps) {
                animationTimer.stop();
            }
        });

        animationTimer.start();
    }
}
