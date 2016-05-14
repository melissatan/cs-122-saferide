/* ***********************************************************************************************
 * 
 * This class manages the observations of waitTime and inVanTime and 
 * computes statistics on them, for one trial.
 * 
 *  
 *************************************************************************************************/

public class ObsManager {
	// used by mapOM (in one trial)
    private int numPass; // number of observations seen in one trial
    private double sumTotalTime; // calltime to dropoff time, summed over one trial
    private double sumInVanTime; // time from pickup to dropoff, summed over one trial
    private double sumMDist; // manhattan distance, summed over one trial
    private double sumOfSquaresTT, sumOfSquaresVT, sumOfSquaresMDist;
    private Map m;
    int[][] pfreq, dfreq; //tracks frequency of pickups & dropoffs at addresses
    

    
    // produces stats from individual trial
    public ObsManager(Map m) {
    	this.m = m;
    	this.pfreq = new int[m.NUM_EW][m.NUM_NS];
    	this.dfreq = new int[m.NUM_EW][m.NUM_NS];
        this.numPass = 0;
        this.sumTotalTime = 0;
        this.sumInVanTime = 0;    
        this.sumMDist = 0;
        this.sumOfSquaresTT = 0;
        this.sumOfSquaresVT = 0;
        this.sumOfSquaresMDist = 0;
    }
 
    /* return the number of passengers seen so far */
    public int getNumPass() {
        return numPass;
    }

    /* recordObs: add a passenger to the manager, from which we can extract data */
    public void recordPass(Passenger p, int currentTime) {  //currentTime = dropOffTime
    	int totalt = currentTime - p.callTime;
    	int inVant = currentTime - p.pickUpTime;   	
    	int mdist = m.Mdist(p.pickUp, p.dropOff);
    	
    	sumTotalTime += totalt;
    	sumInVanTime += inVant;
    	sumMDist += mdist;
    	
        sumOfSquaresTT += totalt * totalt;
        sumOfSquaresVT += inVant * inVant;
        sumOfSquaresMDist += mdist * mdist;
        
        
        numPass++;
     
        int[] pcoords = m.findCoords(p.pickUp);
        int s = pcoords[0];
        int t = pcoords[1];
        pfreq[s][t]++;
        
        int[] dcoords = m.findCoords(p.dropOff);
        int u = dcoords[0];
        int v = dcoords[1];
        dfreq[u][v]++;        
    }

    /* compute average total time */
    public double avgTotalTime() {
        return sumTotalTime / numPass;
    }
    
    /* compute average inVan time */
    public double avgInVanTime() {
    	return sumInVanTime / numPass;
    }
    
    /* compute average manhattan dist */
    public double avgMDist() {
    	return sumMDist / numPass;
    }        

    /* compute std dev of total time */
    public double sdTT() {
        double mean = sumTotalTime / numPass;
        return Math.sqrt((sumOfSquaresTT - sumTotalTime * mean) / numPass);
    }
    
    /* compute std dev of inVanTime */
    public double sdVT() {
        double mean = sumInVanTime / numPass;
        return Math.sqrt((sumOfSquaresVT - sumInVanTime * mean) / numPass);
    }
    
    /* compute std dev of MDist */
    public double sdMDist() {
        double mean = sumMDist / numPass;
        return Math.sqrt((sumOfSquaresMDist - sumMDist * mean) / numPass);
    }

    /* stdErrMean: compute the standard error of the mean of the total time */
    public double stdErrMeanTT() {
        return sdTT() / Math.sqrt(numPass);
    }
    
    /* compute std err of mean of VT */
    public double stdErrMeanVT() {
        return sdVT() / Math.sqrt(numPass);
    }
    
    /* draw pickup and dropoff counts at each address */
    public void frequency(GenDraw im) {
    	int height = pfreq.length; // same as dfreq.length
    	int width = pfreq[0].length; // same as dfreq[0].length    	
    	// draw pickups
    	for (int i=0; i < height; i++){
    		for (int j=0; j < width; j++) {
    			if (pfreq[i][j]!=0) {
    				im.setPenColor(GenDraw.RED);
    				im.text(j+0.8, Math.abs(i-height)-0.3, Integer.toString(pfreq[i][j]));
    			}
    		}
    	}
    	// draw dropoffs    	
    	for (int i=0; i < height; i++){
    		for (int j=0; j < width; j++) {
    			if (dfreq[i][j]!=0) {
    				im.setPenColor(GenDraw.BLUE);
    				im.text(j+1.2, Math.abs(i-height)-0.3, Integer.toString(dfreq[i][j]));
    			}
    		}
    	}
    }
}
