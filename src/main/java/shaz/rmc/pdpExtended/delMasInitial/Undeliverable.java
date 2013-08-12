/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import rinde.sim.core.TimeLapse;

/**
 * @author Shaza
 *
 */
public class Undeliverable extends OrderAgentState {

	/**
	 * @param orderAgent
	 */
	public Undeliverable(OrderAgentInitial orderAgent) {
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
		// TODO Auto-generated method stub

	}

}
