package org.simulation.shipserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import org.simulation.common.Ship;


public class ServerThread extends Thread {  //1秒小同步，5秒一大同步
	
	private ServerSocket serversocket;
	private boolean logOut = false;
	
	private SmallPanel smallpanel;
	private List<Ship> clientShips;
	private List<Ship> serverShips;
	private List<Socket> sockets;  //在这个类中只要不new一个新的链表，那它就是指向了引用的套接字聊表
	
	public ServerThread(SmallPanel smallpanel, List<Ship> clientShips, List<Ship> serverShips,
			List<Socket> sockets) {  //怎么解决超界限的问题，下一个版本中要加入地球模型
		super();
		this.clientShips = clientShips;  //得到数据引用，并没有创建新的对象
		this.serverShips = serverShips;
		this.smallpanel = smallpanel;
		this.sockets = sockets;
	}
	
	@Override
	public void run() {  //server总线程
		super.run();
		try {
			serversocket = new ServerSocket(6000);  //打开服务端口，接受客户端请求
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		/******************服务端创建船舶进行同步************************************/
		new Thread() {
			@Override
			public void run() {  //对服务端的船舶对象同步
				while (!logOut) {
					int loop=0;
					if (loop/5<1) {
						for (Ship ship : serverShips) {  //难道就没有简单的办法吗？感觉这样计算量太大了
							ship.goAhead();  //本地前进一步
							if (loop/serverShips.size()<1) {  //可以这样，默认1秒前进一次，每隔5秒同步一次
								for (Socket sk : sockets) {
									try {
										sendData(sk, ship.getName()+",go");
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								loop++;
							}
						}
					}else{
						for(Ship ship: serverShips){
							ship.goAhead();
							for (Socket sk : sockets) {
								Sync(sk,
									ship.getName(),
									ship.getParameter(1),
									ship.getParameter(2),
									ship.getParameter(3),
									ship.getParameter(4)
								);
							}
						}
						loop=0;
					}
					Iterator<Socket> items = sockets.iterator();
					while(items.hasNext()){  //检测有没有关闭的客户端
						Socket it = items.next();
						if (it.isClosed()) {
							items.remove();  //移除当前指向
						}
					}
					smallpanel.repaint();
					try {
						sleep(1000);  //要和本地相同的刷新率
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		/****************来一个客户端就接受，然后新建线程处理***************************************/
		while (!logOut) {
			try {
				Socket newsocket = serversocket.accept();
				sockets.add(newsocket);
				
				/*Iterator<Socket> items = sockets.iterator();
				while(items.hasNext()){  //检测有没有关闭的客户端
					Socket it = items.next();
					if (it.isClosed()) {
						items.remove();  //移除当前指向
					}
				}*/
				new Thread() {  //每接收到一个新客户端，就开启一个新线程针对这个新的客户端
					@Override
					public void run() {
						super.run();
						Socket socket = sockets.get(sockets.size() - 1);
						
						String getData = null;
						String[] change = null;
						String name = null;  //根据名字判断是不是自己
						BufferedReader input = null;
						//PrintWriter output = null;
						try {
							input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							//output = new PrintWriter(socket.getOutputStream());
						} catch (IOException exception) {
							exception.printStackTrace();
						}
						while (!socket.isClosed()) {  //本线程对应的客户端线程
							try {
								getData = input.readLine();  //拿到数据
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if (getData==null) {
								continue;
							}
							change = getData.split(",");  //将接收的数据分离，并准备解析
							name=change[0];
							if (change[1].equals("logIn")) {  //这里处理本地的事情
								//既然是新的客户端，那一定是登陆进来的
								clientShips.add(new Ship(change[0], Double.parseDouble(change[2]), Double.parseDouble(change[3]),
										Double.parseDouble(change[4]), Double.parseDouble(change[5]), change[6]));
								//logIn(getData);  //发布登录信息
							}else if (change[1].equals("logOut")) {
								Iterator<Ship> s = clientShips.iterator();
								while(s.hasNext()){
									Ship get = s.next();
									if (get.getName().equals(name)) {
										s.remove();
										break;
									}
								}
								Iterator<Socket> items = sockets.iterator();
								while(items.hasNext()){  //检测有没有关闭的客户端
									Socket it = items.next();
									if (it.isClosed()) {
										items.remove();  //移除当前指向
									}
								}
								try {
									input.close();
									socket.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								break;
							} else if (change[1].equals("speed")) {
								//speed通信格式
								Iterator<Ship> s = clientShips.iterator();
								while(s.hasNext()){
									Ship get = s.next();
									if (get.getName().equals(name)) {
										get.setValue(4, get.getParameter(4)+Double.parseDouble(change[2]));
										break;
									}
								}
							} else if (change[1].equals("course")) {
								//course通信格式
								Iterator<Ship> s = clientShips.iterator();
								while(s.hasNext()){
									Ship get = s.next();
									if (get.getName().equals(name)) {
										get.setValue(3, get.getParameter(3)+Double.parseDouble(change[2]));
										break;
									}
								}
							} else if (change[1].equals("go")) {
								//本地前进一步
								Iterator<Ship> s = clientShips.iterator();
								while(s.hasNext()){
									Ship get = s.next();
									if (get.getName().equals(name)) {
										get.goAhead();
										break;
									}
								}
							}
							//所有的动作都对本船无关，只需要发送到其他客户端即可,由客户端根据情况处理
							for (Socket sk : sockets) {
								try {
									sendData(sk, getData);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							smallpanel.repaint();
						}
					}
				}.start();
				
				Iterator<Socket> items = sockets.iterator();
				while(items.hasNext()){  //检测有没有关闭的客户端
					Socket it = items.next();
					if (it.isClosed()) {
						items.remove();  //移除当前指向
					}
				}
				
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void serverClose(){
		for(Ship ship:clientShips){  //应该让客户端具有离线运行的能力
			for(Socket sk:sockets){
				if (!sk.isClosed()) {
					try {
						sendData(sk, ship.getName()+"kickOut");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void sendData(Socket socket, String data) throws IOException {
		if (!socket.isClosed()) {
			PrintWriter output = new PrintWriter(socket.getOutputStream());
			output.println(data);
			output.flush();
		}
	}
	
	public String getData(Socket socket) throws IOException {
		BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String data = input.readLine();
		return data;
	}
	//单个对象进行同步
	//登录信息可以作为大同步,把login替换成sync
	public void Sync(Socket socket, String name, double x,double y,double course, double speed){
		//大同步，5秒一次
		String command = name +",sync,"+x+","+y+","+course+","+speed;
		try {
			sendData(socket, command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//2017.3.7  服务端无权踢出用户
	/*public void kickOut(String name){  //服务端将某个客户端踢出
		for(int i=0;i<sockets.size();i++){
			//给客户端发送提出信息，客户端关闭发送端口，退出程序
			try {
				sendData(sockets.get(i), "kickout,"+name);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("kick out "+name);
	}*/
}







