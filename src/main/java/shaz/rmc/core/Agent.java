/**
 * 
 */
package shaz.rmc.core;

import rinde.sim.core.TickListener;
import rinde.sim.core.model.communication.CommunicationUser;

/**
 * @author Shaza
 *
 */
public interface Agent extends TickListener, CommunicationUser {
	
	public int getId();

}
