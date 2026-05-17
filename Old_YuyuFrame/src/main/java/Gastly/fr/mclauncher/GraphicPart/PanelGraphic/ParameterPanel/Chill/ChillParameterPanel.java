package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.Chill;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillJCheckBox;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillJSlider;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillParameterLabel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.Random;

public class ChillParameterPanel {

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame) throws IOException, FontFormatException {

        // Créer un panneau avec un layout null (sans gestionnaire de disposition)
        JPanel panel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Convertir Graphics en Graphics2D
                Graphics2D g2d = (Graphics2D) g;

                // Obtenir la taille du panneau
                int width = getWidth();
                int height = getHeight();

                //differantes couleur
                // Section 1 (20% de la hauteur)
                int section1Height = (int) (height * 0.45);
                g2d.setColor(ColorMonitor.Section1);
                g2d.fillRect(0, 0, width, section1Height);

                // Section 2 (50% de la hauteur)
                int section2Height = (int) (height * 0.05);
                g2d.setColor(ColorMonitor.Section2);
                g2d.fillRect(0, section1Height, width, section2Height);

                // Section 3 (30% de la hauteur)
                int section3Height = height - section1Height - section2Height;
                g2d.setColor(ColorMonitor.Section3);
                g2d.fillRect(0, section1Height + section2Height, width, section3Height);

                // dessiner les point
                g2d.setColor(Color.BLACK);
                int pointSize = 3; // Taille des points
                int spacing = 40;  // Espacement entre les points
                int margin = 20;   // Distance par rapport au bord

                // Dessiner sur le bord droit
                for (int y = margin; y < height - margin; y += spacing) {
                    g2d.fillOval(width - margin - pointSize, y, pointSize, pointSize);
                }

                // Dessiner sur le bord gauche
                for (int y = margin; y < height - margin; y += spacing) {
                    g2d.fillOval(margin, y, pointSize, pointSize);
                }

                // Dessiner sur le bord haut
                for (int x = margin; x < width - margin; x += spacing) {
                    g2d.fillOval(x, margin, pointSize, pointSize);
                }

                // Dessiner sur le bord bas
                for (int x = margin; x < width - margin; x += spacing) {
                    g2d.fillOval(x, height - margin - pointSize, pointSize, pointSize);
                }

                //dessiner les nuage
                // Utiliser une couleur de nuage (gris clair ou blanc)
                g2d.setColor(new Color(255, 255, 255, 180)); // Blanc avec 180 d'opacité (translucide)

                // Créer un générateur de nombres aléatoires pour la position et la taille des cercles
                Random rand = new Random();

                // Dessiner plusieurs cercles de différentes tailles et positions
                for (int i = 0; i < 25; i++) {
                    // Générer des valeurs aléatoires pour la position et la taille des cercles
                    int x = rand.nextInt(getWidth() - 100); // Position horizontale dans les limites de la fenêtre
                    int y = rand.nextInt(getHeight() - 100); // Position verticale dans les limites de la fenêtre
                    int diameter = rand.nextInt(50) + 30; // Diamètre des cercles (entre 30 et 80 pixels)

                    // Dessiner un cercle à la position (x, y) avec un diamètre aléatoire
                    g2d.fillOval(x, y, diameter, diameter);
                }

                // Dessiner une "page" au-dessus des trois sections avec une ombre

                // Ajouter l'ombre portée (décalage de 5px, flou de 10px, couleur gris foncé)
                g2d.setColor(new Color(0, 0, 0, 60)); // Ombre avec une transparence
                g2d.fillRoundRect(55, 45, width - 90, height - 90, 20, 20); // Ombre décalée de 5px et flou

                // Dessiner la "page" (rectangle avec la couleur)
                g2d.setColor(ColorMonitor.Section2);
                g2d.fillRoundRect(50, 40, width - 100, height - 100, 20, 20); // Page avec coins arrondis

                g2d.setColor(Color.BLACK); // Couleur de la ligne
                g2d.setStroke(new BasicStroke(2)); // Largeur de 4 pixels
                int lineX = 51;
                int lineY = 180;
                int lineHeight = 498;
                g2d.drawLine(lineX, lineY, lineX + lineHeight, lineY);

                g2d.setColor(Color.BLACK); // Couleur de la ligne
                g2d.setStroke(new BasicStroke(2)); // Largeur de 4 pixels
                int lineY1 = 400;
                g2d.drawLine(lineX, lineY1, lineX + lineHeight, lineY1);
            }
        };
        panel.setLayout(null);

        Personalize_ChillJSlider slider = new Personalize_ChillJSlider(1024, 12288, 4096);
        slider.setBounds(75,70,450,80);

        // Ajouter le slider au panneau
        panel.add(slider);

        //ajout des boolean
        Personalize_ChillJCheckBox disposeFrameCheck = new Personalize_ChillJCheckBox("Dispose Frame to Launch", LogicParameter.disposeframetolaunch, value -> LogicParameter.disposeframetolaunch = value, 490);
        disposeFrameCheck.setBounds(55, 220, 490, 30);

        panel.add(disposeFrameCheck);

        Personalize_ChillJCheckBox refreshTokenCheck = new Personalize_ChillJCheckBox("Refresh Token", LogicParameter.RefreshTheToken, value -> LogicParameter.RefreshTheToken = value, 490);
        refreshTokenCheck.setBounds(55, 260, 490, 30);

        panel.add(refreshTokenCheck);

        Personalize_ChillJCheckBox gamerCheck = new Personalize_ChillJCheckBox("Gamer Mode", LogicParameter.Gamer, value -> LogicParameter.Gamer = value, 490);
        gamerCheck.setBounds(55, 300, 490, 30);

        panel.add(gamerCheck);

        Personalize_ChillJCheckBox changePadletCheck = new Personalize_ChillJCheckBox("Change Padlet to Hour", LogicParameter.ChangePadletToHour, value -> LogicParameter.ChangePadletToHour = value, gamerCheck, 490);
        changePadletCheck.setBounds(55, 340, 490, 30);

        panel.add(changePadletCheck);

        Personalize_ChillParameterLabel padletMorning = new Personalize_ChillParameterLabel("Padlet Morning", LogicParameter.PadletMorning, changePadletCheck, true, value -> LogicParameter.PadletMorning = Integer.parseInt(value));
        padletMorning.setBounds(55, 420, 490, 30);

        panel.add(padletMorning);

        Personalize_ChillParameterLabel padletAfternoon = new Personalize_ChillParameterLabel("Padlet Afternoon", LogicParameter.PadletAfternoon, changePadletCheck, true, value -> LogicParameter.PadletAfternoon = Integer.parseInt(value));
        padletAfternoon.setBounds(55, 450, 490, 30);

        panel.add(padletAfternoon);

        Personalize_ChillParameterLabel padletNight = new Personalize_ChillParameterLabel("Padlet Night", LogicParameter.PadletNight, changePadletCheck, true, value -> LogicParameter.PadletNight = Integer.parseInt(value));
        padletNight.setBounds(55, 480, 490, 30);

        panel.add(padletNight);

        Personalize_ChillParameterLabel defaultPadlet = new Personalize_ChillParameterLabel("Default padlet", LogicParameter.PadletDefault, changePadletCheck, false, value -> LogicParameter.PadletDefault = Integer.parseInt(value));
        defaultPadlet.setBounds(55, 450, 490, 30);

        panel.add(defaultPadlet);

        // Ajout du bouton pour revennir au menu principal
        JButton BackButton = new JButton("Back");
        BackButton.setFont(new Font("Arial", Font.BOLD, 20));
        BackButton.setHorizontalAlignment(SwingConstants.CENTER);
        BackButton.setBackground(ColorMonitor.Section2);
        BackButton.setBorderPainted(false); // Pas de bordure peinte
        BackButton.setFocusPainted(false); // Pas de surlignement au focus

        BackButton.setBounds(50, 580, 500, 30);
        panel.add(BackButton);

        BackButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "AuthPage");
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
        });

        BackButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                BackButton.setBackground(ColorMonitor.SombreSection2); // Changer la couleur de fond au survol
            }

            @Override
            public void mouseExited(MouseEvent e) {
                BackButton.setBackground(ColorMonitor.Section2); // Changer la couleur de fond au desurvol
            }
        });

        return panel;
    }
}
