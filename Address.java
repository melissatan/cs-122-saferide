/* tclo, melissatan
 * 
 */
public class Address {	
	String EW_name, NS_name;	
	int EW, NS;         
	
	public Address(String EW, String NS) {
		this.EW_name = EW;
		this.NS_name = NS;
		// coordinate positions are set when map file is read in.
	}
	
	/* compares to see if this address is the same as a */
	public boolean equals(Address a) {
		return (this.EW_name.equals(a.EW_name) && this.NS_name.equals(a.NS_name));
	}
		
	public String toString() {
		return EW_name + "&" + NS_name;
	}
}
