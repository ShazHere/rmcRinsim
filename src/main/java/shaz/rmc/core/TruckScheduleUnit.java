/**
 * 
 */
package shaz.rmc.core;

import rinde.sim.core.graph.Point;
import shaz.rmc.core.TimeSlot;
//import shaz.rmc.pdpExtended.masDisco.RmcDelivery;
//import shaz.rmc.pdpExtended.masDisco.RmcOrderAgent;
//import shaz.rmc.pdpExtended.masDisco.RmcProductionSite;

/**
 * @author Shaza
 * Though I made it Immutable class, but due to some problem TruckScheduleUnit wasn't proving to be immutable to be used 
 * as a key for the <key,value> pair in a Map, in a TruckAgent.
 * 
 */
public abstract class TruckScheduleUnit {
	private final Point startLocation;
	private final Point endLocation;
	private final Agent truck;
	private final TimeSlot timeSlot; //It includes full slot, including loading time at station, then St to CY time then unloading time then CY to next ST time include travel time etc.

//	public TruckScheduleUnit(Agent pTruck) {
//		timeSlot = null;
//		truck = pTruck;
//	}
	public TruckScheduleUnit(Agent pTruck, TimeSlot pSlot, Point pStartLocation, Point pEndLocation) {
		truck = pTruck;
		timeSlot = pSlot;
		startLocation = pStartLocation;
		endLocation = pEndLocation;
	}

	public TimeSlot getTimeSlot() {
		return timeSlot;
	}
	public Agent getTruck() {
		return truck;
	}
	public Point getStartLocation() {
		return startLocation;
	}

	public Point getEndLocation() {
		return endLocation;
	}

	public String getSummary() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Delivery[");
		sb.append("\n  truck=").append(truck.getId());
		sb.append("\n Unit start time=").append(timeSlot.getStartTime());
		sb.append("\n Unit end time=").append(timeSlot.getEndTime());
		sb.append("]");
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return this.getSummary();
	}

}