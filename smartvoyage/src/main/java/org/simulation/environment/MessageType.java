/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.environment;

import java.io.Serializable;

public class MessageType implements Serializable{
    
    private String fromId = null;
    private String toId = null;
    private String content = null;
    
    public MessageType(){
        //空消息，方便调试
    }
    public MessageType(String fromId, String toId, String content){
        this.fromId = fromId;
        this.toId = toId;
        this.content = content;
    }
    
    public String getTo(){
        return this.toId;
    }
    public String getFrom(){
        return this.fromId;
    }
    public String getContent(){
        return this.content;
    }
    
    @Override
    public String toString(){
        return this.fromId+","+this.toId+","+this.content;
    }
    
}
