/**
 * 
 */
package shaz.rmc.core;

import org.joda.time.Duration;

import rinde.sim.core.graph.Point;


/**
 * @author Shaza
 * @date 11/07/2013
 * immutable
 *
 */
public class TruckTravelUnit extends TruckScheduleUnit {

	private final Duration travelTime;

//	public TruckTravelUnit(Agent pTruck) {
//		super(pTruck);
//	}
	public TruckTravelUnit(Agent pTruck, TimeSlot pSlot, Point pStartLocation, Point pEndLocation, Duration pTravelTime) {
		super(pTruck, pSlot , pStartLocation, pEndLocation);

		travelTime = pTravelTime;
	}

	public Duration getTravelTime() {
		return travelTime;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Travel[");
		sb.append("\n  truck=").append(getTruck().getId());
		sb.append("\n Unit start time=").append(getTimeSlot().getStartTime());
		sb.append("\n Unit end time=").append(getTimeSlot().getEndTime());
		sb.append("\n Start Location=").append(getStartLocation());
		sb.append("\n End Location=").append(getEndLocation());
		sb.append("\n Travel Time=").append(travelTime);
		sb.append("]");
		
		return sb.toString();
	}
}
