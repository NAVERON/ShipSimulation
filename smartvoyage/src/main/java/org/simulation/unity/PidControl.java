package org.simulation.unity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class PidControl extends JPanel {
    
    public static float kp = 2F, ki = 20F, kd = 10F;
    public static float desireValue = 80,//目标值，目标航向
            curValue = 0;  //当前值,本船航向
    public static float theta = 0;  //舵角>>>>>>>>>>>>>>>>>>>>>>>>
    
    public float cur_error = 0, //当前的误差
            last_error = 0, //上次误差
            pre_error = 0, //上上次误差
            sum_error = 0;  //误差之和，求取积分和
    public float last_u = 0, //这个是什么？？--->可以记录上次计算后的输出值
            cur_u = 0;  //当前偏差,对舵角的偏移值
    
    public ArrayList<Float> uArray = new ArrayList<>();  //计算输出值存储-->现在用来存储当前值与目标值的误差
    public ArrayList<Float> value = new ArrayList<>();  //被控制量的变化情况
    public ArrayList<Float> thetas = new ArrayList<>();  //舵角变化情况
    float K = 0.0785F, T = 3.12F;  //船舶操纵性能，根据操纵性能求出舵角theta后->角度变化
    
    public PidControl() {
        super();
    }
    
    public static void main(String[] args) {  //假定舵可以在采样一个周期达到任何舵角
        // TODO 界面
        JFrame frame = new JFrame();

        PidControl draw = new PidControl();
        draw.setBackground(Color.WHITE);
        frame.setContentPane(draw);

        //frame.pack();
        frame.setBounds(50, 50, 1200, 700);
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);

        //开始计算数据
        //draw.test();
        //draw.p();  //比例
        //draw.pi();
        //draw.pd();
        draw.pid();
        //draw.pidAdd();
        //draw.repaint();
        draw.repaint();

        //draw.toFile(draw.value, draw.thetas);
    }

    public void test() {  //测试方法
        uArray.clear();
        value.clear();
        thetas.clear();

        desireValue = 80;
        curValue = 0;
        //System.out.println(K*35*(-2.12+T*Math.exp((-1/T))));
        while (Math.abs(desireValue - curValue) > 1) {
            cur_error = desireValue - curValue;  //当前距离目标值的差
            cur_u = kp * (cur_error - last_error);  //pid计算后输出增量
            //System.out.println("当前舵角变化值->"+cur_u);
            //根据当前状态输出舵角
            theta += cur_u;  //当前舵角
            //System.out.println("当前舵角->"+theta);
            if (theta > 35 || theta < -35) {  //当增大到最大舵角后不再增大-->这里有一个问题，并不是舵角越大转角效果越好？怎么办？映射函数？
                theta -= cur_u;
            }
            //根据舵角计算下一次采集的航向值
            float div = (float) (K * theta * (1 - T + T * Math.pow(Math.E, -1 / T)));
            //System.out.println("角度增加/变化->"+div);
            curValue += div;  //根据计算出的角度变化求出当前角度值，即下一次的采集角度值
            //System.out.println("当前角度值->"+curValue);
            uArray.add(cur_u);
            value.add(curValue);
            thetas.add(theta);

            last_error = cur_error;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            repaint();
        }
    }

    public void p() {  //kp越大越快
        uArray.clear();
        value.clear();
        thetas.clear();

        desireValue = 80;
        curValue = 0;
        while (Math.abs(desireValue - curValue) > 1) {
            cur_error = desireValue - curValue;  //当前距离目标值的差
            cur_u = kp * (cur_error - last_error);
            //curValue +=cur_u;
            theta += cur_u;
            if (theta > 35 || theta < -35) {  //当增大到最大舵角后不再增大-->这里有一个问题，并不是舵角越大转角效果越好？怎么办？映射函数？
                theta -= cur_u;
            }
            //根据舵角计算下一次采集的航向值
            float div = (float) (K * theta * (1 - T + T * Math.pow(Math.E, -1 / T)));
            curValue += div;
            System.out.println("+-------------------------------"
                    + "\n|当前值：" + curValue
                    + "\n|本次舵角-->" + theta
                    + "\n|本次误差: " + cur_error
                    + "\n+------------------------------------"
            );
            uArray.add(cur_error);
            value.add(curValue);
            thetas.add(theta);

            last_error = cur_error;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            repaint();
        }
    }

    public void pi() {
        uArray.clear();
        value.clear();
        desireValue = 80;
        curValue = 0;
        //比例和积分
        while (Math.abs(curValue - desireValue) > 1) {
            cur_error = desireValue - curValue;
            cur_u = kp * (cur_error - last_error) + (kp / ki) * cur_error;
            pre_error = last_error;
            last_error = cur_error;
            //curValue +=cur_u;
            theta += cur_u;
            if (theta > 35 || theta < -35) {  //当增大到最大舵角后不再增大-->这里有一个问题，并不是舵角越大转角效果越好？怎么办？映射函数？
                theta -= cur_u;
            }
            //根据舵角计算下一次采集的航向值
            float div = (float) (K * theta * (1 - T + T * Math.pow(Math.E, -1 / T)));
            curValue += div;

            System.out.println("+-------------------------------"
                    + "\n|当前值：" + curValue
                    + "\n|本次增量-->" + cur_u
                    + "\n|距离目标的误差-->" + cur_error
                    + "\n+------------------------------------"
            );
            uArray.add(cur_error);  //当前的误差
            value.add(curValue);
            thetas.add(theta);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            repaint();
        }

    }

    public void pd() {
        uArray.clear();
        value.clear();
        desireValue = 80;
        curValue = 0;
        //微分+比例
        while (Math.abs(curValue - desireValue) > 1) {
            cur_error = desireValue - curValue;
            cur_u = kp * (cur_error - last_error) + (kp * kd) * (cur_error - 2 * last_error + pre_error);
            pre_error = last_error;
            last_error = cur_error;
            //curValue +=cur_u;
            theta += cur_u;
            if (theta > 35 || theta < -35) {  //当增大到最大舵角后不再增大-->这里有一个问题，并不是舵角越大转角效果越好？怎么办？映射函数？
                theta -= cur_u;
            }
            //根据舵角计算下一次采集的航向值
            float div = (float) (K * theta * (1 - T + T * Math.pow(Math.E, -1 / T)));
            curValue += div;

            System.out.println("+-------------------------------"
                    + "\n|当前值：" + curValue
                    + "\n|本次增量-->" + cur_u
                    + "\n|距离目标的误差-->" + cur_error
                    + "\n+------------------------------------"
            );
            uArray.add(cur_error);  //当前的误差
            value.add(curValue);
            thetas.add(theta);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            repaint();
        }
    }

    float x = 0, y = 0;
    float speed = 2;
    ArrayList<Float> xs = new ArrayList<>();
    ArrayList<Float> ys = new ArrayList<>();
    
    public void pid() {  //稳定快速准确----->K=0.0785F, T = 3.12F;  //船舶操纵性能，根据操纵性能求出舵角theta后->角度变化
        uArray.clear();
        value.clear();
        desireValue = 10;
        curValue = 0;
        //K = 0.1F;T = 3F;
        /*float a = kp*(1+1/ki+kd);
		float b = kp*(1+2*kd);
		float c = kp*kd;*/
        while (!(Math.abs(curValue - desireValue) < 1 && Math.abs(theta) < 1)) {
            cur_error = desireValue - curValue;  //当前误差
            cur_u = kp * (cur_error - last_error) + (kp / ki) * cur_error + kp * kd * (cur_error - 2 * last_error + pre_error);   //计算得出增量
            //cur_u = a*cur_error-b*last_error+c*pre_error;
            pre_error = last_error;
            last_error = cur_error;
            //curValue +=cur_u;
            theta += cur_u;  //下面这步相当于matlab里的设定阈值
            if (theta > 35 || theta < -35) {  //当增大到最大舵角后不再增大-->这里有一个问题，并不是舵角越大转角效果越好？怎么办？映射函数？
                theta -= cur_u;
            }
            //根据舵角计算下一次采集的航向值---->
            float div = (float) (K * theta * (2 - T + T * Math.pow(Math.E, -2 / T)));

            curValue += div;  //计算当前航向角
            x = (float) (x + speed*Math.sin(Math.toRadians(curValue)));
            y = (float) (y + speed*Math.cos(Math.toRadians(curValue)));
            xs.add(x);
            ys.add(y);
            
            float r = (float) (K * theta * (1 - Math.exp(-2 / T)));
            System.out.println(r);
            System.out.println("+-------------------------------"
                    + "\n|当前值：" + curValue
                    + "\n|本次增量-->" + cur_u
                    + "\n|距离目标误差==>" + cur_error
                    + "\n+------------------------------------"
            );
            uArray.add(cur_error);  //当前的误差
            value.add(curValue);  //当前航向角
            thetas.add(theta);  //当前舵角
            /*try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			repaint();*/
        }
    }

    public void pidAdd() {  //位置式
        uArray.clear();
        value.clear();
        desireValue = 80;
        curValue = 0;
        while (Math.abs(curValue - desireValue) > 1) {
            cur_error = desireValue - curValue;  //当前误差
            cur_u = kp * cur_error + (kp / ki) * sum_error + kp * kd * (cur_error - last_error);
            last_error = cur_error;
            //curValue = cur_u;
            theta += cur_u;
            if (theta > 35 || theta < -35) {
                theta -= cur_u;
            }
            //根据舵角计算下一次采集的航向值
            float div = (float) (K * theta * (1 - T + T * Math.pow(Math.E, -1 / T)));
            curValue += div;

            System.out.println("+-------------------------------"
                    + "\n|当前值：" + curValue
                    + "\n|当前误差==>" + cur_error
                    + "\n+------------------------------------"
            );
            uArray.add(cur_error);  //当前的误差
            value.add(curValue);
            thetas.add(theta);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            repaint();
        }
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);  //去掉这句不会全部重新绘制，只会绘制新值，并覆盖原值
        //**************************************************
        //坐标系，左下角  0 500处-------->画出坐标系
        //x轴
        Graphics2D g = (Graphics2D) graphics.create();

        g.setColor(Color.BLACK);
        //x轴横线
        g.drawLine(40, 500, 1200, 500);
        //x坐标点
        g.setFont(new Font("Times New Roman", Font.PLAIN, 18));
        for (int i = 0; i < 1200; i += 40) {
            //g.drawOval(i+18, 500, 5, 5);
            g.fillOval(i + 40, 500, 3, 3);
            g.drawString(String.valueOf(i * 2 / 40), i + 40, 520);
        }
        //y轴
        for (int i = 0; i < 501; i += 25) {
            //g.drawOval(20, 500-i, 5, 5);
            g.fillOval(40, 500 - i, 3, 3);
            g.drawString(String.valueOf(i / 5), 10, 510 - i);
        }
        //g.setStroke(new BasicStroke(3.0F));
        g.drawLine(40, 500, 40, 0);
        /**
         * **********************坐标画完了********************************
         */
        //显示数据--------------------------------------------------------------
        /*g.setFont(new Font("宋体", Font.PLAIN, 18));
		g.setStroke(new BasicStroke(3.0F));
		
		g.drawString("时间 / s", 850, 550);
		g.drawString("舵角及航向角度数 / °", 40, 40);
		g.drawString("kp = "+kp
				+ "     ki = "+ki
				+ "     kd = "+kd,
				750, 350);  //当前pid的值
		//g.drawString("cur error", 600, 40);
		g.drawString("目标航向", 750, 200);
		g.drawString("当前航向", 750, 250);
		g.drawString("舵角", 750, 300);*/
        g.setStroke(new BasicStroke(2.0F));
        g.setColor(Color.RED);
        //g.drawLine(900, 200, 1000, 200);
        g.drawLine(0, (int) (500 - desireValue * 5), 1200, (int) (500 - desireValue * 5));  //目标线
        //g.setColor(Color.BLUE);
        //g.drawLine(750, 40, 850, 40);  //表示那种颜色对应哪种变量
        g.setColor(Color.BLACK);
        /**
         * *********************************************************
         */
        Stroke backup = g.getStroke();
        /*Stroke dash = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 2.5f, new float[] { 5, 20, },
                0f);
        g.setStroke(dash);*/
        float j = 0;
        /*for(int i=0;i<uArray.size();i++){  //增量值
			float geti = uArray.get(i);  //自动拆箱
			g.drawOval(j, 500-(int) geti*5, 5, 5);
			j+=5;
		}*/
        g.setColor(Color.GREEN);
        //g.drawLine(900, 250, 1000, 250);  //颜色对应的变量
        j = 0;
        /*float hello = value.get(0);
		for (int i = 1; i < value.size(); i++) { // 画出当前值
			float geti = value.get(i);
			// g.drawOval(j, 500-(int) geti*5, 5, 5);
			//g.fillOval(j + 20, 500 - (int) geti * 5, 5, 5);
			g.drawLine(j, 500 -(int)hello*5, j+5, 500-(int)geti*5);
			j += 5;
			hello = geti;
		}*/
        for (int i = 0; i < value.size(); i++) {  //画出当前值
            float geti = value.get(i);
            //g.drawOval(j, 500-(int) geti*5, 5, 5);
            g.fillOval((int) j + 40, 500 - (int) geti * 5, 5, 5);
            j += 3;
        }

        g.setStroke(backup);
        //g.setColor(Color.ORANGE);
        g.setColor(Color.BLUE);
        //g.drawLine(900, 300, 1000, 300);
        j = 0;
        /*float temp = thetas.get(0);
		for(int i=1;i<thetas.size();i++){
			float geti = thetas.get(i);
			//g.fillOval(j+20, 500-(int)geti*5, 5, 5);
			g.drawLine(j+20, 500-(int)temp*5, j+25, 500-(int)geti*5);
			j+=5;
			temp = geti;
		}*/
        for (int i = 0; i < thetas.size(); i++) {
            float geti = thetas.get(i);
            g.fillOval((int) j + 40, 500 - (int) geti * 5, 5, 5);
            j += 3;
        }
    }

    public void toFile(ArrayList<Float> value, ArrayList<Float> thetas) {  ///输出到文件中
        //将数据输出文件
        File out = new File("C:\\Users\\oo\\Desktop\\data\\trend.csv");
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(out);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(new String("Time"));
            for (int i = 1; i <= value.size(); i++) {
                if (i % 10 == 0) {
                    bufferedWriter.write("," + Integer.toString(i / 10));
                }
            }
            bufferedWriter.newLine();
            
            bufferedWriter.write("Course");
            for (int i = 0; i < value.size(); i++) {
                if (i % 10 == 0) {
                    bufferedWriter.write("," + value.get(i));
                }
            }
            bufferedWriter.newLine();

            bufferedWriter.write("Rudder");
            for (int i = 0; i < thetas.size(); i++) {
                if (i % 10 == 0) {
                    bufferedWriter.write("," + thetas.get(i));
                }
            }
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.write("X");
            for (int i = 0; i < xs.size(); i++) {
                if (i % 10 == 0) {
                    bufferedWriter.write("," + xs.get(i));
                }
            }
            bufferedWriter.newLine();
            
            bufferedWriter.write("Y");
            for (int i = 0; i < ys.size(); i++) {
                if (i % 10 == 0) {
                    bufferedWriter.write("," + ys.get(i));
                }
            }
            
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
}
