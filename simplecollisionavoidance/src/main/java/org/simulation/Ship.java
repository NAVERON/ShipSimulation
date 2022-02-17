
package org.simulation;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ship {
	private double x, y, s, c;
	public LinkedList<Ship> dangerList = new LinkedList<>();
	public LinkedList<Double> dataList = new LinkedList<>();
	public ArrayList<Point> shipTrack = new ArrayList<>();

	public final float K = 0.05F, T = 10F;
	//////////////////////////////////////////////////////////////////////
	// action表示舵角
	public int Action = 0; // if 0 , no option , speed up 1//speed down 2//turn left 3//turn right 4
	// 2016.12.09更改：action作为舵角标志量而非只是指示的参数，
	// action 舵角的大小并根据角加速度的变化来计算角速度的变化
	public int time = 0; // 计算时间
	public float rt = 0;// 角加速度
	public float r = 0;// 角速度
	// public float relcourse = 0; //变化的角度
	public boolean danger = false; // 指示本船是否危险状态
	// 判断是否存在偏差
	public boolean courseDeviate = false;
	public boolean trackDeviate = false;
	// 记录避碰前的相关信息
	public boolean dangerBegin = false;// 判断避碰的始末
	public boolean dangerEnd = true;
	public double orignCourse = 0;// 避碰前的航向
	public Point orignPoint = new Point();// 避碰前位置
	///////////////////////////////////////////////////////////////////////
	public int Type = 0;// if 0, normal ship, sailing, fishing, out of control, limit by control, limit
						// by draft

	private double stepx, stepy;
	private double c2r; // 注意三角函数参数都是弧度而不是角度

	public Ship(double x, double y, double s, double c, int type) {
		this.x = x;
		this.y = y;
		if (s > 20)
			this.s = 20;
		else
			this.s = s;
		this.c = c;
		this.Type = type;
	}

	public Ship() {
		this.x = DataBase.defaultx;
		this.y = DataBase.defaulty;
		this.s = DataBase.defaults;
		this.c = DataBase.defaultc;
		this.Type = 0;
	}

	public double getParameter(int index) {
		switch (index) {
		case 1:
			return x;
		case 2:
			return y;
		case 3:
			return s;
		case 4:
			return c;
		default:
			return 0;// error
		}
	}

	public synchronized void giveValue(int index, double newValue) {// change ship's parameter
		switch (index) {
		case 1:
			x = newValue;
			break;
		case 2:
			y = newValue;
			break;
		case 3:
			s = newValue;
			break;
		case 4:
			c = newValue;
			break;
		default:
			x = DataBase.defaultx;
			y = DataBase.defaulty;
			s = DataBase.defaults;
			c = DataBase.defaultc;
			break;
		}

		while (this.c < 0 || this.c >= 360) {
			if (this.c < 0)
				this.c += 360;
			if (this.c >= 360)
				this.c -= 360;
		}
		if (this.s < 0 || this.s > 20)
			System.err.println("speed out of limits!!");
	}

	int count = 0;

	public void goAhead() {// position, course, speed test
		if (!danger && !this.dangerBegin) { // 在向前走之前，判断： 是否危险，是否航向偏离或者航迹偏离
			// 在无危险时，如果存在航迹或航向偏差，进行校正
			if (courseDeviate || trackDeviate) {
				// 如果存在航向偏差
				int relCourse = (int) (this.getParameter(4) - orignCourse); // 当前船舶的航向与避碰前航向的差值
				int relLine = (int) DataBase.CaculateRatio(this.orignPoint.getX(), this.orignPoint.getY(),
						this.getParameter(1), this.getParameter(2));

				int relTrack = (int) (relLine - this.getParameter(4));
				// 左舷- 右舷+
				System.out.println("我在航迹恢复");
				if (relTrack > 180) {
					relTrack -= 360;
				}
				if (relTrack < -180) {
					relTrack += 360;
				}
				if (relCourse > 180) {
					relCourse -= 360;
				}
				if (relCourse < -180) {
					relCourse += 360;
				}
				// 进行综合判断
				if (relTrack < 0 && relCourse < 0) {// 如果位置在左舷，航向向右
					this.Action = 20;
				} else {
					this.Action = 0;
				}
				if (relTrack > 0 && relCourse > 0) {
					this.Action = -20;
				} else {
					this.Action = 0;
				}
				this.Action = -10;
				this.shipDatachange();
				if (relTrack < 5) {
					this.dangerEnd = true;
					this.dangerBegin = false;
				}
//                if (relCourse<-180||(relCourse>0 && relCourse<180)) {
//                    //如果负值 <-180 左转, -180<course<0向右转
//                    this.Action = -10;
//                }
//                else if((relCourse>-180 && relCourse<0) || relCourse>180){
//                    this.Action = 10;
//                }
			}
		}
		c2r = Math.toRadians(c);
		stepx = s * Math.sin(c2r);
		stepy = s * Math.cos(c2r);
		giveValue(1, x + stepx);
		giveValue(2, y - stepy);

		if (shipTrack.size() > 100) { // 轨迹点记录的处理
			new Thread() {
				@Override
				public void run() {
					try {
						File f = new File("trackStore.txt");
						if (!f.exists()) {
							f.createNewFile();
							f = new File("trackStore.txt");
						}
						FileWriter fw = new FileWriter(f);
						BufferedWriter bw = new BufferedWriter(fw);
						for (int i = 0; i < shipTrack.size(); i++) {
							Point p = shipTrack.get(i);
							fw.write(p.x + "," + p.y + "\n");
							// fw.write("\n\r"); //回车换行有点问题
						}
						bw.flush();
						fw.flush();
						bw.close();
						fw.close();
						shipTrack.clear();
					} catch (IOException ex) {
						Logger.getLogger(Ship.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}.start();
		}
		count++;
		if (count > 5) { // 每隔5个点记录一次
			if (!DataBase.trackrecord) {
				shipTrack.add(new Point((int) x, (int) y));
				count = 0;
			}
		}
		// 超出边界的处理，以前是进行边界反射，现在从另一边出来，以后应该考虑地球模型，旋转一圈，回到原地
		if (x < 0)
			x = 1120;
		if (x > 1120)
			x = 0;
		if (y < 0)
			y = 740;
		if (y > 740)
			y = 0;

	}

	// 更改船舶自身的参数，以便于后续的计算
	public void shipDatachange() {
		// 假定船舶参数已知： K= 0.05 T= 10
		if (Action != 0) { // 有舵角时，存在角加速度，没有舵角时，角速度逐渐变为0，航向稳定
			time++;
		} else { // 当舵角为0时，角速度逐渐变小
			if (time > 0) {
				time -= 1;
			} else {
				time = 0;
			}
			if (r > 1) {
				r -= 1;
			} else if (r < -1) {
				r += 1;
			} else { // 当角速度足够小时，忽略不计
				r = 0;
			}
		}
		// 其中action=0， 表示没有角加速度，但是有角速度
		rt = (float) (K / T * Action * Math.exp(-time / T));
		r += rt * time;
//        r = (float) (K*this.Action*(1-Math.exp(-time/T)));
		// relcourse = r; //每次的角度变化按照角速度来算
		// r = (float) (K*Action*(1 - Math.exp(-time/T)));
		// relcourse = (float) (K*Action*(time - T + T*Math.exp(-time/T)));
		// System.out.println("角加速度："+rt+" 角速度："+r+" 角度变化："+r+"\n");
		c += r;
	}

}
