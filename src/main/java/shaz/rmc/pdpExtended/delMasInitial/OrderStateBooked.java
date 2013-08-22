/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;


import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;

/**
 * @author Shaza
 *
 */
public class OrderStateBooked extends OrderAgentState {

	public OrderStateBooked(OrderAgentInitial orderAgent) {
		super(orderAgent);
	}
	@Override
	int getStateCode() {
		return OrderAgentState.BOOKED;
	}
	
	@Override
	protected void processExplorationAnts(OrderAgentPlan orderPlan,long startTime) {
	//If order is fully booked, but still explorations are there then ignore 
		explorationAnts.clear();
	}
	
	@Override
	protected void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		//already order booked, no need to send feasibility info now.
		return;
	}
	@Override
	protected void processIntentionAnts(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		refreshDeliveryBookings(orderPlan, timeLapse);
	}
	@Override
	protected void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void checkDeliveryStatuses(OrderAgentPlan orderPlan, long startTime) {
		if (!orderPlan.areDeliveriesRefreshing(startTime)){
			orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
			if (!makeOrderPlanAdjustment(orderPlan, startTime))
				orderAgent.setOrderState(OrderAgentState.UNDELIVERABLE);
		}
		
	}
}
