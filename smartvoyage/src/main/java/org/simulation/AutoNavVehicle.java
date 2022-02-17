
package org.simulation;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.simulation.navigator.Vessel;
import org.simulation.service.ComServer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AutoNavVehicle extends Application {  
	
	//规则和环境没有添加，以后才有机会制作，现阶段先做一个大概
	
	private static final Logger log = Logger.getLogger(AutoNavVehicle.class.getName());
    
    public static List<Vessel> navigators = new LinkedList<>();  //存储航行器对象Vessel
    public static float level = 1;  //缩放比例
    public static boolean pause = false;  //暂停的实现，不是很高效
    public Loop loop = null;
    public static boolean com = true;
    
    private Stage stage = null;  //将里面的stage导出来
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/fxml/MajorUI.fxml"));
        log.info("uri ---> " + loader.getLocation().toString());
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        stage.setTitle("群无人艇智能避碰仿真平台");
        stage.setScene(scene);
        stage.show();
        
        loop = new Loop(navigators, root);  //循环计算并前进
        loop.start();
        ComServer.getInstance().start();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    public Stage getStage(){  //获取舞台的引用，看到过以前的代码有切换界面的
        return stage;
    }
    
    public static class Launcher {
    	public static void main(String[] args) {
			AutoNavVehicle.main(args);
		}
    }
    
}





