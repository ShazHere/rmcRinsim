/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.domain.Delivery;

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
	 * @param Delivery
	 * @param currTime
	 * @param isConfirmedDelivery if delivery Reply is UNDER_PROCESS then isConfirmed = false, if Delivery Reply is WEEKACCEPT then isConfirmed = true.
	 */ 
	protected void acceptIntentionArangementInOrder(Delivery del, DateTime currTime, boolean isConfirmedDelivery) {
		checkArgument(orderAgent.getOrderState() == OrderAgentState.IN_PROCESS ||
				orderAgent.getOrderState() == OrderAgentState.TEAM_NEED , true);
		refreshTimes.put(del, currTime);
		deliveries.add(del);
		isPhysicallyCreated.put(del, false);
		isConfirmed.put(del, isConfirmedDelivery);
		timeForLastIntention = currTime;
		sortDeliveries(deliveries);
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
			//interestedTime = orderAgent.getOrder().getStartTime(); //TODO there should be .plus(DelayStartTime)
			interestedTime = orderAgent.getOrder().getStartTime().plus(delayStartTime);
			interestedDeliveryNo = 0;
		}			
		if (remainingToBookVolume <= 0) {
			orderAgent.setOrderState(OrderAgentState.BOOKED);
			logger.info(orderAgent.getOrder().getId() + "O fully BOOKED");
		}
		this.timeForLastIntention = currTime;
		
	}
	protected int calculateRemainingVolume() {
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
			int cushionMinutes = GlobalParameters.MINUTES_BEFOR_DELIVERY_CREATED; 
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
		removeDelivery(d);
		orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
		logger.info(orderAgent.getOrder().getId() + "O Order Delivery Reset");
	}
	protected void removeDelivery(Delivery d) {
		deliveries.remove(d);
		refreshTimes.remove(d);
		isConfirmed.remove(d); 
		isPhysicallyCreated.remove(d);
		sortDeliveries(deliveries);
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
	
	protected ArrayList<DeliveryTruckInitial> getDeliveryTrucks() {
		checkArgument(orderAgent.getOrderState() == OrderAgentState.BOOKED );
		ArrayList<DeliveryTruckInitial> truckList = new ArrayList<DeliveryTruckInitial>();
		for (Delivery d : this.deliveries) {
			truckList.add((DeliveryTruckInitial)d.getTruck());
		}
		return truckList;
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
			if (new Duration (currMilli).getStandardSeconds() >= GlobalParameters.INTENTION_EVAPORATION_SEC) 
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
			if (new Duration (currMilli).getStandardSeconds() >= GlobalParameters.INTENTION_EVAPORATION_SEC) 
				return d;
		}
		return null;
	}

	public TimeSlot getTimeWindowFailedDelivery(Delivery d) {
		int index = deliveries.indexOf(d);
		DateTime delStartTime;
		DateTime delEndTime;
		//if-else below are made considereing if there was some lagtime between deliveries
		if (index>0)
			delStartTime = deliveries.get(index-1).getUnloadingFinishTime().plusMinutes(1);
		else
			delStartTime = d.getDeliveryTime().plusMinutes(1);
		if (index < deliveries.size()-1)
			delEndTime = deliveries.get(index+1).getDeliveryTime().minusMinutes(1);
		else
			delEndTime = d.getUnloadingFinishTime();
		return new TimeSlot(delStartTime, delEndTime);
	}
	
	protected void removeDeliveriesAccordingTofFailedDeliveryAndCurrentTime(Delivery failedDel, DateTime currTime) {
		ArrayList<Delivery> removableDel = new ArrayList<Delivery>();
		for (Delivery d : deliveries) {
			if (deliveries.indexOf(d) > deliveries.indexOf(failedDel))// || d.getLoadingTime().compareTo(currTime) <= 0)// || loadingTime of d is < currTime then delete too
				removableDel.add(d);
		}
		removableDel.add(failedDel);
		checkArgument(removableDel.isEmpty() == false, true);
		for (Delivery del: removableDel) {
			this.removeDelivery(del);
			orderAgent.sendOrderPlanInformerAnt(del);
		}
		checkArgument(isDeliveryOrderCorrect() == true, true);
	}
	private boolean isDeliveryOrderCorrect() {
		for (int i = 0; i< deliveries.size()-1; i ++){
			if (deliveries.get(i).getDeliveryNo() >= deliveries.get(i+1).getDeliveryNo())
				return false;
		}
		return true;
	}
	public boolean isOrderDeliveryingStarted(DateTime currTime) {
		if (deliveries.size() == 0)
			return false;
		if (currTime.compareTo(deliveries.get(0).getDeliveryTime()) >= 0)
			return true;
		else
			return false;
	}
	
	/**
	 * @param pSchedule
	 */
	private void sortDeliveries(ArrayList<Delivery> deliveries) {
		if (deliveries.size() <= 1)
			return;
		Collections.sort(deliveries, new Comparator<Delivery>(){
		    public int compare( Delivery a, Delivery b ){
		        return a.getDeliveryNo()- b.getDeliveryNo();
		    }
		});
	} 
}
