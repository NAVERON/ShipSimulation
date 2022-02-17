/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.unity;

/**
 * @author NAVERON
 */

public class LocalVessel implements Comparable<LocalVessel>{  //采用map映射匹配，这个类作为值   ------------具有航行器的主要属性
    public String id;
    
    public float longitude;
    public float latitude;
    public float head;
    public float speed;
    
    public LocalVessel(){
    }
    
    public LocalVessel(String id, float longitude, float latitude, float head, float speed){
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.head = head;
        this.speed = speed;
    }
    
    public float getSpeedX(){  //向右 > 0，向左 < 0
        return (float) (Math.sin(Math.toRadians(head))*speed);
    }
    
    public float ratio;  //相对于当前航行器的角度
    public int belong = -1;  //分组的标志  -1表示没有计算过
    public boolean danger = false;  //危险标志，如果为true，表示当前航行器是危险的
    
    @Override
    public int compareTo(LocalVessel other) {  //升序
        if (this.ratio > other.ratio) {
            return 1;
        }else if(this.ratio < other.ratio){
            return -1;
        }else{  //角度相同的情况下，距离远的在前面
            if( (this.longitude > other.longitude && this.latitude > other.latitude)
                    || (this.longitude > other.longitude && this.latitude < other.latitude)
                    || (this.longitude < other.longitude && this.latitude < other.latitude)
                    || (this.longitude < other.longitude && this.latitude > other.latitude) ){
                return 1;
            }else{
                return -1;
            }
        }
    }
    
    @Override
    public String toString() {
        return "LocalVessel{" + "id=" + id + ", longitude=" + longitude + ", latitude=" + latitude + ", head=" + head + ", speed=" + speed + ", ratio=" + ratio + '}';
    }
    
}
