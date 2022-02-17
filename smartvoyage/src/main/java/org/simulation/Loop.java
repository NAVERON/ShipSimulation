/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.navigator.Vessel;

import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * @author NAVERON
 */

public class Loop extends Thread{
    
    private List<Vessel> navigators = null;
    public Stage stage;
    public Pane show;
    public Group tracks = new Group();
    public Loop(List<Vessel> navigators, Parent root){
        //导入显示面板和所有对象的列表
        this.navigators = navigators;
        //show = (Pane) root.lookup("#showPane");
        //tracks.setId("tracks");
        //show.getChildren().add(tracks);
        this.setDaemon(true);  //守护线程
    }
    
    @Override
    public void run() {  //相当于动画控制器
        //总体控制前进是因为以前使用独立线程，显示会不同步，计算会有偏差，一起计算可以避免这个问题
        //super.run(); //To change body of generated methods, choose Tools | Templates.
        while(true){
            while (!AutoNavVehicle.pause) {
                for (Iterator<Vessel> items = navigators.iterator(); items.hasNext();) {
                    Vessel next = items.next();
                    next.goAhead();
                }
                try {
                    sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Loop.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Loop.class.getName()).log(Level.SEVERE, null, ex);
            }
            //show.getChildren().clear();
        }
    }
    
}
