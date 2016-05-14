/**************************************************************************************
 * CS122 Winter 2010
 * tclo, melissatan
 * 
 *
 * Operator will draw two images: one animated graph, and one
 * non-animated picture of the frequencies of pickups/dropoffs
 * at a location on the map.
 * 
 * Operator will also print statistics at the end.
 * 
 * Methods:
 * 
 * - compare(Van[] vans, Passenger p) --> returns int[3] with cheapest van and 
 * 										  insertion indices for pickup and dropoff
 * 										  to main.
 * 
 * - simpleCompare(Van[] vans, Passenger p) --> returns int[2] with cheapest van and
 * 										  		insertion index for pickup to main. 
 * 
 * 
 * KNOWN PROBLEMS:
 * 	- we thought simple algorithm would be worse than ours,
 * 		but this seems to not be the case.
 *
 ***************************************************************************************/
public class Operator {
	static int currentTime = 0; //real-time incrementation
	static int timeCap = 20; //max time we'll make a passenger stay in the van
	public Operator() {
		
	}
	
		
	
	/* Our alternate operator algorithm. Returns the cheapest van and 
	 * its recommended insertion indices for pickup and dropoff. */
	public static int[] compare(Van[] vans, Passenger p) {
		int[] rv = new int[3];
		rv[0] = -1;
		rv[1] = -1;
		rv[2] = -1;
		int[] checkvans = new int[3];
		int cost = Integer.MAX_VALUE;
		for (int i=0; i<vans.length; i++) {			
			checkvans = vans[i].check(p, currentTime);			
			if (checkvans[0] < cost) {
				cost = checkvans[0];
				rv = checkvans;
				rv[0] = i;
			}
		}
		return rv;
	}
	
	/* Simple operator algorithm : Returns the cheapest van and the 
	 * recommended pickup insertion index. */
	public static int[] simpleCompare(Van[] vans, Passenger p) {		
		int[] rv = new int[2];
		rv[0] = -1;
		rv[1] = -1;
		int[] checkvans = new int[2];
		int cost = Integer.MAX_VALUE;		
		for (int i=0; i<vans.length; i++) {			
			checkvans = vans[i].simpleCheck(p, currentTime);			
			if (checkvans[0] < cost) {
				cost = checkvans[0];
				rv = checkvans;
				rv[0] = i;				
			}
		}
		return rv;
	}
	
	/* main: draw animation for one trial and print statistics */
	public static void main(String[] args){
		
		if (args.length < 3) {
			System.out.println("Usage: mapfile experimentfile mapscale [optional seed]");
		}
		
		Map campus = new Map(args[0]);
		Experiment e = new Experiment(args[1]);
		PassengerSample ps = new PassengerSample(campus, e);
		int mapscale = Integer.parseInt(args[2]);
        
		ObsManager o = new ObsManager(campus);
		ObsManager simpleo = new ObsManager(campus);
		
		// random seed
		long seed;
        if (args.length == 4)
            seed = Long.parseLong(args[3]);
        else
            seed = System.currentTimeMillis();       
        PassengerSample.initRandGen(seed);
        
        // make two groups of vans
		Van[] vans = new Van[e.numVan]; //for alternate algorithm
		Van[] svans = new Van[e.numVan]; //for simple algorithm
		for(int i = 0;i < e.numVan;i++) {
			vans[i] = new Van(campus, o, e.startAddress, e.vanCap);
			svans[i] = new Van(campus, simpleo, e.startAddress, e.vanCap);
		}
		
		// make two images: 
		// graph shows animation of vans (svans not animated)
		// count shows frequency of passengers.
		GenDraw graph = campus.mkMapCanvas(mapscale);	
		GenDraw count = campus.mkMapCanvas(mapscale);
		campus.drawMap(count);
		
		// begin simulation:
		while(currentTime <= e.endTime) {
			graph.clear();
			campus.drawMap(graph);
			
			// update van positions in map
			for(int i=0;i < vans.length;i++) {				
				vans[i].updatePos(currentTime);
				//System.out.println("simple:");
				svans[i].updatePos(currentTime);
			}
			
			// get new passenger who calls at current time
			Passenger nextPass = ps.getNextPassenger(currentTime);		
			while(nextPass != null) {
				System.out.println("ps.getNextPassenger produces nextPass = "+nextPass);
				
				// assign this passenger to a van, and update that van's route
				int[] results = compare(vans, nextPass);
				System.out.println("We choose Van "+results[0]);
				Van bestvan = vans[results[0]];
				int indexp = results[1];
				int indexd = results[2];
				bestvan.assign(nextPass, indexp, indexd);
				bestvan.updateRoute();
				bestvan.printRoute(results[0]);
				
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
			
			// draw passengers and van on map, then show image
			for(int i=0;i < vans.length;i++) {				
				campus.drawPassengers(vans[i], graph);
			}
			campus.drawVan(vans, currentTime, graph);			
			graph.show();			

			currentTime++;
			System.out.printf("increase t from %d to %d\n", currentTime-1, currentTime);		
			
		}
		o.frequency(count);
		count.show();
		
		// data
		// if we had more time, we would make this write to a file and 
		// make TrialManager read the output file
		System.out.println("DATA:        numPass = "+o.getNumPass());
		System.out.println("DATA: NEW    avg total time is "+o.avgTotalTime());
		System.out.println("DATA: NEW    avg sdTT is "+o.sdTT());
		System.out.println("DATA: NEW    avg inVan time is "+o.avgInVanTime());
		System.out.println("DATA: NEW    avg sdVT is "+o.sdVT());
		System.out.println("DATA: NEW    avg mdist is "+o.avgMDist());
		System.out.println("DATA: SIMPLE avg total time is "+simpleo.avgTotalTime());
		System.out.println("DATA: SIMPLE avg sdTT is "+simpleo.sdTT());
		System.out.println("DATA: SIMPLE avg inVan time is "+simpleo.avgInVanTime());
		System.out.println("DATA: SIMPLE avg sdVT is "+simpleo.sdVT());
		System.out.println("DATA: SIMPLE avg mdist is "+simpleo.avgMDist());
	}
}