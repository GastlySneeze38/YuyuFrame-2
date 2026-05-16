package Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer;

import Gastly.fr.mclauncher.ImageLocations;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Personalize_QuestComponent extends JButton {

    private Personalize_GamerJProgressBar progress = new Personalize_GamerJProgressBar();

    public Personalize_QuestComponent(int Width, String Name, String Difficulty, String Coins, String STRQuestType, String Percent){
        setLayout(null);
        setPreferredSize(new Dimension(Width, 130));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBackground(Color.BLACK);

        JLabel QuestName = new JLabel(Name);
        QuestName.setOpaque(false);
        QuestName.setHorizontalAlignment(SwingConstants.CENTER);
        QuestName.setForeground(Color.WHITE);
        QuestName.setFont(new Font("Arial", Font.BOLD, 18));
        QuestName.setBounds(10, 10, Width - 20, 40);
        add(QuestName);

        JButton difficulty = new JButton(Difficulty);
        difficulty.setBackground(Color.ORANGE);
        difficulty.setForeground(Color.BLACK);
        difficulty.setFont(new Font("Arial", Font.BOLD, 10));
        difficulty.setFocusPainted(false);
        difficulty.setBorderPainted(false);
        difficulty.setBounds(60, 55, 70, 25);
        add(difficulty);

        Personalize_GamerButtons CoinsReward = new Personalize_GamerButtons(String.valueOf(Coins), 5);
        Image CoinsIcon = new ImageIcon(ImageLocations.CoinsImage).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        CoinsReward.setIcon(new ImageIcon(CoinsIcon));
        CoinsReward.setHorizontalTextPosition(LEFT);
        CoinsReward.setForeground(Color.WHITE);
        CoinsReward.setBounds(135, 55, 100, 25);
        add(CoinsReward);

        JButton QuestType = new JButton(STRQuestType);
        QuestType.setBackground(Color.DARK_GRAY);
        QuestType.setForeground(Color.WHITE);
        QuestType.setFont(new Font("Arial", Font.BOLD, 10));
        QuestType.setFocusPainted(false);
        QuestType.setBorderPainted(false);
        QuestType.setBounds(240, 55, 70, 25);
        add(QuestType);

        progress.setBounds(10, 100, Width - 20, 20);
        progress.setValue(Integer.parseInt(Percent));
        add(progress);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Appliquer un clip pour les coins arrondis
        Shape clip = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
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
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
        g2.dispose();
    }
    public int getAdvancementQuest(){
        return progress.getValue();
    }
}