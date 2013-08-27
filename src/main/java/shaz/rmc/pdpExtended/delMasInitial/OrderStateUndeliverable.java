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
	public
	int getStateCode() {
		return OrderAgentState.UNDELIVERABLE;
	}


	@Override
	public void processExplorationAnts(OrderAgentPlan orderPlan,
			long startTime) {
		explorationAnts.clear();
	}

	
	@Override
	public void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		return;
	}

	
	@Override
	public void processIntentionAnts(OrderAgentPlan orderPlan,
			TimeLapse timeLapse) {
		intentionAnts.clear();
	}


	@Override
	public void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) {
	}


	@Override
	public void checkDeliveryStatuses(OrderAgentPlan orderPlan,long startTime) {
		// Nothing needed, order already undeliverable!
		
	}


	@Override
	public void doSpecial(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

}