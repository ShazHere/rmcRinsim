/**
 * 
 */
package shaz.rmc.core;



/**
 * @author Shaza
 * @Desciiption originated by Order agent, to call for higher then d1 deliveries( i.e d2++) 
 * @date 8/11/2011
 */
public class FeasibilityAnt extends Ant {
	
	private int id = 0;
	private static int totalants = 0;

	
	public FeasibilityAnt(Agent a) {
		super (a);
		id = totalants;
		totalants++;
	}
	
	public boolean sendTo() {
		
		return false;
		
	}
	
}
