
package org.simulation;

import java.awt.Graphics;
import java.util.LinkedList;

public class DataBase {
	public static double defaultx = 300, defaulty = 400, defaults = 5, defaultc = 0, linecourse = 30;
	public static LinkedList<Ship> ships = new LinkedList<>();
	public static int dirpointradius = 10;
	public static Graphics g;
	// flags
	public static boolean tracklock = false;
	public static boolean trackrecord = true;
	public static boolean begin = false;
	public static boolean pause = false;
	public static boolean danger = false; // 判断最新创建的对象是否危险状态

	/**
	 * caculate area
	 * <p>
	 * start x and start y to end x and end y, caculate ratio of course ,based on up
	 * and clock direction
	 * </p>
	 * 
	 * @param start_x*
	 * @param start_y*
	 * @param end_x*
	 * @param end_y*
	 * 
	 * @return realCourse
	 */

	public static double CaculateRatio(double start_x, double start_y, double end_x, double end_y) {
		double differentx = end_x - start_x;
		double differenty = end_y - start_y;
		double course = 0;
		int adjust = 0;// switch case///

		if (differentx == 0 && differenty == 0)
			adjust = 0;
		else if (differentx >= 0 && differenty < 0)
			adjust = 1;
		else if (differentx < 0 && differenty <= 0)
			adjust = 2;
		else if (differentx <= 0 && differenty > 0)
			adjust = 3;
		else if (differentx > 0 && differenty >= 0)
			adjust = 4;

		switch (adjust) {
		case 0:
			course = 0;
			break;
		case 1:
			course = 450 - Math.toDegrees(Math.atan2(-differenty, differentx));
			break;
		case 2:
			course = 90 - Math.toDegrees(Math.atan2(-differenty, differentx));
			break;
		case 3:
			course = 90 - Math.toDegrees(Math.atan2(-differenty, differentx));
			break;
		case 4:
			course = 90 - Math.toDegrees(Math.atan2(-differenty, differentx));
			break;
		}

		while (course < 0 || course >= 360) {
			if (course < 0)
				course += 360;
			if (course >= 360)
				course -= 360;
		}
		return course;
	}

}
