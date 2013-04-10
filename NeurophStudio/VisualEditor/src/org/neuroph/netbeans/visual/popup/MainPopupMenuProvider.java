package org.neuroph.netbeans.visual.popup;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.widget.Widget;
import org.neuroph.core.Layer;
import org.neuroph.netbeans.visual.dialogs.AddCustomLayerDialog;
import org.neuroph.netbeans.visual.widgets.NeuralNetworkScene;
import org.openide.windows.WindowManager;

/**
 *
 * @author remote
 */
public class MainPopupMenuProvider implements PopupMenuProvider {

    JPopupMenu mainPopupMenu;

    @Override
    public JPopupMenu getPopupMenu(final Widget widget, final Point point) {

        mainPopupMenu = new JPopupMenu();
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenu addLayer = new JMenu("Add Layer");
        JMenuItem addEmptyLayerItem = new JMenuItem("Empty Layer");
        JMenuItem addCustomLayerItem = new JMenuItem("Custom Layer");
        JMenuItem showConnections = new JMenuItem("Show/Hide Connections");
        refreshItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((NeuralNetworkScene) widget.getScene()).refresh();
            }
        });

        addEmptyLayerItem.addActionListener(new ActionListener() {
            int dropIdx = 0;
            public void actionPerformed(ActionEvent e) {
                Widget neuralNetworkWidget = widget.getChildren().get(0).getChildren().get(0);
                for (int i = 0; i < (neuralNetworkWidget.getChildren().size()); i++) {
                    double layerWidgetPosition = neuralNetworkWidget.getChildren().get(i).getLocation().getY();
                    if (point.getY() < layerWidgetPosition) {
                        dropIdx = i - 1; // 
                        break;
                    } else {
                        dropIdx = neuralNetworkWidget.getChildren().size();
                    }
                }

                Layer layer = new Layer();
                NeuralNetworkScene scene = (NeuralNetworkScene) widget.getScene();
                scene.getNeuralNetwork().addLayer(dropIdx, layer);
                scene.refresh();
            }
        });

        addCustomLayerItem.addActionListener(new ActionListener() {
            int dropIdx = 0;
            public void actionPerformed(ActionEvent e) {
                Widget neuralNetworkWidget = widget.getChildren().get(0).getChildren().get(0);
                for (int i = 0; i < (neuralNetworkWidget.getChildren().size()); i++) {
                    double layerWidgetPosition = neuralNetworkWidget.getChildren().get(i).getLocation().getY();
                    if (point.getY() < layerWidgetPosition) {
                        dropIdx = i - 1; 
                        break;
                    } else {
                        dropIdx = neuralNetworkWidget.getChildren().size();
                    }
                }
                NeuralNetworkScene scene = (NeuralNetworkScene) widget.getScene();
                AddCustomLayerDialog dialog = new AddCustomLayerDialog(null, true, scene, dropIdx);
                dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                dialog.setVisible(true);
                scene.refresh();
            }
        });

        showConnections.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NeuralNetworkScene scene = (NeuralNetworkScene) widget.getScene();
                scene.setShowConnections(!scene.isShowConnections());
                scene.refresh();
            }
        });
        addLayer.add(addEmptyLayerItem);
        addLayer.add(addCustomLayerItem);
        mainPopupMenu.add(addLayer);
        mainPopupMenu.add(refreshItem);
        mainPopupMenu.add(showConnections);


        return mainPopupMenu;

    }
}