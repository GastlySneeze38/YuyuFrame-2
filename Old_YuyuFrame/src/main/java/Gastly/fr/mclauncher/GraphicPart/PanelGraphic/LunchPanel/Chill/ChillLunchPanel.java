package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillButtons;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillJComboBox;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Chill.Personalize_ChillTextfield;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerButtons;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.SyncroniseLunchInfo;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.ImageLocations;
import Gastly.fr.mclauncher.LauncherCore;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.data.Souds_Effect;
import Gastly.fr.mclauncher.newdata.LoadedVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;

public class ChillLunchPanel {

    public static Personalize_ChillButtons StartButton;
    public static JButton loginofMainpanel;

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame) throws IOException, FontFormatException {
        // Créer un panneau avec un layout null (sans gestionnaire de disposition)
        JPanel panel = new JPanel() {
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

                // Espacement entre les deux groupes de points
                int spaceBetweenGroups = 400;

                // Calculer la largeur disponible pour placer les points (en tenant compte des marges et de l'espace entre les groupes)
                int availableWidth = width - 2 * margin - spaceBetweenGroups;

                // Calculer l'espacement horizontal pour 5 points dans chaque groupe
                int horizontalSpacing = availableWidth / 9; // 5 points + 4 espaces (entre points et entre groupes)

                // Premier groupe de 5 points
                for (int i = 0; i < 5; i++) {
                    int x = margin + i * horizontalSpacing;
                    g2d.fillOval(x, margin, pointSize, pointSize);
                }

                // Espace entre les groupes
                int xOffsetForSecondGroup = margin + 5 * horizontalSpacing + spaceBetweenGroups;

                // Deuxième groupe de 5 points
                for (int i = 0; i < 5; i++) {
                    int x = xOffsetForSecondGroup + i * horizontalSpacing;
                    g2d.fillOval(x, margin, pointSize, pointSize);
                }

                // Dessiner sur le bord droit
                for (int y = margin; y < height - margin; y += spacing) {
                    g2d.fillOval(width - margin - pointSize, y, pointSize, pointSize);
                }

                // Dessiner sur le bord gauche
                for (int y = margin; y < height - margin; y += spacing) {
                    g2d.fillOval(margin, y, pointSize, pointSize);
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
                g2d.fillRoundRect(55, 135, width - 90, height - 190, 20, 20); // Ombre décalée de 5px et flou

                // Dessiner la "page" (rectangle avec la couleur)
                g2d.setColor(ColorMonitor.Section2);
                g2d.fillRoundRect(50, 130, width - 100, height - 200, 20, 20); // Page avec coins arrondis

                g2d.setColor(Color.BLACK); // Couleur de la ligne
                g2d.setStroke(new BasicStroke(2)); // Largeur de 4 pixels
                int lineX = 750;
                int lineY = 131;
                int lineHeight = 468;
                g2d.drawLine(lineX, lineY, lineX, lineY + lineHeight);
            }
        };
        panel.setLayout(null);

        // Charger la police personnalisée
        Font customFont = Font.createFont(Font.TRUETYPE_FONT, ImageLocations.TitleClientFont.openStream());
        customFont = customFont.deriveFont(60f);

        // Ajouter le JLabel pour "FlowFrame"
        JLabel labelFF = new JLabel("YuyuFrame");
        labelFF.setFont(customFont);
        labelFF.setForeground(Color.BLACK);
        labelFF.setBounds(350, 20, 800, 100); // Positionner le label

        // Ajouter l'image du serv
        ImageIcon imgserv1 = new ImageIcon(ImageLocations.ServImage);
        Image imgserv2 = imgserv1.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        Personalize_GamerButtons buttonimgserv = new Personalize_GamerButtons(new ImageIcon(imgserv2), 30);
        buttonimgserv.setFocusPainted(false);
        buttonimgserv.setBounds(800, 140, 200, 200); // Positionner l'image (ajustez la position selon vos besoins)

        //Ajout de l'image d'acceuille
        ImageIcon imgacc1 = new ImageIcon(ImageLocations.AccImage);
        Image imgacc2 = imgacc1.getImage().getScaledInstance(600, 250, Image.SCALE_SMOOTH);
        Personalize_GamerButtons buttonacc = new Personalize_GamerButtons(new ImageIcon(imgacc2), 30);
        buttonacc.setFocusPainted(false);
        buttonacc.setBounds(100,150,600,250);

        // Ajouter les version pour lancer le jeu
        String[] versions = {"1.21.4","1.21", "1.20.6", "1.20.4", "1.19.4", "1.19.2", "1.18.2", "1.17", "1.16.5", "1.14.4", "1.13.2", "1.12.2", "1.8.9" };
        Personalize_ChillJComboBox versionComboBox = new Personalize_ChillJComboBox(versions, 200);
        versionComboBox.setBounds(300,475, 200,30);

        //ajouter le bouton pour lancer minecraft
        SyncroniseLunchInfo.Wait_To_Lunch = "lancer " + (String) versionComboBox.getSelectedItem();
        Personalize_ChillButtons startButton = new Personalize_ChillButtons(SyncroniseLunchInfo.Wait_To_Lunch, 30, true);
        startButton.setForeground(Color.BLACK);
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setBorder(null);
        startButton.setBounds(250,420, 300,50);
        StartButton = startButton;

        //ajouter l'image du joueur
        ImageIcon icon = DataBaseOfUser.getPlayerImage();
        Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        Personalize_GamerButtons login = new Personalize_GamerButtons(new ImageIcon(img), 20);
        login.setFocusPainted(false);
        login.setBounds(360,510,80,80);
        panel.add(login);
        loginofMainpanel = login;

        Personalize_ChillButtons createServButton = new Personalize_ChillButtons("Crée le serveur", 30, true);
        createServButton.setForeground(Color.BLACK);
        createServButton.setFont(new Font("Arial", Font.BOLD, 16));
        createServButton.setBorder(null);
        createServButton.setBounds(780,350, 240,50);

        // Ajouter les version pour lancer le jeu
        Personalize_ChillJComboBox versionServComboBox = new Personalize_ChillJComboBox(versions, 240);
        versionServComboBox.setBounds(780,410, 240,50);

        Personalize_ChillTextfield textField = new Personalize_ChillTextfield("nom du serveur");
        textField.setBounds(780, 470, 240, 50);

        Personalize_ChillButtons changeServ = new Personalize_ChillButtons(">", 30, true);
        changeServ.setForeground(Color.BLACK);
        changeServ.setFont(new Font("Arial", Font.BOLD, 24));
        changeServ.setBorder(null);
        changeServ.setBounds(840,530, 50,50);

        ImageIcon icon6 = new ImageIcon(ImageLocations.ParameterEmptyImage); // Remplacez par le chemin vers votre image
        Image img7 = icon6.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH);

        Personalize_ChillButtons parameterServ = new Personalize_ChillButtons(new ImageIcon(img7), 30, true);
        parameterServ.setForeground(Color.BLACK);
        parameterServ.setFont(new Font("Arial", Font.BOLD, 24));
        parameterServ.setBorder(null);
        parameterServ.setBounds(900,530, 50,50);

        versionComboBox.addActionListener(e -> {
            String selectedVersion = (String) versionComboBox.getSelectedItem();
            startButton.setText("Lancer " + selectedVersion);
            Souds_Effect.play("click.wav");
        });

        startButton.addActionListener(e -> {
            StartButton.setText("launching ...");
            Souds_Effect.play("click.wav");
            // Lancer Minecraft lorsque le bouton est cliqué
            try {
                //You may remove or adjust this as you please, but if you run it from an IDE its helpful to have.
                String ideUsername = "Ecconia";
                String ideProfile = (String) versionComboBox.getSelectedItem();

                SyncroniseLunchInfo.versionToLaunch = ideProfile;

                //Install and or Run:
                LauncherCore core = new LauncherCore();
                LoadedVersion version = core.loadVersion(ideProfile);

                System.out.println();
                if(version == null)
                {
                    LauncherCore.error("Could not load version '" + ideProfile + "'");
                }
                else
                {
                    System.out.println();
                    LauncherCore.normal("Successfully loaded version '" + ideProfile + "'");

                    SyncroniseLunchInfo.currentVersion = version;

                    try
                    {
                        core.downloadNecessaryFiles(version);
                    }
                    catch(IOException e1)
                    {
                        e1.printStackTrace();
                    }
                    SyncroniseLunchInfo.Wait_To_Lunch = "lancer " + (String) versionComboBox.getSelectedItem();

                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() throws InterruptedException {

                             //Start the game from the selected version.
                            Gastly.fr.mclauncher.MCLauncher.run(SyncroniseLunchInfo.currentVersion);
                            return null;
                        }
                    };
                    // Execute the SwingWorker
                    worker.execute();

                    if (LogicParameter.disposeframetolaunch){
                        frame.dispose();
                    }

                }

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        login.addActionListener(e -> {
            cardLayout.show(mainPanel, "AuthPage");
            frame.setSize(600, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });

        // Ajouter les elements
        panel.add(labelFF);
        panel.add(buttonimgserv);
        panel.add(buttonacc);
        panel.add(versionComboBox);
        panel.add(startButton);
        panel.add(login);
        panel.add(versionServComboBox);
        panel.add(createServButton);
        panel.add(textField);
        panel.add(changeServ);
        panel.add(parameterServ);

        return panel;
    }
    public static void getRechargeAvatarAccount(){
        try {
            ImageIcon newIcon = DataBaseOfUser.getPlayerImage();
            Image newImg = newIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);

            // Mettre à jour l'image du label existant
            loginofMainpanel.setIcon(new ImageIcon(newImg));

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }
}