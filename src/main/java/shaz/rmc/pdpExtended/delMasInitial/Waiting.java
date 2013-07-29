/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;


import java.util.ArrayList;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.TimeLapse;
import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 *
 */
public class Waiting extends OrderAgentState{
	public Waiting(OrderAgentInitial orderAgent) {
		super(orderAgent);
	}
	@Override
	int getStateCode() {
		return OrderAgentState.WAITING;
	}
	@Override
	protected void processExplorationAnts(OrderAgentPlan orderPlan, long startTime) {
	//If order is in wait state, but still explorations are there then ignore 
		explorationAnts.clear();
	}
	
	@Override
	protected void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (isFeasibilityIntervalPassed(currTime) ){
			Delivery d = checkOrderStatus(orderPlan, startTime);
			if (d != null){
				orderPlan.reSetOrderDelivery(d);
				orderAgent.sendFAntToPS();
			}
			orderAgent.setTimeForLastFeaAnt(currTime);
		}
	}
	/**
	 * Called when order is WAITING	for the confirmation from the Truck Agent. If the WAIT gets too long, the interestedTime, 
	 * and interested DeliveryNo need to be reconsidered. So this method checks all the deliveries, if any of t hem isn't refreshed,
	 * then it will Re-set order.
	 * @param currMilli current time
	 * @return The delivery which caused the Re-set of order
	 */
	private Delivery checkOrderStatus(OrderAgentPlan orderPlan, long currMilli) {
		long currMilliInterval;
		for (Delivery d : orderPlan.getDeliveries()){
			currMilliInterval = GlobalParameters.START_DATETIME.plusMillis((int)currMilli).getMillis() - orderPlan.getRefreshTimes().get(d).getMillis();
			if (orderPlan.getIsConfirmed().get(d) == false 
					&& (new Duration (currMilliInterval)).getStandardMinutes() > GlobalParameters.INTENTION_EVAPORATION_MIN) {
				return d;
			}
		}
		return null;
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
				if (refreshBooking(orderPlan, currTime, iAnt, d)) { //so its not just recently added delivery. second condition is added since there could be 2 intention ants from same truck
					if (orderPlan.getIsConfirmed().get(iAnt.getCurrentUnit().getDelivery()) == false ){
						orderPlan.putInIsConfirmed(iAnt.getCurrentUnit().getDelivery(), true);
						orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
						orderPlan.setOrderInterests();
					} //else we shouldn't touch order state
					orderPlan.putInRefreshTimes(iAnt.getCurrentUnit().getDelivery(), currTime);
					iAnt.getCurrentUnit().setOrderReply(Reply.WEEK_ACCEPT); //So earlier it could be UnderProcess, but once confirmed, its Weekly accepted
					logger.debug(orderAgent.getOrder().getId() + "O int-" + iAnt.getOriginator().getId()+" booking refreshed");
				}
				else //now its sure order isn't interested!
					iAnt.getCurrentUnit().setOrderReply(Reply.REJECT);
			sendAccordingToNextUnit(currTime, iAnt);
			i.remove();
		}
	}
}
