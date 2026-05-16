package Gastly.fr.mclauncher.Login;

import Gastly.fr.mclauncher.Login.PanelloginSecondPage.PasseworldPanel;
import Gastly.fr.mclauncher.Login.PanelloginSecondPage.emailPanel;

import javax.swing.*;
import java.awt.*;

public class loginSecondPageMain {

    public static void createAndShowSmallWindow() {
        SwingUtilities.invokeLater(() -> {
            Color customColorWindow = new Color(19, 19, 21);

            // Créer la petite fenêtre (JDialog ou JFrame)
            JFrame frame = new JFrame();
            frame.setSize(400, 300);

            // Utiliser CardLayout pour gérer les panneaux
            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            // Créer et ajouter les panneaux
            JPanel EmailPage = emailPanel.email(cardLayout, mainPanel, frame);
            JPanel PassPage = PasseworldPanel.passworld(cardLayout, mainPanel, frame);

            mainPanel.add(EmailPage, "EmailPage");
            mainPanel.add(PassPage, "PassPage");

            // Ajouter le panneau principal à la fenêtre
            frame.add(mainPanel);

            // Afficher la page principale initialement
            cardLayout.show(mainPanel, "EmailPage");
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
