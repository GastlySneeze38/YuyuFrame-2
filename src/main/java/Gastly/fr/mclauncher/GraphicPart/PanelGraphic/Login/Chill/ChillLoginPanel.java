package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.Login.Chill;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillButtons;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillJComboBox;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerButtons;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ChillLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;
import Gastly.fr.mclauncher.ImageLocations;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.Login.loginSecondPageMain;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Random;
import java.util.Set;

public class ChillLoginPanel {

    public static JButton loginofLoginpanel;
    public static JComboBox ActualizeJComboLogin;

    public static JPanel login(CardLayout cardLayout, JPanel mainPanel, JFrame frame) throws MalformedURLException {
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
                int lineX = 180;
                int lineY = 41;
                int lineHeight = 568;
                g2d.drawLine(lineX, lineY, lineX, lineY + lineHeight);
            }
        };
        panel.setLayout(null);

        // Obtenir les IDs des comptes
        Set<String> accountIds = DataBaseOfUser.getAccountIds();
        String[] accountArray = accountIds.toArray(new String[0]);

        Personalize_ChillJComboBox comboBox = new Personalize_ChillJComboBox(accountArray, 200);
        ActualizeJComboLogin = comboBox;
        comboBox.setBounds(260, 350, 200, 50);

        panel.add(comboBox);

        String selectedAccountId = (String) comboBox.getSelectedItem();
        DataBaseOfUser.loadAccount(selectedAccountId);

        // Ajout de l'image en haut
        ImageIcon icon = DataBaseOfUser.getPlayerImage();
        Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        Personalize_GamerButtons imageLabel = new Personalize_GamerButtons(new ImageIcon(img), 20);
        imageLabel.setFocusPainted(false);
        imageLabel.setBounds(285, 100, 150, 150);
        panel.add(imageLabel);
        loginofLoginpanel = imageLabel;

        // Ajout du bouton pour login Microsoft
        Personalize_ChillButtons button = new Personalize_ChillButtons("+", 30, true);
        button.setFont(new Font("Arial", Font.PLAIN, 100));
        button.setBounds(300, 450, 100, 100);
        panel.add(button);

        //buton pour changer de entre + ou -
        JButton changeButton = new JButton("▼");
        changeButton.setFont(new Font("Arial", Font.PLAIN, 25));
        changeButton.setBounds(320, 550, 60, 58);
        changeButton.setBackground(ColorMonitor.Section2);
        changeButton.setForeground(Color.lightGray);
        changeButton.setBorderPainted(false); // Pas de bordure peinte
        changeButton.setFocusPainted(false); // Pas de surlignement au focus
        changeButton.setContentAreaFilled(false); // Pas de remplissage du bouton
        changeButton.setOpaque(true);
        panel.add(changeButton);

        //buton discord
        ImageIcon icon1 = new ImageIcon(ImageLocations.DiscordEmptyImage);
        Image img1 = icon1.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

        Personalize_ChillButtons discord = new Personalize_ChillButtons(new ImageIcon(img1), 20, true);
        discord.setBounds(75, 400, 70, 70);
        panel.add(discord);

        //buton parametre
        ImageIcon icon2 = new ImageIcon(ImageLocations.ParameterEmptyImage);
        Image img2 = icon2.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

        Personalize_ChillButtons para = new Personalize_ChillButtons(new ImageIcon(img2), 20, true);
        para.setBounds(75, 150, 70, 70);
        panel.add(para);

        // Ajout du bouton pour revennir au menu principal
        JButton BackButton = new JButton("Back");
        BackButton.setFont(new Font("Arial", Font.BOLD, 20));
        BackButton.setHorizontalAlignment(SwingConstants.CENTER);
        BackButton.setBackground(ColorMonitor.Section2);
        BackButton.setBorderPainted(false); // Pas de bordure peinte
        BackButton.setFocusPainted(false); // Pas de surlignement au focus

        BackButton.setBounds(181, 40, 369, 30);
        panel.add(BackButton);

        discord.addActionListener(e -> {
            Souds_Effect.play("click.wav");
            try {
                // Vérifiez si le bureau est pris en charge
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    // Spécifiez l'URL à ouvrir
                    URI url = new URI("https://discord.gg/mX8A6mnssy");
                    // Ouvrez l'URL dans le navigateur par défaut
                    desktop.browse(url);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        BackButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "MainPage");
            frame.setSize(1100, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        para.addActionListener(e -> {
            cardLayout.show(mainPanel, "Parametre");
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        changeButton.addActionListener(e -> {
            Souds_Effect.play("click.wav");

            String action = button.getText();
            if (action == "+"){

                button.setText("-");
            } else if (action == "-") {

                button.setText("+");
            }
            comboBox.removeAllItems();
            for (String item : accountArray){comboBox.addItem(item);}
        });
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Souds_Effect.play("click.wav");

                // Créer et afficher la petite fenêtre
                String action = button.getText();

                if (action == "+"){
                    loginSecondPageMain.createAndShowSmallWindow();

                } else if (action == "-") {
                    DataBaseOfUser.deleteAccount((String) comboBox.getSelectedItem());

                    ChillLunchPanel.getRechargeAvatarAccount();
                    ChillLoginPanel.getRechargeAvatarAccount();

                }
            }
        });

        BackButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                BackButton.setBackground(ColorMonitor.SombreSection2); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                BackButton.setBackground(ColorMonitor.Section2); // Changer la couleur de fond au desurvol
            }
        });

        discord.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ImageIcon icon1 = new ImageIcon(ImageLocations.DiscordVideImage);
                Image img1 = icon1.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

                discord.setIcon(new ImageIcon(img1));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ImageIcon icon1 = new ImageIcon(ImageLocations.DiscordEmptyImage);
                Image img1 = icon1.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

                discord.setIcon(new ImageIcon(img1));
            }
        });

        para.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ImageIcon icon1 = new ImageIcon(ImageLocations.ParameterVideImage);
                Image img1 = icon1.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

                para.setIcon(new ImageIcon(img1));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ImageIcon icon1 = new ImageIcon(ImageLocations.ParameterEmptyImage);
                Image img1 = icon1.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);

                para.setIcon(new ImageIcon(img1));
            }
        });

        changeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                changeButton.setForeground(Color.WHITE); // Changer la couleur de fond au survol
                Souds_Effect.play("hover.wav");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                changeButton.setForeground(Color.lightGray); // Changer la couleur de fond au desurvol
            }
        });

        // Créer un champ pour afficher les détails du compte
        JTextArea accountDetails = new JTextArea();
        accountDetails.setEditable(false);
        panel.add(new JScrollPane(accountDetails), BorderLayout.CENTER);

        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedAccountId = (String) comboBox.getSelectedItem();
                    if (selectedAccountId != null) {
                        DataBaseOfUser.loadAccount(selectedAccountId);

                        try {
                            // Charger la nouvelle image avec l'UUID du compte sélectionné
                            ImageIcon newIcon = DataBaseOfUser.getPlayerImage();
                            Image newImg = newIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);

                            // Mettre à jour l'image du label existant
                            imageLabel.setIcon(new ImageIcon(newImg));

                            ChillLoginPanel.getRechargeAvatarAccount();

                        } catch (MalformedURLException ex) {
                            ex.printStackTrace();
                        }
                    }else{
                        ImageIcon newIcon = new ImageIcon(ImageLocations.UnconnectedImage);
                        Image newImg = newIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);

                        // Mettre à jour l'image du label existant
                        imageLabel.setIcon(new ImageIcon(newImg));
                    }
                }
            }
        });
        return panel;
    }
    public static void getRechargeAvatarAccount(){
        try {
            ImageIcon newIcon = DataBaseOfUser.getPlayerImage();
            Image newImg = newIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);

            // Mettre à jour l'image du label existant
            loginofLoginpanel.setIcon(new ImageIcon(newImg));

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

}
