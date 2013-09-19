/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;
import rinde.sim.core.TimeLapse;
import shaz.rmc.pdpExtended.delMasInitial.communication.OrderStrategyCoalition;

/**
 * @author Shaza
 * @date 22/08/2013
 * The main difference from OrderStateBooked is this class will act differently if breakdown occurs. 
 */
public class OrderStateJointBooked extends OrderStateBooked {

	OrderStrategyCoalition strategy;
	public OrderStateJointBooked(OrderAgentInitial orderAgent) {
		super(orderAgent);
		checkArgument(orderAgent.getStrategy() instanceof OrderStrategyCoalition,true);
		strategy = (OrderStrategyCoalition)orderAgent.getStrategy();
	}
	
	@Override
	public void checkDeliveryStatuses(OrderAgentPlan orderPlan, long startTime) {
		if (!orderPlan.areDeliveriesRefreshing(startTime)){
			orderAgent.setOrderState(OrderAgentState.TEAM_NEED);
//			orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
//			if (!makeOrderPlanAdjustment(orderPlan, startTime))
//				orderAgent.setOrderState(OrderAgentState.UNDELIVERABLE);
		}
		
	}
	@Override
	public void doSpecial(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		if (strategy.getTc() == null) {//means tc havn't made so far and this is first time that this method is called.
			strategy.setTc(new TruckCoalition());
			strategy.addMemebers(orderPlan.getDeliveryTrucks());
		}
	}
}
