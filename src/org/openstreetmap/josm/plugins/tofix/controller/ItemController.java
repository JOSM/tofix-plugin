package org.openstreetmap.josm.plugins.tofix.controller;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.tofix.bean.AccessToTask;
import org.openstreetmap.josm.plugins.tofix.bean.ResponseBean;
import org.openstreetmap.josm.plugins.tofix.bean.TaskCompleteBean;
import org.openstreetmap.josm.plugins.tofix.bean.items.Item;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemKeeprightBean;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemKrakatoaBean;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemNycbuildingsBean;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemSmallcomponents;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemStrava;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemTigerdeltaBean;
import org.openstreetmap.josm.plugins.tofix.bean.items.ItemUnconnectedBean;

import org.openstreetmap.josm.plugins.tofix.util.Request;
import org.openstreetmap.josm.plugins.tofix.util.Util;

/**
 *
 * @author ruben
 */
public class ItemController {

    Gson gson = new Gson();
    Item item = new Item();
    ResponseBean responseBean = new ResponseBean();

    AccessToTask accessToTask;

    public AccessToTask getAccessToTask() {
        return accessToTask;
    }

    public void setAccessToTask(AccessToTask accessToTask) {
        this.accessToTask = accessToTask;
    }

    public Item getItem() {

        try {
            responseBean = Request.sendPOST(accessToTask.getTask_url());
            item.setStatus(responseBean.getStatus());

            switch (responseBean.getStatus()) {
                case 200:
                    if (accessToTask.getTask_source().equals("unconnected")) {

                        //gson.fromJson(responseBean.getValue(), ItemUnconnectedBean.class);
                        Util.print(responseBean.getValue());
                        JsonReader reader = Json.createReader(new StringReader(responseBean.getValue()));
                        JsonObject object = reader.readObject();

                        ItemUnconnectedBean iub = new ItemUnconnectedBean();
                        //Common format for unconnected
                        //[key="09c0907befaf30b4b7d2d0e2ed50ba51", value={"way_id":"329295263","node_id":"3361629405","st_astext":"POINT(139.066516 37.902725)"}]
                        iub.setKey(object.getString("key"));
                        JsonObject value = object.getJsonObject("value");
                        if (value.containsKey("way_id") && value.containsKey("node_id") && value.containsKey("st_astext")) {
                            iub.setNode_id(Long.parseLong(value.getString("node_id")));
                            iub.setWay_id(Long.parseLong(value.getString("way_id")));
                            iub.setSt_astext(value.getString("st_astext"));
                            item.setItemUnconnectedBean(iub);
                        } else if (value.containsKey("XR") && value.containsKey("Y") && value.containsKey("way_id") && value.containsKey("node_id")) {
                            //Format from Arun
                            //"X":"-72.206718","Y":"-89.695837","rank":"45","problem_id":"23","way_id":"224014700","node_id":"2328445972","highwaykey":"9","distance":"5","iso_a2":"AQ"
                            String st_astext = "POINT(" + value.getString("X") + " " + value.getString("Y") + ")";
                            iub.setNode_id(Long.parseLong(value.getString("node_id")));
                            iub.setWay_id(Long.parseLong(value.getString("way_id")));
                            iub.setSt_astext(st_astext);
                            item.setItemUnconnectedBean(iub);                          
                        }else{
                        //if the format does not math , responde a status 
                            item.setStatus(520);// response 520 Unknown Error                            
                        }                        
                        //TODO
                        //if the structure change , we need to customize this site, to easy resolve , but we need to standardize the source in each task.

                    }
                    if (accessToTask.getTask_source().equals("keepright")) {
                        item.setItemKeeprightBean(gson.fromJson(responseBean.getValue(), ItemKeeprightBean.class));
                    }
                    if (accessToTask.getTask_source().equals("tigerdelta")) {
                        item.setItemTigerdeltaBean(gson.fromJson(responseBean.getValue(), ItemTigerdeltaBean.class));
                    }
                    if (accessToTask.getTask_source().equals("nycbuildings")) {
                        item.setItemNycbuildingsBean(gson.fromJson(responseBean.getValue(), ItemNycbuildingsBean.class));
                    }
                    if (accessToTask.getTask_source().equals("krakatoa")) {
                        item.setItemKrakatoaBean(gson.fromJson(responseBean.getValue(), ItemKrakatoaBean.class));
                    }
                    if (accessToTask.getTask_source().equals("strava")) {
                        item.setItemStrava(gson.fromJson(responseBean.getValue(), ItemStrava.class));
                    }
                    if (accessToTask.getTask_source().equals("components")) {
                        item.setItemSmallcomponents(gson.fromJson(responseBean.getValue(), ItemSmallcomponents.class));
                    }
                    break;
                case 410:
                    item.setTaskCompleteBean(gson.fromJson(responseBean.getValue().replace("\\", "").replace("\"{", "{").replace("}\"", "}"), TaskCompleteBean.class));
                    break;
                case 503:
                    //Servidor en mantenimiento
                    break;
            }

        } catch (Exception ex) {
            Util.alert(ex);
            Logger.getLogger(ItemController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return item;
    }
}
