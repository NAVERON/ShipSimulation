/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.navigator;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @function
 *   航行器的动态信息
 * @author ERON
 */

public class DynInfo implements Serializable{  //navigator dynamic information
    //实现序列化接口的作用是能够将动态信息存储在本地文件中
    
    /**
     *  @parameter
     *     head 航行器船首方向
     *     course 航行器的真正航行方向，在没有风流干扰的情况下与船首向相同
     *     speed 航行器速度
     *     latitude 纬度
     *     longitude 经度
     *     state 航行器状态
     *     updateTime 航行器最近一次更新航行状态信息的时间
     *     rudderAngle
     * 
     * @warning
     *     下面使用public修饰为了方便访问动态属性的参数
     */
    
    public float head;
    public float course;
    public float speed;
    public float longitude;
    public float latitude;
    public char state;  //---------与type不同，type采用字母区分，这里采用10个数字区分不同的状态
    public String updateTime;  //以字符串存储时间信息
    
    public float rudderAngle;  //后来加的，表示舵角
    
    /*****************************构造函数**********************************************/
    
    public DynInfo(float head, float course, float speed,
            float longitude, float latitude,
            char state, Date updateTime, float  rudderAngle) {
        this.head = head;
        this.course = course;
        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
        this.state = state;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.updateTime = sdf.format(updateTime);
        
        this.rudderAngle = rudderAngle;  //默认舵角为0
    }
    
    @Override
    public String toString() {
        //if(head > 180){
        //    head -= 360;
        //}
        return head + "," + course + "," + speed + "," + longitude + "," + latitude + "," + state + "," + updateTime + "," + rudderAngle;
    }
    
    
}