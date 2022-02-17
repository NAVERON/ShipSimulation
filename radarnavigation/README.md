# 关于

船舶避碰仿真的第3个版本, 实现服务端，客户端位置等信息的通信交换    

## 功能改进  

- [x] 借助之前的仿真第2个版本，实现了此版本的服务端，并增加通信功能  
- [x] 增加以雷达形式展示的客户端，与服务端通信，交换位置，航向航速等信息  
- [x] 多线程实现。每个对象单独作为一个线程更新界面却由独立的线程刷新，多线程协作模式  

## 当前存在的问题

1. 多线程模式下效率存在问题，界面卡顿感导致使用感受不佳  
2. 多线程的协作没有信号量，而是使之间没有关系才使各个组件之间没有出现问题，对程序异构影响不好
3. 结构和算法缺失，单独使用通信作为各个子系统的沟通桥梁  
4. 没有从本质上改进显示形式，依然是重绘和不断的刷新界面  

# 工程结构介绍

## 思路 

分为两部分，服务端和客户端，ui组建部分公用，使用socket接口通信，没有使用上层的tcp/udp,借助socket实现了类似tcp的确认功能  
主要的两个类为Shipanager和Radaravigation，分别启动各自的通信线程  
服务端时刻舰艇socket特定的端口，客户端创建后在刷新界面的时候与服务端通信  

## Bugs 

- [ ] 组件绘制方式使用Panel重绘, 不算的删除重新绘制, 创作思路和显示效果都不好  
- [ ] 多线程协作问题，金肯呢个保证线程人物独立，没有关联，否则使用信号量/锁实现  





