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
	private final Point startLocation;
	private final Point endLocation;
	private final Duration travelTime;

//	public TruckTravelUnit(Agent pTruck) {
//		super(pTruck);
//	}
	public TruckTravelUnit(Agent pTruck, TimeSlot pSlot, Point pStartLocation, Point pEndLocation, Duration pTravelTime) {
		super(pTruck, pSlot);
		startLocation = pStartLocation;
		endLocation = pEndLocation;
		travelTime = pTravelTime;
	}

	public Point getStartLocation() {
		return startLocation;
	}

	public Point getEndLocation() {
		return endLocation;
	}

	public Duration getTravelTime() {
		return travelTime;
	}

}
