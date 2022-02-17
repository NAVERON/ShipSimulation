/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.navigator;

/**
 * @function
 *     实现不同操纵特性的功能
 * @author ERON
 */
public interface Manipulation {
    
    public void goAhead();  //根据舵角以及角速度改变方向和位置
    public void setRudder(float rudderAngle);  //设置舵角  左舵为负值，右舵为正值  范围大小为   -35 - 35
    public void turnTo(int dirDiff);//---根据方向进行操舵---dirDiff 表示与当前推进方向的差值，正值 0- 180 右侧推进， -0 - -180 左侧推进
    default void accelerate(float toSpeed){
        System.out.println("navigator.Manipulation.accelerate()");
    }
    default void decelerate(float toSpeed){
        System.out.println("navigator.Manipulation.decelerate()");
    }
    public void speedTo(float toSpeed);
    public void stop();
}
