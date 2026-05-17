package Gastly.fr.mclauncher;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Personalize_Titlebar;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.InformationPanel.Gamer.GamerInformationPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.Login.Chill.ChillLoginPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.Login.Gamer.GamerLoginPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ChillLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ColorMonitor;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Gamer.GamerLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.MineShot.Gamer.GamerMineShotPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ModPanel.Gamer.GamerModPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.Chill.ChillParameterPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.Gamer.GamerParameterPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.QuestPanel.Gamer.QuestPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ServerPanel.Gamer.ServerPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.TexturePackPanel.Gamer.GamerTexturePackPanel;
import Gastly.fr.mclauncher.Login.API_auth;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.data.Souds_Effect;
import Gastly.fr.mclauncher.newdata.LoadedVersion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;

public class MCLauncher
{
	//© 2025 YuyuFrame. Le code source et les ressources de cette application sont protégés par des droits d'auteur. Aucune reproduction, distribution, ou modification sans autorisation préalable n'est autorisée.

	public static CardLayout cardLayout1 = null;
	public static JPanel mainPanel1 = null;
	public static JFrame frame1 = null;

	public static void main(String[] args) {

		LogicParameter.loadSettings("settings.properties");

		if (LogicParameter.ChangePadletToHour){
			getPadletForTime();
		}else{
			new ColorMonitor(LogicParameter.PadletDefault);
		}

		SwingUtilities.invokeLater(() -> {

			try {

				//Color
				Color customColortitlebar = new Color(5, 5, 5);

				// Créer la fenêtre
				JFrame frame = new JFrame() {

					@Override
					public void paint(Graphics g) {
						super.paint(g);
						Graphics2D g2d = (Graphics2D) g;
						g2d.setColor(Color.DARK_GRAY);
						g2d.setStroke(new BasicStroke(3)); // Épaisseur de la bordure
						g2d.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 15, 15)); // Dessiner la bordure
					}
				};

				Souds_Effect.Start(frame);
				LogicParameter.loadAudioParameter();
				Souds_Effect.playLoop("musique.wav", true);

				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setSize(1100, 700);
				frame.setResizable(false);
				frame.setUndecorated(true);
				frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));

				Image icon = new ImageIcon(ImageLocations.IconImage).getImage();
				frame.setIconImage(icon);
				frame.setTitle("YuyuFrame");

				// Créer un panneau avec une bordure
				JPanel borderPanel = new JPanel();
				borderPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 5)); // Bordure noire de 5 pixels
				borderPanel.setLayout(new BorderLayout());

				// Créer un JPanel pour agir comme une barre de titre personnalisée
				JPanel titleBar = new JPanel();
				titleBar.setBackground(customColortitlebar); // Couleur personnalisée pour la barre de titre
				titleBar.setLayout(new FlowLayout(FlowLayout.RIGHT)); // Alignement des boutons à droite
				titleBar.setPreferredSize(new Dimension(frame.getWidth(), 30));

				// Ajouter un bouton de minimisation
				JButton minimizeButton = new JButton("-");
				minimizeButton.setForeground(Color.RED);
				minimizeButton.setBorderPainted(false); // Pas de bordure peinte
				minimizeButton.setFocusPainted(false); // Pas de surlignement au focus
				minimizeButton.setContentAreaFilled(false); // Pas de remplissage du bouton
				minimizeButton.addActionListener(e -> {
					frame.setState(Frame.ICONIFIED);
					Souds_Effect.stop("musique.wav");
				}); // Minimiser la fenêtre
				minimizeButton.setOpaque(true);
				minimizeButton.setBackground(customColortitlebar);

				// Ajouter des boutons à la barre de titre
				JButton closeButton = new JButton("X");
				closeButton.setForeground(Color.RED);
				closeButton.setBorderPainted(false); // Pas de bordure peinte
				closeButton.setFocusPainted(false); // Pas de surlignement au focus
				closeButton.setContentAreaFilled(false); // Pas de remplissage du bouton
				closeButton.addActionListener(e -> {
					frame.dispose();
					Souds_Effect.close("musique.wav");
				});
				closeButton.setOpaque(true);
				closeButton.setBackground(customColortitlebar);

				titleBar.add(minimizeButton);
				titleBar.add(closeButton);

				closeButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseEntered(MouseEvent e) {
						closeButton.setBackground(new Color(169, 53, 53)); // Changer la couleur de fond au survol
					}

					@Override
					public void mouseExited(MouseEvent e) {
						closeButton.setBackground(customColortitlebar); // Changer la couleur de fond au desurvol
					}
				});

				minimizeButton.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseEntered(MouseEvent e) {
						minimizeButton.setBackground(new Color(82, 82, 82)); // Changer la couleur de fond au survol
					}

					@Override
					public void mouseExited(MouseEvent e) {
						minimizeButton.setBackground(customColortitlebar); // Changer la couleur de fond au desurvol
					}
				});

				// Utiliser ComponentMover pour rendre la barre de titre draggable
				new Personalize_Titlebar(frame, titleBar);

				// Ajouter la barre de titre et d'autres composants au cadre principal
				frame.add(titleBar, BorderLayout.NORTH);

				// Utiliser CardLayout pour gérer les panneaux
				CardLayout cardLayout = new CardLayout();
				JPanel mainPanel = new JPanel(cardLayout);

				// Créer et ajouter les panneaux
				JPanel authPage;
				JPanel mainPage;
				JPanel PPage;
				JPanel ServPage;
				JPanel QuestPage;
				JPanel ModManagerPage;
				JPanel TexturePage;
				JPanel MineShotPage;
				JPanel InformationPage;

				if (LogicParameter.Gamer){

					authPage = GamerLoginPanel.login(cardLayout, mainPanel, frame);
					mainPage = GamerLunchPanel.MainPanel(cardLayout, mainPanel, frame);
					PPage = GamerParameterPanel.MainPanel(cardLayout, mainPanel, frame);
					ServPage = ServerPanel.MainPanel(cardLayout, mainPanel, frame);
					QuestPage = QuestPanel.MainPanel(cardLayout, mainPanel, frame);
					ModManagerPage = GamerModPanel.MainPanel(cardLayout, mainPanel, frame);
					TexturePage = GamerTexturePackPanel.MainPanel(cardLayout, mainPanel, frame);
					MineShotPage = GamerMineShotPanel.MainPanel(cardLayout, mainPanel, frame);
					InformationPage = GamerInformationPanel.MainPanel(cardLayout, mainPanel, frame);
				}else {

					authPage = ChillLoginPanel.login(cardLayout, mainPanel, frame);
					mainPage = ChillLunchPanel.MainPanel(cardLayout, mainPanel, frame);
					PPage = ChillParameterPanel.MainPanel(cardLayout, mainPanel, frame);
					ServPage = ServerPanel.MainPanel(cardLayout, mainPanel, frame);
					QuestPage = QuestPanel.MainPanel(cardLayout, mainPanel, frame);
					ModManagerPage = GamerModPanel.MainPanel(cardLayout, mainPanel, frame);
					TexturePage = GamerTexturePackPanel.MainPanel(cardLayout, mainPanel, frame);
					MineShotPage = GamerMineShotPanel.MainPanel(cardLayout, mainPanel, frame);
					InformationPage = GamerInformationPanel.MainPanel(cardLayout, mainPanel, frame);
				}

				mainPanel.add(mainPage, "MainPage");
				mainPanel.add(authPage, "AuthPage");
				mainPanel.add(PPage, "Parametre");
				mainPanel.add(ServPage, "ServerPage");
				mainPanel.add(QuestPage, "QuestPage");
				mainPanel.add(ModManagerPage, "ModPage");
				mainPanel.add(TexturePage, "TexturePackPage");
				mainPanel.add(MineShotPage, "MineShotPage");
				mainPanel.add(InformationPage, "InfoPage");

				// Ajouter le panneau principal à la fenêtre
				frame.add(mainPanel);

				cardLayout1 = cardLayout;
				mainPanel1 = mainPanel;
				frame1 = frame;

				// Afficher la page principale initialement
				cardLayout.show(mainPanel, "MainPage");
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);

				if (DataBaseOfUser.isTokenExpired(DataBaseOfUser.accessToken)){

					if (!LogicParameter.RefreshTheToken){

						DataBaseOfUser.deleteAccount(DataBaseOfUser.playerName);
					}else{
						API_auth.refreshAccessToken();
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	public static void run(LoadedVersion currentVersion) throws InterruptedException {
		if(currentVersion == null)
		{
			throw new IllegalStateException("Please run \"setCurrentVersion\" first.");
		}

        DataBaseOfUser profile = DataBaseOfUser.getInstance();

        if(profile == null)
        {
            System.err.println("Something went wrong querying the login data from the Mojang server.");
            System.exit(1);
        }
        MCLauncherLab.run(currentVersion, profile);
		Souds_Effect.close("musique.wav");
    }
	public static void getPadletForTime(){
		LocalTime morning = LocalTime.of(7, 0);
		LocalTime afternoon = LocalTime.of(12, 0);
		LocalTime night = LocalTime.of(20, 0);

		// Obtenir l'heure actuelle
		LocalTime maintenant = LocalTime.now();

		// Vérifier si l'heure actuelle est entre 12h et 20h
		if (!maintenant.isBefore(morning) && !maintenant.isAfter(afternoon)) {
			new ColorMonitor(LogicParameter.PadletMorning);

		} else if (!maintenant.isBefore(afternoon) && !maintenant.isAfter(night)) {
			new ColorMonitor(LogicParameter.PadletAfternoon);

		}else {
			new ColorMonitor(LogicParameter.PadletNight);

		}
	}
}