/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;

import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 *
 */
public class Booked extends OrderAgentState {

	public Booked(OrderAgentInitial orderAgent) {
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
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
		if ( intentionAnts.isEmpty()) 
			return;
		sortIntentionArts(); 
		Iterator<IntAnt> i = intentionAnts.iterator();
		while (i.hasNext()) { //at the moment just select the first one
			IntAnt iAnt = i.next();
			 //order isn't interested, yet it could be refreshing of a previous booking
				Delivery d = deliveryExists(orderPlan, iAnt.getCurrentUnit().getDelivery());
				if (refreshBooking(orderPlan, currTime, iAnt, d)) {
					// we shouldn't touch order state
					orderPlan.putInRefreshTimes(iAnt.getCurrentUnit().getDelivery(), currTime);
					iAnt.getCurrentUnit().setOrderReply(Reply.ACCEPT); //order fully booked, so now reply full accept
					logger.debug(orderAgent.getOrder().getId() + "O int-" + iAnt.getOriginator().getId()+" booking refreshed");
				}
				else //now its sure order isn't interested!
					iAnt.getCurrentUnit().setOrderReply(Reply.REJECT);
			sendAccordingToNextUnit(currTime, iAnt);
			i.remove();
		}
	}
	@Override
	protected void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) {
		// TODO Auto-generated method stub
		
	}
}
