/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 * @date 1/07/2013
 * 
 * Abstract class to represents the abstract States of Order agent. State Pattern from GoF is followed. 
 * May contain methods, commonly used by all states,
 * and data members that do not requiring permenance similar to the permenance required by members of OrderAgentInitial class. 
 * State transition is handled by the concrete states.  
 */
abstract class OrderAgentState {
	protected final Logger logger; //for logging
	
	protected OrderAgentInitial orderAgent;
	protected ArrayList<ExpAnt> explorationAnts; 
	protected ArrayList<IntAnt> intentionAnts;
	
	public OrderAgentState(OrderAgentInitial orderAgent) {
		super();
		this.orderAgent = orderAgent;
		this.logger = Logger.getLogger(OrderAgentInitial.class);
		
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();

	}


	abstract int getStateCode();
	protected abstract void processExplorationAnts(OrderAgentPlan orderPlan, long startTime);
	/**
	 * manages to send feasibility ants to PS
	 * @param startTime
	 */
	protected abstract void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime);
	protected abstract void processIntentionAnts(OrderAgentPlan orderPlan, TimeLapse timeLapse) ;
	
	/**
	 * @param orderPlan
	 * @param startTime
	 * 
	 * Identifies if Order should change is start time and start a new plan? if yes then it starts with new plan
	 */
	protected abstract void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) ;

	/**
	 * @param orderPlan
	 * @param startTime
	 * Checks, if all the deliveries properly get refreshed? Otherwise, order will assume delivery failure and make adjustments accordingly. (10/08/2013)
	 * 
	 */
	protected abstract void checkDeliveryStatuses(OrderAgentPlan orderPlan, long startTime);
	
	static OrderAgentState newState(int newState, OrderAgentInitial orderAgent) {
		switch(newState) {
		case IN_PROCESS:
			return new InProcess(orderAgent);
		case WAITING:
			return new Waiting(orderAgent);
		case BOOKED:
			return new Booked(orderAgent);
		case UNDELIVERABLE:
			return new Undeliverable(orderAgent);
		case SERVED:
			return new Served(orderAgent);
			default:
				throw new IllegalArgumentException( "Illegal order  Agent State");
		}
	}
	
	/**
	 * @param currTime
	 * @param iAnt
	 */
	protected void sendAccordingToNextUnit(DateTime currTime, IntAnt iAnt) {
		logger.debug(orderAgent.getOrder().getId() + "O int-" + iAnt.getOriginator().getId()+" reply is " + iAnt.getCurrentUnit().getOrderReply() + " currTime= " + currTime);
		IntAnt newAnt = iAnt.clone(orderAgent);
		//newAnt.setNextCurrentUnit();
		//orderAgent.getcApi().send(iAnt.getCurrentUnit().getDelivery().getReturnStation(), newAnt);
		if (newAnt.setNextCurrentUnit()) 
			orderAgent.getcApi().send(newAnt.getCurrentUnit().getDelivery().getLoadingStation(), newAnt);
		else //send to origniator truck
			orderAgent.getcApi().send(newAnt.getOriginator(), newAnt);
	}
	
	/**
	 * @param currTime
	 * @param iAnt
	 * @param d
	 * @return
	 */
	protected boolean isRefreshBooking(OrderAgentPlan orderPlan, DateTime currTime, IntAnt iAnt,
			Delivery d) {  //so its not just recently added delivery. second condition is added since there could be 2 intention ants from same truck
		return d != null && orderPlan.getRefreshTimes().get(d).compareTo(currTime) < 0 
				&& iAnt.getCurrentUnit().getPsReply() == Reply.WEEK_ACCEPT  //make it check argument
				&& iAnt.getCurrentUnit().isAddedInTruckSchedule() == true;
	}
	
	/**
	 * chk for which delivery truck is intending
	 *  if for first deliver, then make it week.Accept
	 *  if not for first delivery, then chk are rest of deliveries before this delivery are intended? if no then
	 *  wait and see rest of intentions
	 *  
	 *  if a delivery week accepted, order shud change its interestedTime at the the PS by sending feasiblity ants
	 *       
	 *  Also set the logic, if no one is sending intention for long time
	 *  adjust the starttime delay and interestedtime delay..
	 */
	protected Delivery deliveryExists(OrderAgentPlan orderPlan, Delivery newDel) {
		for (Delivery existingDel: orderPlan.getDeliveries()) {
			if (existingDel.equals(newDel))
				return existingDel;
		}
		return null;
	}
	/**
	 * sorts intention ants according to schedule score
	 */
	protected void sortIntentionArts() {
/*this.intentionAnts
//		if (GlobalParameters.LAG_TIME_ENABLE) { //a temproary sorting mechanism to sort intention ants, so that we can execute lag time..TEsted as well
//			Collections.sort(this.intentionAnts, new Comparator<IntAnt>(){
//		        public int compare( IntAnt a, IntAnt b ){//sort descending order based in lag time
//		            return (int)(b.getScheduleLagTime().minus(a.getScheduleLagTime()).getStandardSeconds());
//		        }
//			});
//			return;
//		}*/
		Collections.sort(this.intentionAnts, new Comparator<IntAnt>(){
	        public int compare( IntAnt a, IntAnt b ){ //sort in ascending order based on schedule score
	            return a.getCurrentUnitScore() - b.getCurrentUnitScore();
	        }
		});
	}
	/**
	 * @param startTime
	 * @return
	 */
	protected boolean isFeasibilityIntervalPassed(DateTime currTime) {
		return currTime.minusMinutes(orderAgent.getTimeForLastFeaAntInMin()).getMinuteOfDay() >= GlobalParameters.FEASIBILITY_INTERVAL_MIN;
	}
	
	protected void addExpAnt(ExpAnt eAnt) {
		explorationAnts.add(eAnt);
	}
	protected void addIntAnt(IntAnt iAnt) {
		intentionAnts.add(iAnt);
	}
	
/**
	 * @param orderPlan
	 * @param startTime
	 * return true if orderPlanAdjustments made succesfully, and order was Deliverable
	 */
	protected boolean makeOrderPlanAdjustment(OrderAgentPlan orderPlan, long startTime) {
		Delivery d = orderPlan.getFailedDelivery(startTime);
		checkArgument(d != null, true);
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		logger.debug(orderAgent.getOrder().getId() + "O Delivery Failure detected; DeliveryNO = " + d.getDeliveryNo());
		//return orderPlan.adjustOrderPlanAfterFailedDelivery(d, currTime);
		 orderPlan.removeDeliveriesAccordingTofFailedDeliveryAndCurrentTime(d, currTime);
		 if (orderAgent.isOrderDeliverable(currTime)) {
			orderPlan.setOrderInterests(currTime);
			if (currTime.compareTo(orderPlan.getInterestedTime()) >= 0) 
				orderAgent.makeNewOrderPlan(currTime);
			return true;
		 }
		 else
			return false;
	}

		/**
 * @param orderPlan
 * @param timeLapse
 */
protected void refreshDeliveryBookings(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
	DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
	if ( intentionAnts.isEmpty()) 
		return;
	sortIntentionArts(); 
	Iterator<IntAnt> i = intentionAnts.iterator();
	while (i.hasNext()) { //at the moment just select the first one
		IntAnt iAnt = i.next();
		 //order isn't interested, yet it could be refreshing of a previous booking
			Delivery d = deliveryExists(orderPlan, iAnt.getCurrentUnit().getDelivery());
			if (isRefreshBooking(orderPlan, currTime, iAnt, d)) {
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

		static final int IN_PROCESS = 0; // The normal and general state
		static final int WAITING = 1; // order is waiting for a delivery's confirmation from TruckAgent
		static final int BOOKED = 2; //whole concrete of order is booked.
		static final int UNDELIVERABLE = 3; //order cannot be delivered in current day. 
		static final int SERVED = 4; //means no need to do anything. No effect of truck breakdown since order is completely served
		//Actually SERVED is only conceptually different from  UNDELIVERABLE. Served means every ting done and results should be including this order's results as well, but 
		// UNDELIVERABLE menas nothing is done..and not possible to be done. 
		
		//TEAM_NEED //order was fully booked, but then one of the team members broke, so rest of the team has to
					// fullfill the order as they committed 

		
}
