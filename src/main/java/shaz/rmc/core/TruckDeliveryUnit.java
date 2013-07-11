/**
 * 
 */
package shaz.rmc.core;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;

/**
 * @author Shaza
 * @date 11/07/2013
 * Make it immutable ..
 */
public class TruckDeliveryUnit extends TruckScheduleUnit {
	private final Delivery delivery; //TODO The type should not be RmcDlivery, rather, it shud b some common type like prodcution site. BUt abi kaam chuloa mamla hay..:(

	public TruckDeliveryUnit(Agent pTruck) {
		super(pTruck);
		delivery = null;

	}
	public TruckDeliveryUnit(Agent pTruck, TimeSlot pSlot, Delivery pDelivery) {
		super(pTruck, pSlot);
		delivery = pDelivery;
	}

	public Delivery getDelivery() {
		return delivery;
	}


	public String getSummary() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Delivery[");
		if (delivery != null)
			sb.append("\n  order=").append(((OrderAgentInitial)delivery.getOrder()).getOrder().getId());
		sb.append("\n  truck=").append(getTruck().getId());
		sb.append("\n Unit start time=").append(getTimeSlot().getStartTime());
		sb.append("\n  loading time=").append(delivery.getDeliveryTime().minus(delivery.getLoadingDuration()).minus(delivery.getStationToCYTravelTime())).append(", from Station=" + delivery.getLoadingStation());
		//sb.append("\n  departs from station = ").append(((ProductionSite)delivery.getLoadingStation()).getStation().getId() + 
			//	" at time "+ delivery.getDeliveryTime().minus(delivery.getStationToCYTravelTime()));
		if (delivery != null)
			sb.append("\n  unloading time=").append(delivery.getDeliveryTime());
		sb.append("\n leaves CY at =").append(delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()));
		//sb.append("\n reaches at Station = ").append(((RmcProductionSite)delivery.getReturnStation()).getStation().getId() + 
			//	" at time "+delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()).plus(delivery.getCYToStationTravelTime()));
		sb.append("\n Unit end time=").append(getTimeSlot().getEndTime()).append(" at station= " + delivery.getReturnStation());

		sb.append("]");
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return this.getSummary();
	}
}
