/*********************************************************************************************
 * CS122 Winter 2010
 * tclo, melissatan
 * 
 * This class simulates numT trials of Saferide operations.
 * 
 * Similar to Operator, except that this has no animation.
 * 
 * Methods:
 * 
 * - compare(Van[] vans, Passenger p) --> returns int[3] with cheapest van and 
 * 										  insertion indices for pickup and dropoff
 * 										  to runTrial.
 * 
 * - simpleCompare(Van[] vans, Passenger p) --> returns int[2] with cheapest van and
 * 										  		insertion index for pickup to runTrial.
 * 
 * - runTrial(Map m, Experiment e, long seed) --> returns statistics to main.
 * 
 *
 *
 *	KNOWN BUGS:
 * - doesn't run properly with 5x5, it takes forever 
 * - if we initialize rv[0] = rv[1] = -1 in the compare fns,
 * 		compare will break if all the vans are full. We didn't
 * 		have time to write a fn to assign passenger to the
 * 		least busy van if that situation occurs -- now it
 * 		just auto-assigns to van0 if all full.
 *
 *
 *********************************************************************************************/
public class Simulate {
	static int currentTime = 0; //real-time incrementation
	static int timeCap = 20; //max time we'll make a passenger stay in the van
	static int numT = 1000; //no. of trials
	
	public Simulate() {
		
	}		
	
	/* Returns the cheapest van and its recommended insertion 
	 * indices for pickup and dropoff. */
	private static int[] compare(Van[] vans, Passenger p) {
		int[] rv = new int[3];
		int[] checkvans = new int[3];
		int cost = Integer.MAX_VALUE;
		for (int i=0;i<vans.length;i++) {			
			checkvans = vans[i].check(p, currentTime);			
			if (checkvans[0] < cost) {
				cost = checkvans[0];
				rv = checkvans;
				rv[0] = i;
			}
		}
		return rv;
	}
	
	/* Simple version : Returns the cheapest van and recommended 
	 * pickup insertion index. */
	private static int[] simpleCompare(Van[] vans, Passenger p) {		
		int[] rv = new int[2];
		int cost = Integer.MAX_VALUE;		
		int[] checkvans = new int[2];
		for (int i=0;i<vans.length;i++) {			
			checkvans = vans[i].simpleCheck(p, currentTime);
			if (checkvans[0] < cost) {
				cost = checkvans[0];
				rv = checkvans;
				rv[0] = i;				
			}
		}
		return rv;
	}
	
	
	/* Runs a trial, getting stats for both the alternate and simple operators */
	public static double[][] runTrial(Map m, Experiment e, long seed){
		currentTime = 0;
		// return statistics generated in rv.
		double[][] rv = new double[2][3];
		
		PassengerSample ps = new PassengerSample(m, e);
        PassengerSample.initRandGen(seed);
        
		// two obs managers per trial
		ObsManager o = new ObsManager(m);
		ObsManager simpleo = new ObsManager(m);	
        
        // two groups of vans, for each operator
		Van[] vans = new Van[e.numVan];  //our treatment group
		Van[] svans = new Van[e.numVan]; //the original control group
		for(int i = 0;i < e.numVan;i++) {
			vans[i] = new Van(m, o, e.startAddress, e.vanCap);
			svans[i] = new Van(m, simpleo, e.startAddress, e.vanCap);
		}
		
		// begin simulation:
		while(currentTime <= e.endTime) {
			
			// update van positions in map
			for(int i=0;i < vans.length;i++) {				
				vans[i].updatePos(currentTime);				
				svans[i].updatePos(currentTime);
			}
			
			// get new passenger who calls at current time
			Passenger nextPass = ps.getNextPassenger(currentTime);		
			while(nextPass != null) {				
				// assign this passenger to a van, and update that van's route
				int[] results = compare(vans, nextPass);				
				Van bestvan = vans[results[0]];
				int indexp = results[1];
				int indexd = results[2];
				bestvan.assign(nextPass, indexp, indexd);
				bestvan.updateRoute();				
				
				// simple case:
				int[] sresult = simpleCompare(svans, nextPass);
				Van sbestvan = svans[sresult[0]];
				sbestvan.simpleAssign(nextPass, sresult[1]);
				sbestvan.updateRoute();				
				
				// get all other passengers who are calling at this time.
				nextPass = ps.getNextPassenger(currentTime);
			}
			
			// slow down, for animation purposes
			try {
		        Thread.sleep(0);		        
		    } catch (InterruptedException ex) {;}
			
			currentTime++;
		}
		
		// data		
		rv[0][0] = o.avgTotalTime();		
		rv[0][1] = o.avgInVanTime();		
		rv[0][2] = o.avgMDist();		
		
		rv[1][0] = simpleo.avgTotalTime();
		rv[1][1] = simpleo.avgInVanTime();
		rv[1][2] = simpleo.avgMDist();		
		return rv;
	}
	
	
	/* main: run each operator numT times to generate statistics */
	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("Usage: mapfile experimentfile mapscale [optional seed]");
		}	
		Map campus = new Map(args[0]);
		Experiment e = new Experiment(args[1]);
						
		// random seed
		long seed;
        if (args.length <= 3)
            seed = Long.parseLong(args[2]);
        else
            seed = System.currentTimeMillis();   
        
        // handles data from the alternate algorithm operator:
        TrialManager altData = new TrialManager();    
        // handles data from the simple algorithm operator:
        TrialManager simpleData = new TrialManager(); 
        
        double[] altstats = new double[3];
        double[] simplestats = new double[3];
        
        // for numT times,
        for (int i=0; i<numT; i++) {
        	double[][] data = runTrial(campus, e, seed);        		
        	for (int j=0; j<altstats.length; j++) {
        		altstats[j] = data[0][j];
        		simplestats[j] = data[1][j];
        	}
        	
        	altData.recordTrial(altstats);
        	simpleData.recordTrial(simplestats);
        }
        System.out.println("DATA: numtrials ="+altData.getNumObservations());
        System.out.println("DATA: numtrials2="+simpleData.getNumObservations());
        System.out.println("DATA: NEW    avg ATT = "+altData.meanATT());
        System.out.println("DATA: NEW    sd  ATT = "+altData.sdATT());
        System.out.println("DATA: NEW    avg AVT = "+altData.meanAVT());
        System.out.println("DATA: NEW    sd  AVT = "+altData.sdAVT());
        System.out.println("DATA: SIMPLE avg ATT = "+simpleData.meanATT());
        System.out.println("DATA: SIMPLE sd  ATT = "+simpleData.sdATT());
        System.out.println("DATA: SIMPLE avg AVT = "+simpleData.meanAVT());
        System.out.println("DATA: SIMPLE sd  AVT = "+simpleData.sdAVT());        
	}
	
}
