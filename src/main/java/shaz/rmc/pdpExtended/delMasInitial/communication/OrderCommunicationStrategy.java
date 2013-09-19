/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import rinde.sim.core.TimeLapse;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentPlan;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentState;

/**
 * @author Shaza
 * @date 22/08/2013
 */
public abstract class OrderCommunicationStrategy {

	protected OrderAgentState state;
	protected OrderAgentInitial orderAgent;
	/**
	 * @param orderAgentInitial 
	 * 
	 */
	public OrderCommunicationStrategy(OrderAgentInitial orderAgentInitial) {
		orderAgent = orderAgentInitial;
	}
	
	public abstract void executeStrategy(TimeLapse timeLapse, OrderAgentPlan orderPlan);
	public abstract void handleMessages(TimeLapse timeLapse);

	public OrderAgentState getState() {
		return state;
	}
	//TODO: check if i shud completely exclude state form TruckAgent?
	public void setState(OrderAgentState newState) {
		this.state = newState;
	} 
}
