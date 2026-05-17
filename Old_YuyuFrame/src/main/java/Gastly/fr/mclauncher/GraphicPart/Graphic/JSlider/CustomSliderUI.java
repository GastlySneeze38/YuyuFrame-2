package Gastly.fr.mclauncher.GraphicPart.Graphic.JSlider;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CustomSliderUI extends BasicSliderUI {

    private static final int TRACK_HEIGHT = 8;
    private static final int TRACK_WIDTH = 8;
    private static final int TRACK_ARC = 5;
    private static final Dimension THUMB_SIZE = new Dimension(20, 20);
    private final RoundRectangle2D.Float trackShape = new RoundRectangle2D.Float();
    private JLabel valueLabel; // Label to show the value above the thumb
    private Color tumbColor;

    public CustomSliderUI(final JSlider b, Color tumbColor) {
        super(b);

        this.tumbColor = tumbColor;

        // Initialize the value label
        valueLabel = new JLabel(String.valueOf(b.getValue()));
        valueLabel.setForeground(Color.BLACK); // Set label color
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Set font size
        valueLabel.setOpaque(false); // Make label background transparent

        // Add a ChangeListener to update the label value dynamically
        b.addChangeListener(e -> updateLabelPosition());
    }

    @Override
    protected void calculateTrackRect() {
        super.calculateTrackRect();
        if (isHorizontal()) {
            trackRect.y = trackRect.y + (trackRect.height - TRACK_HEIGHT) / 2;
            trackRect.height = TRACK_HEIGHT;
        } else {
            trackRect.x = trackRect.x + (trackRect.width - TRACK_WIDTH) / 2;
            trackRect.width = TRACK_WIDTH;
        }
        trackShape.setRoundRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height, TRACK_ARC, TRACK_ARC);
    }

    @Override
    protected void calculateThumbLocation() {
        super.calculateThumbLocation();
        if (isHorizontal()) {
            thumbRect.y = trackRect.y + (trackRect.height - thumbRect.height) / 2;
        } else {
            thumbRect.x = trackRect.x + (trackRect.width - thumbRect.width) / 2;
        }
    }

    @Override
    protected Dimension getThumbSize() {
        return THUMB_SIZE;
    }

    private boolean isHorizontal() {
        return slider.getOrientation() == JSlider.HORIZONTAL;
    }

    @Override
    public void paint(final Graphics g, final JComponent c) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paint(g, c);
    }

    @Override
    public void paintTrack(final Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Shape clip = g2.getClip();

        boolean horizontal = isHorizontal();
        boolean inverted = slider.getInverted();

        // Paint shadow.
        g2.setColor(new Color(170, 170 ,170));
        g2.fill(trackShape);

        // Paint track background.
        g2.setColor(new Color(200, 200 ,200));
        g2.setClip(trackShape);
        trackShape.y += 1;
        g2.fill(trackShape);
        trackShape.y = trackRect.y;

        g2.setClip(clip);

        // Paint selected track.
        if (horizontal) {
            boolean ltr = slider.getComponentOrientation().isLeftToRight();
            if (ltr) inverted = !inverted;
            int thumbPos = thumbRect.x + thumbRect.width / 2;
            if (inverted) {
                g2.clipRect(0, 0, thumbPos, slider.getHeight());
            } else {
                g2.clipRect(thumbPos, 0, slider.getWidth() - thumbPos, slider.getHeight());
            }

        } else {
            int thumbPos = thumbRect.y + thumbRect.height / 2;
            if (inverted) {
                g2.clipRect(0, 0, slider.getHeight(), thumbPos);
            } else {
                g2.clipRect(0, thumbPos, slider.getWidth(), slider.getHeight() - thumbPos);
            }
        }
        g2.setColor(tumbColor);
        g2.fill(trackShape);
        g2.setClip(clip);
    }

    @Override
    public void paintThumb(final Graphics g) {
        g.setColor(tumbColor);
        g.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
    }

    @Override
    public void paintFocus(final Graphics g) {}

    // Method to update the label position dynamically
    private void updateLabelPosition() {
        int sliderValue = slider.getValue();
        valueLabel.setText(String.valueOf(sliderValue));

        // Get the position of the thumb and adjust the label position accordingly
        int thumbX = thumbRect.x + thumbRect.width / 2;
        int thumbY = thumbRect.y - valueLabel.getHeight() - 5; // 5px above the thumb

        // Position the label
        valueLabel.setBounds(thumbX - valueLabel.getWidth() / 2, thumbY, valueLabel.getWidth(), valueLabel.getHeight());

        // Revalidate and repaint to update the position
        slider.repaint();
    }

    // Method to add the label to the slider
    public void addLabelToSlider() {
        slider.add(valueLabel);
    }
}