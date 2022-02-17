/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simulation.environment.MessageType;
import org.simulation.navigator.Navigator;
import org.simulation.navigator.Vessel;
import org.simulation.unity.LocalVessel;

/**
 * @function 实现 对象向3服务线程4发送信息
 * @author NAVERON
 */

//客户端通信线程  调用服务对象将信息放入队列中
public class ComThread extends Thread {
	// 需要有两个队列，一个代表发送的信息，另一个代表接收的信息 -- 2017.5.31 --> remove this
	public ArrayBlockingQueue<MessageType> messages = new ArrayBlockingQueue<>(100); // 个体对象消息队列

	// 通过回调传递参数
	Navigator navigator = null;

	public ComThread(Navigator navigator) { // 初始化的过程中应当将该对象this传递近来，然后就可以调用Navigator的方法了
		// 初始化过程中处理
		this.navigator = navigator;
		setDaemon(true);
	}

	// 主动发送，被动接收-----------------
	@Override
	public void run() { // 航行器在将接收的message加入到messages后，开启线程并处理
		// super.run();
		MessageType message = null;

		while (navigator.comRunning) { // 针对每一个信息单独分析，还是求解可行域，最后求交集
			if (messages.isEmpty()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) {
					Logger.getLogger(ComThread.class.getName()).log(Level.SEVERE, null, ex);
				}
				continue;
			}
			try {
				message = messages.take();
				// System.out.println(message.toString());
			} catch (InterruptedException ex) {
				Logger.getLogger(ComThread.class.getName()).log(Level.SEVERE, null, ex);
			}
			// 根据协商协议的规则 --- 分析messge内容 --> 需要发送就调用发送方法，其实就是在服务端队列中添加消息
			// 通信分析的结果，优先级较高
			// 找出发信人的信息
			LocalVessel get = null;
			for (LocalVessel next : navigator.locals) {
				if (next.id.equals(message.getFrom())) {
					get = next;
					navigator.isCom = true;

					break;
				}
			}
			if (message.getContent().equals("bow")) {
				// 船首过
				if (get.ratio > -30 && get.ratio < 0) {
					navigator.comHeadDecision = -10;
				} else if (get.ratio > 0 && get.ratio < 90) {
					navigator.comHeadDecision = get.ratio > navigator.headDecision ? get.ratio : navigator.headDecision;
				} else if (get.ratio > 210 && get.ratio < 330) {
					navigator.comHeadDecision = -10;
				} else {
					navigator.comHeadDecision = 0;
				}
			} else if (message.getContent().equals("astern")) {
				// 船尾过
				if (get.ratio > -30 && get.ratio < 0) {
					navigator.comSpeedDecision = 1;
				} else if (get.ratio > 0 && get.ratio < 90) {
					if (navigator.one.isEmpty()) {
						navigator.comHeadDecision = -10;
					} else {
						// navigator.speedDecision = 1;
						break;
					}
				} else if (get.ratio > 210 && get.ratio < 330) {
					if (navigator.headDecision > 0) {
						navigator.comHeadDecision = navigator.headDecision;
					} else {
						// navigator.speedDecision = 1;
						break;
					}
				} else {
					navigator.comHeadDecision = -10;
				}
			}
		}
	}

	public boolean sendToSingle(String toId, String content) {
		navigator.isCom = true;
		boolean isOk = false;
		String fromId = navigator.getIdNumber();

		MessageType message = new MessageType(fromId, toId, content);
		ComServer.getInstance().addQueue(message);

//        if (ComServer.getInstance().isAlive()) {  //需要判断线程处理是否正在进行，如果存在，就不需要再次调用了
//            isOk = true;
//        }else{
//            ComServer.getInstance().start();  //处理这个发送信息，调用服务程序，处理信息表
//            isOk = true;
//        }

		return isOk; // 需要返回确认吗？应该不需要
	}

	public boolean sendToSome(List<LocalVessel> some, String content) {
		navigator.isCom = true;
		boolean isOk = false;

		for (Iterator<LocalVessel> items = some.iterator(); items.hasNext();) {
			ComServer.getInstance().addQueue(new MessageType(navigator.getIdNumber(), items.next().id, content));
		}

//        if (ComServer.getInstance().isAlive()) {  //需要判断线程处理是否正在进行，如果存在，就不需要再次调用了
//            isOk = true;
//        }else{
//            ComServer.getInstance().start();  //处理这个发送信息，调用服务程序，处理信息表
//            isOk = true;
//        }
		return isOk;
	}

	public boolean sendToAll(String content) {
		navigator.isCom = true;
		boolean isOk = false;
		String fromId = navigator.getIdNumber();

		for (Iterator<Vessel> items = navigator.getOthers().iterator(); items.hasNext();) {
			Vessel next = items.next();
			if (!next.getIdNumber().equals(navigator.getIdNumber())) {
				ComServer.getInstance().addQueue(new MessageType(fromId, next.getIdNumber(), content));
			}
		}
//        if (ComServer.getInstance().isAlive()) {  //需要判断线程处理是否正在进行，如果存在，就不需要再次调用了
//            isOk = true;
//        }else{
//            ComServer.getInstance().start();  //处理这个发送信息，调用服务程序，处理信息表
//            isOk = true;
//        }

		return isOk;
	}

}
