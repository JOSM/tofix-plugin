package org.openstreetmap.josm.plugins.tofix;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.tofix.bean.ItemBean;
import org.openstreetmap.josm.plugins.tofix.bean.ListTaskBean;
import org.openstreetmap.josm.plugins.tofix.controller.ItemController;
import org.openstreetmap.josm.plugins.tofix.controller.ListTaskController;
import org.openstreetmap.josm.plugins.tofix.layer.TofixLayer;
import org.openstreetmap.josm.plugins.tofix.util.*;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author ruben
 */
public class TofixDialog extends ToggleDialog implements ActionListener {

    private final SideButton editButton;
    private final SideButton skipButton;
    private final SideButton fixedButton;

    String host = "http://54.147.184.23:8000";
    String url_task = url_task = host + "/task/unconnectedmajor";

    ListTaskBean listTaskBean = null;
    ListTaskController listTaskController = new ListTaskController("http://osmlab.github.io/to-fix/src/data/tasks.json");

    ItemController itemController = null;
    ItemBean itemBean = null;

    Bounds bounds = null;
    DownloadOsmTask downloadOsmTask = new DownloadOsmTask();

    //Tofix Layer
    MapView mv = Main.map.mapView;
    TofixLayer tofixLayer = new TofixLayer("Tofix-layer");

    JPanel valuePanel = new JPanel();
    JPanel jcontenpanel = new JPanel(new GridLayout(0, 2));

    public TofixDialog() {
        super(tr("To-fix"), "icontofix", tr("Open to-fix window."),
                Shortcut.registerShortcut("tool:to-fix", tr("Toggle: {0}", tr("To-fix")),
                        KeyEvent.VK_F, Shortcut.CTRL_SHIFT), 150);

        editButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Edit"));
                putValue(SMALL_ICON, ImageProvider.get("mapmode", "edit.png"));
                putValue(SHORT_DESCRIPTION, tr("Dowload data"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                edit();

            }
        });
        editButton.setEnabled(false);
        skipButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Skip"));
                putValue(SMALL_ICON, ImageProvider.get("mapmode", "skip.png"));
                putValue(SHORT_DESCRIPTION, tr("Skip Error"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                // String userName = XmlWriter.encode(comment.getUser().getName());
                //                    if (userName == null || userName.trim().length() == 0) {
                //                        userName = "&lt;Anonymous&gt;";
                //                    }
                //node.setCoor(mv.getLatLon(mv.lastMEvent.getX(), mv.lastMEvent.getY()));
                editButton.setEnabled(true);
                skip();
            }

            // repaint to make sure new data is displayed properly.
        });

        fixedButton = new SideButton(new AbstractAction() {
            {
                putValue(NAME, tr("Fixed"));
                putValue(SMALL_ICON, ImageProvider.get("mapmode", "fixed.png"));
                putValue(SHORT_DESCRIPTION, tr("Fixed Error"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showConfirmDialog(Main.parent, Status.isInternetReachable());
            }
        });

        valuePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jcontenpanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        ButtonGroup group = new ButtonGroup();

        if (Status.isInternetReachable()) { //checkout  internet connection
            listTaskBean = listTaskController.getListTasksBean();
            for (int i = 0; i < listTaskBean.getTasks().size(); i++) {
                JRadioButton jRBItem = new JRadioButton(listTaskBean.getTasks().get(i).getTitle());
                group.add(jRBItem);
                jRBItem.setActionCommand(listTaskBean.getTasks().get(i).getId());
                jcontenpanel.add(jRBItem);
                jRBItem.addActionListener(this);
                if (i == 0) {
                    jRBItem.setSelected(true);
                }
            }
            if (!Status.server()) {
                editButton.setEnabled(false);
                skipButton.setEnabled(false);
                fixedButton.setEnabled(false);
            }

        } else {
            editButton.setEnabled(false);
            skipButton.setEnabled(false);
            fixedButton.setEnabled(false);
        }

        this.setPreferredSize(new Dimension(0, 92));
        createLayout(jcontenpanel, false, Arrays.asList(new SideButton[]{
            editButton, skipButton, fixedButton
        }));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        url_task = host + "/task/" + e.getActionCommand();
    }

    public void skip() {
        itemController = new ItemController(url_task);
        itemBean = itemController.getItemBean();

        LatLon coor = new LatLon(itemBean.getValue().getY(), itemBean.getValue().getX());
        if (coor.isOutSideWorld()) {
            JOptionPane.showMessageDialog(Main.parent, tr("Can not find outside of the world."));
            return;
        }
        BoundingXYVisitor v = new BoundingXYVisitor();
        //double ex = 0.0001; = 2.34 m
        double ex = 0.0007;// 16.7 m
        bounds = new Bounds(itemBean.getValue().getY() - ex, itemBean.getValue().getX() - ex, itemBean.getValue().getY() + ex, itemBean.getValue().getX() + ex);
        v.visit(bounds);
        Main.map.mapView.zoomTo(v);

        if (!Main.map.mapView.hasLayer(tofixLayer)) {
            mv.addLayer(tofixLayer);
            tofixLayer.add_coordinate(coor);
        } else {
            tofixLayer.add_coordinate(coor);
        }

    }

    public void edit() {
        Download.Download(downloadOsmTask, bounds, itemBean);
    }
}
