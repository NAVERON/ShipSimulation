/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.navigator;

public class VesselCreateFactory {  //备用，产生船舶对象
    
    public static Vessel createVessel(){
        return new Vessel('a');
    }
    
    public static Vessel createShip(){
        return new Vessel('b');
    }
    
    public static Vessel createVessel(String idNumber, String name, int navLength, int beam){
        //有参数
        Vessel vesselNew = new Vessel(idNumber, name, navLength, beam, 'a');
        return vesselNew;
    }
    
    public static Vessel createShip(String idNumber, String name, int navLength, int beam,
            String imoNumber, String callNumber, String destination, String expTime){
        Vessel shipNew = new Vessel(idNumber, name, navLength, beam, imoNumber, callNumber, expTime);
        return shipNew;
    }
}
