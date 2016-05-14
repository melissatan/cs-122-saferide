/*
 * tclo, melissatan
 * 
 */
public class Passenger {
	int id;
	Address pickUp, dropOff;
	int callTime, pickUpTime;
	boolean pickdrop = true; //true if we need to pick him up, false if already in Van and needs to be dropped off.
	
	public Passenger(int id, int callTime, Address pickUp, Address dropOff) {
		this.id = id;
		this.callTime = callTime; 	// time at which passenger requests pickup		
		this.pickUp = pickUp; 		// pickup location
		this.dropOff = dropOff;		// dropoff location		
	}
	
	/* weight function, used in scheduling algorithm (Van.check) */
	public double weight(int currentTime) {
		// multiplier might still need to be tweaked
		return (currentTime - callTime)*0.02 + 1;
	}
	
	public String toString() {
		return "Passenger " + id + " going from "+pickUp+" to "+dropOff +" who called at "+ callTime;
	}
	
}
