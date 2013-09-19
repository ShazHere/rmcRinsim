/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.Queue;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.communication.Message;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentPlan;

/**
 * @author Shaza
 *
 */
public class OrderStrategyDelMAS extends OrderCommunicationStrategy{

	/**
	 * @param orderAgentInitial 
	 * 
	 */
	public OrderStrategyDelMAS(OrderAgentInitial orderAgentInitial) {
		super(orderAgentInitial);
	}

	@Override
	public void executeStrategy(TimeLapse timeLapse, OrderAgentPlan orderPlan) {
		state.processExplorationAnts(orderPlan, timeLapse.getStartTime()); 
		state.processIntentionAnts(orderPlan, timeLapse); 
		state.changeOrderPlan(orderPlan, timeLapse.getStartTime());
		state.checkDeliveryStatuses(orderPlan, timeLapse.getStartTime());
		state.sendFeasibilityInfo(orderPlan, timeLapse.getStartTime());
		//state.doSpecial(orderPlan, timeLapse);
	}

	@Override
	public void handleMessages(TimeLapse timeLapse) {
		Queue<Message> messages = orderAgent.receiveMessages();
		if (messages.size() > 0) {
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					state.addExpAnt((ExpAnt)m);
				}
				else if (m.getClass() == IntAnt.class) {
					state.addIntAnt((IntAnt)m);
				}
			}
		}		
	}

}
