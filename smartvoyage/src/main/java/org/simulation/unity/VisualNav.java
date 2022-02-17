
package org.simulation.unity;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;

public class VisualNav {
    
    public String visualID;
    public Point2D position;
    public Point2D velocity;
    public double head;
    public Circle area;
    
    public VisualNav(String visualID, double x, double y, Point2D velocity, Circle area){
        this.visualID = visualID;
        this.position = new Point2D(x, y);
        this.velocity = velocity;
        this.head = calHead(velocity.getX(), velocity.getY());
        this.area = area;
    }
    
    public double getX(){
        return this.position.getX();
    }
    public double getY(){
        return this.position.getY();
    }
    public double getSpeed(){
        return Math.sqrt( velocity.getX()*velocity.getX() + velocity.getY()*velocity.getY() );
    }
    public Circle getArea(){
        return this.area;
    }
    public void setArea(double centerX, double centerY, double radius){
        this.area.setCenterX(centerX);
        this.area.setCenterY(centerY);
        this.area.setRadius(radius);
    }
    
    private double calHead(double x, double y){
        double theta = Math.atan2(y, x);
        double angle = Math.toDegrees(theta);
        if (angle < 0) {
            angle += 360;
        }
        return angle;  //返回航向
    }
    
    public boolean isColliding(Circle other){
        return this.area.getBoundsInParent().intersects(other.getBoundsInParent());
    }
    
    @Override
    public String toString() {
        return "LocalVessel{" + "id=" + visualID
                + ", longitude=" + position.getX() + ", latitude=" + position.getY()
                + ", head=" + head + ", speed=" + getSpeed() + ", area=" + getArea().toString() + '}';
    }
}





