/**********************************************************************
 * tclo, melissatan
 * 
 * Public Methods:
 * -----------------
 *  Address getFirst() --> returns address of first event in sched
 *  
 * 	int updatePos(int currentTime) --> updates van's position 
 *  void updateRoute() 			   --> updates the van's route based on sched  
 *  void printRoute() 			   --> prints route.
 * 
 * 	Passenger[] numWaiting() --> returns array of passengers waiting for this van
 * 
 * 	int[] check(Passenger, int currentTIme) --> returns the cheapest pickup and dropoff positions 
 *  int[] simpleCheck(Passenger, int currentTime) --> returns cheapest pickup position
 * 
 * 	void assign(Passenger, int i, int j) --> inserts Passenger into schedule at i (pickup) and j (dropoff) 
 *  void simpleAssign(Passenger, int i)  --> inserts Passenger into schedule at i(pickup) and tail(dropoff)   
 * 
 * 	Passenger find(Passenger p, bool pickdrop) --> returns passenger, either pickup or dropoff
 *  
 *  boolean cancel(Passenger) --> removes Passenger, and returns bool to check success of remove
 *  (we didn't have the time to implement a passenger order system that would use this function)
 *  
 *  boolean isFull()  --> checks if sched is at or over capacity
 *  boolean isEmpty() --> checks if sched is empty
 * 
 * ******************************************************************* */

import java.util.*;

public class Van {
	public Map m;
	public ObsManager o;
	public Address here;
	public Address[] route;
	public int capacity, ri; // so we can easily retrieve route[ri]	

	private LinkedList<PassengerPair> schedule;

	public Van(Map m, ObsManager o, Address startAddress, int capacity) {
		this.m = m;
		this.o = o;
		this.here = startAddress;
		this.capacity = capacity;
		this.schedule = new LinkedList<PassengerPair>();
	}

	

	/* return the Address at the head of the schedule. */ 
	public Address getFirst() {
		Address rv = (schedule.getFirst().p.pickdrop) ? schedule.getFirst().p.pickUp : schedule.getFirst().p.dropOff;
		return rv;
	}

	/* updates van position from current to next pos on route, after
	 assignment. */
	public void updatePos(int currentTime) {
		if (route == null) {
			//System.out.printf("Van %d: no route, stays at %s\n",	vanID, here);
			return;
		}
		// if street flag, don't increment ri yet:
		while (route.length == 1) {
			PassengerPair removeme = schedule.removeFirst();
			System.out.println("REMOVE (street flag)"+removeme);		
			if (removeme.p.pickdrop){
				removeme.p.pickUpTime = currentTime; // we don't use this, but just to be safe
				Passenger counterpart = find(removeme.p, false);
				counterpart.pickUpTime = currentTime;				
			}
			else 										
				o.recordPass(removeme.p, currentTime);

			//System.out.println("Van " + vanID + ": REMOVE " + removeme);
			//System.out.println("Van " + vanID + " new sched: " + schedule);			
			updateRoute();
		}
		
		ri++;
		here = route[ri];
		//System.out.printf("Van %d updatePos: %d->%d out of total length %d\n", vanID, ri - 1, ri, route.length);		
		
		// otherwise, for regular pickups and dropoffs:
		while (route != null && ri == route.length - 1) {
			PassengerPair removeme = schedule.removeFirst();
			//System.out.println("REMOVE "+removeme);
			// if we're picking up someone,
			if (removeme.p.pickdrop){
				removeme.p.pickUpTime = currentTime;
				// look for the passenger's scheduled dropoff event
				Passenger counterpart = find(removeme.p, false);
				//if (counterpart == null) {
				//	System.out.println("van.updatepos error: pickup doesnt have dropoff inserted");
				//	System.out.println(schedule);
				//}
				counterpart.pickUpTime = currentTime;
			}
			else 										
				o.recordPass(removeme.p, currentTime);
			//System.out.print("Prior to REMOVE, Van " + vanID + " is at " + here + "; ");
			//System.out.println("REMOVE " + removeme);
			//System.out.println("Van " + vanID + " new sched: " + schedule);
			updateRoute();
		}
		//System.out.println("Van " + vanID + ": current Address=" + here);
	}
	
	/* updates van's route. */
	public void updateRoute() {
		PassengerPair next = schedule.peek();
		if (next == null) {
			here = route[ri];
			route = null;
			ri = 0;
			//System.out.println("Van stops here:" + here);
			return;
		}
		Address destination = (next.p.pickdrop) ? next.p.pickUp	: next.p.dropOff;
		route = m.route(here, destination);
		ri = 0;
	}
	
	/* print route of van, for checking */
	public void printRoute(int vanID) {
		if (route == null)
			return;
		int rl = route.length;
		String s = "";
		for (int i = 0; i < rl; i++) {
			s += route[i] + "-";
		}
		System.out.println("Van " + vanID + " gets new route: " + s);
	}

	

	/* Function that returns all passengers either waiting for this van to
	 * pick them up (pickdrop == true) or drop them off (pickdrop==false). */
	public Passenger[] numWaiting(boolean waitingPick) {
		int len = schedule.size();
		LinkedList<PassengerPair> plist = new LinkedList<PassengerPair>();
		for (int i = 0; i < len; i++) {
			PassengerPair ith = schedule.get(i);
			if (waitingPick && ith.p.pickdrop)
				plist.add(ith);
			else if (!waitingPick && !ith.p.pickdrop)
				plist.add(ith);
		}
		int count = plist.size();
		Passenger[] rv = new Passenger[count];
		for (int i = 0; i < count; i++) {
			rv[i] = plist.get(i).p;
		}
		return rv;
	}

	/* get lowest cost and the recommended pickup and dropoff indexes, 
	 * within this van, to Operator. */
	public int[] check1(Passenger student, int currentTime) {
		int extradist, bestp, bestd, pcost, dcost;
		bestp = bestd = -1;
		pcost = dcost = Integer.MAX_VALUE;
		int[] rv = new int[3];

		PassengerPair aboveP, belowP, aboveD, belowD;
		Address addressAbove, addressBelow;
		int distOld, distAbove, distBelow;

		if (schedule.isEmpty()) {
			//System.out.println("For van at " + here	+ ", we are checking a empty sched:");
			rv[0] = m.distance(here, student.pickUp);
			rv[1] = 0;
			rv[2] = 0;
			return rv;
		}

		if (schedule.size() == capacity) {
			//System.out.println("full capacity!");
			rv[0] = Integer.MAX_VALUE;
			rv[1] = 0;
			rv[2] = 0;
			return rv;
		}

		int len = schedule.size();
		//System.out.print("For van at " + here + ", we are checking a non-empty sched: ");
		//System.out.println(schedule);
		// *** DECIDE PICKUP ***
		for (int i = 0; i <= len; i++) {
			int weightsum = 1;
			if (i == 0)
				addressAbove = here;
			else {
				aboveP = schedule.get(i - 1);
				addressAbove = (aboveP.p.pickdrop) ? aboveP.p.pickUp : aboveP.p.dropOff;
			}
			if (i == len) {
				addressBelow = null;
				extradist = m.distance(addressAbove, student.pickUp);
			} else {
				belowP = schedule.get(i);
				addressBelow = (belowP.p.pickdrop) ? belowP.p.pickUp
						: belowP.p.dropOff;
				distOld = m.distance(addressAbove, addressBelow);
				distAbove = m.distance(addressAbove, student.pickUp);
				distBelow = m.distance(student.pickUp, addressBelow);
				extradist = distAbove + distBelow - distOld;
			}
			for (int k = i; k < len; k++) {
				weightsum += schedule.get(k).p.weight(currentTime);
			}
			if (extradist * weightsum < pcost) {
				pcost = extradist * weightsum;
				bestp = i;
			}

		}
		// *** DECIDE DROPOFF ***
		for (int j = bestp; j <= len; j++) {
			int weightsum = 1;
			if (j == 0)
				addressAbove = here;
			else {
				aboveD = schedule.get(j - 1);
				addressAbove = (aboveD.p.pickdrop) ? aboveD.p.pickUp : aboveD.p.dropOff;
			}
			if (j == len) {
				addressBelow = null;
				extradist = m.distance(addressAbove, student.dropOff);
			} else {
				belowD = schedule.get(j);
				addressBelow = (belowD.p.pickdrop) ? belowD.p.pickUp : belowD.p.dropOff;
				distOld = m.distance(addressAbove, addressBelow);
				distAbove = m.distance(addressAbove, student.dropOff);
				distBelow = m.distance(student.dropOff, addressBelow);
				extradist = distAbove + distBelow - distOld;
			}
			for (int k = j; k < len; k++) {
				weightsum += schedule.get(k).p.weight(currentTime);
			}
			if (extradist * weightsum < dcost) {
				dcost = extradist * weightsum;
				bestd = j;
			}
		}
		rv[0] = pcost + dcost;
		rv[1] = bestp;
		rv[2] = bestd;
		return rv;
	}
	
	/* check cost and indexes of lowest cost insertion */
	public int[] check(Passenger student, int currentTime) {
		int extradistP, extradistD, bestp, bestd, cost;
		bestp = bestd = -1;
		cost = Integer.MAX_VALUE;
		int[] rv = new int[3];

		PassengerPair aboveP, belowP, aboveD, belowD;
		Address addressAboveP, addressBelowP, addressAboveD, addressBelowD;
		int distOldP, distAboveP, distBelowP, distOldD, distAboveD, distBelowD;

		if (schedule.isEmpty()) {
			//System.out.println("For van at " + here + ", we are checking a empty sched:");
			rv[0] = m.distance(here, student.pickUp);
			rv[1] = 0;
			rv[2] = 1;
			return rv;
		}

		/*if (isFull()) {
			System.out.println(schedule);
			System.out.println("full capacity!");
			rv[0] = Integer.MAX_VALUE;
			rv[1] = schedule.size();
			rv[2] = schedule.size()+1;
			return rv;
		}
		 */
		int len = schedule.size();
		//System.out.print("For van at " + here + ", we are checking a non-empty sched: ");
		//System.out.println(schedule);
		// *** DECIDE PICKUP ***
		for (int i = 0; i <= len; i++) {
			int weightsumP = 1;
			int pcost;
			int dcost = Integer.MAX_VALUE;
			int bestpd = -1;
			if (i == 0)
				addressAboveP = here;
			else {
				aboveP = schedule.get(i - 1);
				addressAboveP = (aboveP.p.pickdrop) ? aboveP.p.pickUp : aboveP.p.dropOff;
			}
			if (i == len) {
				addressBelowP = null;
				extradistP = m.distance(addressAboveP, student.pickUp);
			} else {
				belowP = schedule.get(i);
				addressBelowP = (belowP.p.pickdrop) ? belowP.p.pickUp : belowP.p.dropOff;
				distOldP = m.distance(addressAboveP, addressBelowP);
				distAboveP = m.distance(addressAboveP, student.pickUp);
				distBelowP = m.distance(student.pickUp, addressBelowP);
				extradistP = distAboveP + distBelowP - distOldP;
			}
			for (int k = i; k < len; k++) {
				weightsumP += schedule.get(k).p.weight(currentTime);
			}
			pcost = weightsumP * extradistP;

			for (int j = i + 1; j <= len + 1; j++) {
				int weightsumD = 1;
				if (j == i + 1)
					addressAboveD = student.pickUp;
				else {
					aboveD = schedule.get(j - 2);
					addressAboveD = (aboveD.p.pickdrop) ? aboveD.p.pickUp : aboveD.p.dropOff;
				}
				if (j == len + 1) {
					addressBelowD = null;
					extradistD = m.distance(addressAboveD, student.dropOff);
				} else {
					belowD = schedule.get(j - 1);
					addressBelowD = (belowD.p.pickdrop) ? belowD.p.pickUp : belowD.p.dropOff;
					distOldD = m.distance(addressAboveD, addressBelowD);
					distAboveD = m.distance(addressAboveD, student.dropOff);
					distBelowD = m.distance(student.dropOff, addressBelowD);
					extradistD = distAboveD + distBelowD - distOldD;
				}
				for (int k = j; k < len + 1; k++) {
					weightsumD += schedule.get(k - 1).p.weight(currentTime);
				}
				if (weightsumD * extradistD < dcost) {
					dcost = extradistD * weightsumD;
					bestpd = j;
				}
			}
			if (pcost + dcost < cost) {
				cost = pcost + dcost;
				bestp = i;
				bestd = bestpd;
			}
		}
		rv[0] = cost*(1+len/10);
		rv[1] = bestp;
		rv[2] = bestd;
		return rv;
	}
	
	public int[] simpleCheck(Passenger student, int currentTime) {
		int[] rv = new int[2];
		if (schedule.isEmpty()) {
			//System.out.println("For van at " + here	+ ", we are checking a empty sched:");
			rv[0] = m.Mdist(here, student.pickUp);
			rv[1] = 0;
			return rv;
		}
		// if the only scheduled event is a dropoff:
		if (schedule.size()==1 && !schedule.getFirst().p.pickdrop) {
			rv[0] = m.Mdist(here, student.pickUp);
			rv[1] = 1;
			return rv;
		}
		// else, check for best place to pickup
		rv[0] = Integer.MAX_VALUE;
		rv[1] = -1;		
		int len = schedule.size();
		assert (len != 0);
		for(int i=0; i<len; i++) {
			PassengerPair event = schedule.get(i);
			if(event.p.pickdrop) {
				int dist = m.Mdist(event.p.pickUp, student.pickUp);
				//System.out.println("simplecheck dist="+dist);
				//System.out.println("simplecheck rv0="+rv[0]);
				if(dist < rv[0]) {
					rv[0]= dist;
					rv[1] = i;
				}
			}		
		}
		return rv;
	}

	/* assign Passenger to van, with pickup at i and dropoff at j. */
	public void assign(Passenger student, int i, int j) {
		insert(student, i, true);
		if (i == 0)
			updateRoute();
		insert(student, j, false);
		System.out.println("sched: " + schedule);
	}
	
	/* assign Passenger to van, at the tail */
	public void simpleAssign(Passenger student, int i) {
		int len = schedule.size();
		insert(student, i, true);
		insert(student, len+1, false);
		System.out.println("simple sched: "+schedule);
	}

	/* Function to insert a pickup or dropoff event into van's schedule, and
		recalculate distToNext for the events before and after. */
	private void insert(Passenger student, int i, boolean pick) {
		PassengerPair above, below;
		Address addressAbove, addressBelow;
		PassengerPair incoming = null;

		Passenger studentP = new Passenger(student.id, student.callTime, student.pickUp, student.dropOff);
		studentP.pickdrop = true;

		Passenger studentD = new Passenger(student.id, student.callTime, student.pickUp, student.dropOff);
		studentD.pickdrop = false;

		student = (pick) ? studentP : studentD;
		Address event = (student.pickdrop) ? student.pickUp : student.dropOff;

		if (i == 0) {
			// this must also be a pickup.
			assert(pick == true);
			above = null;
			addressAbove = here;
			if (schedule.isEmpty()) {
				//System.out.print("van.insert at head when schedule empty -- ");
				incoming = new PassengerPair(student, m.distance(student.pickUp, student.dropOff));
			} else {
				below = schedule.get(i);
				addressBelow = (below.p.pickdrop) ? below.p.pickUp : below.p.dropOff;
				//System.out.print("van.insert.i=0.schedule !Empty: ");
				int distBelow = m.distance(student.pickUp, addressBelow);
				incoming = new PassengerPair(student, distBelow);
			}
			//System.out.println("add incoming student to head of schedule: " + incoming);
			schedule.addFirst(incoming);
			return;
		} else if (i > 0 && i == schedule.size()) {
			// can be either.
			above = schedule.get(i - 1);
			addressAbove = (above.p.pickdrop) ? above.p.pickUp : above.p.dropOff;
			below = null;
			addressBelow = null;

			above.distToNext = m.distance(addressAbove, event);
			incoming = new PassengerPair(student, 0);
			//System.out.println("add incoming student to tail of schedule: " + incoming);
			schedule.addLast(incoming);
			return;
		} else {
			//System.out.println("middle of sched:"+i+" out of length " + schedule.size());
			above = schedule.get(i - 1);
			addressAbove = (above.p.pickdrop) ? above.p.pickUp : above.p.dropOff;
			below = schedule.get(i);
			addressBelow = (below.p.pickdrop) ? below.p.pickUp : below.p.dropOff;
			above.distToNext = m.distance(addressAbove, event);
			int distBelow = m.distance(event, addressBelow);
			incoming = new PassengerPair(student, distBelow);
			//System.out.println("incoming student to middle of schedule:" + incoming);
			schedule.add(i, incoming);
			return;
		}

	}

	/* retrieve Passenger, either waiting for pickup or dropoff. */
	public Passenger find(Passenger p, boolean pickdrop) {
		PassengerPair ith = getPP(p, pickdrop);
		if (ith != null)
			return ith.p;
		return null;
	}

	/* retrieve PP given passenger and pickdrop */
	private PassengerPair getPP(Passenger p, boolean pickdrop) {
		int len = schedule.size();
		for (int i = 0; i < len; i++) {
			PassengerPair ith = schedule.get(i);
			if (ith.p.id == p.id && ith.p.pickdrop == pickdrop)
				return ith;
		}
		return null;
	}

	/* cancel a passenger who doesn't want pickup anymore. */
	public boolean cancel(Passenger flake) {
		PassengerPair cancelme = getPP(flake, true);
		if (cancelme.p.pickdrop == true)
			return schedule.remove(cancelme);
		System.out.println("error:trying to cancel a passenger, " + flake + ", that has no pickup scheduled");
		return false;
	}
	/* check if van is full */
	public boolean isFull() {		
		return (schedule.size() >= capacity);
	}
	
	/* check if van is empty. */
	public boolean isEmpty() {
		return schedule.isEmpty();
	}

	/* Subclass: PassengerPair holds a Passenger plus distToNext, i.e. distance
		to van's next destination (not necessarily the passenger's dropOff
		address). */
	private class PassengerPair {
		Passenger p;
		int distToNext;

		public PassengerPair(Passenger p, int distToNext) {
			this.p = p;
			this.distToNext = distToNext;
		}

		public String toString() {
			String s;
			if (p.pickdrop)
				s = "Pickup";
			else
				s = "Dropoff";
			return s + " " + p + ",with distToNext=" + distToNext;
		}
	}

	public String toString() {
		int len = schedule.size();
		String rv = "Van contains:";
		for (int i = 0; i < len; i++) {
			rv += schedule.get(i) + "\n";
		}
		return rv;
	}

}
