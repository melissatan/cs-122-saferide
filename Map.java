/*******************************************************************************
 * tclo, melissatan
 * 
 * Public Methods in Map:
 * ------------------------
 * 
 * int Mdist(Address from, Address to) --> computes Manhattan distance between from and to * 
 * int distance(Address from, Address to) --> gets shortest distance between from and to, from path()
 * Address[] route(Address from, Address to) --> gets shortest route between from and to, from path()
 * 
 * GenDraw mkMapCanvas(int scale) --> makes the image to be drawn on.
 * void drawMap(GenDraw im) --> draws map on im. 
 * void drawVan(Van[] vans, int currentTime, im) --> draws vans and the time ticker, on im.
 * void drawPassengers(Van, im) --> draws passengers in van, on im.
 * 
 * bool isReachable(Address) --> checks if address is reachable.
 * int[] findCoords(Address) --> gets array coords of address.
 * 	
 *******************************************************************************/


import java.io.*;
import java.util.*;
import java.awt.*;

public class Map {
	public int NUM_EW, NUM_NS; //NUM_EW = total no. of e-w running roads.
	public String[] EW_list, NS_list; //EW: streets that run e-w e.g. 55th
	private Node[][] grid;
	
	
	/* compute Manhattan distance between two addresses */
	public int Mdist(Address from, Address to) {
		if (from.equals(to))
			return 0;
		return Mdist(findNode(from), findNode(to));
	}	
	
	/* compute Manhattan distance between two address nodes */
	private int Mdist(Node start, Node goal) {
			int xdist, ydist;
			xdist = Math.abs(start.here.NS - goal.here.NS);
			ydist = Math.abs(start.here.EW - goal.here.EW);
		return xdist + ydist;
	}

	/* A* Distance Algorithm: shortest path between two addresses*/
	private Pair path(Address from, Address to) {
		if (!isReachable(from) || !isReachable(to))
			System.out.println("Error: Map.path must take in reachable addresses");
		
		PriorityQueue<Node> OpenPQ = new PriorityQueue<Node>();
		LinkedList<Node> CloseList = new LinkedList<Node>(); 
		
		if (from.equals(to)) {
			Address[] route = {to};
			return new Pair<Integer, Address[]>(0, route);
		}
		
		Node start = findNode(from);
		Node goal = findNode(to);
				
		OpenPQ.add(start); 
		
		// use Manhattan distance to goal as heuristic
		start.hscore = Mdist(start, goal);
		start.fscore = start.hscore;
		
		while(!OpenPQ.isEmpty()) {
			Node cheapest = OpenPQ.poll();
			CloseList.add(cheapest);
			// check neighbors in all four directions:
			for(int i = 0; i < 4; i++) {
				if(cheapest.neighbor[i] != null && !CloseList.contains(cheapest.neighbor[i])) {
					// if neighbor isn't on openPQ, compute its scores and add it to openPQ
					if(!OpenPQ.contains(cheapest.neighbor[i])) { 
						cheapest.neighbor[i].parent = cheapest;
						cheapest.neighbor[i].gscore = cheapest.gscore + 1;
						cheapest.neighbor[i].hscore = Mdist(cheapest.neighbor[i], goal);
						cheapest.neighbor[i].fscore = cheapest.neighbor[i].gscore + cheapest.neighbor[i].hscore;
						OpenPQ.add(cheapest.neighbor[i]);
						// if neighbor happens to be target destination, construct & return path
						if(cheapest.neighbor[i] == goal) {
							Pair path = constructPath(start, goal);
							// reset lists
							while(!OpenPQ.isEmpty()) {
								Node n = OpenPQ.remove();
								n.fscore = n.gscore = n.hscore = 0;
								n.parent = null;
							}
							while(!CloseList.isEmpty()) {
								Node n = CloseList.removeFirst();
								n.fscore = n.gscore = n.hscore = 0;
								n.parent = null;
							}
							return path;
						}
					}
					// if the neighbor is already on openPQ, update its gscore if we 
					// can get lower gscore by passing through current cheapest node.
					else { 
						if(cheapest.gscore + 1 < cheapest.neighbor[i].gscore) {
							cheapest.neighbor[i].parent = cheapest;
							cheapest.neighbor[i].gscore = cheapest.gscore + 1;
							cheapest.neighbor[i].fscore = cheapest.neighbor[i].gscore + cheapest.neighbor[i].hscore;
							OpenPQ.remove(cheapest.neighbor[i]);
							OpenPQ.add(cheapest.neighbor[i]);
						} 
						// if not, do nothing
					}
				}
			}
		}
		
		// if we couldn't get a path and openPQ is empty, empty closelist
		while(!CloseList.isEmpty()) {
			Node n = CloseList.removeFirst();
			n.fscore = n.gscore = n.hscore = 0;
			n.parent = null;
		}		
		return null;
	}
	
	/* computes the distance and sequence of addresses in shortest path. */
	private Pair constructPath(Node start, Node goal) {
		Node n = goal;
		int dist = 0;
		while(n != start) {			
			n = n.parent;
			dist++;
		}
		Address[] route = new Address[dist+1];
		n = goal;
		int i = dist;
		while(n != null) {
			route[i] = n.here;
			i--;
			n = n.parent;
		}
		return new Pair<Integer, Address[]>(dist, route);
	}
		
	/* Returns dist of shortest path */
	public int distance(Address from, Address to) {		
		Pair<Integer, Address> path = path(from, to);		
		return path.first;
	}
	
	/* Returns the sequence of addresses in the shortest path */
	public Address[] route(Address from, Address to) {
		Pair<Integer, Address[]> path = path(from, to);
		return path.second;
	}
	
	/* Make map image, for drawing on */
	public GenDraw mkMapCanvas(int n) {
		// n indicates how large a map we want to make.
		int width = NUM_NS;
		int height = NUM_EW;
		
		GenDraw im = new GenDraw("Coverage map");
		im.setCanvasSize(width * n, height * n);
		// the offsets ensure enough space in margins.
		im.setXscale(0.0, width * 1.0 + 0.5);
		im.setYscale(0.0, height* 1.0 + 1.0);
		
		return im;
	}
	
	/* Draw map tiles onto map image */
	public void drawMap(GenDraw im) {
		int width = NUM_NS;
		int height = NUM_EW;
	 				
		// Walk down each column and draw tiles.
		for (int j=0; j < width; j++) {			
			for (int i=0; i < height; i++) {		
				Node mapnode = getNode(i,j);
				if (mapnode.reachable) {
					// for debugging node directions we can call printNode(mapnode).
					boolean n, s, e, w;
					n = s = e = w = false;
					if (mapnode.north != null)
						n = true;
					if (mapnode.south != null)
						s = true;
					if (mapnode.east != null)
						e = true;
					if (mapnode.west != null)
						w = true;
					// mapTile is a custom GenDraw function.
					im.mapTile(j+1, Math.abs(i-height)-0.5, 0.5, n, s, e, w); 
				}
				// if node !reachable, don't bother drawing
			}	
		}
		// Include street names in margins
		im.setPenColor(GenDraw.BLUE);
		for (int i = 0; i < height; i++) {
			im.text(0.0, Math.abs(i-height)-0.5, EW_list[i]);
		}
		for (int j = 0; j < width; j++) {
			im.rotText(j+1, height+0.9, 90, NS_list[j]);
		}
	}
	
	
	/* Draw vans on map image and draw current time*/
	public void drawVan(Van[] vans, int currentTime, GenDraw im) {
		int height = NUM_EW;
		// to help differentiate vans.
		final Color[] vancolors = {GenDraw.MAGENTA, GenDraw.ORANGE,
								   GenDraw.GREEN, GenDraw.CYAN};		
		for (int k = 0; k < vans.length; k++) {
			Address vanpos = vans[k].here;
			Node vanNode = findNode(vanpos);
			int i = vanNode.here.EW;
			int j = vanNode.here.NS;
			im.setPenColor(vancolors[k%vancolors.length]);
			// represent van with a circle plus its index no.
			im.circle(j+1, Math.abs(i-height)-0.5, 0.47);
			im.circle(j+1, Math.abs(i-height)-0.5, 0.46);
			im.circle(j+1, Math.abs(i-height)-0.5, 0.45);
			String s = Integer.toString(k);
			im.text(j+0.6, Math.abs(i-height), s);
			if (vans[k].isFull())
				im.text(j+1.4, Math.abs(i-height), "FULL!");
		}
		// also draw time ticker near topleft corner
		im.setPenColor(GenDraw.RED);
		im.text(0.3, height+0.4, Integer.toString(currentTime));		
	}
	
	/* Draw each van's waiting passengers on Map */
	public void drawPassengers(Van v, GenDraw im) {
		int height = NUM_EW;
		// first draw their pickup locations, slightly NW of tile center
		Passenger[] plist = v.numWaiting(true);
		for (int k=0;k<plist.length;k++) {
			Address ppos = plist[k].pickUp;
			Node pnode = findNode(ppos);
			int i = pnode.here.EW;
			int j = pnode.here.NS;
			// pickups are drawn in red & yellow, plus passenger id
			im.setPenColor(GenDraw.RED);
			im.filledCircle(j+0.95, Math.abs(i-height)-0.45, 0.15);
			String s= Integer.toString(plist[k].id);
			im.text(j+1.3, Math.abs(i-height)-0.25, s);
			im.setPenColor(GenDraw.YELLOW);
			im.filledCircle(j+0.95, Math.abs(i-height)-0.45, 0.1);
		}
		// next draw their dropoff locations, slightly SE of tile center
		Passenger[] dlist = v.numWaiting(false);
		for (int k=0;k<dlist.length;k++) {
			Address ppos = dlist[k].dropOff;
			Node dnode = findNode(ppos);
			int i = dnode.here.EW;
			int j = dnode.here.NS;
			// dropoffs are drawn in blue & green, plus passenger id
			im.setPenColor(GenDraw.BLUE);
			im.filledCircle(j+1.05, Math.abs(i-height)-0.55, 0.14);			
			String s= Integer.toString(dlist[k].id);
			im.text(j+1.3, Math.abs(i-height)-0.25, s);
			im.setPenColor(GenDraw.CYAN);
			im.filledCircle(j+1.05, Math.abs(i-height)-0.55, 0.09);
		}
	}	
	
	/* Map constructor. Scans file to populate grid (file must have a particular format) */
	public Map(String Filename) {
		try{
			Scanner line = new Scanner(new File(Filename));
			line.useDelimiter("\n");
			NUM_EW = line.nextInt();
			NUM_NS = line.nextInt();
			
			// make the grid (2d node array):
			grid = new Node[NUM_EW][NUM_NS];
			
			// populate the list of street names from file
			Scanner EW_names = new Scanner(line.next());
			Scanner NS_names = new Scanner(line.next());
			EW_list = new String[NUM_EW];
			NS_list = new String[NUM_NS];
			EW_names.useDelimiter(",");
			NS_names.useDelimiter(",");			
			for(int i = 0; i < NUM_EW; i++) {
				EW_list[i] = EW_names.next();
			}
			for(int i = 0; i < NUM_NS; i++) {
				NS_list[i] = NS_names.next();
			}
			
			// put nodes into every grid cell.
			for(int i = 0; i < NUM_EW; i++) {			
				for(int j = 0; j < NUM_NS; j++) {
					grid[i][j] = new Node(new Address(EW_list[i], NS_list[j]));
					grid[i][j].here.EW = i;
					grid[i][j].here.NS = j;
				}
			}
			
			// For each E-W (horizontal) street,
			for(int i = 0; i < NUM_EW; i++) {
				if(!line.next().equals(EW_list[i])) 
					System.out.print("File Error: E-W street name doesn't match");
				// walk across, left to right.
				for(int j = 0; j < NUM_NS; j++) {
					Scanner direction = new Scanner(line.next());
					direction.useDelimiter(",");
					// file uses 1 & 0 to denote which directions we can exit this
					// location, in nsew order e.g. 1,0,0,1=can only leave by N and W.
					if(direction.nextInt() == 1 && i > 0) 
						grid[i][j].north = grid[i][j].neighbor[0] = grid[i-1][j];						
					if(direction.nextInt() == 1 && i < NUM_EW) 
						grid[i][j].south = grid[i][j].neighbor[1] = grid[i+1][j];				
					if(direction.nextInt() == 1 && j < NUM_NS)
						grid[i][j].east = grid[i][j].neighbor[2] = grid[i][j+1];
					if(direction.nextInt() == 1 && j > 0)
						grid[i][j].west = grid[i][j].neighbor[3] = grid[i][j-1];
				}
			}
			
			// check if each node is reachable(at least one entrance and one exit).
			for(int i = 0; i < NUM_EW; i++) {				
				for(int j = 0; j < NUM_NS; j++) {
					// first, check if we can enter it
					if(i < NUM_EW-1 && grid[i+1][j].north != null)
						grid[i][j].reachable = true;
					if(i > 0 && grid[i-1][j].south != null)
						grid[i][j].reachable = true;
					if(j > 0 && grid[i][j-1].east != null)
						grid[i][j].reachable = true;
					if(j < NUM_NS-1 && grid[i][j+1].west != null)
						grid[i][j].reachable = true;
					// then, check if we can exit it
					int blocks = 0;
					for(int k = 0; k < 4; k++) {
						if(grid[i][j].neighbor[k] == null)
							blocks++;
					}
					if(blocks == 4)
						grid[i][j].reachable = false;
				}
			}
		}
		catch(FileNotFoundException ex) {
		    System.out.println("File not Found");
		    System.exit(0);
		}		
	}
	
	
	
	/* Constructor for Node */
	private class Node implements Comparable<Node> {
		Address here; // stores node's address in map
		boolean reachable; // true if node is reachable by van
		Node[] neighbor = new Node[4];
		Node north, south, east, west, parent;
		int fscore, gscore, hscore; //for path algorithm
		
		public Node(Address a) {
			here = a;
			reachable = false;
			fscore = gscore = hscore = 0;
			north = south = east = west = parent = null;
		}
		
		// node comparator, used in path algorithm.
		public int compareTo(Node n) {
			double v = this.fscore - n.fscore;
			if(v < 0)
				return -1; 		
			else return 1;
		}
	}
		
	/* tells us if an address is reachable */
	public boolean isReachable(Address a) {
		return findNode(a).reachable;		
	}
		
	/* return int coords of an address */
	public int[] findCoords(Address a) {
		int[] rv = new int[2];
		int m, n;		
		m = n = -1;	
		// first get EW coord
		for(int i=0; i < EW_list.length;i++) {
			if(EW_list[i].equals(a.EW_name)) {
				m = i;				
				break;	
			}
		}
		// next get NS coord
		for(int j=0; j < NS_list.length; j++) {
			if(NS_list[j].equals(a.NS_name)) {
				n = j;
				break;
			}
		}
		// if at least of the coords can't be found
		if(m < 0 || n < 0)
			return null;
		rv[0] = m;
		rv[1] = n;
		return rv;
	}
	
	
	/* takes in an address and returns the specific 
	 * node that is at that address. Doesn't check reachability*/
	private Node findNode(Address a) {
		int[] coords = findCoords(a);
		if (coords != null)
			return getNode(coords[0],coords[1]);		
		return null;
	}
	
	/* retrieves the node at the i,j position in grid */
	private Node getNode(int i, int j) {
		if(i >= NUM_EW || i < 0 || j >= NUM_NS || j < 0) {
			System.out.println("Map.getNode error: Target out of range in Map.getNode.");
			return null;
		}
		Node rv = grid[i][j];
		if (rv == null)
			System.out.println("Map.getNode error: Node you're trying to get doesn't exist!");		
		return rv;		
	}
	
	/* print node to test if Node was made correctly with the right directions */
	private void printNode(Node n) {
		if (n == null) {
			System.out.println("Map.printNode error: node is null");
			return;
		}
		String rv = n.here + " has dir: ";
		if (n.north != null)
			rv += "n ";
		else rv += "!n ";
		
		if (n.south != null)
			rv += "s ";
		else rv += "!s ";
		
		if (n.east != null)
			rv += "e ";
		else rv += "!e ";
		
		if (n.west != null)
			rv += "w ";
		else rv += "!w ";
		
		System.out.println(rv);
	}
	
	/* prints out contents of EW_list and NS_list in "grid" form */
	public String toString() {
		String s = "";
		for(int i = 0; i < NUM_EW; i++) {
			for(int j = 0; j < NUM_NS; j++) {
				s = s + EW_list[i]+"&"+NS_list[j]+" ";
			}
			s = s +"\n";
		}
		return s;
	}
}