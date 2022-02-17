/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simulation.environment;

public interface Rule {  //接口像一个规范，强制实现它的类必须实现所有必要方法，避免实现部分方法导致不能使用
    
    public boolean canDo(Situation situation);  //判断当前动作是否正确
    public Option shouldDo(Situation situation);  //根据传进来的参数,判断应当怎么做
    public void setRule(Rule rule);  //怎么表示当前的交通规则？再想想
    
}
