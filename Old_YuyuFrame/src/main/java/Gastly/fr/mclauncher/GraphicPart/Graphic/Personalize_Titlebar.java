package Gastly.fr.mclauncher.GraphicPart.Graphic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Personalize_Titlebar extends MouseAdapter {

    private final Component target;
    private final Component source;
    private Point start_drag;
    private Point start_loc;

    public Personalize_Titlebar(Component target, Component source) {
        this.target = target;
        this.source = source;
        source.addMouseListener(this);
        source.addMouseMotionListener(this);
    }

    public Personalize_Titlebar(JFrame target, Component source) {
        this.target = target;
        this.source = source;
        source.addMouseListener(this);
        source.addMouseMotionListener(this);
    }

    public Point getScreenLocation(MouseEvent e) {
        Point cursor = e.getPoint();
        Point target_location = this.target.getLocationOnScreen();
        return new Point((int) (target_location.getX() + cursor.getX()),
                (int) (target_location.getY() + cursor.getY()));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.start_drag = this.getScreenLocation(e);
        this.start_loc = this.target.getLocation();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point current = this.getScreenLocation(e);
        Point offset = new Point((int) current.getX() - (int) start_drag.getX(),
                (int) current.getY() - (int) start_drag.getY());
        JFrame frame = (JFrame) target;
        Point new_location = new Point(
                (int) (this.start_loc.getX() + offset.getX()), (int) (this.start_loc.getY() + offset.getY()));
        frame.setLocation(new_location);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        source.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        source.setCursor(Cursor.getDefaultCursor());
    }

}
