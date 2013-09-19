/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.Queue;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.communication.Message;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.TruckAgentState;
import shaz.rmc.pdpExtended.delMasInitial.TruckStateTeamCommitment;

/**
 * @author Shaza
 *	In this strategy, possible states are TruckStateInProcess, TruckStateBroken, TruckStateTeamCommitment
 * State Transitions are not handled very consistently, Normal State is InProcess.Whenever truck receives any Commitment
 * ant, state = TruckStateTeamCommitment.
 *  
 * From InProcess or TeamCommitment, if canIBreak == true, it will simply break and will never return to function. 
 * 
 */
public class TruckStrategyCoalition extends TruckCommunicationStrategy {

	/**
	 * 
	 */
	public TruckStrategyCoalition(DeliveryTruckInitial pTruckAgent) {
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
		
		state.processGeneralAnts(timeLapse.getStartTime());
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
				else if (m.getClass() == CommitmentAnt.class) {
					if (!(state instanceof TruckStateTeamCommitment)) {
						truckAgent.setTruckState(TruckAgentState.TEAM_COMMITMENT);
					}
					state.addGeneralAnt((CommitmentAnt)m);
				}
				else if (m.getClass() == OrderPlanInformerAnt.class){
					state.addOrderPlanInformerAnt((OrderPlanInformerAnt) m);
				}
			}
		}	
	}

}
