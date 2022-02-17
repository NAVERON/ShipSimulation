/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.unity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.navigator.DynInfo;

/**
 *@function
 *     实现把动态信息写入本地磁盘文件保存
 * @author oo
 */
public class FileDataManager {  //存储对象流  取消，改编为存储字符串，之后按照规则读取
    public static int dynInfoIndex = 0;  //分文件存储
    //存储航行器动态信息
    public static void StoreDynInfo(List<DynInfo> objs, String filename){  //以id为文件名，作为唯一的识别号
        //fileName 保存文件名称的信息，检索，并添加
        //将航行器动态信息存储在磁盘文件中
        //File file = new File("dynInfo"+dynInfoIndex+".data");
        dynInfoIndex++;
        
        File file = new File("store\\"+filename+".data");
        if (!file.exists()) {  //如果文件不存在，则创建这个文件
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
//            if (file.length() != 0) {
//                //文件标记
//            }
            //下面可以写文件了
            for(DynInfo info : objs){
                out.writeObject(info);
                out.flush();
            }
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static List<DynInfo> ReadDynInfo(String filename){
        //从文件读取动态信息对象
        List<DynInfo> objs;
        objs = new ArrayList<DynInfo>();
        
        File file = new File("store\\"+filename+".data");
        if (!file.exists()) {
            return null;
        }
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            
            System.out.println("unity.FileDataManager.ReadDynInfo()");
            DynInfo dyninfo = null;
            while( (dyninfo = (DynInfo)in.readObject()) != null ){
                System.out.println(dyninfo.longitude+","+dyninfo.latitude+"==");
                objs.add(dyninfo);
            }
            in.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return objs;
    }
    
    public static void saveDynInfos(List<DynInfo> objs, String filename){
        
        File file = new File("store\\"+filename+".csv");
        if (!file.exists()) {  //如果文件不存在，则创建这个文件
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        FileWriter save = null;
        try {
             save = new FileWriter(file, true);
             for(DynInfo dyninfo : objs){
                 save.write(dyninfo.toString());
                 save.write("\n");
                 save.flush();
             }
             
             save.close();
        } catch (IOException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static List<DynInfo> readDyninfos(String filename){
        List<DynInfo> objs = new ArrayList<>();
        
        File file = new File("store\\"+filename+".csv");
        if (!file.exists()) {
            return null;
        }
        FileReader read = null;
        BufferedReader in = null;
        try {
            read = new FileReader(file);
            in = new BufferedReader(read);
            
            String str = null;
            String[] elements = null; //分割后的字符串
            DynInfo info = null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            while ( (str = in.readLine()) != null ) {
                elements = str.split(",");
                info = new DynInfo(Float.parseFloat(elements[0]), Float.parseFloat(elements[1]), Float.parseFloat(elements[2]), Float.parseFloat(elements[3]),
                        Float.parseFloat(elements[4]), '0', sdf.parse(elements[6]), Float.parseFloat(elements[7]));
                objs.add(info);
            }
            
            in.close();
            read.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(FileDataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return objs;
    }
    
    public static boolean deleteDynInfoFile(String filename){
        boolean isOk = false;
        
        File file = new File("store\\"+filename);
        if ( file.exists() ) {
            file.delete();
            isOk = true;
        }
        else{
            isOk = true;
            System.out.println("this file is not exist!!");
        }
        
        return isOk;
    }
    
}
