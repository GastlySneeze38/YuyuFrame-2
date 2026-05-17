package Gastly.fr.mclauncher.Login.PanelloginSecondPage;

import Gastly.fr.mclauncher.ImageLocations;

import javax.swing.*;
import java.awt.*;

public class emailPanel {

    public static JPanel email(CardLayout cardLayout, JPanel mainPanel, JFrame frame){

        // Créer le panneau principal
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);

        // Ajouter le logo Microsoft en haut
        ImageIcon logoLabel = new ImageIcon(ImageLocations.MicrosoftImage);
        Image img = logoLabel.getImage().getScaledInstance(250, 70, Image.SCALE_SMOOTH);
        JLabel logo = new JLabel(new ImageIcon(img));
        logo.setBounds(10, 30, 200, 50);// Marge autour du logo
        panel.add(logo);

        // Ajouter le label "Se connecter"
        JLabel titleLabel = new JLabel("Connection");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBounds(50, 90, 300, 30);
        panel.add(titleLabel);

        // Ajouter un champ de texte pour l'adresse e-mail ou le téléphone
        JTextField emailField = new JTextField(20);
        emailField.setFont(new Font("Arial", Font.PLAIN, 16));
        emailField.setBounds(50, 140, 300, 40);
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        emailField.setToolTipText("Email, phone, or Skype");
        panel.add(emailField);

        // Ajouter un bouton "Suivant"
        JButton nextButton = new JButton("Suivant");
        nextButton.setFont(new Font("Arial", Font.PLAIN, 18));
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(new Color(0, 120, 215)); // Couleur bleue de Microsoft
        nextButton.setFocusPainted(false);
        nextButton.setBounds(50, 190, 300, 40); // Positionnement du bouton
        panel.add(nextButton);

        nextButton.addActionListener(e -> {
            PasseworldPanel.getEmailClient(emailField.getText());
            // Recréer le panneau de mot de passe après la mise à jour de l'email
            JPanel passPage = PasseworldPanel.passworld(cardLayout, mainPanel, frame);
            mainPanel.add(passPage, "PassPage");

            cardLayout.show(mainPanel, "PassPage");
            frame.setSize(400, 350);
        });

        return panel;
    }
}
