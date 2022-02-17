/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.environment;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.navigator.Manipulation;
import org.simulation.navigator.Navigator;

/**
 * @function
 *     表达操纵语句，能够传递给操纵类进行执行操纵
 * @author oo
 */
public class Option extends Thread implements Manipulation{
    
    Navigator navigator = null;
    public Option(Navigator navigator){
        this.navigator = navigator;
    }
    
    @Override
    public State getState() {
        return super.getState(); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void run() {
        super.run(); //To change body of generated methods, choose Tools | Templates.
        
    }
    
    @Override
    public synchronized void start() {
        super.start(); //To change body of generated methods, choose Tools | Templates.
    }
    
    //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
    
    @Override
    public void goAhead() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setRudder(float rudderAngle) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void turnTo(int desDir) {
        
    }

    @Override
    public void accelerate(float toSpeed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void decelerate(float toSpeed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
    
    public void pinRudder(float dirCourse){  //只控制航向，不控制航迹
        //根据航向的偏差以一定比例设置舵角
        float div = dirCourse - navigator.getHead();
        if (div > 180) {
            div -= 360;  //变成负值
        }else if(div < -180){
            div += 360;
        }
        if (div == 0) {
            setRudder(0);
        }
        else if(div > 0){  //需要右舵
            if (div < 30) {
                setRudder(5);
            }else if (div <90) {
                setRudder(10);
            }else if(div  > 90){
                setRudder(25);
            }
        }
        else{  //需要左舵角
            if(div > -30){
                setRudder(-5);
            }else if(div > -90){
                setRudder(-10);
            }else if(div < -90){
                setRudder(-25);
            }
        }
    }
    
    public void pinSpeed(float dirSpeed){
        //取消，不用写这个方法，下面的加减速已经可以控制了
        int rest = (int) ((dirSpeed - navigator.getSpeed())/2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * rest);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Navigator.class.getName()).log(Level.SEVERE, null, ex);
                }
                navigator.setSpeed(dirSpeed);
            }
        }).start();
    }

    @Override
    public void speedTo(float toSpeed) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
