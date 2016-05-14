/*
 * tclo,  melissatan
 * 
 * Generates random passenger sample given experiment parameters.
 */

import java.util.Random;

public class PassengerSample {
	private int nextID;
	private int numPassGen; // Number of passengers that we are going to generate for one call.
	private Passenger nextPassenger;
	private int nextCallTime; // call time of next passenger
	private Address from, to;
	private Experiment e;
	private Map m;
	private static Random randGen; // random number generator

    public PassengerSample(Map m, Experiment e) {
    	nextID = 0;
    	numPassGen = 0;
    	this.e = e;
    	this.m = m;
        if (randGen == null)
            randGen = new Random();
        nextPassenger = genPassenger(0);
    }
    
    /* generates reachable addresses */
    private Address genAddress() {
		String randEW = m.EW_list[randGen.nextInt(m.NUM_EW)];
		String randNS = m.NS_list[randGen.nextInt(m.NUM_NS)];
		Address rv = new Address(randEW, randNS);
		if(!m.isReachable(rv))
			rv = genAddress();
		return rv;
    }
    
    /* generates random number of passengers per call */
    private Passenger genPassenger(int currentTime) {
    	if(numPassGen == 0) {
    		int gap = (int)Math.round(exp(e.arrRate)); //time gap between calls
    		numPassGen = (int)(randGen.nextGaussian() + e.passPerCall);
    		if(numPassGen < 1)
    			numPassGen = 1;
    		nextCallTime = currentTime + gap;
    		from = genAddress();
    	} else {
    		nextCallTime = nextPassenger.callTime;
    		from = nextPassenger.pickUp;
    	}
    	to = genAddress();
    	// make sure pickup location isn't dropoff location
    	while (from.equals(to)) {
    		to = genAddress();
    	}
		numPassGen--;
		nextID++;
		return new Passenger(nextID, nextCallTime, from, to);
    }
    
    /* gets the next passenger from sample who calls at current time*/
    public Passenger getNextPassenger(int currentTime) {
    	if(isEmpty(currentTime)) 
    		return null;
    	if(currentTime == nextPassenger.callTime) {
    		Passenger rv = nextPassenger;
    		//get ready a new passenger for the next call
    		nextPassenger = genPassenger(currentTime);   		
    		return rv;  		
    	}
    	return null;
    }
    
    /* checks if we're done generating passengers */
    public boolean isEmpty(int currentTime) {
        return (currentTime > e.endTime);
    }
    
    /* initialize random generator */
    public static void initRandGen(long seed) {
        randGen = new Random(seed);
    }
    
    /* Generate a random variable from an exponential distribution with parameter lambda. */
    private double exp(double lambda) {
        double r = randGen.nextDouble();
        return -Math.log(r) / lambda;
    }
    
    /* for testing */
    public static void main(String[] args) {
        Experiment e = new Experiment(args[0]);
        Map m = new Map(args[1]);	
        long seed;
        if (args.length == 3)
            seed = Long.parseLong(args[2]);
        else
            seed = System.currentTimeMillis();
        System.out.println("Using seed: " + seed);
        PassengerSample.initRandGen(seed);
        PassengerSample ps = new PassengerSample(m, e);
        int currentTime = 0;
        while (!ps.isEmpty(currentTime)) {
        	Passenger nextPass = ps.getNextPassenger(currentTime);
            while(nextPass != null) {
            	System.out.println("Current time: " + currentTime);
            	System.out.println(nextPass);
            	nextPass = ps.getNextPassenger(currentTime);
            }
            currentTime++;
        }
    }
}
