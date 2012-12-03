/**
 * 
 */
package shaz.rmc.core;

import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationUser;

/**
 * @author Shaza
 *
 */
public interface ProductionSite extends CommunicationUser {
	public Point getLocation();

}
