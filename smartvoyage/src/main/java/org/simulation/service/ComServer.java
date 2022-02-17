/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.AutoNavVehicle;
import org.simulation.environment.MessageType;
import org.simulation.navigator.Vessel;

import java.util.Iterator;

public class ComServer extends Thread{
    
    public BlockingQueue<MessageType> messageQueue = new ArrayBlockingQueue<>(100);
    private static ComServer instance = new ComServer();  //实例化自己
    
    private ComServer(){
        this.setDaemon(true);  //设置为守护线程
    }
    public static ComServer getInstance(){
        return instance;
    }
    
    public void addQueue(MessageType message){
        try {
            messageQueue.put(message);
        } catch (InterruptedException ex) {
            Logger.getLogger(ComServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public MessageType getQueue(){
        MessageType message = null;
        try {
            message = messageQueue.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(ComServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return message;
    }
    
    @Override
    public void run() {
        //super.run();
        while (AutoNavVehicle.com) {
            
            if(messageQueue.isEmpty()){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ComServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;
            }
            MessageType message = null;
            try {
                message = messageQueue.take();
            } catch (InterruptedException ex) {
                Logger.getLogger(ComServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            for(Iterator<Vessel> items = AutoNavVehicle.navigators.iterator();items.hasNext();){
                Vessel next = items.next();
                if (next.getIdNumber().equals(message.getTo())) {
                    next.dealCom(message);
                    break;
                }
            }
        }
    }
    
}



