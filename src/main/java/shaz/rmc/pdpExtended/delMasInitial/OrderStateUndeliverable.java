/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import rinde.sim.core.TimeLapse;

/**
 * @author Shaza
 *
 */
public class OrderStateUndeliverable extends OrderAgentState {

	/**
	 * @param orderAgent
	 */
	public OrderStateUndeliverable(OrderAgentInitial orderAgent) {
		super(orderAgent);
	}


	@Override
	int getStateCode() {
		return OrderAgentState.UNDELIVERABLE;
	}


	@Override
	protected void processExplorationAnts(OrderAgentPlan orderPlan,
			long startTime) {
		explorationAnts.clear();
	}

	
	@Override
	protected void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		return;
	}

	
	@Override
	protected void processIntentionAnts(OrderAgentPlan orderPlan,
			TimeLapse timeLapse) {
		intentionAnts.clear();
	}


	@Override
	protected void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) {
	}


	@Override
	protected void checkDeliveryStatuses(OrderAgentPlan orderPlan,long startTime) {
		// Nothing needed, order already undeliverable!
		
	}

}
