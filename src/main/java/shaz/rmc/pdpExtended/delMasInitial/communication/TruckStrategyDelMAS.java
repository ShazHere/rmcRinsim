/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.Queue;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.communication.Message;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;

/**
 * @author Shaza
 *
 */
public class TruckStrategyDelMAS extends TruckCommunicationStrategy {


	public TruckStrategyDelMAS(DeliveryTruckInitial pTruckAgent) {
		truckAgent = pTruckAgent;
	}

	@Override
	public void executeStrategy(TimeLapse timeLapse) {
		state.processIntentionAnts(timeLapse.getStartTime());
		state.processOrderPlanInformerAnts(timeLapse.getStartTime());
		state.sendExpAnts(timeLapse.getStartTime());
		state.sendIntAnts(timeLapse.getStartTime());
		state.followPracticalSchedule(timeLapse);
		state.letMeBreak(timeLapse.getStartTime());
	}

	@Override
	public void handleMessages(TimeLapse timeLapse) {
		Queue<Message> messages = truckAgent.receiveMessages();
		if (messages.size() > 0) {
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					state.addExpAnt((ExpAnt)m);
				}
				else if (m.getClass() == IntAnt.class) {
					state.addIntAnt((IntAnt)m);
				}
				else if (m.getClass() == OrderPlanInformerAnt.class){
					state.addOrderPlanInformerAnt((OrderPlanInformerAnt) m);
				}
			}
		}		
	}

}
