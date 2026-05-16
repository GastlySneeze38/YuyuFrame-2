package Gastly.fr.mclauncher.GraphicPart.Graphic.Animate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.atomic.AtomicInteger;

public class ComponentBounds implements MouseListener {

    private Timer timerUp = null;
    private Timer timerDown = null;
    private Boolean AsUp = false;
    private Boolean AsDown = true;
    public static Component component;
    public static boolean HoverBorder = false;
    private boolean hover;

    public ComponentBounds(Component Target, Boolean hover){
        component = Target;
        this.hover = hover;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        HoverBorder = hover;

        if (AsUp) return;
        if (timerUp != null && timerUp.isRunning()) return; // Empêche de lancer un nouveau Timer de montée
        if (timerDown != null && timerDown.isRunning()) return; // Empêche la montée si un Timer de descente tourne

        AtomicInteger i = new AtomicInteger();
        timerUp = new Timer(40, e1 -> {
            if (i.get() <= 3) { // "<" au lieu de "<=" pour éviter un déplacement supplémentaire
                component.setLocation(component.getX(), component.getY() - 1);
                i.incrementAndGet();
            } else {
                timerUp.stop();

                AsUp = true;
                AsDown = false;

                if (HoverBorder) return;
                if (AsDown) return;
                if (timerDown != null && timerDown.isRunning()) return; // Empêche un nouveau Timer de descente

                AtomicInteger i1 = new AtomicInteger();
                timerDown = new Timer(40, e2 -> {
                    if (i1.get() <= 3) { // "<" au lieu de "<=" pour éviter un déplacement supplémentaire
                        component.setLocation(component.getX(), component.getY() + 1);
                        i1.incrementAndGet();
                    } else {
                        timerDown.stop();

                        AsUp = false;
                        AsDown = true;
                    }
                    component.repaint();
                });

                timerDown.start();
            }
            component.repaint();
        });

        timerUp.start();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        HoverBorder = false; // Changer la couleur de fond au desurvol

        if (timerDown != null && timerDown.isRunning()) return; // Empêche un nouveau Timer de descente
        if (timerUp != null && timerUp.isRunning()) return; // Empêche la descente si une montée est en coursmer de descente tourne
        if (AsDown) return;

        AtomicInteger i = new AtomicInteger();
        timerDown = new Timer(40, e1 -> {
            if (i.get() <= 3) { // "<" au lieu de "<=" pour éviter un déplacement supplémentaire
                component.setLocation(component.getX(), component.getY() + 1);
                i.incrementAndGet();
            } else {
                timerDown.stop();

                AsUp = false;
                AsDown = true;
            }
            component.repaint();
        });

        timerDown.start();
    }
}
