/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation;

import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.navigator.Vessel;
import org.simulation.navigator.VesselCreateFactory;
import org.simulation.unity.FileDataManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;



/**
 * FXML Controller class
 * @author ERON
 */

public class MajorUIController implements Initializable {
    //公共区域
    @FXML private Hyperlink common;
    //@FXML private Label idNumber_label, name_label, length_label, beam_label, type_label;
    @FXML
    private TextField idNumber_text, name_text, length_text, beam_text;
    @FXML private ChoiceBox<String> type_choice;  //选择对象类型
    //船舶信息
    @FXML private Hyperlink ship;
    //@FXML private Label imoNumber_label, callNumber_label, destination_label, expTime_label;
    @FXML private TextField imoNumber_text, callNumber_text, destination_text;
    @FXML private DatePicker expTimePick;
    //动态信息
    //@FXML
    //private Label head_label,course_label, speed_label, latitude_label, longitude_label, state_label, updateTime_label;
    @FXML
    private TextField head_text, course_text, speed_text, latitude_text, longitude_text, state_text;
    @FXML
    //private DateTimePicker updateTimePick;
    private DatePicker updateTimePicker;
    @FXML private ListView<String> storedList;  //存储相关   
    @FXML private  Button save_track;
    @FXML private ListView<String> navigatorsList;
    
    @FXML private Pane showPane;  //显示动画展示的区域
    @FXML private StackPane analysePane;  //上边显示分析的区域
    //上边分析区域的下层结构
    @FXML private HBox analyseHBox;
    @FXML private Button create_button, cancle_button, clearAll_button, clearOthers;
    
    //自定义变量区域
    /*************************************************************************/
    //获取点击对象的引用
    //public static Vessel currentNavigator = null;
    public Vessel currentNavigator = null;
    private double clickedX, clickedY;  //鼠标点击的坐标
    
    //开始判断输入的数据，并填充缺省值----------静态属性
    private String idNumber, name;
    private int navLength, beam;
    private String choiseType;  //获取选择的类型，根据类型设置type的值
    private char type;
    private String imoNumber, callNumber, destination, expTime;  //船舶的属性
    //动态属性
    private float head, course, speed, latitude, longitude;
    private char state;
    //private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //时间的格式
    //生成动态信息时格式已经转换过了
    private Date updateTime;
    /*****************************************************************************/
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {  //这里可以初始化界面的初始参数
        type_choice.getItems().add("Vessel");
        type_choice.getItems().add("Ship");
        type_choice.getSelectionModel().selectFirst();
        
        chartShow.getData().addAll(courseSeries, speedSeries, rudderSeries);
        showPane.setId("showPane");
        
        //showPane.getChildren().add(new Circle(300, 50, 10, Color.BLUE));
    }
    
    //显示临时创建的缓存区，创建动作激活后，清空缓存
    private Vessel tempVessel = null;
    //update Creating option
    public void mousePosition(MouseEvent event){
        //System.out.println("x-->"+event.getX()+"    y-->"+event.getY());
    }
    @FXML
    public void setCreatePos(MouseEvent event){
        //得到鼠标点击的位置
        if (event.getButton() != MouseButton.PRIMARY) {
            System.out.println( "x-->"+event.getX()+"    y-->"+event.getY() );
            AutoNavVehicle.pause = !AutoNavVehicle.pause;
            return;
        }
        this.clickedX = event.getX();
        this.clickedY = event.getY();
        this.latitude_text.setText(Double.toString(500 - this.clickedY));
        this.longitude_text.setText(Double.toString(this.clickedX));
        //设置
        this.latitude = (float) this.clickedY;
        this.longitude = (float) this.clickedX;
        //-------提取方向和长，宽-----------------------------------------------//
        if ( (choiseType = type_choice.getValue()).equalsIgnoreCase("Vessel")) {
            this.type = 'a';
        } else if( (choiseType = type_choice.getValue()).equalsIgnoreCase("Ship") ){
            this.type = 'b';
        }
        if (tempVessel == null) {
            if (type == 'a') {
                tempVessel = VesselCreateFactory.createVessel();
            }
            else if(type == 'b'){
                tempVessel = VesselCreateFactory.createShip();
            }
            showPane.getChildren().add(tempVessel);  //先创建没有属性的一个空对象，之后进行设置
        }
        //更新方向-------------------->根据输入框获取数据可以统一为更新输入信息--validate
        if ( head_text.getText().trim().length() == 0 ) {
            this.head = 0.0F;
        }else{
            this.head = Float.parseFloat(head_text.getText().trim());
        }
        if ( length_text.getText().trim().length()==0 ) {  //如果长度为0，表示是空的
            navLength = 10;
        }else{  //如果不为空，获取内容并转换成int类型的数值
            navLength = Integer.parseInt(length_text.getText().trim());
        }
        if ( beam_text.getText().trim().length() == 0 ) {
            beam = 4;
        }else{
            beam = Integer.parseInt(beam_text.getText().trim());
        }
        //设置创建对象的属性，主要是显示的属性，船首向和位置--->不用设置对象本身的属性，因为只是显示
        tempVessel.setType(type);  //设置显示颜色的变化===================
        tempVessel.setLayoutX(longitude - navLength/2);
        tempVessel.setLayoutY(latitude - beam/2);
        tempVessel.setRotate(head - 90); //朝向问题，显示和后台的数据应当不一致，前面显示的坐标系不同。。。
        tempVessel.setPrefWidth(this.navLength);
        tempVessel.setPrefHeight(this.beam);
    }
    @FXML
    public void cancleCreate(){
        //点击之后显示，cancle之后消失，create之后变化样式，正式创建
        if (tempVessel != null) {
            showPane.getChildren().remove(tempVessel);
            tempVessel = null;
        }
    }
    @FXML
    public void clearAllNavigator(){
        //清除所有对象
        //showPane.getChildren().clear();
        for(Iterator<Vessel> items = AutoNavVehicle.navigators.iterator();items.hasNext();){
            Vessel temp = items.next();
            showPane.getChildren().remove(temp);
            temp.comRunning = false;
        }
        showPane.getChildren().remove(circle);
        
        tempVessel = null;
        AutoNavVehicle.navigators.clear();
        navigatorsList.getItems().clear();  //清除列表的记录
        this.currentNavigator = null;
        if (chartupdate != null && chartupdate.isAlive()) {
            chartupdate.setCurNavigator(null);
        }
        chartupdate = null;  //结束这个线程
        System.gc();  //回收垃圾
    }
    
    public void clearOthers(){
        for(Iterator<Vessel> items = AutoNavVehicle.navigators.iterator();items.hasNext();){
            Vessel next = items.next();
            if ( !next.getIdNumber().equals("12") ) {
                showPane.getChildren().remove(next);
                next.comRunning = false;
                items.remove();
            }
        }
        tempVessel = null;
        //没有清除列表的信息
    }
    
    /*****************************创建对象的动作*************************************/
    @FXML
    public void createNavigator(){  //创建的时候直接移动缓存对象即可
        //多级判断，首先判断必要项，如果为空----------------------------------------------
        idNumber = idNumber_text.getText().trim();
        if (idNumber.isEmpty() || tempVessel == null) {  //判断id号和临时对象是否为空
            System.out.println("creating vessel idnumber is null  OR tempVessel is NULL, please click in pane, and input id number ......");
            return;
        }
        //id应当放在前面判断，唯一识别号保持唯一性
        for (Iterator<Vessel> items = AutoNavVehicle.navigators.iterator(); items.hasNext();) {
            Vessel next = items.next();
            if (idNumber.equalsIgnoreCase(next.getIdNumber())) {  //检测到则跳出循环
                return;  //break 换成了return
            }
        }
        /////////////////////////////////-----获取第一个动态信息-----///////////////////////////////
        //动态信息判断
        if ( head_text.getText().trim().length() == 0 ) {
            this.head = 0.0F;
        }else{
            this.head = Float.parseFloat(head_text.getText().trim());
        }
        if ( course_text.getText().trim().length() == 0) {
            this.course = 0.0F;
        }else{
            this.course = Float.parseFloat(course_text.getText());
        }
        if (speed_text.getText().trim().length() == 0) {
            this.speed = 2.0F;
        }else{
            this.speed = Float.parseFloat(speed_text.getText().trim());
        }
        if(latitude_text.getText().trim().length() != 0){
            this.latitude = 500 - Float.parseFloat(latitude_text.getText().trim());
        }
        if(longitude_text.getText().trim().length() != 0){
            this.longitude = Float.parseFloat(longitude_text.getText().trim());
        }
        //--------------------经纬度不用考虑，在点击过程中已经获得----------------------//
        //将当前缓存对象显示出来
        if ( state_text.getText().trim().length() == 0 ) {
            this.state = '0';  //状态的意义稍后再讨论
        }else{
            this.state = state_text.getText().trim().toCharArray()[0];  //拿到第一个字符
        }
        updateTime = new Date();  //获取当前时间
        /*********************************动态信息结束**********************************/
        
        /*******************************0000静态信息0000***********************************/
        if ( (name = name_text.getText().trim()).length()==0 ) {
            name = "default";
        }else{
            name = name_text.getText().trim();
        }
        if ( length_text.getText().trim().length()==0 ) {  //如果长度为0，表示是空的
            navLength = 10;
        }else{  //如果不为空，获取内容并转换成int类型的数值
            navLength = Integer.parseInt(length_text.getText().trim());
        }
        if ( beam_text.getText().trim().length() == 0 ) {
            beam = 4;
        }else{
            beam = Integer.parseInt(beam_text.getText().trim());
        }
        //判断选择的创建类型
        if ( (choiseType = type_choice.getValue()).equalsIgnoreCase("Vessel") ) {
            type = 'a';
            //这里就可以开始创建vessel对象了
            if ( (destination = destination_text.getText().trim()).length() == 0 ) {
                destination = "300,50";
            }
            tempVessel.setStaticAttribute(idNumber, name, navLength, beam, type, destination);
        }else{  //如果是船舶类型则进一步判断
            type = 'b';
            if ( (imoNumber = imoNumber_text.getText().trim()).length() == 0 ) {
                imoNumber = "defaultImoNumber";
            }
            if ( (callNumber = callNumber_text.getText().trim()).length() == 0 ) {
                callNumber = "defaultCallNumber";
            }
            if ( (destination = destination_text.getText().trim()).length() == 0 ) {
                destination = "300,50";
            }
            if ( expTimePick.getValue() == null ) {
                expTime = "null";
            }
            else{
                expTime = expTimePick.getValue().toString();
            }
            //这里就可以创建ship对象了
            tempVessel.setStaticAttribute(idNumber, name, navLength, beam, type,
                    imoNumber, callNumber, destination, expTime);
        }
        /*------------以前放在前面的，因为需要使用静态的长宽信息，所以需要放在这里---------------*/
        //将第一个动态信息加入
        float rudderAngle = 0.0F;
        tempVessel.setDynAttribute(head, course, speed, longitude, latitude, state, expTime, rudderAngle);
        //tempVessel.addDynInfo(new DynInfo(head, course, speed, longitude, latitude, state, updateTime, rudderAngle));
        tempVessel.setDestination(new Point2D(longitude + Math.sin(Math.toRadians(head))*speed*200,
                latitude - Math.cos(Math.toRadians(head))*speed*200));
        
        //将临时创建的对象添加到全局对象中，提供全局控制
        AutoNavVehicle.navigators.add(tempVessel);
        if (tempVessel.getIdNumber().equals("12")) {
            currentNavigator = tempVessel;  //当前引用指针指向特定id的船舶对象
            //chartThread(true);
            chartupdate = new chartUpdate(this.currentNavigator);
            chartupdate.start();
            
            circle = new Circle(tempVessel.getDestination().getX(), tempVessel.getDestination().getY(), 5, Color.BLUE);
            showPane.getChildren().add( circle );
        }
        //showPane.getChildren().add( new Circle(tempVessel.getDestination().getX(), tempVessel.getDestination().getY(), 5, Color.BLUE) );
        //还原为空值
        System.out.println(tempVessel.toString());
        tempVessel = null;
        navigatorsList.getItems().add("idNumber : "+this.idNumber+"<->Name : "+this.name);
        //最后需要加入清空输入信息的操作     ----  2017.6.28 cancle
        
    }
    
    @FXML
    public void saveFunction(){  //存储并实现更新存储显示列表
        for(Vessel vessel : AutoNavVehicle.navigators){
            FileDataManager.saveDynInfos(vessel.dynInfos, vessel.getIdNumber());
            storedList.getItems().add(vessel.getIdNumber() + " : tracks -> " + vessel.dynInfos.size());
        }
    }
    
    Circle circle;  //目标点
    /********折线图的动态显示*****************************************/
    
    @FXML private NumberAxis xAxis, yAxis;
    @FXML private LineChart chartShow;
    
    public XYChart.Series<Number, Number> courseSeries = new XYChart.Series<>();
    public XYChart.Series<Number, Number> speedSeries = new XYChart.Series<>();
    public XYChart.Series<Number, Number> rudderSeries = new XYChart.Series<>();
    
    //public ExecutorService executor;
//    public ConcurrentLinkedQueue<Number> dataQ1 = new ConcurrentLinkedQueue<>();
//    public ConcurrentLinkedQueue<Number> dataQ2 = new ConcurrentLinkedQueue<>();
//    public ConcurrentLinkedQueue<Number> dataQ3 = new ConcurrentLinkedQueue<>();
    
    public chartUpdate chartupdate = null;
    public int time = 0;
    //public Group tracks = new Group();
    public LinkedList<Circle> historys = new LinkedList<>();
    
    private class chartUpdate extends Thread{
        
        Vessel currentNavigator = null;
        
        public chartUpdate(Vessel currentNavigator) {
            this.setDaemon(true);
            this.currentNavigator = currentNavigator;
        }
        public void setCurNavigator(Vessel currentNavigator){
            this.currentNavigator = currentNavigator;
        }
        
        @Override
        public void run() {
            //super.run();
            while(this.currentNavigator != null){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MajorUIController.class.getName()).log(Level.SEVERE, null, ex);
                }
                if ( AutoNavVehicle.pause ) {
                    continue;
                }
                if (courseSeries == null || speedSeries == null || rudderSeries == null || currentNavigator == null) {  //最后结束时候本线程还在运行的错误
                    continue;
                }
                courseSeries.getData().add( new XYChart.Data<>( time, currentNavigator.getHead()) );
                speedSeries.getData().add( new XYChart.Data<>( time, currentNavigator.getSpeed()) );
                rudderSeries.getData().add( new XYChart.Data<>( time, currentNavigator.getRudderAngle()) );
                if ( courseSeries.getData().size() > 150 || speedSeries.getData().size() > 150 || rudderSeries.getData().size() > 150 ) {
                    courseSeries.getData().remove(0);
                    speedSeries.getData().remove(0);
                    rudderSeries.getData().remove(0);
                }
                time++;
            }
            //在停止更新后清空数据，等待下一次的重建
            time = 0;
            courseSeries.getData().clear();
            speedSeries.getData().clear();
            rudderSeries.getData().clear();
            
        }
    }
    
}
