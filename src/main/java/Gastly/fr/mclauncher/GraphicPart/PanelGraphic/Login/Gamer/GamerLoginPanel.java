package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.Login.Gamer;

import Gastly.fr.mclauncher.GraphicPart.Graphic.Animate.ColorTransformation;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_GamerButtons;
import Gastly.fr.mclauncher.GraphicPart.Graphic.Gamer.Personalize_Gamer_JComboBox;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Gamer.GamerLunchPanel;
import Gastly.fr.mclauncher.ImageLocations;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.Login.loginSecondPageMain;
import Gastly.fr.mclauncher.data.Souds_Effect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.MalformedURLException;
import java.util.Set;

public class GamerLoginPanel {

    public static JButton loginofLoginpanel;
    public static JComboBox ActualizeJComboLogin;

    public static JPanel login(CardLayout cardLayout, JPanel mainPanel, JFrame frame) throws MalformedURLException {

        Color customColorWindow = new Color(19, 19, 21);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(customColorWindow);

        // Obtenir les IDs des comptes
        Set<String> accountIds = DataBaseOfUser.getAccountIds();
        String[] accountArray = accountIds.toArray(new String[0]);

        Personalize_Gamer_JComboBox comboBox = new Personalize_Gamer_JComboBox(accountArray);
        comboBox.setBounds(150, 350, 200, 50);
        ActualizeJComboLogin = comboBox;
        panel.add(comboBox);

        String selectedAccountId = (String) comboBox.getSelectedItem();
        DataBaseOfUser.loadAccount(selectedAccountId);

        // Ajout du bouton pour revennir au menu principal
        JButton Backbutton = new JButton("Back");
        Backbutton.setFont(new Font("Arial", Font.BOLD, 20));
        Backbutton.setBounds(2, 0, 496, 30);
        Backbutton.setBackground(customColorWindow);
        Backbutton.setForeground(Color.lightGray);
        Backbutton.setBorderPainted(false); // Pas de bordure peinte
        Backbutton.setFocusPainted(false); // Pas de surlignement au focus
        Backbutton.setContentAreaFilled(false); // Pas de remplissage du bouton
        Backbutton.setOpaque(true);
        panel.add(Backbutton);

        // Ajout de l'image en haut
        ImageIcon icon = DataBaseOfUser.getPlayerImage();
        Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        Personalize_GamerButtons imageLabel = new Personalize_GamerButtons(new ImageIcon(img), 20);
        imageLabel.setFocusPainted(false);
        imageLabel.setBounds(175, 100, 150, 150);
        panel.add(imageLabel);
        loginofLoginpanel = imageLabel;

        // Ajout du bouton pour login Microsoft
        Personalize_GamerButtons button = new Personalize_GamerButtons("+", 30, new Color(82, 82, 82), Color.BLACK);
        button.setFont(new Font("Arial", Font.PLAIN, 100));
        button.setFocusPainted(false);
        button.setBackground(Color.BLACK);
        button.setForeground(Color.WHITE);
        button.setBounds(195, 500, 100, 100);
        panel.add(button);

        //buton pour changer de entre + ou -
        JButton changeButton = new JButton("▼");
        changeButton.setFont(new Font("Arial", Font.PLAIN, 22));
        changeButton.setBounds(215, 600, 60, 60);
        changeButton.setBackground(customColorWindow);
        changeButton.setForeground(Color.lightGray);
        changeButton.setBorderPainted(false); // Pas de bordure peinte
        changeButton.setFocusPainted(false); // Pas de surlignement au focus
        changeButton.setContentAreaFilled(false); // Pas de remplissage du bouton
        changeButton.setOpaque(true);
        panel.add(changeButton);

        Backbutton.addActionListener(e -> {
            cardLayout.show(mainPanel, "MainPage");
            frame.setSize(1100, 700);
            frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 15, 15));
            frame.setLocationRelativeTo(null);
            Souds_Effect.play("click.wav");
        });
        Backbutton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        Backbutton.addMouseListener(new ColorTransformation(Backbutton, customColorWindow, new Color(82, 82, 82), true));

        changeButton.addActionListener(e -> {
            Souds_Effect.play("click.wav");

            String action = button.getText();
            if (action == "+"){

                button.setText("-");
            } else if (action == "-") {

                button.setText("+");
            }
        });
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Créer et afficher la petite fenêtre
                String action = button.getText();

                if (action == "+"){
                    loginSecondPageMain.createAndShowSmallWindow();

                } else if (action == "-") {
                    DataBaseOfUser.deleteAccount((String) comboBox.getSelectedItem());

                    GamerLunchPanel.getRechargeAvatarAccount();
                    GamerLoginPanel.getRechargeAvatarAccount();
                }
                // Obtenir les IDs des comptes
                Set<String> accountIds = DataBaseOfUser.getAccountIds();
                String[] accountArray = accountIds.toArray(new String[0]);

                comboBox.removeAllItems();
                for (String item : accountArray){comboBox.addItem(item);}
                Souds_Effect.play("click.wav");
            }
        });
        changeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Souds_Effect.play("hover.wav");
            }
        });
        changeButton.addMouseListener(new ColorTransformation(changeButton, Color.lightGray, Color.WHITE, false));

        // Créer un champ pour afficher les détails du compte
        JTextArea accountDetails = new JTextArea();
        accountDetails.setEditable(false);
        panel.add(new JScrollPane(accountDetails), BorderLayout.CENTER);

        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedAccountId1 = (String) comboBox.getSelectedItem();
                if (selectedAccountId1 != null) {
                    DataBaseOfUser.loadAccount(selectedAccountId1);

                    try {
                        // Charger la nouvelle image avec l'UUID du compte sélectionné
                        ImageIcon newIcon = DataBaseOfUser.getPlayerImage();
                        Image newImg = newIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);

                        // Mettre à jour l'image du label existant
                        imageLabel.setIcon(new ImageIcon(newImg));

                        GamerLunchPanel.getRechargeAvatarAccount();

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