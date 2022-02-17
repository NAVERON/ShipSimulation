/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.navigator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.AutoNavVehicle;
import org.simulation.environment.MessageType;
import org.simulation.environment.Option;
import org.simulation.environment.Rule;
import org.simulation.environment.Situation;
import org.simulation.service.ComThread;
import org.simulation.unity.LocalVessel;
import org.simulation.unity.VisualNav;

import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * @function 航行器抽象，用来完成普通船舶和航行器的公共实现部分
 * @author ERON
 */
public abstract class Navigator extends Button implements Rule, Manipulation {
	// 对于各种航行器的抽象类， Rule接口定义航行器应当遵守的规则， Manipulation定义操纵性能， Communication定义通信过程
	// 底层的地图实现以后再做
	public List<DynInfo> dynInfos = new LinkedList<>(); // 障碍物信息存储
	private ComThread comThread = null; // 通信对象，可以单独线程处理
	public Option option = null;
	public boolean isDanger = false; // 当前是否危险？是 -> 分析并打舵 == 否 -> 恢复航迹向

	/**
	 * @parameter idNumber 唯一识别号，船舶上是MMSI name 航行器名称 longth 长度 width 宽度 type 类型
	 */

	///////////////////// ---静态属性存储区域---////////////////////////
	private String idNumber = null;
	private String name = null;
	private int navLength = 5;
	private int beam = 2; // 宽度
	private char type = 'a'; // 标志是普通航行器，还是船舶类型的，更改了原先的意义(以前是表示船舶类型的变量的)
	// 标志航行器类型的变量 a表示航行器 b表示船舶对象
	///////////////////////// ---静态属性存储结束---动态开始---////////////////////////
	private float head = 0; // 这里存储的信息相当于临时的变化，每次变化后new 一个动态数据
	private float course = 0; // 然后存储到链表中
	private float speed = 0;
	private float latitude = 0; // y
	private float longitude = 0; // x
	private char state = '0'; // -----状态标识符，使用数字字符标识，10种状态
	private String updateTime;
	private float rudderAngle = 0;
	public float preRudder = 0;
	public float lastRudder = 0;
	/* 显示操纵过程中的动态变化 */
	// private float rotateRate = 0.0F; //转角速度----------暂时不用
	///////////////////////////// ---动态存储区结束---/////////////////////////////

	public static int idIndex = 0; // 默认id号的索引

	public Navigator(char type) {
		this.idNumber = "0000" + idIndex++;
		this.name = "default";
		this.navLength = 5;
		this.beam = 2;
		this.type = type;
		initialNav();
	}

	public Navigator(String idNumber, String name, int navLength, int beam, char type) {
		this.idNumber = idNumber;
		this.name = name;
		this.navLength = navLength;
		this.beam = beam;
		this.type = type;
		initialNav();
	}

	public boolean comRunning = true;

	private void initialNav() {
		this.setMinSize(5.0F, 5.0F);
		this.setPrefSize(this.beam * AutoNavVehicle.level, this.navLength * AutoNavVehicle.level);
		comThread = new ComThread(this); // 专门处理通信的对象
		comThread.start();

		option = new Option(this); // 操纵线程
	}

	// 获取航行器属性的方法------开始
	public String getName() {
		return this.name;
	}

	public String getIdNumber() {
		return this.idNumber;
	}

	public float getHead() {
		float geted = this.head;
		if (geted >= 360) {
			geted -= 360;
		} else if (geted < 0) {
			geted += 360;
		}
		return geted;
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float dirSpeed) {
		this.speed = dirSpeed;
	}

	public Point2D getPosition() {
		return new Point2D(longitude, latitude);
	}

	public void setPosition(Point2D position) {
		longitude = (float) position.getX();
		latitude = (float) position.getY();
	}

	public float getRudderAngle() {
		return this.rudderAngle;
	}

	public void setType(char type) {
		this.type = type;
		if (this.type == 'a') { // 根据不同的类型选择不同的样式
			this.setStyle("-fx-background-color : red;");
		} else if (this.type == 'b') {
			this.setStyle("-fx-background-color : blue;");
		}
	}
	// 获取属性方法-------结束

	/***********************************
	 * Go-AND-Test
	 *********************************/
	public List<Vessel> otherNavs = new LinkedList<>(); // 存储通信范围内的对象
	public List<LocalVessel> locals = new LinkedList<>(); // 存储转换信息
	public float[] oneRange, twoRange, threeRange, fourRange; // 四个领域的可行范围
	// 分组后的组存储
	public ArrayList<LinkedList<LocalVessel>> one;
	public ArrayList<LinkedList<LocalVessel>> two;
	public ArrayList<LinkedList<LocalVessel>> three;
	public ArrayList<LinkedList<LocalVessel>> four;

	// 下面记录速度和角度的偏差
	private double originHead, originSpeed;
	public Point2D destination; // 目标地
	public double lastHeadDecision = 0; // 记录上次的操纵----1表示右舷，0表示保持，-1表示左舷转向 7.18----->正向右，负向左
	public double lastSpeedDecision = 0; // 可能用不到----记录本次的决定 与之相同，正负号表示左右方向，带有大小
	public double headDecision = 0; // 现在需要去的航向
	public double speedDecision = 0; // 现在最终速度

	public double comHeadDecision = 0;
	public double comSpeedDecision = 0;
	public boolean isCom = false; // 现在是否使用通信决策 -- 判断使用通信的决议
	/*
	 * ============================================特殊区域=============================
	 * =========================
	 */

	public void analyse() { // 分析当前形式
		otherNavs.clear(); // 清空上次计算的 --- 思考如何能够减少这种重复遍历的计算
		locals.clear();

		// 遍历当前全局航行器，找出距离符合条件的 200 px
		float radius = this.speed * 100; // 更改---根据速度判断危险区域
		for (Iterator<Vessel> items = AutoNavVehicle.navigators.iterator(); items.hasNext();) { // 暂时定义一像素代表1米
			Vessel next = items.next();
			if (this.idNumber.equals(next.getIdNumber())) { // 这里可以使用对象比较吗？都是引用链表上的对象 --可以
				continue;
			}
			if (Math.abs(this.getPosition().getX() - next.getPosition().getX()) < radius // 经度代表x坐标
					&& Math.abs(this.getPosition().getY() - next.getPosition().getY()) < radius) { // 纬度是y坐标
				otherNavs.add(next); // 添加的是指向引用
			}
		}
		// 如果周围没有其他对象，则没必要进行下面的计算 ---2017.10.1如果没有危险，就复航
		// 先进行坐标转换行不行呢？
		// ------------ 坐标转换
		// ---------------------需要临时存储，具体参看链接：http://blog.csdn.net/can3981132/article/details/52518833
		double headRadius = Math.toRadians(this.head); // 表示将要围绕某点旋转的旋角度 ---> r
		for (Iterator<Vessel> items = otherNavs.iterator(); items.hasNext();) {
			Vessel other = items.next();

			String id = other.getIdNumber();
			float dx = (float) (other.getPosition().getX() - this.longitude);
			float dy = (float) (other.getPosition().getY() - this.latitude);
			float x, y; // 转换后的坐标点
			// 这个是绕一点旋转的公式-----------------测试完成6.30 边界测试正常，普通测试正常
			// 证明过程 https://www.zybang.com/question/aafa923e7f7c318cbd6f5bf8987c6c6c.html
			headRadius = -headRadius;
			x = (float) (dx * Math.cos(headRadius) - dy * Math.sin(headRadius)); // 相对本航行器的坐标
			y = (float) (dx * Math.sin(headRadius) + dy * Math.cos(headRadius)); // 顺时针旋转为 正，逆时针为 负
			// 下面这个是坐标轴旋转的公式------------------------经过math验证，两个公式是一样的
			float dh = other.getHead() - this.head; // ==========角度旋转
			while (dh >= 360 || dh < 0) {
				if (dh >= 360) {
					dh -= 360;
				}
				if (dh < 0) {
					dh += 360;
				}
			}
			y = -y; // 这里转换成左下角坐标系
			locals.add(new LocalVessel(id, x, y, dh, other.getSpeed())); // 相对于本航行器，其他的位置和方向存储-----速度不受坐标系转换的影像
		}
		// 2017.7.1 ---- 这里的计算斜率需要重新计算以左下角为原点的坐标系计算
		for (int i = 0; i < locals.size(); i++) { // 计算每个点相对角度
			locals.get(i).ratio = getRatio2(locals.get(i).longitude, locals.get(i).latitude); // 求取斜率时，y坐标添加负号，为了转换坐标，只是暂时解决
		}
		// --------- 聚类分析 ----------拿local做计算
		// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
		LinkedList<LocalVessel> part1 = new LinkedList<>(); // -30 -> 30
		LinkedList<LocalVessel> part2 = new LinkedList<>(); // 30 -> 90
		LinkedList<LocalVessel> part3 = new LinkedList<>(); // 90 -> 210
		LinkedList<LocalVessel> part4 = new LinkedList<>(); // 210 - 330

		for (LocalVessel getLocalVessel : locals) { // 分区

			if (getLocalVessel.ratio >= 330 || getLocalVessel.ratio <= 30) {
				part1.add(getLocalVessel);
			}
			if (getLocalVessel.ratio > 30 && getLocalVessel.ratio <= 90) {
				part2.add(getLocalVessel);
			}
			if (getLocalVessel.ratio > 90 && getLocalVessel.ratio <= 210) {
				part3.add(getLocalVessel);
			}
			if (getLocalVessel.ratio > 210 && getLocalVessel.ratio < 330) {
				part4.add(getLocalVessel);
			}
		}
		/* 判断是否恢复航向和航迹 */
		if (!isDanger) { // 速度决策表示的是变化量
			pinSpeed((float) -lastSpeedDecision);
		}
		if (part1.isEmpty() && part2.isEmpty() && part4.isEmpty() && !isDanger) { // 如果正前方和左右没有其他船舶，则复航
			voyageReturn(); // 准备恢复航线
			isCom = false;
			return;
		}
		// 这里转换成负数，方便后边排序计算-----part1---根据ratio排序
		for (int g = 0; g < part1.size(); g++) { // 只需要对第一个进行特殊处理
			LocalVessel temp = part1.get(g);
			if (temp.ratio >= 330) {
				temp.ratio -= 360;
			}
		}
		// 将三部分排序-----升序
		Collections.sort(part1);
		Collections.sort(part2);
		Collections.sort(part3);
		Collections.sort(part4);
		// 对三个几何进行聚类 ----- 聚类算法
		int i = 1; // 分组类别指示
		for (Iterator<LocalVessel> it = part1.iterator(); it.hasNext();) {
			LocalVessel t = it.next();
			if (t.belong != -1) { // 如果已经分配过，则不用计算了
				continue;
			}
			// 走到这里的表示还没有被分配过
			///////////////////////////////////////////////////////////////////////////////////
			boolean[] index = new boolean[part1.size()];
			for (int f = 0; f < index.length; f++) { // 初始化为 false
				index[f] = false;
			}
			int has = -1; // 标记周边是否有已经标记过的，如果有，则直接赋值，没有就需要累加
			////////////////////////////////////////////////////////////////////////////////////
			for (int n = 0; n < part1.size(); n++) { // 这里的处理有一些问题，比如先后的问题，以后有机会再解决，现在实在没办法
				LocalVessel g = part1.get(n);
				if (t != g) {
					float dx = Math.abs(t.longitude - g.longitude);
					float dy = Math.abs(t.latitude - g.latitude);
					float dh = Math.abs(t.head - g.head);
					double d = Math.sqrt(dx * dx + dy * dy + dh * dh);
					if (d <= 100) {
						index[n] = true;
						if (g.belong != -1) {
							has = n;
						}
					}
				}
			}
			// 最终决定标记
			if (has != -1) { // 如果周围有标记过的，则对每一个进行相同的标记
				int a = part1.get(has).belong; // 得到索引的belong值
				t.belong = a;
				for (int n = 0; n < part1.size(); n++) {
					if (index[n]) {
						part1.get(n).belong = a;
					}
				}
			} else {
				t.belong = i;
				for (int n = 0; n < part1.size(); n++) {
					if (index[n]) {
						part1.get(n).belong = i;
					}
				}
				i++;
			}
		}
		// 第二部分
		int j = 1;
		for (Iterator<LocalVessel> it = part2.iterator(); it.hasNext();) {
			LocalVessel t = it.next();

			if (t.belong != -1) { // 如果已经分配过，则不用计算了
				continue;
			}
			// 走到这里的表示还没有被分配过
			///////////////////////////////////////////////////////////////////////////////////
			boolean[] index = new boolean[part2.size()];
			for (int f = 0; f < index.length; f++) { // 初始化为 false
				index[f] = false;
			}
			int has = -1; // 标记周边是否有已经标记过的，如果有，则直接赋值，没有就需要累加
			////////////////////////////////////////////////////////////////////////////////////
			for (int n = 0; n < part2.size(); n++) { // 这里的处理有一些问题，比如先后的问题，以后有机会再解决，现在实在没办法
				LocalVessel g = part2.get(n);
				if (t != g) {
					float dx = Math.abs(t.longitude - g.longitude);
					float dy = Math.abs(t.latitude - g.latitude);
					float dh = Math.abs(t.head - g.head);
					double d = Math.sqrt(dx * dx + dy * dy + dh * dh);
					if (d <= 100) {
						index[n] = true;
						if (g.belong != -1) {
							has = n;
						}
					}
				}
			}
			// 最终决定标记
			if (has != -1) { // 如果周围有标记过的，则对每一个进行相同的标记
				int a = part2.get(has).belong; // 得到索引的belong值
				t.belong = a;
				for (int n = 0; n < part2.size(); n++) {
					if (index[n]) {
						part2.get(n).belong = a;
					}
				}
			} else {
				t.belong = j;
				for (int n = 0; n < part2.size(); n++) {
					if (index[n]) {
						part2.get(n).belong = j;
					}
				}
				j++;
			}
		}
		// 第三部分
		int k = 1; // 标记一共有多少类
		for (Iterator<LocalVessel> it = part3.iterator(); it.hasNext();) {
			LocalVessel t = it.next();

			if (t.belong != -1) { // 如果已经分配过，则不用计算了
				continue;
			}
			// 走到这里的表示还没有被分配过
			///////////////////////////////////////////////////////////////////////////////////
			boolean[] index = new boolean[part3.size()];
			for (int f = 0; f < index.length; f++) { // 初始化为 false
				index[f] = false;
			}
			int has = -1; // 标记周边是否有已经标记过的，如果有，则直接赋值，没有就需要累加
			////////////////////////////////////////////////////////////////////////////////////
			for (int n = 0; n < part3.size(); n++) { // 这里的处理有一些问题，比如先后的问题，以后有机会再解决，现在实在没办法
				LocalVessel g = part3.get(n);
				if (t != g) {
					float dx = Math.abs(t.longitude - g.longitude);
					float dy = Math.abs(t.latitude - g.latitude);
					float dh = Math.abs(t.head - g.head);
					double d = Math.sqrt(dx * dx + dy * dy + dh * dh);
					if (d <= 100) {
						index[n] = true;
						if (g.belong != -1) {
							has = n;
						}
					}
				}
			}
			// 最终决定标记
			if (has != -1) { // 如果周围有标记过的，则对每一个进行相同的标记
				int a = part3.get(has).belong; // 得到索引的belong值
				t.belong = a;
				for (int n = 0; n < part3.size(); n++) {
					if (index[n]) {
						part3.get(n).belong = a;
					}
				}
			} else {
				t.belong = k;
				for (int n = 0; n < part3.size(); n++) {
					if (index[n]) {
						part3.get(n).belong = k;
					}
				}
				k++;
			}
		}
		// 第四部分
		int h = 1; // 标记一共有多少类
		for (Iterator<LocalVessel> it = part4.iterator(); it.hasNext();) {
			LocalVessel t = it.next();

			if (t.belong != -1) { // 如果已经分配过，则不用计算了
				continue;
			}
			// 走到这里的表示还没有被分配过
			///////////////////////////////////////////////////////////////////////////////////
			boolean[] index = new boolean[part4.size()];
			for (int f = 0; f < index.length; f++) { // 初始化为 false
				index[f] = false;
			}
			int has = -1; // 标记周边是否有已经标记过的，如果有，则直接赋值，没有就需要累加
			////////////////////////////////////////////////////////////////////////////////////
			for (int n = 0; n < part4.size(); n++) { // 这里的处理有一些问题，比如先后的问题，以后有机会再解决，现在实在没办法
				LocalVessel g = part4.get(n);
				if (t != g) {
					float dx = Math.abs(t.longitude - g.longitude);
					float dy = Math.abs(t.latitude - g.latitude);
					float dh = Math.abs(t.head - g.head);
					double d = Math.sqrt(dx * dx + dy * dy + dh * dh);
					if (d <= 100) {
						index[n] = true;
						if (g.belong != -1) {
							has = n;
						}
					}
				}
			}
			// 最终决定标记
			if (has != -1) { // 如果周围有标记过的，则对每一个进行相同的标记
				int a = part4.get(has).belong; // 得到索引的belong值
				t.belong = a;
				for (int n = 0; n < part4.size(); n++) {
					if (index[n]) {
						part4.get(n).belong = a;
					}
				}
			} else {
				t.belong = h;
				for (int n = 0; n < part4.size(); n++) {
					if (index[n]) {
						part4.get(n).belong = h;
					}
				}
				h++;
			}
		}
		// 对分类后进行分别存储
		// for part1
		i--;
		one = new ArrayList<LinkedList<LocalVessel>>();
		for (int x = 0; x < i; x++) { // 准备存储空间
			one.add(new LinkedList<>());
		}
		for (int x = 0; x < part1.size(); x++) {
			int belong = part1.get(x).belong;
			one.get(belong - 1).add(part1.get(x));
		}
		// for part2
		j--;
		two = new ArrayList<LinkedList<LocalVessel>>();
		for (int x = 0; x < j; x++) {
			two.add(new LinkedList<>());
		}
		for (int x = 0; x < part2.size(); x++) {
			int belong = part2.get(x).belong;
			two.get(belong - 1).add(part2.get(x));
		}
		// for part3
		k--;
		three = new ArrayList<LinkedList<LocalVessel>>();
		for (int x = 0; x < k; x++) {
			three.add(new LinkedList<>());
		}
		for (int x = 0; x < part3.size(); x++) {
			int belong = part3.get(x).belong;
			three.get(belong - 1).add(part3.get(x));
		}
		h--;
		four = new ArrayList<LinkedList<LocalVessel>>();
		for (int x = 0; x < h; x++) {
			four.add(new LinkedList<>());
		}
		for (int x = 0; x < part4.size(); x++) {
			int belong = part4.get(x).belong;
			four.get(belong - 1).add(part4.get(x));
		}
		// 2017.7.18 建议使用map映射，键值对
		// 对第一个区间的需要进行变换，否则排序不能正常 --------这部分放在了前面
		for (int n = 0; n < one.size(); n++) { // 每一个分组，角度从小到大
			Collections.sort(one.get(n));
		}
		for (int n = 0; n < two.size(); n++) {
			Collections.sort(two.get(n));
		}
		for (int n = 0; n < three.size(); n++) {
			Collections.sort(three.get(n));
		}
		for (int n = 0; n < four.size(); n++) {
			Collections.sort(four.get(n));
		}
		// 如果belong = -1，则表示不属于任何分组，否则 它会属于其中一个分组，也就是说在当前航行器前面有几个跟他属性相似的
		// 这里添加是否属于周边障碍物的一个分组
		// 取得分组后的极值DCPA
		LinkedList<VisualNav> oneVisuals = new LinkedList<>();
		LinkedList<VisualNav> twoVisuals = new LinkedList<>();
		LinkedList<VisualNav> threeVisuals = new LinkedList<>();
		LinkedList<VisualNav> fourVisuals = new LinkedList<>();

		oneDCPA = getPoleDCPA(one);
		twoDCPA = getPoleDCPA(two);
		threeDCPA = getPoleDCPA(three);
		fourDCPA = getPoleDCPA(four);
		// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//=====================================================
		// 排序完成后，可以进行找极值了，之后可以知道------先最小值，后最大值
		onePole = getPoleRatio(one);
		twoPole = getPoleRatio(two);
		threePole = getPoleRatio(three);
		fourPole = getPoleRatio(four);

		// 判断本航行器属于哪个组？ //虚拟一个本地对象
		LocalVessel the = new LocalVessel(this.idNumber, 0, 0, 0, this.speed); // 如果分析之后周边不会有危险，则恢复航线
		LinkedList<LocalVessel> thegroup = new LinkedList<>();
		for (int index = 0; index < locals.size(); index++) { // 只找直接关系的，也行，因为同组之间存在传递关系
			LocalVessel temp = locals.get(index);
			float dx = temp.longitude;
			float dy = temp.latitude;
			float dh = temp.head;
			if (dh > 180) {
				dh = 360 - dh;
			}
			double d = Math.sqrt(dx * dx + dy * dy + dh * dh);
			if (d <= 100) { // 需要进一步的判断
				thegroup.add(temp);
			}
		}
		if (!thegroup.isEmpty()) { // 先是1，2区域，然后左边区域
			Collections.sort(thegroup); // 升序
			for (LocalVessel temp : thegroup) {
				if ((temp.ratio > -30 && temp.ratio < 90) || (temp.ratio > 270 && temp.ratio < 330)) { // 跟随状态
					pinSpeed(temp.speed - the.speed);
					pinRudder(temp.head - 0); // 输入的是差值

					isDanger = true;
					return; // 同组的话 就不需要，只要跟着前面就行了
				}
			}
			// 如果在领航则应当自主判断，并后面做出对应的决策
		}

		// 得到极值点之后怎么办？分析可以直接操舵了
		/* 一共分成四个领域 */
//        oneRange = new float[]{-30F, 30F};
//        twoRange = new float[]{30F, 90F};
//        threeRange = new float[]{90F, 210F};
//        fourRange = new float[]{210F, 330F};
		// 下面分析 ================ 这里有一个问题，如果做出了相同的决策，则下面不需要重新进行动作
//        oneRange = calRange(one, oneRange, oneDCPA, onePole);
//        twoRange = calRange(two, twoRange, twoDCPA, twoPole);
//        threeRange = calRange(three, threeRange, threeDCPA, threePole);
//        fourRange = calRange(four, fourRange, fourDCPA, fourPole);
		if (one.size() > 0) { // 先判断第一区域
			for (int a = 0; a < one.size(); a++) { // 有危险转30度，要么不转
				LocalVessel oneTemp = one.get(a).getLast();
				double oneDcpa = calDCPA(the, oneTemp);
				if (Math.abs(oneDcpa) < 20) { // 如果存在危险，则右转向
					headDecision = 10; // 右转30度--- 11.25改变10度小角度
				} else { // 否则，如果前面判断转向，则不变，否则继续保向
					headDecision = (headDecision > 0) ? 10 : 0;
				}
			}
			for (int b = 0; b < two.size(); b++) { // 过船首加速并转向，发送协商信号 否则大幅度右转
				LocalVessel twoTemp = two.get(b).getFirst();
				double twoDcpa = calDCPA(the, twoTemp);
				if (twoDcpa < 20) { // 对方更趋向于过后面，我应当过其前面
					headDecision = (headDecision > 0) ? 60 : 0;
					// speedDecision = 1; //这是变化量
					// lastSpeedDecision -= speedDecision;

					// sendToSome(two.get(b), "bow"); //stern
					// System.out.println(this.idNumber + "对右舷的航行器说：我要过你们的船首");
				} else { // 取最右的
					headDecision = (headDecision > two.get(b).getLast().ratio) ? headDecision
							: two.get(b).getLast().ratio;
				}
			}
			for (int d = 0; d < four.size(); d++) {
				LocalVessel fourTemp = four.get(d).getLast();
				double fourDcpa = calDCPA(the, fourTemp);
				if (Math.abs(fourDcpa) < 20) {
					headDecision = (headDecision > 0 && !isCom) ? headDecision : comHeadDecision;
				} else {
					headDecision = headDecision == 0 ? 0 : headDecision;
				}
			}
		} else if (two.size() > 0) {
			// 船首向没有障碍物，但是右舷有
			LocalVessel twoTemp = two.get(0).getFirst();
			double twoDcpa = calDCPA(the, twoTemp);
			if (twoDcpa < 20) { // 船首向没有就加速通过，否则右转30度通过
				headDecision = (headDecision > 0) ? headDecision : -10;
				// speedDecision = 1;
				// lastSpeedDecision -= speedDecision;

				// sendToSome(two.get(0), "bow"); //stern
				// System.out.println("船首没有船 ____" + this.idNumber + "对右舷的航行器说： 我需要过船首");
			} else { // 取最右的
				headDecision = (headDecision > two.get(two.size() - 1).getLast().ratio) ? headDecision
						: two.get(two.size() - 1).getLast().ratio;
			}
		} else if (four.size() > 0) {
			// 船首向和右舷都没有，左舷有
			LocalVessel fourTemp = four.get(four.size() - 1).getLast(); // 取最后一组
			double fourDcpa = calDCPA(the, fourTemp);
			if (Math.abs(fourDcpa) < 20) {
				headDecision = 10;
			} else {
				headDecision = 0;
			}
		} else if (three.size() > 0) {
			// 其他地方都没有，右后方有
			headDecision = -10;
		} else {
			headDecision = 0;
			speedDecision = 0;
		}

		if (this.speed > 10) {
			speedDecision = comSpeedDecision = -2;
		}

		pinRudder((float) (isCom ? comHeadDecision : headDecision));
		pinSpeed((float) (isCom ? comSpeedDecision : speedDecision));
	}

	/**
	 * 计算可行域
	 * 
	 * @param oneDCPA    领域极值点的DCPA
	 * @param curRange   当前的可行域
	 * @param judgeRange 判断领域，即当前障碍物的角度范围
	 * @param one        分组后的链表
	 * @param n          代表当前是第几个分组
	 * @return 返回计算之后缩小的可行域
	 */
	// 以后有机会写一个计算两个区间交集、并集和差集的运算方法，写成API
	double[][] oneDCPA, twoDCPA, threeDCPA, fourDCPA;
	float[][] onePole, twoPole, threePole, fourPole;

	public float[] calRange(List<LinkedList<LocalVessel>> list, float[] curRange, double[][] DCPA, float[][] Pole) { // 分别计算区域的最终方向
		for (int i = 0; i < list.size(); i++) {
			LocalVessel temp1 = list.get(i).getFirst();
			LocalVessel temp2 = list.get(i).getLast();
			// 先判断大体的方位
			float px = (temp1.longitude + temp2.longitude) / 2;
			if (px <= 0) {
				// 在左舷
				if (DCPA[i][1] >= 0) {
					if (temp2.getSpeedX() >= 0) {
						curRange[1] = Pole[i][0];
						return curRange;
					} else {
						curRange[0] = Pole[i][1];
					}
				} else if (DCPA[i][1] < 0) {
					curRange[0] = 0;
				}
			} else if (px > 0) {
				// 在右舷
				if (DCPA[i][0] > 0) {
					if (temp1.getSpeedX() >= 0) {
						curRange[1] = Pole[i][0];
						return curRange;
					} else {
						curRange[0] = Pole[i][1];
					}
				} else if (DCPA[i][0] <= 0) {
					curRange[1] = 0;
				}
			}
		}

		return curRange;
	}

	public double calDCPA(LocalVessel the, LocalVessel vessel) {
		double DCPA;
		double dis = Math.sqrt(vessel.longitude * vessel.longitude + vessel.latitude * vessel.latitude);
		// 计算矢量和角度
		float rx = (float) (vessel.speed * Math.sin(Math.toRadians(vessel.head)));
		float ry = (float) (vessel.speed * Math.cos(Math.toRadians(vessel.head)) - the.speed);
		float rh = getRatio2(rx, ry); // 矢量和的角度
		// 真方位角度
		double th = 180 + vessel.ratio;
		while (th >= 360) { // 保证范围在0-360之间
			th -= 360;
		}
		double alpha = rh - th;
		if (th < 180) {
			alpha = -alpha;
		}
		alpha = Math.toRadians(alpha);
		DCPA = Math.sin(alpha) * dis;
//        if (this.idNumber.equals("12")) {
//            if (DCPA > 20) {
//                System.out.println(vessel.id + "==过船首");
//            } else if (DCPA < -20) {
//                System.out.println(vessel.id + "==过船尾");
//            } else {
//                System.out.println("要撞了......");
//            }
//        }

		return DCPA;
	}

	public double[][] getPoleDCPA(List<LinkedList<LocalVessel>> list) { // DCPA最小30，是观测得到的，没有计算
		int size = list.size();
		double[][] DCPA = new double[size][2];

		for (int n = 0; n < size; n++) {
			LocalVessel first = list.get(n).getFirst();
			LocalVessel last = list.get(n).getLast();
			// 先计算两者的距离
			double dis;
			dis = Math.sqrt(first.longitude * first.longitude + first.latitude * first.latitude);
			// 计算矢量和角度
			float rx, ry, rh;
			rx = (float) (first.speed * Math.sin(Math.toRadians(first.head)));
			ry = (float) (first.speed * Math.cos(Math.toRadians(first.head)) - this.speed);
			rh = getRatio2(rx, ry); // 矢量和的角度
			// 真方位角度
			double th;
			th = 180 + first.ratio;
			while (th >= 360) { // 保证范围在0-360之间
				th -= 360;
			}
			double alpha;
			alpha = rh - th; // 范围在-360~360之间
			if (th < 180) { // 这里的判断是保证从船尾过，计算的值是负的
				alpha = -alpha;
			}
			alpha = Math.toRadians(alpha);
			DCPA[n][0] = Math.sin(alpha) * dis;
//            if (this.idNumber.equals("12")) {
////                System.out.print("First:In getPoleDCPA method   ");
//                if (DCPA[n][0] > 10) {
//                    System.out.println(first.id + "==过船首");
//                }else if (DCPA[n][0] < -10) {
//                    System.out.println(first.id + "==过船尾");
//                }else{
//                    System.out.println("要撞了......");
//                }
//            }
//            if (this.idNumber.equals("12")) {
//                System.out.println(this.idNumber + " =111= 距离："+dis + "  矢量和角度："+rh+"  真方位角度"+th + "  求得的夹角 ："+alpha);
//                System.out.println("第一个极点DCPA："+DCPA[n][0]);
//            }
			// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//============================
			// 计算另一个极值点
			dis = Math.sqrt(last.longitude * last.longitude + last.latitude * last.latitude);
			// 计算矢量和角度
			rx = (float) (last.speed * Math.sin(Math.toRadians(last.head)));
			ry = (float) (last.speed * Math.cos(Math.toRadians(last.head)) - this.speed);
			rh = getRatio2(rx, ry); // 矢量和的角度
			// 真方位角度 == 自己在对方的什么方位角
			th = 180 + last.ratio;
			while (th >= 360) { // 保证范围在0-360之间
				th -= 360;
			}
			alpha = rh - th;
			if (th < 180) {
				alpha = -alpha;
			}
			alpha = Math.toRadians(alpha);
			DCPA[n][1] = Math.sin(alpha) * dis;
		}

		return DCPA;
	}

	public float[][] getPoleRatio(List<LinkedList<LocalVessel>> list) {
		int size = list.size();
		float[][] ratios = new float[size][2];
		for (int n = 0; n < size; n++) {
			ratios[n][0] = list.get(n).getFirst().ratio;
			if (ratios[n][0] > 180) {
				ratios[n][0] -= 360;
			}
			ratios[n][1] = list.get(n).getLast().ratio;
			if (ratios[n][1] > 180) {
				ratios[n][1] -= 360;
			}
		}

		return ratios;
	}

	// 得到点的集合，计算出一个序列，返回stack---------完全可以使用链表做，把链表当作栈用，更方便
	// 叉乘计算的方法 证明过程
	// ->链接见网址：https://www.zybang.com/question/f78a7e9b076367b03f1df832a8c131b3.html
	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
	// 需要增加KT系数来计算，以及pid系数
	private float K = 0.0785F, T = 3.12F;
	private float kp = 2F, ki = 20F, kd = 10F;
	private float curDiff, preDiff = 0, lastDiff = 0; // 解决内部类不能引用局部变量的情况
	private boolean isTurning = false; // 是否正在转向？
	private boolean isSpeeding = false; // 是否正在变速

	@Override
	public void goAhead() {
		analyse();
//        float a = rudderAngle - lastRudder;
//        float b = lastRudder - preRudder;
//        if( a*b<0 && (Math.abs(a)>5 || Math.abs(b)>5) ){  //符号相反
//            lastRudder = (preRudder+rudderAngle)/2;
//        }
		longitude += speed * Math.sin(Math.toRadians(head)); // couse是船首向和风流作用的合力方向
		latitude -= speed * Math.cos(Math.toRadians(head)); // 这样计算之后如果需要添加风流效果，可以再次求矢量和
		// 超界处理
		if (longitude > 600) {
			longitude = 0;
		}
		if (longitude < 0) {
			longitude = 600;
		}
		if (latitude > 500) {
			latitude = 0;
		}
		if (latitude < 0) {
			latitude = 500;
		}
		// 根据rudderAngle计算角速度，2017.7.14增加：如果没有速度，舵没有效果
		if (this.rudderAngle != 0) {
			this.head += K * rudderAngle * (2 - T + T * Math.pow(Math.E, -2 / T));
		}
		// if (this.lastRudder != 0) { this.head += K*lastRudder*(
		// 2-T+T*Math.pow(Math.E, -2/T) ); }
		while (this.head >= 360 || this.head < 0) { // 保证范围在0-360之间 -- 使用if也可以
			if (this.head >= 360) {
				this.head -= 360;
			}
			if (this.head < 0) {
				this.head += 360;
			}
		}
//        preRudder = lastRudder;
//        lastRudder = rudderAngle;

		this.course = this.head; // ----这个风流问题怎么解决？
		this.relocate(longitude - navLength / 2, latitude - beam / 2);
		this.setRotate(head - 90);
		// System.out.println(this.idNumber + "=航向："+this.head + "
		// 舵角"+this.rudderAngle);
		addDynInfo(new DynInfo(head, course, speed, longitude, 500 - latitude, state, new Date(), rudderAngle));
	}

	@Override
	public void setRudder(float rudderAngle) {
		// 设置舵角，暂时不用
		this.rudderAngle = rudderAngle;
	}

	public float finalRudder = 0; // 以后再说，打舵不能一下就到了
	// 自动打舵，根据航向的偏差，设置舵角，与 setRudder() 不同
	// 出现diff过大或则求得的rudderdiff过大原因，就是将PID离散化，需要3步才能清楚遗留的舵角矫正

	public synchronized void pinRudder(float diff) { // 只控制航向，不控制航迹 --- 相当于进行比例操舵
		float ruddernow = this.rudderAngle;
		// 根据航向的偏差以一定比例设置舵角
		// float diff = dirCourse - this.head;
		System.out.println(this.idNumber + " : " + diff);
		if (diff > 180) {
			diff -= 360; // 变成负值
		} else if (diff < -180) {
			diff += 360;
		}
		float rudderDiff = kp * (diff - lastDiff) + (kp / ki) * diff + kp * kd * (diff - 2 * lastDiff + preDiff);
		System.out.println("diff = " + diff + "    " + this.idNumber + "rudder diff : " + rudderDiff);
		rudderAngle += rudderDiff;
		if (rudderAngle > 35 || rudderAngle < -35) {
			rudderAngle -= rudderDiff;
		}
		preDiff = lastDiff;
		lastDiff = diff;

	}

	public synchronized void pinSpeed(float rest) { // 速度不能一下增加，不正常
		// 取消，不用写这个方法，下面的加减速已经可以控制了
		this.speed += rest / 4.0;
	}

	@Override
	public void turnTo(int desDir) { // 太慢了，怎么解决 --- 使用setrudder人工调节--->pinRudder
		curDiff = desDir - head;
//        preDiff = 0;  //记录上上次的误差
//        lastDiff = 0;  //记录上次的误差
		/* 开始动作 */
		if (curDiff > 180) {
			curDiff -= 360;
		} else if (curDiff < -180) {
			curDiff += 360;
		}
		float rudderDiff = kp * (curDiff - lastDiff) + (kp / ki) * curDiff
				+ kp * kd * (curDiff - 2 * lastDiff + preDiff);
		rudderAngle += rudderDiff;
		if (rudderAngle > 35 || rudderAngle < -35) {
			rudderAngle -= rudderDiff;
		}
		preDiff = lastDiff;
		lastDiff = curDiff;
		/* 动作结束 */
		Thread turning = new Thread(new Runnable() { // 多次调用转向程序怎么办？想办法 -- >搜索java某方法不想多次被调用，如何处理
			@Override
			public void run() {
				isTurning = true;

				while (!(Math.abs(curDiff) < 2 && Math.abs(rudderAngle) < 2) && isTurning) {
					curDiff = desDir - head;
					if (curDiff > 180) {
						curDiff -= 360;
					} else if (curDiff < -180) {
						curDiff += 360;
					}

					float rudderDiff = kp * (curDiff - lastDiff) + (kp / ki) * curDiff
							+ kp * kd * (curDiff - 2 * lastDiff + preDiff);
					rudderAngle += rudderDiff;
					System.out.println("turnTo method -- > rudderDiff" + rudderDiff);
					System.out.println("rudder-->" + rudderAngle);

					if (rudderAngle > 35 || rudderAngle < -35) {
						rudderAngle -= rudderDiff;
					}
					// rotateRate = (float) (K*rudderAngle*(1-Math.exp(-2/T))); //旋转速率
					preDiff = lastDiff;
					lastDiff = curDiff;

					try {
						Thread.sleep(500);
					} catch (InterruptedException ex) {
						Logger.getLogger(Navigator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				Navigator.this.rudderAngle = 0; // 前面判断精度不够，需要在这里还原
				System.out.println("turn head complete!"); // 这里有一个问题，如果中途多次调用，岂不是混乱？
			}
		});

	}

	@Override
	public void speedTo(float toSpeed) { // 决策的地方应当将决策作为全局，可以检测当前是否有决策
		double diff = toSpeed - Navigator.this.speed;
		speed += diff / 4;
		if (isSpeeding) {
			isSpeeding = false;
		}

		Thread speeding = new Thread(new Runnable() {
			@Override
			public void run() {
				isSpeeding = true;
				while (Math.abs(Navigator.this.speed - toSpeed) < 0.5 && isSpeeding) {
					double diff = toSpeed - Navigator.this.speed;
					speed += diff / 4;
					try {
						Thread.sleep(500);
					} catch (InterruptedException ex) {
						Logger.getLogger(Navigator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		});

	}

	@Override
	public void accelerate(float toSpeed) {
		// 加速
		new Thread(new Runnable() {
			@Override
			public void run() { // 这里的计算有点不合理，应当使用阻力公式进行计算
				while (Navigator.this.speed < toSpeed) {
					speed += 1;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						Logger.getLogger(Navigator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}).start();
	}

	@Override
	public void decelerate(float toSpeed) {
		// 减速
		new Thread(new Runnable() {
			@Override
			public void run() { // 这里的计算有点不合理，应当使用阻力公式进行计算
				while (Navigator.this.speed > toSpeed) {
					speed -= 1.5;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						Logger.getLogger(Navigator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}).start();
	}

	@Override
	public void stop() {
		// 速度逐渐减为0
		new Thread(new Runnable() {
			@Override
			public void run() { // 这里的计算有点不合理，应当使用阻力公式进行计算
				while (speed > 0) {
					speed -= 2;
					try {
						Thread.sleep(500);
					} catch (InterruptedException ex) {
						Logger.getLogger(Navigator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}).start();
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// 规则实现的方法
	@Override
	public boolean canDo(Situation situation) {
		return true;
	}

	@Override
	public Option shouldDo(Situation situation) { // 生成操纵动作表示链
		// 根据环境生成操纵链
		return this.option;
	}

	@Override
	public void setRule(Rule rule) { // 设置本船应当遵守的规则为Rule，所以在本类中应当有Rule局部变量
		// 这里可以实现用户自定义规则
	}

	// --------------------------通信部分，主动发送信息，被动接收信息---------------
	public void dealCom(MessageType message) { // 相当于接收处理然后调用发送线程发送一个
		// 调用ComThrad中的方法，可以按照规则启动处理线程-----------这个方法是让
		comThread.messages.add(message);
//        if (!comThread.isAlive()) {
//            comThread.start();  //开启消息队列处理程序
//        }
	}

	/**
	 * @param to
	 * @param content
	 */
	public void sendToSingle(String to, String content) {
		// 发送信息到特定的对象
		comThread.sendToSingle(to, content);
	}

	public void sendToSome(List<LocalVessel> some, String content) {
		// 发送到相关的周边航行器中
		comThread.sendToSome(some, content);
	}

	public void sendToAll(String content) {
		comThread.sendToAll(content);
		System.out.println("向所有航行器发送消息，开始协商程序");
	}

	public List<Vessel> getOthers() {
		return this.otherNavs;
	}

	public List<LocalVessel> getLocals() {
		return this.locals;
	}
	/************************ 在单独的线程类中可以处理 ***********************************/
	// --------------------------通信方法部分 结束

	/*********************************
	 * Attribute
	 ***********************************/
	public void setStaticAttribute(String idNumber, String name, int navLength, int beam, char type,
			String destination) {
		this.idNumber = idNumber;
		this.name = name;
		this.navLength = navLength;
		this.beam = beam;
		this.type = type;

		String[] point = destination.split(",");
		Point2D des = new Point2D(Double.parseDouble(point[0]), Double.parseDouble(point[1]));
		// System.out.println(des.toString());
		this.destination = des;

		this.setPrefWidth(this.navLength);
		this.setPrefHeight(this.beam);
		if (this.type == 'a') { // 根据不同的类型选择不同的样式
			this.setStyle("-fx-background-color : red;");
		} else if (this.type == 'b') {
			this.setStyle("-fx-background-color : blue;");
		}
		this.setTooltip(new Tooltip(this.idNumber));
	}

	public void setDynAttribute(float head, float course, float speed, float longitude, float latitude, char state,
			String updateTime, float rudderAngle) {
		this.head = head; // 更改这里，显示并计算争取的方向
		this.course = course;
		this.speed = speed;
		this.longitude = longitude;
		this.latitude = latitude;
		this.state = state;
		this.updateTime = updateTime;
		this.rudderAngle = rudderAngle;

		this.setLayoutX(longitude - this.navLength / 2); // 先位置，后旋转角度
		this.setLayoutY(latitude - this.beam / 2);
		this.setRotate(head - 90);

		// this.lastSpeedDecision = this.speed;
	}

	/********************* 动态信息处理 **********************************************/
	public synchronized void addDynInfo(DynInfo dynInfo) {
		dynInfos.add(dynInfo);
	}

	public List<DynInfo> getDynInfos() { // 获取当前内存中的--所有--动态信息
		return dynInfos;
	}

	public DynInfo getLastDynInfo() {
		return dynInfos.get(dynInfos.size());
	}

	/*********************************
	 * 特殊计算区域
	 **************************************/
	public float getRatio2(float x, float y) { // 坐标系在左下角的情况下，计算坐标系内一点与原心形成的夹角，以正向上为0 度计算的
		// 以左下角为原点的坐标系
		double ratio;
		ratio = Math.atan2(y, x);
		ratio = Math.toDegrees(ratio);
		if (ratio >= 0) {
			// 在上边，是正值
			if (ratio <= 90) {
				ratio = 90 - ratio;
			} else {
				ratio = 450 - ratio;
			}
		} else {
			ratio = 90 - ratio;
		}

		return (float) ratio;
	}

	public float getRatio(float x, float y) { // 计算一点 与 原点的斜率-------程序坐标，左上角
		double ratio = 0;
		int adjust = 0;// switch case///

		if (x == 0 && y == 0)
			adjust = 0;
		else if (x >= 0 && y < 0)
			adjust = 1;
		else if (x < 0 && y <= 0)
			adjust = 2;
		else if (x <= 0 && y > 0)
			adjust = 3;
		else if (x > 0 && y >= 0)
			adjust = 4;

		switch (adjust) {
		case 0:
			ratio = 0;
			break;
		case 1:
			ratio = 450 - Math.toDegrees(Math.atan2(-y, x));
			break;
		case 2:
			ratio = 90 - Math.toDegrees(Math.atan2(-y, x));
			break;
		case 3:
			ratio = 90 - Math.toDegrees(Math.atan2(-y, x));
			break;
		case 4:
			ratio = 90 - Math.toDegrees(Math.atan2(-y, x));
			break;
		}

		while (ratio < 0 || ratio >= 360) {
			if (ratio < 0)
				ratio += 360;
			if (ratio >= 360)
				ratio -= 360;
		}
		return (float) ratio;
	}

	public float getRatio(double start_x, double start_y, double end_x, double end_y) {
		// 这里返回角度是度，不是弧度
		double differentx = end_x - start_x;
		double differenty = end_y - start_y;
		double course = 0;
		int adjust = 0;// switch case///

		if (differentx == 0 && differenty == 0) {
			adjust = 0;
		} else if (differentx >= 0 && differenty < 0) {
			adjust = 1;
		} else if (differentx < 0 && differenty <= 0) {
			adjust = 2;
		} else if (differentx <= 0 && differenty > 0) {
			adjust = 3;
		} else if (differentx > 0 && differenty >= 0) {
			adjust = 4;
		}

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
		default:
			System.err.println("calcute error!!");
		}

		while (course < 0 || course >= 360) {
			if (course < 0) {
				course += 360;
			}
			if (course >= 360) {
				course -= 360;
			}
		}
		return (float) course;
	}

	@Override
	public String toString() {
		return "Navigator{" + "idNumber=" + idNumber + ", name=" + name + ", navLength=" + navLength + ", beam=" + beam
				+ ", head=" + head + ", course=" + course + ", speed=" + speed + ", latitude=" + latitude
				+ ", longitude=" + longitude + '}';
	}

	public void voyageReturn() {
		lastHeadDecision = headDecision;
		headDecision = calAngle(destination.getX() - this.longitude, destination.getY() - this.latitude);
		pinRudder((float) headDecision);

		pinSpeed((float) lastSpeedDecision);
		lastSpeedDecision = 0;
	}

	/**
	 * @param dx 输入x点坐标
	 * @param dy 输入y点坐标
	 * @return 返回的角度，向上为0度角
	 */
	public double calAngle(double dx, double dy) { // 向上是0度角，顺时针旋转
		double theta = Math.atan2(dy, dx);
		theta += Math.PI / 2;
		double angle = Math.toDegrees(theta);
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

}
