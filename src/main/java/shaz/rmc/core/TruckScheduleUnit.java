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

	public TruckScheduleUnit(Agent pTruck) {
		delivery = null;
		timeSlot = null;
		truck = pTruck;
	}
	public TruckScheduleUnit(Agent pTruck, TimeSlot pSlot, Delivery pDelivery) {
		truck = pTruck;
		timeSlot = pSlot;
		delivery = pDelivery;
	}

	public Delivery getDelivery() {
		return delivery;
	}

	public TimeSlot getTimeSlot() {
		return timeSlot;
	}

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

}