/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import rinde.sim.core.TimeLapse;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentPlan;
import shaz.rmc.pdpExtended.delMasInitial.TruckAgentState;

/**
 * @author Shaza
 * @date 22/08/2013
 *
 */
public abstract class TruckCommunicationStrategy {

	
	public TruckAgentState getState() {
		return state;
	}
	//TODO: check if i shud completely exclude state form TruckAgent?
	public void setState(TruckAgentState newState) {
		this.state = newState;
	} 
	protected DeliveryTruckInitial truckAgent;
	protected TruckAgentState state;
	
	/**
	 * 
	 */
	public TruckCommunicationStrategy() {
	}
	
	public abstract void executeStrategy(TimeLapse timeLapse);
	public abstract void handleMessages(TimeLapse timeLapse);
	

}
