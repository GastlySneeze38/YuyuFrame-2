package Gastly.fr.mclauncher.GraphicPart.Graphic.JComboBox;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;

public class BoundsPopupMenuListener implements PopupMenuListener {
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        SwingUtilities.invokeLater(() -> {
            JComboBox<?> comboBox = (JComboBox<?>) e.getSource();

            // Obtenir le popup associé au JComboBox
            BasicComboPopup popup = (BasicComboPopup) comboBox.getAccessibleContext().getAccessibleChild(0);
            JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);

            // Largeur et hauteur dynamiques
            int popupWidth = comboBox.getWidth(); // Largeur du JComboBox
            int visibleRowCount = Math.min(comboBox.getItemCount(), 10); // Limite à 10 lignes visibles
            int listHeight = 30 * visibleRowCount; // Hauteur totale (30px par ligne)

            // Ajuster les dimensions du popup
            popup.setPreferredSize(new Dimension(popupWidth, listHeight));
            popup.setPopupSize(new Dimension(popupWidth, listHeight));

            // Ajuster les dimensions du JScrollPane
            scrollPane.setPreferredSize(new Dimension(popupWidth, listHeight));
            scrollPane.getViewport().setPreferredSize(new Dimension(popupWidth, listHeight));

            // Ajuster les dimensions de la liste
            scrollPane.revalidate();
            scrollPane.repaint();
        });
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // Optionnel : actions lorsque le popup est fermé
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        // Optionnel : actions lorsque le popup est annulé
    }
}
