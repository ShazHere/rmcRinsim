/**
 * 
 */
package shaz.rmc.core;

import org.joda.time.Duration;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;

/**
 * @author Shaza
 * @date 11/07/2013
 * Make it immutable ..
 */
public class TruckDeliveryUnit extends TruckScheduleUnit {
	private final Delivery delivery;
	final private int wastedConcrete; // filed by truck while adding unit
	final private Duration lagTime;// for storing lagTime before this delivery

//	public TruckDeliveryUnit(Agent pTruck) {
//		super(pTruck);
//		delivery = null;
//
//	}
	public TruckDeliveryUnit(Agent pTruck, TimeSlot pSlot, Delivery pDelivery, int pWastedConcrete, Duration pLagTime) {
		super(pTruck, pSlot , pDelivery.getLoadingStation().getLocation(), pDelivery.getOrder().getPosition());
		delivery = pDelivery;
		this.wastedConcrete = pWastedConcrete;
		this.lagTime = pLagTime;
		
	}

	public Delivery getDelivery() {
		return delivery;
	}


	public int getWastedConcrete() {
		return wastedConcrete;
	}

	public Duration getLagTime() {
		return lagTime;
	}

	private String getSummary() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Delivery[");
		if (delivery != null) {
			sb.append("\n order=").append(((OrderAgentInitial)delivery.getOrder()).getOrder().getId());
			sb.append("\n deliveryNo=").append(delivery.getDeliveryNo());
		}
		sb.append("\n  truck=").append(getTruck().getId());
		sb.append("\n Unit start time=").append(getTimeSlot().getStartTime());
		sb.append("\n  loading time=").append(delivery.getDeliveryTime().minus(delivery.getLoadingDuration()).minus(delivery.getStationToCYTravelTime())).append(", from Station=" + delivery.getLoadingStation());
		//sb.append("\n  departs from station = ").append(((ProductionSite)delivery.getLoadingStation()).getStation().getId() + 
			//	" at time "+ delivery.getDeliveryTime().minus(delivery.getStationToCYTravelTime()));
		if (delivery != null){
			sb.append("\n  unloading time=").append(delivery.getDeliveryTime());
			sb.append("\n lagTime=").append(this.lagTime);
			sb.append("\n wastedConcrete=").append(this.wastedConcrete);
		}
		sb.append("\n finishes at CY at =").append(delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()));
		//sb.append("\n reaches at Station = ").append(((RmcProductionSite)delivery.getReturnStation()).getStation().getId() + 
			//	" at time "+delivery.getDeliveryTime().plus(delivery.getUnloadingDuration()).plus(delivery.getCYToStationTravelTime()));
	//	sb.append("\n Unit end time=").append(getTimeSlot().getEndTime()).append(" at station= " + delivery.getReturnStation());

		sb.append("]");
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return this.getSummary();
	}
}
