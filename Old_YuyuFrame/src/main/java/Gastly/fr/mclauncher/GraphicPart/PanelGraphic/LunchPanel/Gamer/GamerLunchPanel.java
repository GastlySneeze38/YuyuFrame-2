package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.*;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.DownloadPage.DownloadPage;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.SyncroniseLunchInfo;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.VideoPlayer;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.ImageLocations;
import Gastly.fr.mclauncher.LauncherCore;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.MCLauncher;
import Gastly.fr.mclauncher.data.Souds_Effect;
import Gastly.fr.mclauncher.newdata.LoadedVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

public class GamerLunchPanel {

    public static Personalize_GamerButtons StartButton;
    public static JPanel panelofMainPanel;
    public static Personalize_GamerButtons loginofMainpanel;
    public static int IconSize = 40;
    public static DownloadPage downloadComponent;

    public static JPanel MainPanel(CardLayout cardLayout, JPanel mainPanel, JFrame frame) throws IOException, FontFormatException {

        JPanel panel = new JPanel();
        panelofMainPanel = panel;
        panel.setLayout(null);

        //les couleur
        Color customColor = new Color(68, 62, 185);
        Color hovercolor = new Color(55, 42, 150);
        Color customColorWindow = new Color(19, 19, 21);

        panel.setBackground(customColorWindow);

        Font customFont = Font.createFont(Font.TRUETYPE_FONT, ImageLocations.TitleClientFont.openStream());
        customFont = customFont.deriveFont(30f);

        // Créer un JLabel avec du texte et une image
        JLabel label = new JLabel("YuyuFrame");
        label.setFont(customFont);
        label.setForeground(Color.white);

        //Ajout de l'image d'acceuille
        ImageIcon icon1 = new ImageIcon(ImageLocations.AccImage);
        Image img1 = icon1.getImage().getScaledInstance(800, 400, Image.SCALE_SMOOTH);
        VideoPlayer button = new VideoPlayer(new ImageIcon(img1), 30);
        panel.add(button);

        //ajouter le bouton de connection
        ImageIcon icon = DataBaseOfUser.getPlayerImage();
        Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);

        Personalize_GamerButtons login = new Personalize_GamerButtons(new ImageIcon(img), 20, true);
        login.setFocusPainted(false);
        panel.add(login);
        loginofMainpanel = login;

        // Ajouter un menu déroulant
        String[] versions = {"1.21.4", "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.17", "1.16.5", "1.14.4", "1.13.2", "1.12.2", "1.8.9"};

        Personalize_Gamer_JComboBox versionComboBox = new Personalize_Gamer_JComboBox(versions);

        //le bouton
        SyncroniseLunchInfo.Wait_To_Lunch = "lancer " + versionComboBox.getSelectedItem();
        Personalize_GamerButtons startButton = new Personalize_GamerButtons(SyncroniseLunchInfo.Wait_To_Lunch, 30, hovercolor, customColor);
        startButton.setBackground(customColor);
        startButton.setForeground(Color.BLACK);
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setBorder(null);
        StartButton = startButton;

        Personalize_TextNHoverComponent CopyRight = new Personalize_TextNHoverComponent("© 2025..", "© 2025 YuyuFrame.", Personalize_GamerHoverComponent.Right);
        CopyRight.setBounds(4, 430, 250, 30);
        panel.add(CopyRight);

        Personalize_GamerNavBar ABOUT = new Personalize_GamerNavBar("ABOUT");
        ABOUT.setBounds(4, 470, 90, 30);
        panel.add(ABOUT);

        Personalize_TextNHoverComponent PrivacyPolicy = new Personalize_TextNHoverComponent("Priva...", "Privacy Policy", Personalize_GamerHoverComponent.Right);
        PrivacyPolicy.setBounds(4, 505, 250, 30);
        panel.add(PrivacyPolicy);

        Personalize_GamerNavBar EULA = new Personalize_GamerNavBar("EULA");
        EULA.setBounds(4, 545, 90, 30);
        panel.add(EULA);

        Personalize_TextNHoverComponent README = new Personalize_TextNHoverComponent("READ...", "README", Personalize_GamerHoverComponent.Right);
        README.setBounds(4, 585, 250, 30);
        panel.add(README);

        Personalize_GamerNavBar LICENCE = new Personalize_GamerNavBar("Licence");
        LICENCE.setBounds(4, 620, 90, 30);
        panel.add(LICENCE);

        Personalize_CardSetting LegalCard = new Personalize_CardSetting(50, 200, new Color(40, 40, 40), false, 8);
        LegalCard.setBounds(15, 425, 70, 230);
        panel.add(LegalCard);

        // Créer et configurer le JSeparator
        Personalize_GamerJSeparator separator = new Personalize_GamerJSeparator("Quêtes", 18, customColorWindow);
        separator.setBounds(90, 440, 995, 40);

        Personalize_GamerJSeparator separator1 = new Personalize_GamerJSeparator("Navigation", 18, customColorWindow);
        separator1.setBounds(90, 550, 995, 40);

        //Information Icon
        ImageIcon icon2 = new ImageIcon(ImageLocations.InformationEmptyImage);
        Image img2 = icon2.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent Information = new Personalize_TextNHoverComponent(img2, "Information", Personalize_GamerHoverComponent.Under, customColorWindow, IconSize, ImageLocations.InformationHoverImage, ImageLocations.InformationEmptyImage);
        panel.add(Information);

        //Discord Icon
        ImageIcon icon3 = new ImageIcon(ImageLocations.DiscordEmptyImage);
        Image img3 = icon3.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent discord = new Personalize_TextNHoverComponent(img3,"My Discord", Personalize_GamerHoverComponent.Under, customColorWindow, IconSize, ImageLocations.DiscordVideImage, ImageLocations.DiscordEmptyImage);
        panel.add(discord);

        //Parameter icon
        ImageIcon icon4 = new ImageIcon(ImageLocations.ParameterEmptyImage);
        Image img4 = icon4.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent para = new Personalize_TextNHoverComponent(img4, "Parameter", Personalize_GamerHoverComponent.Under, customColorWindow, IconSize, ImageLocations.ParameterVideImage, ImageLocations.ParameterEmptyImage, true);
        panel.add(para);

        //TexturePack Icon
        ImageIcon icon5 = new ImageIcon(ImageLocations.TexturePackEmptyImage);
        Image img5 = icon5.getImage().getScaledInstance(IconSize + 5, IconSize + 5, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent TexturePack = new Personalize_TextNHoverComponent(img5, "Add Textures", Personalize_GamerHoverComponent.Under, customColorWindow, IconSize + 5, ImageLocations.ShaderEmptyImage, ImageLocations.TexturePackEmptyImage);
        panel.add(TexturePack);

        //Login Icon
        ImageIcon icon6 = new ImageIcon(ImageLocations.LoginEmptyImage);
        Image img6 = icon6.getImage().getScaledInstance(IconSize + 15, IconSize + 15, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent LoginNavBar = new Personalize_TextNHoverComponent(img6, "Login", Personalize_GamerHoverComponent.Under, customColorWindow, IconSize + 15, ImageLocations.LoginHoverImage, ImageLocations.LoginEmptyImage);
        panel.add(LoginNavBar);

        //Server Icon
        ImageIcon icon7 = new ImageIcon(ImageLocations.ServerEmptyImage);
        Image img7 = icon7.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent Server = new Personalize_TextNHoverComponent(img7, "Server", Personalize_GamerHoverComponent.Under, customColorWindow, IconSize, ImageLocations.ServerVideImage, ImageLocations.ServerEmptyImage);
        panel.add(Server);

        //Mod Icon
        ImageIcon icon8 = new ImageIcon(ImageLocations.ModEmptyImage);
        Image img8 = icon8.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent ModNavBar = new Personalize_TextNHoverComponent(img8, "Mods",Personalize_GamerHoverComponent.Under ,customColorWindow, IconSize, ImageLocations.ModHoverImage, ImageLocations.ModEmptyImage);
        panel.add(ModNavBar);

        //Quest Icon
        ImageIcon icon9 = new ImageIcon(ImageLocations.QuestEmptyImage);
        Image img9 = icon9.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent Quest = new Personalize_TextNHoverComponent(img9, "Quest",Personalize_GamerHoverComponent.Under ,customColorWindow, IconSize, ImageLocations.QuestHoverImage, ImageLocations.QuestEmptyImage);
        panel.add(Quest);

        //MineShot Icon
        ImageIcon icon10 = new ImageIcon(ImageLocations.MineShotEmptyImage);
        Image img10 = icon10.getImage().getScaledInstance(IconSize, IconSize, Image.SCALE_SMOOTH);
        Personalize_TextNHoverComponent MineShot = new Personalize_TextNHoverComponent(img10, "MineShot",Personalize_GamerHoverComponent.Under ,customColorWindow, IconSize, ImageLocations.MineShotHoverImage, ImageLocations.MineShotEmptyImage);
        panel.add(MineShot);

        discord.addButtonActionListener(e -> {
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
        LoginNavBar.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "AuthPage");
            frame.setSize(500, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        ModNavBar.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "ModPage");
            frame.setSize(500, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        TexturePack.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "TexturePackPage");
            frame.setSize(500, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        Information.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "InfoPage");
            frame.setSize(600, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        MineShot.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "MineShotPage");
            frame.setSize(500, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        Quest.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "QuestPage");
            frame.setSize(1000, 640);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        Server.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "ServerPage");
            frame.setSize(500, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        para.addButtonActionListener(e -> {
            cardLayout.show(mainPanel, "Parametre");
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        login.addActionListener(e -> {
            cardLayout.show(mainPanel, "AuthPage");
            frame.setSize(500, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        versionComboBox.addActionListener(e -> {
            String selectedVersion = (String) versionComboBox.getSelectedItem();
            startButton.setText("Lancer " + selectedVersion);
            Souds_Effect.play("click.wav");
        });
        startButton.addActionListener(e -> {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws InterruptedException {
                    StartButton.setText("launching ...");
                    Souds_Effect.play("click.wav");

                    // Lancer Minecraft lorsque le bouton est cliqué
                    try {
                        //You may remove or adjust this as you please, but if you run it from an IDE its helpful to have.
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

                            try {
                                downloadComponent = new DownloadPage();
                                downloadComponent.showIn(panel);
                                core.downloadNecessaryFiles(version);
                            } catch(IOException e1)
                            {
                                e1.printStackTrace();
                            }
                            SyncroniseLunchInfo.Wait_To_Lunch = "lancer " + versionComboBox.getSelectedItem();
                            MCLauncher.run(SyncroniseLunchInfo.currentVersion); //Start the game from the selected version.


                            if (LogicParameter.disposeframetolaunch){
                                frame.dispose();
                            }
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }

                    return null;
                }
            };
            // Execute the SwingWorker
            worker.execute();
        });

        //Ajouter les position
        startButton.setBounds(820, 330, 250, 60);
        versionComboBox.setBounds(820, 240, 250, 50);
        button.setBounds(10, 10, 800, 400);
        login.setBounds(890, 90, 100, 100);
        label.setBounds(850, 20, 250, 60);

        Information.setBounds(100, 595, Information.getHoverComponentWith(), IconSize + Information.getHoverComponentHeight());
        discord.setBounds(195, 595, discord.getHoverComponentWith(), IconSize + discord.getHoverComponentHeight());
        para.setBounds(300, 595, para.getHoverComponentWith(), IconSize + para.getHoverComponentHeight());
        TexturePack.setBounds(400, 590, TexturePack.getHoverComponentWith(), IconSize + 5 + TexturePack.getHoverComponentHeight());
        LoginNavBar.setBounds(545, 588, LoginNavBar.getHoverComponentWith(), IconSize + 12 + LoginNavBar.getHoverComponentHeight());
        Server.setBounds(650, 595, Server.getHoverComponentWith(), IconSize + Server.getHoverComponentHeight());
        ModNavBar.setBounds(770, 595, ModNavBar.getHoverComponentWith(), IconSize + ModNavBar.getHoverComponentHeight());
        Quest.setBounds(880, 595, Quest.getHoverComponentWith(), IconSize + Quest.getHoverComponentHeight());
        MineShot.setBounds(980, 595, MineShot.getHoverComponentWith(), IconSize + MineShot.getHoverComponentHeight());

        // Ajouter les composant a la fenaitre
        panel.add(login);
        panel.add(versionComboBox);
        panel.add(startButton);
        panel.add(separator);
        panel.add(separator1);
        panel.add(label);

        panel.setVisible(true);

        return panel;
    }
    public static void getRechargeAvatarAccount(){
        try {
            ImageIcon newIcon = DataBaseOfUser.getPlayerImage();
            Image newImg = newIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);

            // Mettre à jour l'image du label existant
            loginofMainpanel.setIcon(new ImageIcon(newImg));


        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }
}