/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.ArrayList;
import java.util.Queue;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.communication.Message;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentPlan;
import shaz.rmc.pdpExtended.delMasInitial.TruckCoalition;

/**
 * @author Shaza
 *
 */
public class OrderStrategyCoalition  extends OrderCommunicationStrategy{

	TruckCoalition tc;
	public OrderStrategyCoalition(OrderAgentInitial orderAgentInitial) {
		super(orderAgentInitial);
		tc = null;
		

	}

	/**
	 * @return the tc
	 */
	public TruckCoalition getTc() {
		return tc;
	}

	/**
	 * @param tc the tc to set
	 */
	public void setTc(TruckCoalition tc) {
		this.tc = tc;
	}
	public void addMemebers(ArrayList<DeliveryTruckInitial> truckList) 
	{
		tc.addMemebers(truckList);
	}

	@Override
	public void executeStrategy(TimeLapse timeLapse, OrderAgentPlan orderPlan) {
		state.processExplorationAnts(orderPlan, timeLapse.getStartTime()); 
		state.processIntentionAnts(orderPlan, timeLapse); 
		state.changeOrderPlan(orderPlan, timeLapse.getStartTime());
		state.checkDeliveryStatuses(orderPlan, timeLapse.getStartTime());
		state.sendFeasibilityInfo(orderPlan, timeLapse.getStartTime());

		state.doSpecial(orderPlan, timeLapse);
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
//				else if (m.getClass() == CommitmentAnt.class) {
//					logger.info(order.getId() + "O COMMITMENT reply received");
//					this.commitmentAnts.add((CommitmentAnt)m);
//				}
			}
		}			
	}

}
