package mha;
import java.awt.*;
import java.util.*;

public class trafficControl extends java.applet.Applet implements Runnable{
	Dimension offDimension;
	Image bgImage,fgImage;
	Light L;
	Traffic T;
	TextField countText,scoreText,XYpos;
	int numLight=3,numSection=numLight+1;
	String rts,STR[]={"Reset","Start","Time","maxVelocity","Acceleration","delayTime","lighttime"};
	Color bgColor=new Color(0xE0,0xE0,0xE0);//new Color(0xC8,0xDF,0xD0);
	public void init(){
		setBackground(bgColor);
		for(int i=0;i<STR.length;i++){
			if((rts=getParameter(STR[i]))!=null)
				STR[i]=new String(rts);
		}
		Panel p=new Panel();
		p.add(new Label("N1/N2"));
		p.add(countText=new TextField("0/0/0.00",12));
		countText.setEditable(false);
//		p.add(new Label("T="));
//		p.add(timeText=new TextField("0",3));
//		timeText.setEditable(false);
		p.add(new Label("n1/n2"));
		p.add(scoreText=new TextField("0/0/0.00",9));
		scoreText.setEditable(false);
		p.add(new Button(STR[1]));
		p.add(new Button(STR[0]));
		add("North",p);
		L=new Light(numLight,STR);
		T=new Traffic(L);
		String str;
		if((str=getParameter("accelerationValue"))!=null)
			T.a=Double.valueOf(str).doubleValue();
		if((str=getParameter("vmax"))!=null)
			car.vmax=Double.valueOf(str).doubleValue();
		reset();
	}

	public boolean action(Event ev, Object arg) {
		String label = (String)arg;
		if (ev.target instanceof Button) {
			if(label.equals(STR[1])){
				start();
			}else if(label.equals(STR[0])){
				reset();
			}
			return true;
		}
		return false;
	}

	Graphics g,g0,gb;
	Dimension area;
	public void reset(){
		//stop();
		area=getSize();
		if ( (g == null) ){
//				|| (area.width != bgDimension.width)
//				|| (area.height != bgDimension.height) ) {
//			bgDimension = area;
			bgImage = createImage(area.width, area.height);
			fgImage = createImage(area.width, area.height);
			g = bgImage.getGraphics();
			g0=fgImage.getGraphics();//getGraphics();
		}
		runTime=0;
		lastTime = System.currentTimeMillis();
		g.setColor(bgColor);
		g.fillRect(0,0,area.width,area.height);
		g.setColor(Color.black);
		drawRoad(area);
		gb=this.getGraphics();
	}
	numberInput maxV,acceleration;
	void drawRoad(Dimension area){
		L.init(g,area);
		T.init(g0,L.X[0],L.y-5,area.width);
		maxV=new numberInput(g,5,L.y,STR[3]);
		maxV.setValue((int)car.vmax,5,50);
		acceleration=new numberInput(g,10,L.y+L.lSize.height,STR[4]);
		acceleration.setValue((int)T.a,1,20);
		repaint();
	}
	static boolean running,rightClick,changed=false;
	public boolean mouseDown(Event e, int x, int y){
		if(e.modifiers==Event.META_MASK)//"Right Click, ";
			rightClick=true;
		else rightClick=false;
		int w=L.lSize.width;
		if(L.mouseDown(e,x,y)){
			repaint();
			return true;
		}else if(x< 2*w && y<3*w){
			int sign=0;
			if(e.modifiers==Event.META_MASK || x>w)sign=1;//right click
			else if((L.greenTime>1)&&
						(L.yellowTime-L.greenTime)>1 &&
						(L.redTime-L.yellowTime)>1 )
						sign=-1; // right click add 1, left click sub 1
		
			switch(y/w){
				case 0:L.greenTime+=sign;
				case 1:L.yellowTime+=sign;
				case 2:L.redTime+=sign;
			}
			L.setupLightTime(L.greenTime,L.yellowTime,L.redTime);
			repaint();
		}else if(maxV.mouseDown(e,x,y)){
			car.vmax=maxV.value();
			repaint();
		}else if(acceleration.mouseDown(e,x,y)){
			T.a=acceleration.value();
			repaint();
		}else{
			running=!running;
			changed=true;
			repaint();
		}
		return true;
	}
	//public boolean mouseDrag(Event e, int x, int y){
		//if(e.modifiers==Event.META_MASK)return true;
	//	return true;
	//}
	public boolean mouseUp(Event e, int x, int y){
		if(changed){
			if(!rightClick)running=!running;
			changed=false;
		}
		return true;
	}
	//public boolean mouseMove(Event e, int x, int y){
	//	XYpos.setText(String.valueOf(x)+","+String.valueOf(y));
	//	return true;
	//}
	public void paint(Graphics gs){
		if(g==null)reset();
		update(gs);
	}
	String d2String(double value){
		float f=(float)((int)(value*100.)/100.);
		String str=String.valueOf(f);
		if(str.indexOf(".")==-1)str+=".0";
		return str;
	}

	public void update(Graphics gs){

		g0.drawImage(bgImage, 0, 0, this);
		if(!running)L.drawTrace(g0,acceleration.value(),maxV.value());
		T.advanced(deltaT);
		T.drawVelocity(g0);
		g0.drawString(STR[2]+d2String(runTime)+" s",130,L.y-L.height);

		gs.drawImage(fgImage, 0, 0, this);
	}
	// animation code 
	Thread animThread;
	long lastTime=0;
	double runTime;
	long delay=100,delta=100;
	//  This starts the threads.
	public void start(){
		//Start animating!
		if (animThread == null) {
			animThread = new Thread(this);
			animThread.start();
			//Remember the starting time. of thread
			lastTime = System.currentTimeMillis();
			runTime=0.;
		}
		running=true;
	}
	 public void stop() {
		//Stop the animating thread.
		animThread = null;
	}
	int second=0;
	public void run() {
		//Just to be nice, lower this thread's priority
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		//This is the animation loop.
		while (Thread.currentThread() == animThread) {
			//Advance the animation frame. with delta time
			delta=System.currentTimeMillis()-lastTime;
			lastTime+=delta;
			if(running)advanced(delta/1000.);
			try {
				animThread.sleep(Math.max(0,lastTime-System.currentTimeMillis()));
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	int lastN1=0,lastN2=0,period=100;
	String str="(0/0)=>";
	void showResult(){
		countText.setText(String.valueOf(T.passed)+"/"+
				String.valueOf(T.passed2)+"/"+
				String.valueOf((int)(100.*(T.passed+T.passed/3.)/second)/50.));
		//period=3*(L.redTime);
		if(second%period==0){
		double score=(T.passed-lastN1+(T.passed2-lastN2)/3.)/period;
		scoreText.setText(String.valueOf(T.passed-lastN1)+"/"+
				String.valueOf(T.passed2-lastN2)+"/"+
				String.valueOf((int)(100.*score)/50.));
			lastN1=T.passed;
			lastN2=T.passed2;
		}
	}
	double deltaT;
	void advanced(double dt){
		runTime += dt;
		int t=(int)runTime;
		L.setStatus(t);
//		T.advanced(dt);
		deltaT=dt;
		if(t!=second){
			second=t;
			//timeText.setText(String.valueOf(second));
			showResult();
		}
/*
		g0.drawImage(bgImage, 0, 0, this);
		if(!running)L.drawTrace(g0,acceleration.value(),maxV.value());
		T.advanced(deltaT);
		T.drawVelocity(g0);
		g0.drawString(STR[2]+d2String(runTime)+" s",130,L.y-L.height);
		//g.drawImage(fgImage, 0, 0, this);

		repaint();
*/
		update(gb);
	}
}

class Light{// implements Runnable{
	static int green=0,yellow=1,red=2,count;
	Color color[]={Color.green,Color.yellow,Color.red};
	int X[],status[],y;
	static int height=70;
	int greenTime,yellowTime,redTime;
	int delayArray[];
	Dimension lSize=new Dimension(10,30);
	Graphics g;
	int dd=3;
	numberInput delayControl[];
	int init_delay=4;
	String STR[];
	Light(int n,String s[]){
		count=n;
		X=new int[n];
		status=new int[n];
		delayArray=new int[n];
		delayControl=new numberInput[n-1];
		STR=s;
	}
	int xx,yy,size,size2;
	int width,roadWidth;
	void init(Graphics gi,Dimension area){
		roadWidth=15;
		width=(area.width-count*roadWidth)/count;
		int x0=width/2,dx=width+roadWidth,y0=area.height;
		height=(area.height-3*lSize.height/2-roadWidth)/2;
		g=gi;
		y=y0-height;
		xx=lSize.width+dd;
		yy=y+dd;
		size=lSize.width/2;
		size2=2*size;
		drawRoad(width,roadWidth);// draw streets 
		for(int i=0;i<count;i++){
			X[i]=x0+i*dx;
			status[i]=red;
			delayArray[i]=0;
			drawLight(i,0);
		}
		// draw Left top cornor light
		Color c=g.getColor();
		g.drawRect(0,0,lSize.width,lSize.height);
		for(int i=0;i<3;i++){// 3 lights
			g.setColor(color[i]);
			g.fillOval(0,i*lSize.width,size2,size2);
			g.setColor(Color.black);
			g.drawOval(0,i*lSize.width,size2,size2);
		}
		g.setColor(c);
/*
	if(STR[6].length>0){
		String delimeter=",";
		String[] lv=STR[6].split(delimeter);
		setupLightTime(Integer.parseInt(lv[0]),Integer.parseInt(lv[1]),Integer.parseInt(lv[2]));
	}else 	
*/
		setupLightTime(30,35,70);
		for(int i=1;i<count;i++){ // delay control init
			delayControl[i-1]=new numberInput(g,X[i]-lSize.width-5,yy,STR[5],true);
		}
	}
	void drawRoad(int width,int roadWidth){
		int x=0;
		int w,h=height,dh=h+roadWidth;
		//int dd=5;
		//int xx=lSize.width+dd,yy=dd;
		for(int i=0;i<count+1;i++){ //draw street
			if(i==count || i==0 )w=width/2;
			else w=width;
			g.drawRect(x,y,w,h-1); // lower part
			g.drawRect(x,y-dh,w,h); // upper part
			for(int j=y-dh+h;j>y-dh;j-=10)g.drawLine(x,j,x+5,j);
			for(int j=y-dh+h;j>y-dh;j-=20)g.drawLine(x+5,j,x+10,j);
			x=x+w+roadWidth;
		}
	}

	public boolean mouseDown(Event e, int x, int y){
		for(int i=1;i<count;i++){ // delay control 
			if(delayControl[i-1].mouseDown(e,x,y)){
				setupDelay(i,delayControl[i-1].lastChange());
				return true;
			}
		}
		return false;
	}

	void drawLight(int n,int ts){
		// draw Light according to current status
		Color c=g.getColor();
		int x1=X[n]-xx;
		g.clearRect(x1,yy,lSize.width,lSize.height);
		g.drawRect(x1,yy,lSize.width,lSize.height);
		for(int i=0;i<3;i++)
			g.drawOval(x1,yy+i*lSize.width,size2,size2);

		g.setColor(Color.black);
		g.fillRect(x1-size,yy+5*lSize.width-12,16,14);
		g.setColor(color[status[n]]);
		g.fillOval(x1,yy+status[n]*lSize.width,size2,size2);
		g.drawString(String.valueOf(ts),x1-size,yy+5*lSize.width);
		g.setColor(c);
	}
	void setupLightTime(int tGreen,int tYellow, int tRed){
		greenTime=tGreen;
		yellowTime=tYellow;
		redTime=tRed;
		g.clearRect(2*size2,0,30,3*size2);
		Color c=g.getColor();
		g.setColor(Color.black);
		g.drawString(String.valueOf(greenTime),2*size2,size2);
		g.drawString(String.valueOf(yellowTime-greenTime),2*size2,2*size2);
		g.drawString(String.valueOf(redTime-yellowTime),2*size2,3*size2);
		//drawTiming(running);
		g.setColor(c);
	}
	void drawTrace(Graphics gp,double a,double maxV){
		int xi=width/2+1,xx=xi;
		int yi=y+height;
		double x=0.;//,vx=0.;
		double t,ta=maxV/a,ta2=ta*ta;
		a=a/2.;
		Color c=gp.getColor();
		drawTiming(gp);
		gp.setColor(Color.blue);
		for(t=0.;yi>y-height;t+=0.5){
			if(t<ta)gp.drawLine(xx,yi,xx=xi+(int)(a*t*t),yi--);
			else gp.drawLine(xx,yi,xx=xi+(int)(a*ta2+maxV*(t-ta)),yi--);
			//if(t<ta)gp.drawLine(xx=xi+(int)(a*t*t),yi--,xx,yi);
			//else gp.drawLine(xx=xi+(int)(a*ta2+maxV*(t-ta)),yi--,xx,yi);
		}
		gp.setColor(c);
	}
	void drawTiming(Graphics gp){
		//draw light timing info
		Color c=gp.getColor();
		int xi=width/2,yi;
		for(int i=0;i<count;i++){
			gp.setColor(Color.white);
			gp.drawLine(xi,y+height,xi,y-height);
			//if(status){
			//	gp.setColor(Color.black);
			//	gp.drawLine(xi,y+height,xi,y);
			//	gp.drawLine(xi,y-roadWidth,xi,y-height);
			//}else{
				yi=y+height-2*delayArray[i];
				while(yi>y-height){
					gp.setColor(Color.green);
					gp.drawLine(xi,yi,xi,yi-=2*greenTime);
					gp.setColor(Color.yellow);
					gp.drawLine(xi,yi,xi,yi-=2*(yellowTime-greenTime));
					gp.setColor(Color.red);
					gp.drawLine(xi,yi,xi,yi-=2*(redTime-yellowTime));
				}
				yi=y+height-2*delayArray[i];
				while(yi<y+height){
					gp.setColor(Color.red);
					gp.drawLine(xi,yi,xi,yi+=2*(redTime-yellowTime));
					gp.setColor(Color.yellow);
					gp.drawLine(xi,yi,xi,yi+=2*(yellowTime-greenTime));
					gp.setColor(Color.green);
					gp.drawLine(xi,yi,xi,yi+=2*greenTime);
				}
			//}
			xi+=width+roadWidth;
		}
		gp.setColor(c);
	}
	
	void setupDelay(int id,int Time){
		for(int i=id;i<count;i++)delayArray[i]+=Time;
		//drawTiming(running);
	}

	public int height(){ return height;}

	boolean setStatus(long t){
		boolean out=false;
		long dt;
		int s,ts;
		for(int i=0;i<count;i++){
			s=status[i];
			dt=(t-delayArray[i]) % redTime;
			if(dt<0)dt+=redTime;
			if(dt<greenTime){
				status[i]=green;
				ts=greenTime-(int)dt;
			}else if(dt<yellowTime){
				status[i]=yellow;
				ts=yellowTime-(int)dt;
			}else{
				status[i]=red;
				ts=redTime-(int)dt;
			}
			if(s!=status[i]){
				out=true;
			}
				drawLight(i,ts);
		}
		return out;
	}
}

class numberInput {
	int width,height,xs,ys,x,y;
	String title;
	Graphics g;
	int value=0;
	double scale=1.;
	Rectangle add,sub;
	int change=0;
	boolean bounded=false;
	int min=0,max=100;
	int xwidth=60;
	numberInput(Graphics gs,int xi,int yi,String s,boolean rightAlign){
		g=gs;x=xi;y=yi;title=s;
		FontMetrics fm=g.getFontMetrics();
		if(rightAlign)x-=xwidth;//fm.stringWidth(title);
		init();
	}
	numberInput(Graphics gs,int xi,int yi,String s){
		g=gs;x=xi;y=yi;title=s;
		init();
	}
	void init(){
		FontMetrics fm=g.getFontMetrics();
		int h=fm.getHeight(),w=xwidth;//fm.stringWidth(title);
		int h2=h/2;
		width=w-h-2;
		height=h;
		int yy=y+h;
		g.drawString(title,x,yy);
		yy+=1;
		int xa[]={x,x+h2,x+h2},xb[]={x+w,x+w-h2,x+w-h2};
		int yab[]={yy+h2,yy,yy+h};
		xs=x+h2+3;
		ys=yy+h-2;
		g.drawRect(xs-2,ys-height+2,width-1,height);
		g.fillPolygon(xa,yab,3);
		g.fillPolygon(xb,yab,3);
		sub=new Rectangle(x,y+h+1,h2,h);
		add=new Rectangle(x+w-h2,y+h+1,h2,h);
		setValue(0);
	}
	void setValue(int i,int mi,int mx){
		setValue(i);
		bounded=true;
		min=mi;
		max=mx;
	}
	void setValue(int i,int s){
		scale=10^s;
		setValue(i);
	}
	
	void setValue(int i){
		value=i;
		g.clearRect(xs-2+1,ys-height+3,width-3,height-2);
		Color c=g.getColor();;
		g.setColor(Color.black);
		g.drawString(String.valueOf(i*scale),xs,ys);
		g.setColor(c);
	}
	boolean valueChange(int di){
		if(bounded &&( (value==min && di<0) || (value==max && di>0)))di=0;
		if(di==0)return false;
		value+=change;
		setValue(value);
		return true;
	}
	int lastChange(){ return change;}
	double value(){ return scale*value;}

	public boolean mouseDown(Event e, int x, int y){
		if(add.inside(x,y))change=1;
		else if(sub.inside(x,y))change=-1;
		else change=0;
		return valueChange(change);
	}
}

class Traffic {//implements Runnable{
	// traffic control center
	Vector v=new Vector(); // main traffic
	Vector v2[]; // other traffics
	car c;
	int w=10,h=5,y0,w2=w/2;
	Graphics g;
	static double a=10.;
	static private double a2=a/6.;
	int minDx=3,xMax,Dx,Dx2,minDx2;
	Light L;
	int passed=0,passed2=0;
	int ymax,ymin,y1;
	Traffic(Light Li){
		L=Li;
		v2=new Vector[L.count];
		for(int i=0;i<L.count;i++)v2[i]=new Vector();
	}

	double roadWidth=15.;
	void init(Graphics gs,int x,int y,int xmax){
		passed=0;
		passed2=0;
		a2=a/6.;
		g=gs;
		xMax=xmax+w+minDx;
		y0=y;
		Dx=w+minDx;
		Dx2=Dx+minDx;
		minDx2=2*minDx;
		reset();
		v.removeAllElements();
		for(int i=0;x>0;i++,x-=Dx){
			c=new car((double)(x),0.,0.);
			if(i==0)c.setFront(true);
			else c.setFront(false);
			v.addElement(c);
			drawCar(c,false);
		}
		// for vertical cars
		ymax=y0+L.height();
		ymin=y0-(int)roadWidth-L.height();
		for(int i=0;i<L.count;i++){
			v2[i].removeAllElements();
			int sign=1-2*(i%2);
			int n=L.height()/Dx;
			y1=y0-(int)roadWidth;
			for(int j=0;j<n;j++){
				c=new car(y1-j*Dx,0.,0.);
				if(j==0)c.setFront(true);
					else c.setFront(false);
				v2[i].addElement(c);
				drawCar(c,L.X[i],sign,false);
			}
		}
		// draw car color code
		int xx=15,yy=ymin+15,xx2=xx+w+h;
		g.setColor(Color.black);
		g.drawRect(xx,yy,w,h);
		g.drawString("a>0.",xx2,yy+h);
		g.setColor(Color.yellow);
		g.fillRect(xx,yy,w,h);
		yy+=w;
		g.setColor(Color.black);
		g.drawRect(xx,yy,w,h);
		g.drawString("a=0.",xx2,yy+h);
		g.setColor(Color.green);
		g.fillRect(xx,yy,w,h);
		yy+=w;
		g.setColor(Color.black);
		g.drawRect(xx,yy,w,h);
		g.drawString("a<0.",xx2,yy+h);
		g.setColor(Color.red);
		g.fillRect(xx,yy,w,h);
		g.setColor(Color.black);
	}

	void reset(){
		g.clearRect(0,y0-h,xMax,h+1);
	}
	void drawCar(car c,int x,int sign,boolean erase){
			int xx=x+w2,yy=(int)c.y[0]-w2;
			if(sign==-1)yy=ymin+ymax-yy;
			if(erase)g.clearRect(xx,yy,h+1,w+1);
			else {
				g.drawRect(xx,yy,h,w);
				Color color=g.getColor();
				if(c.acceleration==0.)g.setColor(Color.green);
				else if(c.acceleration<0)g.setColor(Color.red);
				else g.setColor(Color.yellow);
				g.fillRect(xx,yy,h,w);
				g.setColor(color);
			}
	}

	void drawCar(car c,boolean erase){
			if(erase)g.clearRect((int)c.y[0]-w,y0-h,w+1,h+1);
			else {
				g.drawRect((int)c.y[0]-w,y0-h,w,h);
				Color color=g.getColor();
				if(c.acceleration==0.)g.setColor(Color.green);
				else if(c.acceleration<0)g.setColor(Color.red);
				else g.setColor(Color.yellow);
				g.fillRect((int)c.y[0]-w,y0-h,w,h);
				g.setColor(color);
			}
	}
	// traffic flow control
	car cs;
	boolean nextFront;
	void advanced(double dt){
		cs=null;
		c=(car)v.firstElement();
		c.setAcceleration(a);
		if(c.y[0]>xMax){// remove car outside boundary
			v.removeElement(c);
			passed+=1;
			drawCar(c,true);
			c=null;
		}
		nextFront=false;
		int count2=L.count;
		double aNeeded,tReact=1.;
		double minD=Dx*1.2,safeD=2.*Dx;
		for(Enumeration e=v.elements();e.hasMoreElements();){
			c=(car)e.nextElement();
			if(c.lightID<count2){// before last light
				if(cs!=null){
					minD=cs.y[0]-c.y[0]-Dx;
					safeD=minD-c.y[1]*tReact;
					if(minD<0.)c.y[1]=cs.y[1];// too close
				}else{
					minD=Dx;
					safeD=2.*Dx;
				}
				if((cs!=null)&& safeD<0.){ // be safe
							aNeeded=2.*(cs.y[1]*cs.y[1]-c.y[1]*c.y[1])/minD;//(cs.y[0]-c.y[0]-Dx);
							//aNeeded=a;
							c.setAcceleration(aNeeded);
				}else{
					switch(L.status[c.lightID]){
					case 0: // green light
						if(cs!=null && minD>2.*minDx)c.setAcceleration(a);
					break;
					case 1: //yellow
						if(L.X[c.lightID]-c.y[0] < c.y[1]*c.y[1]/(3.*Math.max(a,10.)))break;
					case 2:
						if(c.isFront()){
							if(L.X[c.lightID]-c.y[0] > 2.*Dx2)
								c.setAcceleration(a);
							else if(L.X[c.lightID]-c.y[0] < minDx){
								c.y[1]=0.;
								c.setAcceleration(0.);
							}else {
								aNeeded=c.y[1]*c.y[1]/(L.X[c.lightID]-c.y[0]);
								c.setAcceleration(-aNeeded);
							}
						}else if(cs!=null){
							if(safeD > Dx2)c.setAcceleration(a);
							else if(cs.acceleration<0. && c.acceleration>0.){
								aNeeded=(cs.y[1]*cs.y[1]-c.y[1]*c.y[1])/minDx;//(cs.y[0]-c.y[0]);
								c.setAcceleration(aNeeded);
							}else c.setAcceleration(0.);
						}
						break;
					}
				}
				if(c.y[0]>=L.X[c.lightID]){
					c.lightID+=1; //just pass one traffic light
					nextFront=true;
				}else if((c.y[0]<L.X[c.lightID])&&(nextFront)){
						c.setFront(true);
						nextFront=false;
				}

			} else c.setAcceleration(a);

			// advanced one time step
			//drawCar(c,true);
			c.advanced(dt);
			drawCar(c,false);
			cs=c;
		}
		if((int)c.y[0]>Dx){
			v.addElement(new car(0.,0.,a));
			drawCar(c,false);
		}
		advanced2(dt);
	}

	void advanced2(double dt){
		// if passed lighID=1;
		int sign;
		for(int i=0;i<L.count;i++){
			c=(car)v2[i].firstElement();
			cs=null;
			sign=1-2*(i%2);
			if(c.y[0]>ymax){
				v2[i].removeElement(c);
				passed2+=1;
				drawCar(c,L.X[i],sign,true);
				c=null;
			}
			nextFront=false;
			for(Enumeration e=v2[i].elements();e.hasMoreElements();){
				c=(car)e.nextElement();
				if(c.lightID==0){// not pass light yet
					if(c.y[0]<=y1){
						if(nextFront){
							c.setFront(true);
							nextFront=false;
						}
					}else{
						c.lightID+=1; //just pass one traffic light
						c.setAcceleration(a);// keep accelerating
						nextFront=true;
						if(cs!=null)c.setFront(false); // first car, always front
					}
					double aNeeded=a;
					if(L.status[i]==2){ // red light, green for us
						if(cs==null)c.setAcceleration(a);
						else if((cs.y[0]-c.y[0]) > Dx2 )
							c.setAcceleration(a);
					}else { // green or yellow light, red for us
						if(c.isFront()){
							if(y1-c.y[0] < minDx){
								c.y[1]=0.;
								c.setAcceleration(0);
							}else{
								aNeeded=c.y[1]*c.y[1]/(y1-c.y[0]);
								c.setAcceleration(-aNeeded);
							}
						}else if(cs!=null){
							if(cs.y[0]-c.y[0] > Dx2)
								c.setAcceleration(a);
							else if(cs.y[0]-c.y[0] < Dx){
									c.y[1]=cs.y[1];
									c.setAcceleration(0.);
							}else if(cs.acceleration<=0.){
								aNeeded=(cs.y[1]*cs.y[1]-c.y[1]*c.y[1])/(cs.y[0]-c.y[0]);
								c.setAcceleration(aNeeded);
							}
						}
					}
					if( (cs!=null)   && (cs.y[0]-c.y[0] < Dx) ){
						c.y[1]=0.;
					}
			}

				// advanced one time step
				//drawCar(c,L.X[i],sign,true);
				c.advanced(dt);
				drawCar(c,L.X[i],sign,false);
				cs=c;
			}
			if((int)c.y[0]>ymin+Dx){
				v2[i].addElement(new car(ymin,0.,a2));
				drawCar(c,L.X[i],sign,false);
			}
		}
	}
	void drawVelocity(Graphics g0){
		int size=2,size2=4;
		int xoffset=-w2-size,yoffset=y0-(int)roadWidth-size+h;
		g0.setColor(Color.blue);
		for(Enumeration e=v.elements();e.hasMoreElements();){
			c=(car)e.nextElement();
			g0.fillOval((int)c.y[0]+xoffset,yoffset-(int)(2.*c.y[1]),
				size2,size2);
		}
	}
}

class car extends rk4 {
	double acceleration=10.;
	static double vmax=30.,vmin=0.,vMove=3.;
	double xmax=100.;
	int lightID;
	boolean front=false;

	car(double xi,double vi,double ai){
		init(2,true);
		y[0]=xi;
		y[1]=vi;
		acceleration=ai;
		lightID=0;
	}

	void setAcceleration(double ai){
		acceleration=ai;
	}
	boolean isFront(){return front;}
	void setFront(boolean f){
		front=f;
	}
	void advanced(double dt){
		nextmove(dt);
		if(y[1]<vmin){
			acceleration=0.;
			y[1]=0.;
		}else if(y[1]>vmax){
			acceleration=0.;
			y[1]=vmax;
		}
	}
	public void derivs (double t,double y[],double dydx[]){
		dydx[0]=y[1]; // V_theta
		dydx[1]=acceleration; // alpha=-K theta
	}
}

// RK4
abstract class rk4 {
	int n;
	public double dydx[];
	public double y[],yout[];
	public void init(int dim,boolean self){
		n=dim;
		dydx=new double[n];
		yout=new double[n];
		if(self)y=yout;
			else	y=new double[n];
	}

	 public abstract void derivs(double x,double y1[],double dydx[]);
	/*																																 
	public void next(double x,double h){ // t & dt
		core(x,h);
	}
	public double[]  next(double h){ // dt
		core(0.,h);
		return yout;
	}*/
	public void nextmove(double h,double[] yin){
		y=yin;
		core(0.,h);
	}
	public void nextmove(double h){
		core(0.,h);
	}
	private void core(double x, double h){
	/* Runge-Kutta forth-order method */
		int i;
		double xh,hh,h6;
		double dym[]=new double[n];
		double dyt[]=new double[n];
		double yt[]=new double[n];
		hh=h*0.5;
		h6=h/6.0;
		xh=x+hh;
		derivs(xh,y,dydx);
		for (i=0;i<n;i++) yt[i]=y[i]+hh*dydx[i];
		derivs(xh,yt,dyt);
		for (i=0;i<n;i++) yt[i]=y[i]+hh*dyt[i];
		derivs(xh,yt,dym);
		for (i=0;i<n;i++) {
			yt[i]=y[i]+h*dym[i];
			dym[i] += dyt[i];
		}
		derivs(x+h,yt,dyt);
		for (i=0;i<n;i++)
			yout[i]=y[i]+h6*(dydx[i]+dyt[i]+2.0*dym[i]);
	}
}
