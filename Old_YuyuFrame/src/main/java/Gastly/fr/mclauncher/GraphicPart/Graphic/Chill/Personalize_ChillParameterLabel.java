package Gastly.fr.mclauncher.GraphicPart.Graphic.Chill;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class Personalize_ChillParameterLabel extends JPanel {

    private final int[] padletWrapper;

    public Personalize_ChillParameterLabel(String proposition, int padlet, JCheckBox dependencies, Boolean visible, Consumer<String> variableUpdater) {
        this.padletWrapper = new int[]{padlet};

        // Définir les propriétés du panneau
        setLayout(new BorderLayout()); // Alignement à gauche avec un espace de 10px
        setOpaque(false);

        // Création et configuration du label de proposition
        JLabel propositionLabel = new JLabel(proposition);
        propositionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        propositionLabel.setForeground(Color.WHITE);
        propositionLabel.setOpaque(false);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);

        // Création et configuration du label pour le "padlet"
        JLabel padletLabel = new JLabel(String.valueOf(padlet));
        padletLabel.setFont(new Font("Arial", Font.BOLD, 14));
        padletLabel.setForeground(Color.BLACK);
        padletLabel.setOpaque(false);

        // Création et configuration du bouton
        JButton changePadlet = new JButton("►");
        changePadlet.setFont(new Font("Arial", Font.BOLD, 14));
        changePadlet.setForeground(Color.BLACK);
        changePadlet.setFocusPainted(false); // Supprime la bordure de focus
        changePadlet.setContentAreaFilled(false); // Rend le bouton transparent
        changePadlet.setBorderPainted(false);
        changePadlet.setOpaque(false);

        if (dependencies.isSelected() != visible){setVisible(false);}

        // Ajouter un ActionListener pour rendre la visibilité dynamique
        dependencies.addActionListener(e -> {
            // Synchroniser la visibilité avec l'état de la case à cocher
            if (dependencies.isSelected() != visible){
                setVisible(false);
            } else{
                setVisible(true);
            }

            // Rafraîchir l'interface utilisateur
            revalidate();
            repaint();
        });

        changePadlet.addActionListener(e -> {
            // Utilisation d'un tableau pour rendre la valeur mutable
            padletWrapper[0]++; // Incrémente la valeur dans le tableau

            if (padletWrapper[0] > 3) {
                padletWrapper[0] = 1; // Réinitialise à 1 si la valeur dépasse 3
            }

            variableUpdater.accept(String.valueOf(padletWrapper[0]));
            padletLabel.setText(String.valueOf(padletWrapper[0]));

            LogicParameter.saveSettings("settings.properties");
        });

        changePadlet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                changePadlet.setForeground(Color.GRAY); // Changer la couleur de fond au survol
            }

            @Override
            public void mouseExited(MouseEvent e) {
                changePadlet.setForeground(Color.BLACK); // Changer la couleur de fond au desurvol
            }
        });

        // Ajouter les composants au panneau
        add(propositionLabel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        rightPanel.add(padletLabel);
        rightPanel.add(changePadlet);
    }
}