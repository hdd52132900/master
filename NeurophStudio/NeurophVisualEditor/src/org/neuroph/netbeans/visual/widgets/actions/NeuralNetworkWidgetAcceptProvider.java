package org.neuroph.netbeans.visual.widgets.actions;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.Widget;
import org.neuroph.core.Connection;
import org.neuroph.core.Layer;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.core.learning.UnsupervisedLearning;
import org.neuroph.netbeans.visual.NeuralNetworkEditor;
import org.neuroph.netbeans.visual.dialogs.AddCompetitiveLayerDialog;
import org.neuroph.netbeans.visual.dialogs.AddCustomLayerDialog;
import org.neuroph.netbeans.visual.dialogs.AddInputLayerDialog;
import org.neuroph.netbeans.visual.palette.PaletteItemNode;
import org.neuroph.netbeans.visual.widgets.NeuralNetworkScene;
import org.neuroph.netbeans.visual.widgets.NeuralNetworkWidget;
import org.neuroph.nnet.comp.layer.CompetitiveLayer;
import org.neuroph.nnet.comp.layer.InputLayer;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.LMS;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.nnet.learning.PerceptronLearning;
import org.neuroph.nnet.learning.SigmoidDeltaRule;
import org.neuroph.nnet.learning.UnsupervisedHebbianLearning;
import org.neuroph.util.ConnectionFactory;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.ImageUtilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author hrza
 * @author Boris Perović
 */
public class NeuralNetworkWidgetAcceptProvider implements AcceptProvider {

    private final NeuralNetworkWidget neuralNetworkWidget;
    private final NeuralNetworkScene scene;

    public NeuralNetworkWidgetAcceptProvider(NeuralNetworkWidget neuralNetworkWidget) {
        this.neuralNetworkWidget = neuralNetworkWidget;
        this.scene = ((NeuralNetworkScene) neuralNetworkWidget.getScene());
    }

    @Override
    public ConnectorState isAcceptable(Widget widget, Point point, Transferable t) {
        //   ovde treba napraviti razliku izmedju dnd sa palete i dataobjecta iz projekta jer baca array index exception
        // https://blogs.oracle.com/geertjan/entry/draganddrophandler_and_enclose_in
// http://bits.netbeans.org/dev/javadoc/org-openide-nodes/org/openide/nodes/doc-files/api.html    
// http://netbeans.dzone.com/nb-how-to-drag-drop-with-nodes-api    
        // https://blogs.oracle.com/geertjan/entry/drag_and_drop_without_activeeditorsupport
        // https://blogs.oracle.com/geertjan/entry/draganddrophandler_and_enclose_in

        Node node = NodeTransfer.node(t, NodeTransfer.DND_COPY_OR_MOVE);
        if (node instanceof PaletteItemNode) {
            PaletteItemNode pin = (PaletteItemNode) node;
            Image dragImage = ImageUtilities.loadImage(pin.getPaletteItem().getIcon());
            JComponent view = widget.getScene().getView();
            Graphics2D g2 = (Graphics2D) view.getGraphics();
            view.paintImmediately(view.getVisibleRect());

            Point globalPoint = widget.convertLocalToScene(point);
            g2.drawImage(dragImage, globalPoint.x, globalPoint.y, view);

            Class droppedClass = pin.getPaletteItem().getDropClass();
            return canAccept(droppedClass) ? ConnectorState.ACCEPT : ConnectorState.REJECT;
        } else {
            return ConnectorState.REJECT;
        }
    }

    @Override
    public void accept(Widget widget, Point point, Transferable t) {
        Node node = NodeTransfer.node(t, NodeTransfer.DND_COPY_OR_MOVE);
        PaletteItemNode pin = (PaletteItemNode) node;
        Class droppedClass = pin.getPaletteItem().getDropClass();

        if ((neuralNetworkWidget.getNeuralNetwork().getLayersCount() == 0) && (droppedClass.equals(Layer.class) || droppedClass.getSuperclass().equals(Layer.class))) {
            neuralNetworkWidget.removeChildren();
            neuralNetworkWidget.setLayout(LayoutFactory.createVerticalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 50));
        }

        int dropIdx = 0;
        // note: first child is not layer!
        for (int i = 0; i < neuralNetworkWidget.getChildren().size(); i++) {
            double layerWidgetPosition = neuralNetworkWidget.getChildren().get(i).getLocation().getY();
            if (point.getY() < layerWidgetPosition) {
                //dropIdx = i - 1; // 
                dropIdx = i;
                break;
            } else {
                //  dropIdx = neuralNetworkWidget.getChildren().size()-1;
                dropIdx = neuralNetworkWidget.getChildren().size();
            }

        }
        if (droppedClass.equals(Connection.class)) {
            NeuralNetworkEditor editor = scene.getNeuralNetworkEditor();
            if (dropIdx == 0) {
                JOptionPane.showMessageDialog(null, "Full connectivity cannot be drawn here!");
            } else if (dropIdx == scene.getNeuralNetwork().getLayersCount()) {
                JOptionPane.showMessageDialog(null, "Full connectivity cannot be drawn here!");
            } else {
                editor.createFullConnection(dropIdx);
                /* Layer fromLayer = scene.getNeuralNetwork().getLayerAt(dropIdx - 1);
                 Layer toLayer = scene.getNeuralNetwork().getLayerAt(dropIdx);
                 ConnectionFactory.fullConnect(fromLayer, toLayer);*/
                scene.refresh();
            }

        }
        if (droppedClass.equals(ConnectionFactory.class)) {
            NeuralNetworkEditor editor = scene.getNeuralNetworkEditor();
            if (dropIdx == 0) {
                JOptionPane.showMessageDialog(null, "Direct connectivity cannot be drawn here!");
            } else if (dropIdx == scene.getNeuralNetwork().getLayersCount()) {
                JOptionPane.showMessageDialog(null, "Direct connectivity cannot be drawn here!");
            } else {

                editor.createDirectConnection(dropIdx);
                /*Layer fromLayer = scene.getNeuralNetwork().getLayerAt(dropIdx - 1);
                 Layer toLayer = scene.getNeuralNetwork().getLayerAt(dropIdx);
                
                 // TODO: use forward connect from ConnectionFactory
                 // move this algorithm to forward/direct Connect
                 // existing direct connect throws exception in case of different neuron number
                 // its missing if check below
                
                 int number = 0;
                 if (fromLayer.getNeuronsCount() > toLayer.getNeuronsCount()) {
                 number = toLayer.getNeuronsCount();
                 } else {
                 number = fromLayer.getNeuronsCount();
                 }
                                
                 for (int i = 0;i < number; i++) {
                 Neuron fromNeuron = fromLayer.getNeurons()[i];
                 Neuron toNeuron = toLayer.getNeurons()[i];
                 ConnectionFactory.createConnection(fromNeuron, toNeuron);
                 }*/
                scene.refresh();
            }
        }
        if (droppedClass.equals(Layer.class) || droppedClass.getSuperclass().equals(Layer.class)) {
            NeuralNetworkEditor editor = scene.getNeuralNetworkEditor();
            try {
                //IMPORTANT: it only adds WeightSum layer, also it only adds layer at the end                             
                boolean hasInputLayer = false;
                for (int i = 0; i < scene.getNeuralNetwork().getLayersCount(); i++) {
                    if (scene.getNeuralNetwork().getLayerAt(i) instanceof InputLayer) {
                        hasInputLayer = true;
                        break;
                    }
                }
                if (droppedClass.equals(InputLayer.class) && !hasInputLayer == false) {
                    JOptionPane.showMessageDialog(null, "Network already has input layer!");
                }

                if (droppedClass.equals(InputLayer.class) && hasInputLayer == false) {
                    AddInputLayerDialog dialog = new AddInputLayerDialog(null, true, scene.getNeuralNetwork(), (NeuralNetworkScene) widget.getScene(), dropIdx);
                    dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                    dialog.setVisible(true);
                    scene.refresh();
                } else if (droppedClass.equals(CompetitiveLayer.class)) {
                    AddCompetitiveLayerDialog dialog = new AddCompetitiveLayerDialog(null, true, scene.getNeuralNetwork(), (NeuralNetworkScene) widget.getScene(), dropIdx);
                    dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
                    dialog.setVisible(true);
                    scene.refresh();
                } else {
                    editor.addEmptyLayer(dropIdx, (Layer) droppedClass.newInstance());
                    scene.refresh();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (droppedClass.getSuperclass().equals(LearningRule.class)
                || droppedClass.getSuperclass().equals(SupervisedLearning.class)
                || droppedClass.getSuperclass().equals(PerceptronLearning.class)
                || droppedClass.getSuperclass().equals(BackPropagation.class)
                || droppedClass.getSuperclass().equals(MomentumBackpropagation.class)
                || droppedClass.getSuperclass().equals(UnsupervisedLearning.class)
                || droppedClass.getSuperclass().equals(UnsupervisedHebbianLearning.class)
                || droppedClass.getSuperclass().equals(SigmoidDeltaRule.class)
                || droppedClass.getSuperclass().equals(LMS.class)) {
            //HopfieldLearning, IterativeLearning, KohonenLearning mogu da se nabace na neuralNetwork                                   
            try {
                //neuralNetworkWidget.getNeuralNetwork().setLearningRule((LearningRule) droppedClass.newInstance());
                scene.getNeuralNetworkEditor().setLearningRule((LearningRule) droppedClass.newInstance());
                scene.refresh();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (droppedClass.equals(AddCustomLayerDialog.class)) {

            // showDialogClass 
            AddCustomLayerDialog dialog = new AddCustomLayerDialog(null, true, scene, dropIdx);
            dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            dialog.setVisible(true);
            scene.refresh();
        }

        //WindowManager.getDefault().findTopComponent(GraphViewTopComponent.)
    }

    public boolean canAccept(Class droppedClass) {
        Class superclass = droppedClass.getSuperclass();

        if (superclass != null) {

            return droppedClass.equals(Layer.class) || superclass.equals(Layer.class)
                    || droppedClass.equals(Connection.class) || superclass.equals(Connection.class)
                    || droppedClass.equals(ConnectionFactory.class) || superclass.equals(ConnectionFactory.class)
                    || droppedClass.equals(LearningRule.class)
                    || superclass.equals(LearningRule.class)
                    || superclass.equals(SupervisedLearning.class)
                    || superclass.equals(PerceptronLearning.class)
                    || superclass.equals(BackPropagation.class)
                    || superclass.equals(MomentumBackpropagation.class)
                    || superclass.equals(UnsupervisedLearning.class)
                    || superclass.equals(UnsupervisedHebbianLearning.class)
                    || superclass.equals(SigmoidDeltaRule.class)
                    || superclass.equals(LMS.class)
                    || droppedClass.equals(AddCustomLayerDialog.class); // FIX: why this!? this shoud be removed
        } else {
            return droppedClass.equals(Layer.class)
                    || droppedClass.equals(Connection.class)
                    || droppedClass.equals(ConnectionFactory.class);    // izbaciti i ConnectioFactory? to kad radis punu povezanost i sl.
        }
    }
}
