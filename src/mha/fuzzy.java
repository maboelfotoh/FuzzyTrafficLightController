/*
 * CISC870
 * Muhammad Aboelfotoh
 * Main contribution is here.
 */

package mha;


import net.sourceforge.jFuzzyLogic.FIS;

import java.net.*;
import java.awt.Frame;

public class fuzzy extends trafficControl {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6260952358264823663L;

	FIS fis;
	
	int ext_time = 0;
	int ext = 0; //1st ext., 2nd ext, etc.
	int t_start_green = 0;
	final int minimum_green = 5;
	final int maximum_green = 30;
	final int yellow_time = 3;
	boolean notDone = true;
	
	// common..used to update status..
	int remaining_green = 0;
	int remaining_yellow = 0;
	int end_green = 0;
	int end_yellow = 0;
	int end_maximum = 0;
	
	void messagebox(String str) {
        Frame f = new Frame();
        f.setSize(200,200);
        f.setVisible(true);
		new MsgBox(f , str, true);
	}
	
	int getMaxNS() {
		int max = -1;
		for(int i=0;i<Light.count;i++) {
			if(T.v2[i].size() > max) max = T.v2[i].size();
		}
		return max;
	}
	int getMaxEW() {
		return T.v.size();
	}

	// apply fuzzy rules and perform fuzzy evaluation...
	void fuzzycontrol() {
		int greenq = 0, redq = 0;
		double ext = 0.0;
		
		if(L.status[0] == Light.yellow) return;

		if(L.status[0] == Light.green) {
			// horizontal traffic: three junctions, three queues
			// roughly get the vehicles that haven't passed the intersection yet
			greenq = getMaxEW() / 3;
			redq = getMaxNS();
		}
		else if (L.status[0] == Light.red) {
			redq = getMaxEW();
			// vertical traffic: those who passed the intersection, and those who didn't
			// roughly get the vehicles that haven't passed the intersection yet
			greenq = getMaxNS() / 2;
		}
		try {
        // Set inputs
        fis.setVariable("green", greenq);
        fis.setVariable("red", redq);

        System.out.println("greenq="+greenq);
        System.out.println("redq="+redq);
        
        // Evaluate
        fis.evaluate();

        // Defuzzify output variable 
        ext = fis.getVariable("ext").defuzzify();
		} catch(Exception e) {
			messagebox(e.getMessage());
		}
		
        // set traffic light delay
        // since we're implementing synchronous multi-junction..
        // then all traffic lights are extended with the same delay
        if(end_green + ext > end_maximum) end_green = (int)runTime;
        else end_green += (int)ext;
        System.out.println("ext=" + ext);

	}
	
	//overriding the default method, so one can do fuzzy control of signals...
	
	//called as long as thread is running
	//dt - is the time lapsed since last execution of thread loop in seconds	
	public void advanced(double dt) {
		//super.advanced(dt);
		runTime += dt;
		int t=(int)runTime;
		
		int ts = 0;
		for(int i = 0; i < 1; i++) {
			if(L.status[i] == Light.green) {
				if(remaining_green == 0) {
					L.status[i] = Light.yellow;
					ts = remaining_yellow = yellow_time;
					L.drawLight(i, remaining_yellow);
					end_yellow = t + yellow_time;
				}
				else {
					ts = remaining_green = end_green - t;
					L.drawLight(i, remaining_green);
					if(remaining_green == 0) {
						fuzzycontrol();
						ts = remaining_green = end_green - t;
					}
				}
			}
			else if(L.status[i] == Light.yellow) {
				if(remaining_yellow == 0) {
					L.status[i] = Light.red;
					ts = remaining_green = minimum_green;
					L.drawLight(i, remaining_green);
					end_green = t + minimum_green;
					end_maximum = t + maximum_green;
				}
				else {
					ts = remaining_yellow = end_yellow - t;
					L.drawLight(i, remaining_yellow);
				}
			}
			else if(L.status[i] == Light.red) { // same green vars used for conflicting signal group
				if(remaining_green == 0) {
					L.status[i] = Light.green;
					ts = remaining_green = minimum_green;
					L.drawLight(i, remaining_green);
					end_green = t + minimum_green;
					end_maximum = t + maximum_green;
				}
				else {
					ts = remaining_green = end_green - t;
					L.drawLight(i, remaining_green);
					if(remaining_green == 0) {
						fuzzycontrol();
						ts = remaining_green = end_green - t;
					}
				}
			}
		}
		
		// since its sync., the rest are the same...
		for(int i = 1; i < Light.count; i++) {
			L.status[i] = L.status[0];
			L.drawLight(i, ts);
		}
		
		deltaT=dt;
		if(t!=second){
			second=t;
			//timeText.setText(String.valueOf(second));
			showResult();
		}
		update(gb);
		//end of super
	
	}

	// load fuzzy rules file...
	void loadFCL() {
		String fileName = null;
		String fclurlparam = null;
		URL fclurl = null;
		URLConnection fc = null;
		try {
			fclurlparam = getParameter("Fclurl");
			if(fclurlparam != null) {
				fclurl = new URL(fclurlparam);
		        fc = fclurl.openConnection();
		        fis = FIS.load(fc.getInputStream(), true);
			}
			else {
				// Load from (local FS) 'FCL' file
		        fileName = "fcl/FTJSC.fcl";
		        fis = FIS.load(fileName,true);		
			}
                
		} catch(Exception e) {
			e.printStackTrace();
			messagebox(e.getMessage());
		}
        // Error while loading?
        if( fis == null ) { 
            System.err.println("Can't load file: '" 
                                   + fileName + "'");
            return;
        }

	}
	
	public void init() {
		loadFCL();
		super.init();
		
    	remaining_green = minimum_green;
		for(int i=0;i<Light.count;i++) {
    		L.status[i]=Light.green;
    		L.drawLight(i, remaining_green);
    	}
    	end_green = (int) (runTime) + minimum_green;
    	end_maximum = (int)runTime + maximum_green;
	}
}
