/**
 * 
 */
package shaz.rmc.core;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
//import shaz.rmc.pdpExtended.masDisco.RmcDelivery;
//import shaz.rmc.pdpExtended.masDisco.RmcOrderAgent;
//import shaz.rmc.pdpExtended.masDisco.RmcProductionSite;

/**
 * @author Shaza
 *
 */
public class TruckScheduleUnit {
	private final Delivery delivery; //TODO The type should not be RmcDlivery, rather, it shud b some common type like prodcution site. BUt abi kaam chuloa mamla hay..:(
	private final Agent truck;
	private final TimeSlot timeSlot; //It includes full slot, including loading time at station, then St to CY time then unloading time then CY to next ST time include travel time etc.

//	//TODO: Should be added some where else, since not all truck unit scheule will be usint it..
//	private Reply psReply;  //ps can have NO_REPLY, UNDERPROCESS, WEEK_ACCEPT, REJECT
//	private Reply orderReply;  //order can NO_REPLY, UNDERPROCESS, ACCEPT, WEEK_ACCEPT, REJECT
//	private boolean isAddedInTruckSchedule;
//	
	public TruckScheduleUnit(Agent pTruck) {
		delivery = null;
		timeSlot = null;
		truck = pTruck;
//		psReply = Reply.NO_REPLY;
//		orderReply = Reply.NO_REPLY;
//		isAddedInTruckSchedule = false;
	}
	public TruckScheduleUnit(Agent pTruck, TimeSlot pSlot, Delivery pDelivery) {
		truck = pTruck;
		timeSlot = pSlot;
		delivery = pDelivery;
//		psReply = Reply.NO_REPLY;
//		orderReply = Reply.NO_REPLY;
	}

	public Delivery getDelivery() {
		return delivery;
	}

//	public void setDelivery(Delivery delivery) {
//		this.delivery = delivery;
//	}

	public TimeSlot getTimeSlot() {
		return timeSlot;
	}

//	public void setTimeSlot(TimeSlot timeSlot) {
//		this.timeSlot = timeSlot;
//		// The startLocation at this time slot represnets the return station of the truck after the delivery of this uit is made.
//	}


//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		
//		sb.append("Delivery[");
//		sb.append("\n  order=").append(((RmcOrderAgent)delivery.getOrder()).getOrder().getId());
//		sb.append("\n  truck=").append(truck.getId());
//		sb.append("\n Unit start time=").append(timeSlot.getStartTime());
//		sb.append("\n  loading time=").append(delivery.getDeliveryTime().minus(delivery.getLoadingDuration()).minus(delivery.getStationToCYTravelTime()));
//		sb.append("\n  departs from station = ").append(((RmcProductionSite)delivery.getLoadingStation()).getStation().getId() + 
//				" at time "+ delivery.getDeliveryTime().minus(delivery.getStationToCYTravelTime()));
//		sb.append("\n  unloading time=").append(delivery.getDeliveryTime());
//		sb.append("\n leaves CY at =").append(delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()));
//		sb.append("\n reaches at Station = ").append(((RmcProductionSite)delivery.getReturnStation()).getStation().getId() + 
//				" at time "+delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()).plus(delivery.getCYToStationTravelTime()));
//		sb.append("\n Unit end time=").append(timeSlot.getEndTime());
//
//		sb.append("]");
//		
//		return sb.toString();
//	}
	public String getSummary() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Delivery[");
		if (delivery != null)
			sb.append("\n  order=").append(((OrderAgentInitial)delivery.getOrder()).getOrder().getId());
		sb.append("\n  truck=").append(truck.getId());
		sb.append("\n Unit start time=").append(timeSlot.getStartTime());
		sb.append("\n  loading time=").append(delivery.getDeliveryTime().minus(delivery.getLoadingDuration()).minus(delivery.getStationToCYTravelTime()));
		//sb.append("\n  departs from station = ").append(((ProductionSite)delivery.getLoadingStation()).getStation().getId() + 
			//	" at time "+ delivery.getDeliveryTime().minus(delivery.getStationToCYTravelTime()));
		if (delivery != null)
			sb.append("\n  unloading time=").append(delivery.getDeliveryTime());
		sb.append("\n leaves CY at =").append(delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()));
		//sb.append("\n reaches at Station = ").append(((RmcProductionSite)delivery.getReturnStation()).getStation().getId() + 
			//	" at time "+delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()).plus(delivery.getCYToStationTravelTime()));
		sb.append("\n Unit end time=").append(timeSlot.getEndTime());

		sb.append("]");
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return this.getSummary();
	}
//	public Reply getPsReply() {
//		return psReply;
//	}
//	public void setPsReply(Reply psReply) {
//		this.psReply = psReply;
//	}
//	public Reply getOrderReply() {
//		return orderReply;
//	}
//	public void setOrderReply(Reply orderReply) {
//		this.orderReply = orderReply;
//	}
//	public double getFixedCapacityAmount() {
//		return fixedCapacityAmount;
//	}
//	public void setFixedCapacityAmount(double fixedCapacityAmount) {
//		this.fixedCapacityAmount = fixedCapacityAmount;
//	}
//	public boolean isAddedInTruckSchedule() {
//		return isAddedInTruckSchedule;
//	}
//	public void setAddedInTruckSchedule(boolean isAddedInTruckSchedule) {
//		this.isAddedInTruckSchedule = isAddedInTruckSchedule;
//	}
//

}