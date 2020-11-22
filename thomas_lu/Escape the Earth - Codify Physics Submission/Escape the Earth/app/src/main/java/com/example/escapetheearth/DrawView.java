package com.example.escapetheearth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import static java.lang.Math.*;

public class DrawView extends View {
    // SIMULATION VARIABLES
    private final int DT = 5; // s
    private final int DAYS = 20; // days
    private final int maxSteps = (int)((24.0 * DAYS * 60 * 60 ) / DT);
    private final int timeSteps = 1000; // number of dt's to integrate through for updated drawing
    private float scaling = 1000000; // 1 dp is 1 million meters. Divides real vals.
    private double vScaling = 100000; // multiplies velocity before dividing
    private double aScaling = 1.5e+5*vScaling; // multiplies accel before dividing
    private double rScaling = 15; // multiplies r before dividing

    // PHYSICAL CONSTANTS
    private final double G = 6.674e-11; // m^3 kg^-1 s^-2
    private final double RE = 6.371e+6; // m
    private final double ME = 5.972e+24; // kg
    private final double RM = 1.7374e+6; // m
    private final double MM = 7.349e+22; // kg

    private final double DEM = 3.844e+8; // m, initial distance from earth to moon
    private final double VM = 1023.157; // m/s, initial speed of moon

    // OTHER GLOBALS
    private Paint p;
    private Body[] bodies;
    private int totalSteps = 0;
    boolean started = false; // has simulation been played?
    boolean stopped = false; // if simulation has been played, has it been stopped without reset?
    double v0 = 1000; double ang0 = 40.0; // rocket initial starting states

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        p = new Paint();
        BitmapFactory bf = new BitmapFactory();

        bodies = new Body[3];
        Bitmap earthBm = bf.decodeResource(getResources(),R.drawable.earth);
        bodies[0] = new Body(0,0,0,0, RE, ME, earthBm,true); // earth
        Bitmap moonBm = bf.decodeResource(getResources(),R.drawable.moon);
        bodies[1] = new Body(DEM, 0, 0, VM, RM, MM, moonBm,false); // moon
        bodies[2] = new Body(DEM,0,0,0,0.5*RM,1,Color.GRAY,false); // rocket - mass is negligible
        resetRocket(); // set according to current input (500 m/s, 0.0 deg)

        updateAccel(bodies);
    }

    // onDraw: called repeatedly; the driver of app animation
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawARGB(255, 255,255,255); // set background to white

        if(started && !stopped){
            updateBodies(bodies, timeSteps);
            totalSteps+=timeSteps;
        }

        showBodies(canvas);

        // Text at top left of screen
        p.setColor(Color.BLACK);
        int pTextSize = 50;
        p.setTextSize(pTextSize);
        // Time text
        canvas.drawText("Time: " + ((totalSteps * DT)/(60*60*24)) + " days", 10, pTextSize + 10, p);
        // Crash text, if applicable
        if(stopped)
            canvas.drawText("Crashed!", 10, 2*pTextSize + 20, p);

        if(totalSteps < maxSteps) {
            invalidate();
        }
        else { // reached end of simulation, see if escaped
            p.setColor(Color.BLACK);
            double vf = bodies[2].getV(); // velocity at maxSteps time
            double ve = sqrt(2*G*ME/distance(bodies[0],bodies[2])); // escape velocity from earth
            if(vf > ve){
                canvas.drawText("Escape velocity reached!! v = " + (int)vf + " m/s.", 10, 2*pTextSize + 20, p);
            }
            else{
                canvas.drawText("Didn't reach escape velocity. v = " + (int)vf + " m/s.", 10, 2*pTextSize + 20, p);
            }
        }
    }

    // distance: simple distance calculation
    private double distance(Body b1, Body b2){
        return Math.sqrt(Math.pow(b2.getX()-b1.getX(), 2) + Math.pow(b2.getY()-b1.getY(), 2));
    }

    /*** BUTTON PRESS METHODS ***/
    // startClick: begins simulation if not already begun
    public void startClick(){
        if(!started){
            started = true;
        }
    }

    // resetClick: resets simulation if started
    public void resetClick(){
        if(started){
            started = false;
            stopped = false;
            totalSteps = 0;

            Body moon = bodies[1];
            moon.setX(DEM); moon.setY(0);
            moon.setVx(0); moon.setVy(VM);
            moon.setAx(0); moon.setAy(0);
            resetRocket();

            invalidate();
        }
    }

    // updateAngle: gets newAng from MainActivity and resets rocket conditions
    public void updateAngle(double newAng){
        ang0 = newAng;
        if(!started) // don't reset during simulation
            resetRocket();
    }

    // updateVelocity: gets newV from MainActivity and resets rocket conditions
    public void updateVelocity(double newV){
        v0 = newV;
        if(!started) // don't reset during simulation
            resetRocket();
    }

    // resetRocket: resets rocket to initial state; helper for button methods
    private void resetRocket(){
        Body rocket = bodies[2];
        rocket.setAx(0); rocket.setAy(0);
        double heading = ((ang0%360)/180.0)*PI;
        // set X and Y: angle is CCW from line from moon to earth; distance is 4 moon radii from moon start pos
        rocket.setX(DEM - (rScaling+5) * RM * cos(heading)); // using rScaling makes it appear as though rocket starts on moon surface
        rocket.setY(-(rScaling+5) * RM * sin(heading));
        // set vx and vy: same angle as position
        rocket.setVx(-1*v0*cos(heading));
        rocket.setVy(-1*v0*sin(heading));
    }

    /*** PHYSICS UPDATE METHODS ***/
    // updateBodies: update positions, accelerations, and velocities of all bodies
    private void updateBodies(Body[] bodies, int timeSteps){
        for(int tstep = 0; tstep < timeSteps; tstep++) {
            updatePositions(bodies);
            updateAccel(bodies);
            updateVelocities(bodies);
        }
        checkCollision(bodies);
    }

    // updateAccel: update accelerations of all bodies by using positions and masses
    private void updateAccel(Body[] bodies){
        // for all pairs of bodies, calculate mutual attraction and update accelerations
        for(Body b : bodies){
            b.setAx(0); b.setAy(0);
        }

        for(int i1 = 0; i1 < bodies.length - 1; i1++){
            for(int i2 = i1+1; i2 < bodies.length; i2++){
                Body b1 = bodies[i1]; Body b2 = bodies[i2];
                double r = distance(b1,b2);
                double fg = G*b1.getM()*b2.getM()/Math.pow(r,2);

                double dAx1 = (fg/b1.getM()) * (b2.getX()-b1.getX())/r; // total accel times cos
                double dAy1 = (fg/b1.getM()) * (b2.getY()-b1.getY())/r; // total accel times sin
                double dAx2 = (fg/b2.getM()) * (b1.getX()-b2.getX())/r;
                double dAy2 = (fg/b2.getM()) * (b1.getY()-b2.getY())/r;

                b1.setAx(b1.getAx() + dAx1);
                b1.setAy(b1.getAy() + dAy1);
                b2.setAx(b2.getAx() + dAx2);
                b2.setAy(b2.getAy() + dAy2);
            }
        }
    }

    // updateVelocities: update velocities of all bodies by using current acceleration
    private void updateVelocities(Body[] bodies){
        for(Body b : bodies){
            b.setVx( (b.getVx() + b.getAx()*DT) );
            b.setVy( (b.getVy() + b.getAy()*DT) );
        }
    }

    // updateVelocities: update positions of all bodies by using current velocity
    private void updatePositions(Body[] bodies){
        for(Body b : bodies){
            b.setX( (b.getX() + b.getVx()*DT) );
            b.setY( (b.getY() + b.getVy()*DT) );
        }
    }

    // checkCollision: if two bodies are within their original radii, stop the simulation
    private void checkCollision(Body[] bodies){
        for(int i1 = 0; i1 < bodies.length - 1; i1++) {
            for (int i2 = i1 + 1; i2 < bodies.length; i2++) {
                if(distance(bodies[i1],bodies[i2]) < (bodies[i1].getR0()+bodies[i2].getR0())){
                    stopped = true;
                }
            }
        }
    }

    /** DRAWING HELPER METHODS ***/
    // showBodies: calls draw on every body!
    private void showBodies(Canvas canvas){
        for(Body b : bodies){
            b.draw(canvas, p);
        }
    }

    // scaledX: returns x value for drawing on canvas based on real x value
    private float scaledX(double x){
        return (float)(getWidth()/2.0 + x/scaling);
    }

    // scaledY: returns y value for drawing on canvas based on real y value
    private float scaledY(double y){
        return (float)(getHeight()/2.0 - y/scaling);
    }

    /**
     * Draw an arrow
     * @author Steven Roelants 2017
     * Obtained from https://stackoverflow.com/questions/6713757/how-do-i-draw-an-arrowhead-in-android
     */
    private void drawArrow(Paint paint, Canvas canvas, float from_x, float from_y, float to_x, float to_y)
    {
        float angle,anglerad, radius, lineangle;

        //values to change for other appearance *CHANGE THESE FOR OTHER SIZE ARROWHEADS*
        radius=30f;
        angle=35f;

        //some angle calculations
        anglerad= (float) (PI*angle/180.0f);
        lineangle= (float) (atan2(to_y-from_y,to_x-from_x));

        //tha line
        canvas.drawLine(from_x,from_y,to_x,to_y,paint);

        //tha triangle
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(to_x, to_y);
        path.lineTo((float)(to_x-radius*cos(lineangle - (anglerad / 2.0))),
                (float)(to_y-radius*sin(lineangle - (anglerad / 2.0))));
        path.lineTo((float)(to_x-radius*cos(lineangle + (anglerad / 2.0))),
                (float)(to_y-radius*sin(lineangle + (anglerad / 2.0))));
        path.close();

        canvas.drawPath(path, paint);
    }

    /**
     * BODY CLASS
     * Stores physical and display variables for physics bodies
     * Contains doubles for position, velocity, and acceleration in x and y
     * as well as mass and radius. r0 represents original unscaled radius, as r is scaled for visibility on screen.
     * Display variables are color (used if shown as circle) and bm (used if bitmap sprite is provided).
     * Finally, fixed variable dictates if body responds to acceleration or stays put.
     **/
    private class Body{ // everything here is double
        private double x,y;
        private double vx,vy; // always for current x, y
        private double ax,ay; // always for current x, y
        private double r, m;
        private double r0;
        int color;
        Bitmap bm;
        boolean fixed;

        // Default constructor
        public Body(){
            x=0;y=0;vx=0;vy=0;r=100*rScaling;r0=100;m=100;ax=0;ay=0;color=0xFF000000; fixed = false;
            Bitmap bm = null;
        }
        // Color is provided; draws as circle
        public Body(double X, double Y, double Vx, double Vy, double R, double M, int c, boolean fix){
            x=X;y=Y;r=R*rScaling;m=M;r0=R;
            ax=0;ay=0;
            color = c;
            Bitmap bm = null;
            fixed = fix;
        }
        // Bitmap is provided; draws as sprite
        public Body(double X, double Y, double Vx, double Vy, double R, double M, Bitmap bmap, boolean fix){
            x=X;y=Y;vx=Vx;vy=Vy;r=R*rScaling;m=M;r0=R;
            ax=0;ay=0;
            color = 0xFF222222; // set base as slightly gray
            bm = Bitmap.createScaledBitmap(bmap, (int)(r*2/scaling), (int)(r*2/scaling), false);
            fixed = fix;
        }

        // draw: displays body and velocity/acceleration vectors on given canvas
        public void draw(Canvas canvas, Paint p){
            if(bm == null){ // draw circle
                p.setColor(color);
                canvas.drawCircle(scaledX(x),scaledY(y),(float)r/scaling,p);
            }
            else{ // draw sprite
                canvas.drawBitmap(bm, scaledX(x-r), scaledY(y+r), p);
            }

            // draw vectors if not fixed
            if(!fixed){
                // velocity
                p.setColor(Color.BLACK);
                drawArrow(p, canvas, scaledX(x), scaledY(y),
                        scaledX(x + vx*vScaling),
                        scaledY(y + vy*vScaling)
                );
                // acceleration
                p.setColor(Color.RED);
                drawArrow(p, canvas, scaledX(x), scaledY(y),
                        scaledX(x + ax*aScaling),
                        scaledY(y + ay*aScaling)
                );
            }
        }

        // Getters and Setters

        public double getV(){
            return sqrt(vx*vx+vy*vy);
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            if(!fixed)
                this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            if(!fixed)
                this.y = y;
        }

        public double getVx() {
            return vx;
        }

        public void setVx(double vx) {
            this.vx = vx;
        }

        public double getVy() {
            return vy;
        }

        public void setVy(double vy) {
            this.vy = vy;
        }

        public double getAx() {
            return ax;
        }

        public void setAx(double ax) {
            this.ax = ax;
        }

        public double getAy() {
            return ay;
        }

        public void setAy(double ay) {
            this.ay = ay;
        }

        // theoretically unchanging physical variables, but here just in case
        public double getR() {
            return r;
        }

        public void setR(double r) {
            this.r = r;
        }

        public double getM() {
            return m;
        }

        public void setM(double m) {
            this.m = m;
        }

        public double getR0() {
            return r0;
        }

        public void setR0(double r0) {
            this.r0 = r0;
        }

        // other, for drawing
        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }
}
