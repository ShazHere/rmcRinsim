/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 * @date 23/07/2013
 * To represent plans of the orderAgent. One plan refers to the concept that a an ST_DELAY is fixed (to specific time) and then 
 * rest of the deliveries are tried to be scheduled accordingly. If all deliveries found, then all become 'ACCEPT' and then order
 * state = BOOKED. if any of the delivery is unable to be booked, after specific time, new OrderAgentPlan will be made with 
 * increased ST_DELAY.
 * 
 * This class should be visible only to orderAgentINitial (or may be OrderState Classes). 
 *  
 **/
public class OrderAgentPlan {
	private final Logger logger; //for logging
	private final ArrayList<Delivery> deliveries; //virtual deliveries..twhich stores general information of the deliveries that have been intended by other trucks
	private final OrderAgentInitial orderAgent;
	
	private final Map<Delivery, DateTime> refreshTimes; //for keepting track of when to evaporate intentions
	private final Map<Delivery, Boolean> isPhysicallyCreated;
	private final Map<Delivery, Boolean> isConfirmed;
	
	//REalted to current interest
	private DateTime interestedTime;  //time sent by FeaAnts, to indicate this is the time at which order is interested
	private int interestedDeliveryNo; //the no. of current delivery 
	private int remainingToBookVolume; //=  totalConcrete - (deliverable concrete by all deliveries)
	private final int totalConcreteRequired; //taken form agent.order.
	private DateTime timeForLastIntention;// last time when an iAnt is accepted with status 'UNDERPROCESS'. Doesn't mean the lastTime an intention get refreshed.
	
	private final Duration delayStartTime; //delay in startTime
	
	protected OrderAgentPlan(Duration pDelayStartTime, OrderAgentInitial pOrderAgent, DateTime pCurrTime){
		logger = Logger.getLogger(OrderAgentInitial.class);
		orderAgent = pOrderAgent;
		timeForLastIntention = pCurrTime;
		deliveries = new ArrayList<Delivery>();
		interestedTime = pOrderAgent.getOrder().getStartTime().plus(pDelayStartTime);
		if (pCurrTime.compareTo(interestedTime) >= 0) { // this could be the case when after a delivery failure, OrderPlan was re-initialized.
			interestedTime = pCurrTime.plusMinutes(GlobalParameters.MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED);
			delayStartTime = new Duration(orderAgent.getOrder().getStartTime(), interestedTime);
		}
		else
			delayStartTime = pDelayStartTime;
		checkArgument (pCurrTime.compareTo(interestedTime) < 0, true); 
		interestedDeliveryNo = 0;
		
		remainingToBookVolume = pOrderAgent.getOrder().getRequiredTotalVolume();
		totalConcreteRequired = pOrderAgent.getOrder().getRequiredTotalVolume();
		
		refreshTimes = new LinkedHashMap<Delivery, DateTime>();
		isConfirmed =new LinkedHashMap<Delivery, Boolean>();
		isPhysicallyCreated = new LinkedHashMap<Delivery, Boolean>();
	
	}
	/**
	 * should not be called for refresh deliveries, rather it should be called for the deliveries for which order was really interested.
	 * @param iAnt the ant under process
	 * @param currTime
	 */ 
	protected void acceptIntentionArangementInOrder(IntAnt iAnt, DateTime currTime) {
		checkArgument(orderAgent.getOrderState() == OrderAgentState.IN_PROCESS, true);
		refreshTimes.put(iAnt.getCurrentUnit().getDelivery(), currTime);
		deliveries.add(iAnt.getCurrentUnit().getDelivery());
		isPhysicallyCreated.put(iAnt.getCurrentUnit().getDelivery(), false);
		isConfirmed.put(iAnt.getCurrentUnit().getDelivery(), false);
		timeForLastIntention = currTime;
		//setOrderInterests();
	}
	/**
	 * @param iAnt te ant under process
	 */ 
	protected void setOrderInterests(DateTime currTime) {
		checkArgument(orderAgent.getOrderState() == OrderAgentState.IN_PROCESS);//Actually I will be called from OrderStateWaiting.processIntentionAnts, 
		//but before calling this method, orderState object should have been changed
		remainingToBookVolume = calculateRemainingVolume();
		if (!deliveries.isEmpty()) {
			Delivery lastDelivery = deliveries.get(deliveries.size()-1);
			interestedTime = lastDelivery.getDeliveryTime().plus(lastDelivery.getUnloadingDuration());
			interestedDeliveryNo = lastDelivery.getDeliveryNo() +1;
		}
		else{
			interestedTime = orderAgent.getOrder().getStartTime(); 
			interestedDeliveryNo = 0;
		}			
		if (remainingToBookVolume <= 0) {
			orderAgent.setOrderState(OrderAgentState.BOOKED);
			logger.info(orderAgent.getOrder().getId() + "O fully BOOKED");
		}
			
		
	}
	private int calculateRemainingVolume() {
		int remainingVolume = totalConcreteRequired;
		if (!deliveries.isEmpty()){
			for (Delivery d : deliveries) {
				remainingVolume -= d.getDeliveredVolume()-d.getWastedVolume();
			}
		}
		if (remainingVolume < 0)
			remainingVolume = 0;
		return remainingVolume;
	}
	/**
	 * Generates actual parcels corresponding to the deliveries, to be picked up physically by trucks. 
	 * Parcels are created at the production Site where they have to be picked. 
	 * @param startTime
	 */
	protected void generateParcelDeliveries(OrderAgentInitial pOrderAgent, long startTime) {
		//checkArgument(getOrderState() == OrderAgentState.BOOKED, true); should be checked when i make the change of truck that truck delivers only ACCEPTED deliveries
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (!deliveries.isEmpty()) {
			int dNo = 0;
			int cushionMinutes = 5; //mintues before pickup time, when delivery should be created.
			for (Delivery d : deliveries) { //5 min cushion time, 5min for PS fillling time
				DateTime createTime = d.getDeliveryTime().minus(d.getStationToCYTravelTime()).minusMinutes(cushionMinutes).minusMinutes(GlobalParameters.LOADING_MINUTES); 
				if (currTime.compareTo(createTime) >= 0) {
					if (isPhysicallyCreated.get(d) == false && isConfirmed.get(d) == true){ //isReservd means isPhysically created..
						DeliveryInitial pd = new DeliveryInitial(pOrderAgent, d, dNo, d.getLoadingStation().getPosition(), 
								pOrderAgent.getPosition(), d.getLoadingDuration().getMillis(), d.getUnloadingDuration().getMillis(), (double)d.getDeliveredVolume());
						if (pOrderAgent.registerParcel(pd) == true) {
							logger.debug(pOrderAgent.getOrder().getId() + "O Delivery physically created..del NO = " + dNo + "current time = " + currTime + ", Loading time = " + createTime.plusMinutes(cushionMinutes));
							isPhysicallyCreated.put(d, true);
						}
					}
				}
				dNo ++;
			}
		}
	}
	/**
	 * @param d
	 */
	protected void reSetOrderDelivery(Delivery d) {
		checkArgument(orderAgent.getOrderState() == OrderAgentState.WAITING, true);
		interestedTime = d.getDeliveryTime();
		interestedDeliveryNo = d.getDeliveryNo();
		deliveries.remove(d);
		refreshTimes.remove(d);
		isConfirmed.remove(d); 
		isPhysicallyCreated.remove(d);
		orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
		logger.info(orderAgent.getOrder().getId() + "O Order Delivery Reset");
	}
	public DateTime getInterestedTime() {
		return interestedTime;
	}
	public int getInterestedDeliveryNo() {
		return interestedDeliveryNo;
	}
	public int getRemainingToBookVolume() {
		return remainingToBookVolume;
	}
	
	protected List<Delivery> getDeliveries() {
		return   (List<Delivery>) Collections.unmodifiableList(this.deliveries);
	}
	//TODO this should be refactored with getIsCOnfirmedD(Delivery d)
	protected Map<Delivery, Boolean> getIsConfirmed () {
		return Collections.unmodifiableMap(this.isConfirmed);
				
	} 
	// TODO this should be refactored with getRefreshTimesD(Delivery d)
	protected Map<Delivery, DateTime> getRefreshTimes() {
		return Collections.unmodifiableMap(this.refreshTimes);
				
	} 
	protected Duration getDelayStartTime() {
		return delayStartTime;
	}
	protected int getDelayStartTimeInMin() {
		return (int) delayStartTime.getStandardMinutes();
	}
	protected DateTime getTimeForLastIntention() {
		return timeForLastIntention;
	}
	
	protected void putInRefreshTimes(Delivery d, DateTime dt) {
		refreshTimes.put(d, dt);
	}
	protected void putInIsConfirmed(Delivery d, boolean isConfirm) {
		isConfirmed.put(d, isConfirm);
	}
	/**
	 * @param currTime
	 * @return true if refresh rates of all deliveries are less than intention_evaporation rate. 
	 * else false
	 */
	protected boolean areDeliveriesRefreshing(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		for (Delivery d : deliveries) {
			long currMilli = currTime.getMillis() - refreshTimes.get(d).getMillis();
			if (new Duration (currMilli).getStandardMinutes() >= GlobalParameters.INTENTION_EVAPORATION_MIN) 
				return false;
		}
		return true;
	}
	/**
	 * @param currTime
	 * @return the smallest deliveryno, whose refresh rate wasn't according to INTENTION_EVAPORATION rate.
	 * Must only be called after areDeliveriesRefreshing(long currTime) returns false;
	 */
	protected Delivery getFailedDelivery(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		for (Delivery d : deliveries) {
			long currMilli = currTime.getMillis() - refreshTimes.get(d).getMillis();
			if (new Duration (currMilli).getStandardMinutes() >= GlobalParameters.INTENTION_EVAPORATION_MIN) 
				return d;
		}
		return null;
	}

	protected void removeDeliveriesAccordingTofFailedDeliveryAndCurrentTime(Delivery failedDel, DateTime currTime) {
		ArrayList<Delivery> removableDel = new ArrayList<Delivery>();
		for (Delivery d : deliveries) {
			if (deliveries.indexOf(d) > deliveries.indexOf(failedDel) || d.getLoadingTime().compareTo(currTime) <= 0)// || loadingTime of d is < currTime then delete too
				removableDel.add(d);
		}
		removableDel.add(failedDel);
		checkArgument(removableDel.isEmpty() == false, true);
		for (Delivery del: removableDel)
			deliveries.remove(del);
		checkArgument(isDeliveryOrderCorrect() == true, true);
	}
	private boolean isDeliveryOrderCorrect() {
		for (int i = 0; i< deliveries.size()-1; i ++){
			if (deliveries.get(i).getDeliveryNo() >= deliveries.get(i+1).getDeliveryNo())
				return false;
		}
		return true;
	}
	
}
