/* tclo, melissatan
 * 
 * TrialManager analyzes statistics obtained over numerous trials.
 * 
 * Statistics include average total time, average in-van time, average manhattan dist
 */

public class TrialManager {
    // over many individual trials
    private int numTrials; // number of observations seen thus far
    private double sumATT; 		// sum of the avgtotaltime values 
    private double sumAVT; 		// sum of the avgInVantime values 
    private double sumAMD;      // sum of the avgMDist values 
    private double sumOfSquaresATT;
    private double sumOfSquaresAVT;
    private double sumOfSquaresAMD;    

    public TrialManager() {
    	this.numTrials = 0;
    	this.sumATT = 0;
    	this.sumAVT = 0;
    	this.sumAMD = 0;    	
    	this.sumOfSquaresATT = 0;
        this.sumOfSquaresAVT = 0;
        this.sumOfSquaresAMD = 0;
    }

    // return the number of observations see so far
    public int getNumObservations() {
        return numTrials;
    }

    /* recordObs: add an observation to the manager */
    public void recordTrial(double[] data) {
    	numTrials++;
    	
    	double att = data[0]; //average total time
    	double avt = data[1]; //average in-van time
    	double amd = data[2]; //average mdist    	
    	
		sumATT += att;
		sumAVT += avt;
		sumAMD += amd;
		
		sumOfSquaresATT += att * att;
		sumOfSquaresAVT += avt * avt;
		sumOfSquaresAMD += amd * amd;
    }

    /* mean: compute the mean of the avgTotalTime values seen so far */
    public double meanATT() {
        return sumATT / numTrials;
    }
    
    /* mean: compute mean of the avgInVanTime values seen so far */
    public double meanAVT() {
        return sumAVT / numTrials;
    }

    /* std: compute the standard deviation of the avgTotalTime values seen so far */
    public double sdATT() {
        double mean = sumATT / numTrials;
        return Math.sqrt((sumOfSquaresATT - sumATT * mean) / numTrials);
    }
    
    /* std: compute the standard deviation of the avgInVanTime values seen so far */
    public double sdAVT() {
        double mean = sumAVT / numTrials;
        return Math.sqrt((sumOfSquaresAVT - sumAVT * mean) / numTrials);
    }

    
}