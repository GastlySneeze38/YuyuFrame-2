package Gastly.fr.mclauncher.Login.PanelloginSecondPage;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.Login.Chill.ChillLoginPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.Login.Gamer.GamerLoginPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ChillLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Gamer.GamerLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.ImageLocations;
import Gastly.fr.mclauncher.Login.API_auth;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.MCLauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Set;

public class PasseworldPanel {

    public static String Email = null;
    public static CardLayout cardLayout1 = MCLauncher.cardLayout1;
    public static JPanel mainPanel1 = MCLauncher.mainPanel1;
    public static JFrame frame1 = MCLauncher.frame1;

    public static void getEmailClient(String email){
        Email = email;
    }

    public static JPanel passworld(CardLayout cardLayout, JPanel mainPanel, JFrame frame){

        // Créer le panneau principal
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);

        // Ajouter un bouton "← Back"
        JButton BackButton = new JButton("← Back");
        BackButton.setFont(new Font("Arial", Font.BOLD, 24));
        BackButton.setForeground(Color.BLACK);
        BackButton.setBorderPainted(false); // Pas de bordure peinte
        BackButton.setFocusPainted(false); // Pas de surlignement au focus
        BackButton.setContentAreaFilled(false); // Pas de remplissage du bouton
        BackButton.setBounds(20, 10, 150, 40); // Positionnement du bouton
        panel.add(BackButton);

        // Ajouter le logo Microsoft en haut
        ImageIcon logoLabel = new ImageIcon(ImageLocations.MicrosoftImage);
        Image img = logoLabel.getImage().getScaledInstance(250, 70, Image.SCALE_SMOOTH);
        JLabel logo = new JLabel(new ImageIcon(img));
        logo.setBounds(10, 50, 200, 50);// Marge autour du logo
        panel.add(logo);

        // Ajouter le label "email"
        JLabel titleLabel = new JLabel(Email);
        titleLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        titleLabel.setBounds(50, 110, 300, 30);
        panel.add(titleLabel);

        // Ajouter le label "Mot de passe"
        JLabel titleLabel2 = new JLabel("Mot de passe");
        titleLabel2.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel2.setBounds(50, 150, 300, 30);
        panel.add(titleLabel2);

        // Ajouter un champ de texte pour l'adresse e-mail ou le téléphone
        JPasswordField emailField = new JPasswordField(20);
        emailField.setFont(new Font("Arial", Font.PLAIN, 16));
        emailField.setBounds(50, 190, 300, 40);
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        emailField.setToolTipText("Email, phone, or Skype");
        emailField.setEchoChar('●');
        panel.add(emailField);

        // Ajouter un bouton "Suivant"
        JButton nextButton = new JButton("Se connecter");
        nextButton.setFont(new Font("Arial", Font.PLAIN, 18));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(new Color(0, 120, 215)); // Couleur bleue de Microsoft
        nextButton.setFocusPainted(false);
        nextButton.setBounds(50, 240, 300, 40); // Positionnement du bouton
        panel.add(nextButton);

        JButton toggleButton = new JButton("👁️");

        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (toggleButton.isSelected()) {
                    emailField.setEchoChar((char) 0); // Affiche les lettres
                } else {
                    emailField.setEchoChar('●'); // Masque les lettres avec des points
                }
            }
        });

        nextButton.addActionListener(e -> {
            try {
                boolean Authantification = API_auth.authenticate(Email, emailField.getText());

                if (Authantification){
                    frame.dispose();

                    // Obtenir les IDs des comptes
                    Set<String> accountIds = DataBaseOfUser.getAccountIds();
                    String[] accountArray = accountIds.toArray(new String[0]);
                    if (LogicParameter.Gamer){
                        GamerLunchPanel.getRechargeAvatarAccount();
                        GamerLoginPanel.getRechargeAvatarAccount();

                        GamerLoginPanel.ActualizeJComboLogin.removeAllItems();
                        for (String item : accountArray){GamerLoginPanel.ActualizeJComboLogin.addItem(item);}
                        return;
                    }
                    ChillLunchPanel.getRechargeAvatarAccount();
                    ChillLoginPanel.getRechargeAvatarAccount();

                    ChillLoginPanel.ActualizeJComboLogin.removeAllItems();
                    for (String item : accountArray){
                        ChillLoginPanel.ActualizeJComboLogin.addItem(item);}

                }else{
                    frame.setSize(400, 400);

                    // Ajouter le label "Mot de passe"
                    JLabel errorLabel = new JLabel("Email ou Mot de passe incorrect !");
                    errorLabel.setFont(new Font("Arial", Font.BOLD, 18));
                    errorLabel.setForeground(Color.RED);
                    errorLabel.setBounds(50, 300, 300, 30);
                    panel.add(errorLabel);
                }

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        BackButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "EmailPage");
            frame.setSize(400, 300);
        });

        return panel;
    }

}
