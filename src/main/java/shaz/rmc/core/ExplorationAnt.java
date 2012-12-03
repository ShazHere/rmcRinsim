/**
 * 
 */
package shaz.rmc.core;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.domain.Delivery;

/**
 * @author Shaza
 * @description Exploration ant, used by RmcDeliveryAgent 
 * @date 8/11/2011
 *
 */
public class ExplorationAnt extends Ant {
	
	private int id = 0;
	private static int totalants = 0;
	
	//set by Delivery Agent
	private Agent originTruck; //primary agent, who is sending expAnt
	private ArrayList<TimeSlot> originTruckTimeSlots; //time after which truck is available
	private Duration timeToReach; //time to reach from current location/PC. includes loading time + duraiton to reach CY
	private Agent order; //order to which exploration ant will go
	private long createdTime;
	
	//set by OrderAgent
	private Delivery delivery; //delivery set by order if available
	private Delivery currentDelivery; //used temporaliy by orderAgent, to assign a delivery, before reply is sent to truck
	
	private boolean returned;  //to check if order has replied or not
	//private boolean firstDelivery; //to track that is this exploration related to first deliver of order
	//private DateTime deliveryPossibleAt; // to tell truck that delivery will be possible at time T
	
	public ExplorationAnt(Agent truck, long currentTime) {
		super(truck);
		this.id = totalants;
		totalants++;
		
		this.originTruck = truck;
		this.createdTime = currentTime;
		this.delivery = null;
		this.currentDelivery = null;
		this.order = null;
		//this.firstDelivery = false;
		//this.deliveryPossibleAt = null; //i don't think this is used any more..15/05/2012
		this.returned = false;
		this.originTruckTimeSlots = new ArrayList<TimeSlot>();
	}


	public long getCreatedTime() {
		return createdTime;
	}


	//@Override
	
	/**
	 * @param order to which the exp ant is sent
	 * @param truckAvailableTimeSlot timeslot at which truck is available. This is start time, the loading time and the duraiton to reach is not added in it
	 * @param timeToReach includes the loading time of the expected PC and the time to reach from that PC to CY. PC is mentioned in the available slot.
	 * @return true if successfully sent to order, else send false
	 */
	public boolean configure(Agent order, TimeSlot truckAvailableTimeSlot, Duration timeToReach) {
		// TODO Auto-generated method stub
		//firstDelivery = firstDel;
		this.order = order;
		TimeSlot st = new TimeSlot();
		DateTime start = new DateTime(truckAvailableTimeSlot.getStartTime().plus(timeToReach));
		st.setStartTime(start);
		DateTime end = new DateTime(truckAvailableTimeSlot.getEndTime());
		st.setEndtime(end);
		this.originTruckTimeSlots.add(st);
		this.timeToReach = timeToReach;
		this.originTruckTimeSlots.get(0).setLocationAtStartTime(truckAvailableTimeSlot.getLocationAtStartTime(), truckAvailableTimeSlot.getProductionSiteAtStartTime());

		//try {
			//order.addExplorationAnts(this);
			return true;
		//}
		//catch (Exception e){
			//return false;
		//}
	}
	public boolean reply(Delivery del) { //there should be proposed time as well..based on which truck will make intention
		try { 
			this.returned = true;
			this.delivery = del;
			//this.firstDelivery = isFirst;
			//this.deliveryPossibleAt = del.getDeliveryTime();  
			return true;
		}
		catch (Exception e){
			return false;
		} 
	}
	
	public boolean reply(boolean noDelivery) {
		try { 
			this.returned = true;
			this.delivery = null;
			//this.firstDelivery = false;
			return true;
		}
		catch (Exception e){
			return false;
		} 
	}

	
//	public boolean isFirstDelivery() {
//		return firstDelivery;
//		
//	}
	public boolean isReturned() {
		return returned;
	}
	
	public TimeSlot getAvailableTimeSlot() {
		return this.originTruckTimeSlots.get(0);
	}
	public Agent getDeliveryTruck() {
		return this.originTruck;
	}
	public Delivery getDelivery() {
		return this.delivery;
	}
	public Agent getOrder() {
		return this.order;
	}
//	public DateTime getDeliveryPossibleAt(){
//		return this.deliveryPossibleAt;
//	}
	public Duration getTimeToReach() {
		return timeToReach;
	}
	public void setTimeToReach(Duration timeToReach) {
		this.timeToReach = timeToReach;
	}
	public void arrangeTimeSlots(DateTime deliveryTime) { 
		this.originTruckTimeSlots.get(0).setStartTime(deliveryTime);
		//end time should depend upon which return station is chosen by intention ant latter..
		//TODO: what about truck has to get refilled again..:(...
		//this.originTruckTimeSlots.get(0).setStartTime(deliveryTime);
	}
	
	public Delivery getCurrentDelivery() {
		return currentDelivery;
	}

	public void setCurrentDelivery(Delivery currentDelivery) {
		this.currentDelivery = currentDelivery;
	}

}
