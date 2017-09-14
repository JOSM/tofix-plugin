package org.openstreetmap.josm.plugins.tofix.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.plugins.tofix.TofixDialog;

/**
 *
 * @author ruben
 */
public class Download {

    public static void download(Bounds bounds, final Long osm_obj_id) {
        DownloadOsmTask task = new DownloadOsmTask();
        ProgressMonitor monitor = null;
        final Future<?> future = task.download(true, bounds, monitor);
        Runnable runAfterTask;

        if (osm_obj_id != null) {
            runAfterTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (osm_obj_id != 0) {
                            future.get();
                            //create object
                            if (future.isDone()) {

                                DataSet dataset = MainApplication.getLayerManager().getEditLayer().data;
                                Node node = new Node(osm_obj_id);
                                Relation relation = new Relation(osm_obj_id);
                                Way way = new Way(osm_obj_id);

                                //create list of objects
                                List<OsmPrimitive> selection = new ArrayList<>();

                                if (dataset.allPrimitives().contains(node)) {
                                    selection.add(node);
                                    dataset.setSelected(selection);

                                } else if (dataset.allPrimitives().contains(way)) {
                                    selection.add(way);
                                    dataset.setSelected(selection);

                                } else if (dataset.allPrimitives().contains(relation)) {
                                    selection.add(relation);
                                    dataset.setSelected(selection);
                                }
                                if (!selection.isEmpty()) {
                                    AutoScaleAction.autoScale("selection");
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(TofixDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            MainApplication.worker.submit(runAfterTask);
        }

    }
}
